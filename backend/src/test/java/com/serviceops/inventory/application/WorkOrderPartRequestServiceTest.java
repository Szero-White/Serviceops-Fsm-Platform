package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestCreateRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestReasonRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestUpdateRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.PART_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.REQUEST_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.TENANT_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.WORK_ORDER_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.authenticateTechnician;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.clearAuthentication;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.pendingRequest;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.sparePart;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.workOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartRequestServiceTest {
    @Mock private WorkOrderPartRequestRepository requestRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderPartRequestService service;

    @BeforeEach
    void setUp() {
        var workflowPolicy = new WorkOrderPartWorkflowPolicy(workOrderRepository, requestRepository);
        service = new WorkOrderPartRequestService(
                requestRepository,
                sparePartRepository,
                workflowPolicy,
                auditService,
                notificationService
        );
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    void technicianRequestDoesNotChangeStockAndNotifiesWarehouse() {
        authenticateTechnician();
        WorkOrder workOrder = workOrder(WorkOrderStatus.IN_PROGRESS);
        SparePart part = sparePart("10");
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(sparePartRepository.findByIdAndTenantId(PART_ID, TENANT_ID)).thenReturn(Optional.of(part));
        when(requestRepository.existsByTenantIdAndWorkOrderIdAndSparePartIdAndStatus(
                TENANT_ID, WORK_ORDER_ID, PART_ID, WorkOrderPartRequestStatus.REQUESTED)).thenReturn(false);

        var response = service.createRequest(
                WORK_ORDER_ID,
                new PartRequestCreateRequest(PART_ID, new BigDecimal("2"), "Thay van cấp nước bị lỗi")
        );

        assertThat(response.status()).isEqualTo(WorkOrderPartRequestStatus.REQUESTED);
        assertThat(response.requestedQuantity()).isEqualByComparingTo("2");
        assertThat(part.getStockQuantity()).isEqualByComparingTo("10");
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                eq("Có yêu cầu phụ tùng mới: WO-2026-001234"),
                any()
        );
    }

    @Test
    void technicianCanCorrectPendingQuantityDownwardBeforeIssue() {
        authenticateTechnician();
        WorkOrderPartRequest request = pendingRequest("2");
        stubLockedRequest(request);

        var response = service.updateRequest(
                REQUEST_ID,
                new PartRequestUpdateRequest(new BigDecimal("1"), "Nhập nhầm số lượng, thực tế chỉ cần một van")
        );

        assertThat(response.requestedQuantity()).isEqualByComparingTo("1");
        assertThat(request.getRequestedQuantity()).isEqualByComparingTo("1");
        assertThat(request.getRequestNote()).contains("thực tế chỉ cần một van");
    }

    @Test
    void cancellationKeepsUserEnteredReasonInsteadOfHardCodingIt() {
        authenticateTechnician();
        WorkOrderPartRequest request = pendingRequest("2");
        stubLockedRequest(request);
        String reason = "Kiểm tra lại thấy van hiện tại vẫn hoạt động bình thường";

        var response = service.cancelRequest(REQUEST_ID, new PartRequestReasonRequest(reason));

        assertThat(response.status()).isEqualTo(WorkOrderPartRequestStatus.CANCELLED);
        assertThat(response.resolutionReason()).isEqualTo(reason);
        assertThat(request.getResolvedByDisplayName()).isEqualTo("Trịnh Quốc Tiến");
    }

    @Test
    void terminalWorkOrderExpiresPendingRequestsWithoutDeletingHistory() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.COMPLETED);
        WorkOrderPartRequest request = pendingRequest("2");
        request.setWorkOrder(workOrder);
        when(requestRepository.findDetailedByWorkOrderAndStatus(
                TENANT_ID, WORK_ORDER_ID, WorkOrderPartRequestStatus.REQUESTED)).thenReturn(List.of(request));

        service.expirePendingRequests(workOrder);

        assertThat(request.getStatus()).isEqualTo(WorkOrderPartRequestStatus.EXPIRED);
        assertThat(request.getResolutionReason()).contains("COMPLETED");
        assertThat(request.getResolvedByDisplayName()).isEqualTo("Hệ thống");
        verify(requestRepository, never()).delete(any());
    }

    private void stubLockedRequest(WorkOrderPartRequest request) {
        when(requestRepository.findDetailed(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(request.getWorkOrder()));
        when(requestRepository.findForUpdate(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));
    }
}
