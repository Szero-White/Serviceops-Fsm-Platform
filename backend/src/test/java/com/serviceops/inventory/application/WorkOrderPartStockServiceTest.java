package com.serviceops.inventory.application;

import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartStockServiceTest {
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private NotificationService notificationService;

    @AfterEach
    void tearDown() {
        WorkOrderPartTestFixtures.clearAuthentication();
    }

    @Test
    void issueSnapshotsAssignedTechnicianAsInventoryRecipient() {
        WorkOrderPartTestFixtures.authenticateWarehouse();
        WorkOrder workOrder = WorkOrderPartTestFixtures.workOrder(WorkOrderStatus.IN_PROGRESS);
        SparePart part = WorkOrderPartTestFixtures.sparePart("10");

        WorkOrderPartStockService service = new WorkOrderPartStockService(
                transactionRepository,
                usageRepository,
                notificationService
        );

        service.issue(part, workOrder, new BigDecimal("1"), "Issue recipient snapshot test");

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        InventoryTransaction transaction = captor.getValue();

        assertThat(transaction.getTransactionType()).isEqualTo(InventoryTransactionType.ISSUE);
        assertThat(transaction.getRecipientUserId()).isEqualTo(WorkOrderPartTestFixtures.TECHNICIAN_USER_ID);
        assertThat(transaction.getRecipientDisplayName()).isEqualTo("Trịnh Quốc Tiến");
        assertThat(transaction.getActorDisplayName()).isEqualTo("Nguyễn Nhân viên Kho");
        assertThat(transaction.getActorRole()).isEqualTo("WAREHOUSE_STAFF");
    }

    @Test
    void bulkTotalsAggregateIssueAndReturnForAllRequestedWorkOrders() {
        UUID tenantId = UUID.randomUUID();
        WorkOrder firstWorkOrder = workOrder();
        WorkOrder secondWorkOrder = workOrder();
        SparePart firstPart = sparePart();
        SparePart secondPart = sparePart();

        List<InventoryTransaction> transactions = List.of(
                transaction(firstWorkOrder, firstPart, InventoryTransactionType.RETURN, "1", "2026-08-27T00:59:00Z"),
                transaction(firstWorkOrder, firstPart, InventoryTransactionType.ISSUE, "5", "2026-08-27T01:00:00Z"),
                transaction(firstWorkOrder, firstPart, InventoryTransactionType.RETURN, "2", "2026-08-27T02:00:00Z"),
                transaction(secondWorkOrder, secondPart, InventoryTransactionType.ISSUE, "3", "2026-08-27T03:00:00Z")
        );
        Set<UUID> workOrderIds = Set.of(firstWorkOrder.getId(), secondWorkOrder.getId());
        when(transactionRepository.findWorkflowPartTransactionsForWorkOrders(tenantId, workOrderIds))
                .thenReturn(transactions);

        WorkOrderPartStockService service = new WorkOrderPartStockService(
                transactionRepository,
                usageRepository,
                notificationService
        );

        var totals = service.totalsForWorkOrders(tenantId, workOrderIds);

        assertThat(totals.get(new WorkOrderPartStockService.WorkOrderPartKey(
                firstWorkOrder.getId(), firstPart.getId()
        )).issued()).isEqualByComparingTo("5");
        assertThat(totals.get(new WorkOrderPartStockService.WorkOrderPartKey(
                firstWorkOrder.getId(), firstPart.getId()
        )).returned()).isEqualByComparingTo("2");
        assertThat(totals.get(new WorkOrderPartStockService.WorkOrderPartKey(
                secondWorkOrder.getId(), secondPart.getId()
        )).issued()).isEqualByComparingTo("3");
    }

    @Test
    void bulkTotalsAvoidRepositoryCallWhenNoWorkOrdersAreRequested() {
        WorkOrderPartStockService service = new WorkOrderPartStockService(
                transactionRepository,
                usageRepository,
                notificationService
        );

        assertThat(service.totalsForWorkOrders(UUID.randomUUID(), Set.of())).isEmpty();
    }

    private static WorkOrder workOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        return workOrder;
    }

    private static SparePart sparePart() {
        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        return part;
    }

    private static InventoryTransaction transaction(
            WorkOrder workOrder,
            SparePart part,
            InventoryTransactionType type,
            String quantity,
            String createdAt
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setWorkOrder(workOrder);
        transaction.setSparePart(part);
        transaction.setTransactionType(type);
        transaction.setQuantity(new BigDecimal(quantity));
        transaction.setCreatedAt(Instant.parse(createdAt));
        return transaction;
    }
}
