package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnPartRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.PART_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.TENANT_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.WORK_ORDER_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.authenticateOwner;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.authenticateWarehouse;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.clearAuthentication;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.sparePart;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.transaction;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.usage;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.workOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartReturnServiceTest {
    @Mock private WorkOrderPartRequestRepository requestRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderPartReturnService service;

    @BeforeEach
    void setUp() {
        var workflowPolicy = new WorkOrderPartWorkflowPolicy(workOrderRepository, requestRepository);
        var stockService = new WorkOrderPartStockService(transactionRepository, usageRepository, notificationService);
        service = new WorkOrderPartReturnService(
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
    void ownerCannotConfirmPartReturn() {
        authenticateOwner();

        assertThatThrownBy(() -> service.returnPart(
                WORK_ORDER_ID,
                PART_ID,
                new ReturnPartRequest(BigDecimal.ONE, "Kỹ thuật viên trả lại phụ tùng chưa sử dụng")
        )).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo("PART_WORKFLOW_FORBIDDEN");
            assertThat(ex.getStatus().value()).isEqualTo(403);
        });
    }

    @Test
    void warehouseCanReturnOutstandingPartAfterWorkOrderIsClosedWithoutReopeningIt() {
        authenticateWarehouse();
        WorkOrder workOrder = workOrder(WorkOrderStatus.CLOSED);
        SparePart part = sparePart("7");
        InventoryTransaction issue = transaction(part, InventoryTransactionType.ISSUE, "3", "2026-08-26T03:00:00Z");
        WorkOrderPartUsage usage = usage(workOrder, part, "2");
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(sparePartRepository.findForUpdate(PART_ID, TENANT_ID)).thenReturn(Optional.of(part));
        when(transactionRepository.findWorkflowPartTransactionsForWorkOrderAndSparePart(TENANT_ID, WORK_ORDER_ID, PART_ID))
                .thenReturn(List.of(issue));
        when(usageRepository.findByTenantIdAndWorkOrderIdAndSparePartId(TENANT_ID, WORK_ORDER_ID, PART_ID))
                .thenReturn(Optional.of(usage));

        var response = service.returnPart(
                WORK_ORDER_ID,
                PART_ID,
                new ReturnPartRequest(BigDecimal.ONE, "Kỹ thuật viên trả lại phụ tùng chưa sử dụng")
        );

        assertThat(response.returnableQuantity()).isZero();
        assertThat(part.getStockQuantity()).isEqualByComparingTo("8");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CLOSED);
        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.RETURN);
    }


}
