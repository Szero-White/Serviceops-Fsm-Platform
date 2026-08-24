package com.serviceops.workorder.application;

import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Read-model mapper for the Work Order activity timeline.
 *
 * <p>Status history and inventory transactions remain the source of truth in
 * their own modules. This mapper only combines them for presentation and does
 * not persist duplicate timeline rows.</p>
 */
final class WorkOrderActivityMapper {
    private WorkOrderActivityMapper() {
    }

    static List<WorkOrderActivityResponse> merge(
            List<WorkOrderStatusHistory> statusHistory,
            List<InventoryTransaction> partTransactions
    ) {
        Stream<WorkOrderActivityResponse> statuses = statusHistory.stream()
                .map(WorkOrderActivityMapper::fromStatusHistory);
        Stream<WorkOrderActivityResponse> parts = partTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == InventoryTransactionType.CONSUME
                        || transaction.getTransactionType() == InventoryTransactionType.RETURN)
                .map(WorkOrderActivityMapper::fromPartTransaction);

        return Stream.concat(statuses, parts)
                .sorted(Comparator.comparing(WorkOrderActivityResponse::createdAt))
                .toList();
    }

    private static WorkOrderActivityResponse fromStatusHistory(WorkOrderStatusHistory history) {
        return new WorkOrderActivityResponse(
                history.getId(),
                WorkOrderActivityType.STATUS_CHANGE,
                history.getToStatus(),
                history.getNote(),
                history.getChangedBy(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                history.getCreatedAt()
        );
    }

    private static WorkOrderActivityResponse fromPartTransaction(InventoryTransaction transaction) {
        var part = transaction.getSparePart();
        WorkOrderActivityType type = transaction.getTransactionType() == InventoryTransactionType.CONSUME
                ? WorkOrderActivityType.PART_CONSUMED
                : WorkOrderActivityType.PART_RETURNED;

        return new WorkOrderActivityResponse(
                transaction.getId(),
                type,
                null,
                transaction.getNote(),
                transaction.getCreatedBy(),
                transaction.getActorDisplayName(),
                transaction.getActorRole(),
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                transaction.getQuantity(),
                transaction.getCreatedAt()
        );
    }
}
