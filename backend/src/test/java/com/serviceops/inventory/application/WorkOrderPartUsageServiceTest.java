package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageUpdateRequest;
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
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.TENANT_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.WORK_ORDER_ID;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.authenticateTechnician;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.clearAuthentication;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.sparePart;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.transaction;
import static com.serviceops.inventory.application.WorkOrderPartTestFixtures.workOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartUsageServiceTest {
    @Mock private WorkOrderPartRequestRepository requestRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderPartUsageService service;

    @BeforeEach
    void setUp() {
        var workflowPolicy = new WorkOrderPartWorkflowPolicy(workOrderRepository, requestRepository);
        var stockService = new WorkOrderPartStockService(transactionRepository, usageRepository, notificationService);
        service = new WorkOrderPartUsageService(
                usageRepository,
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
    void technicianCanUpdateActualUsageAfterCompletionBeforeCustomerAcceptance() {
        authenticateTechnician();
        WorkOrder workOrder = workOrder(WorkOrderStatus.COMPLETED);
        SparePart part = sparePart("7");
        InventoryTransaction issue = transaction(part, InventoryTransactionType.ISSUE, "3", "2026-08-26T03:00:00Z");
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(sparePartRepository.findByIdAndTenantId(PART_ID, TENANT_ID)).thenReturn(Optional.of(part));
        when(transactionRepository.findWorkflowPartTransactionsForWorkOrderAndSparePart(TENANT_ID, WORK_ORDER_ID, PART_ID))
                .thenReturn(List.of(issue));
        when(usageRepository.findForUpdate(TENANT_ID, WORK_ORDER_ID, PART_ID)).thenReturn(Optional.empty());

        var response = service.updateUsage(
                WORK_ORDER_ID,
                new PartUsageUpdateRequest(PART_ID, new BigDecimal("2"))
        );

        assertThat(response.usedQuantity()).isEqualByComparingTo("2");
        verify(usageRepository).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void technicianActualUsageDoesNotMoveStockAndCannotExceedIssuedMinusReturned() {
        authenticateTechnician();
        WorkOrder workOrder = workOrder(WorkOrderStatus.IN_PROGRESS);
        SparePart part = sparePart("7");
        InventoryTransaction issue = transaction(part, InventoryTransactionType.ISSUE, "3", "2026-08-26T03:00:00Z");
        InventoryTransaction returned = transaction(part, InventoryTransactionType.RETURN, "1", "2026-08-26T04:00:00Z");
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(sparePartRepository.findByIdAndTenantId(PART_ID, TENANT_ID)).thenReturn(Optional.of(part));
        when(transactionRepository.findWorkflowPartTransactionsForWorkOrderAndSparePart(TENANT_ID, WORK_ORDER_ID, PART_ID))
                .thenReturn(List.of(issue, returned));
        when(usageRepository.findForUpdate(TENANT_ID, WORK_ORDER_ID, PART_ID)).thenReturn(Optional.empty());

        var response = service.updateUsage(
                WORK_ORDER_ID,
                new PartUsageUpdateRequest(PART_ID, new BigDecimal("2"))
        );

        assertThat(response.issuedQuantity()).isEqualByComparingTo("3");
        assertThat(response.usedQuantity()).isEqualByComparingTo("2");
        assertThat(response.returnedQuantity()).isEqualByComparingTo("1");
        assertThat(response.outstandingQuantity()).isZero();
        assertThat(part.getStockQuantity()).isEqualByComparingTo("7");
        verify(transactionRepository, never()).save(any());

        assertThatThrownBy(() -> service.updateUsage(
                WORK_ORDER_ID,
                new PartUsageUpdateRequest(PART_ID, new BigDecimal("2.001"))
        )).isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getCode()).isEqualTo("PART_USAGE_EXCEEDS_AVAILABLE"));
    }
}
