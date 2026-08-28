package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestReasonRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.PART_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.REQUEST_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.TECHNICIAN_USER_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.TENANT_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.WORK_ORDER_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.authenticateWarehouse;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.clearAuthentication;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.pendingRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartFulfillmentServiceTest {
    @Mock private WorkOrderPartRequestRepository requestRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderPartFulfillmentService service;

    @BeforeEach
    void setUp() {
        var workflowPolicy = new WorkOrderPartWorkflowPolicy(workOrderRepository, requestRepository);
        var stockService = new WorkOrderPartStockService(transactionRepository, usageRepository, notificationService);
        service = new WorkOrderPartFulfillmentService(
                sparePartRepository,
                workflowPolicy,
                stockService,
                auditService
        );
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    void warehouseIssueDecrementsStockExactlyOnceAndSnapshotsReceiver() {
        authenticateWarehouse();
        WorkOrderPartRequest request = pendingRequest("2");
        SparePart part = request.getSparePart();
        part.setStockQuantity(new BigDecimal("5"));
        stubLockedRequest(request);
        when(sparePartRepository.findForUpdate(PART_ID, TENANT_ID)).thenReturn(Optional.of(part));

        var response = service.issue(REQUEST_ID);

        assertThat(part.getStockQuantity()).isEqualByComparingTo("3");
        assertThat(response.status()).isEqualTo(WorkOrderPartRequestStatus.ISSUED);
        assertThat(response.issuedQuantity()).isEqualByComparingTo("2");
        assertThat(response.issuedByDisplayName()).isEqualTo("Nguyễn Nhân viên Kho");
        assertThat(response.receivedByUserId()).isEqualTo(TECHNICIAN_USER_ID);
        assertThat(response.receivedByDisplayName()).isEqualTo("Trịnh Quốc Tiến");

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.ISSUE);
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo("3");
    }

    @Test
    void warehouseUnavailableKeepsUserEnteredReason() {
        authenticateWarehouse();
        WorkOrderPartRequest request = pendingRequest("2");
        stubLockedRequest(request);
        String reason = "Kho hiện chỉ còn hàng lỗi, chưa đủ điều kiện để cấp";

        var response = service.markUnavailable(REQUEST_ID, new PartRequestReasonRequest(reason));

        assertThat(response.status()).isEqualTo(WorkOrderPartRequestStatus.UNAVAILABLE);
        assertThat(response.resolutionReason()).isEqualTo(reason);
        assertThat(response.resolvedByDisplayName()).isEqualTo("Nguyễn Nhân viên Kho");
    }

    @Test
    void resolvedRequestCannotBeIssuedTwice() {
        authenticateWarehouse();
        WorkOrderPartRequest request = pendingRequest("2");
        request.setStatus(WorkOrderPartRequestStatus.ISSUED);
        when(requestRepository.findDetailed(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(request.getWorkOrder()));
        when(requestRepository.findForUpdate(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.issue(REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("PART_REQUEST_ALREADY_RESOLVED"));
    }

    private void stubLockedRequest(WorkOrderPartRequest request) {
        when(requestRepository.findDetailed(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(request.getWorkOrder()));
        when(requestRepository.findForUpdate(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(request));
    }
}
