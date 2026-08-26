package com.serviceops.workorder.application;

import com.serviceops.audit.domain.AuditLog;
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
 * <p>Status history, dispatch audit events and inventory transactions remain
 * the source of truth in their own modules. The operational Work Order timeline
 * intentionally shows only technician CONSUME transactions; warehouse RETURN
 * remains in the inventory ledger and invoice net calculation so dispatch users
 * are not shown warehouse bookkeeping as field progress. This mapper only
 * combines read models for presentation and does not persist duplicate rows.</p>
 */
final class WorkOrderActivityMapper {
    private WorkOrderActivityMapper() {
    }

    static List<WorkOrderActivityResponse> merge(
            List<WorkOrderStatusHistory> statusHistory,
            List<InventoryTransaction> partTransactions
    ) {
        return merge(statusHistory, partTransactions, List.of());
    }

    static List<WorkOrderActivityResponse> merge(
            List<WorkOrderStatusHistory> statusHistory,
            List<InventoryTransaction> partTransactions,
            List<AuditLog> dispatchEvents
    ) {
        Stream<WorkOrderActivityResponse> statuses = statusHistory.stream()
                .map(WorkOrderActivityMapper::fromStatusHistory);
        Stream<WorkOrderActivityResponse> parts = partTransactions.stream()
                .filter(WorkOrderActivityMapper::isTechnicianConsumption)
                .map(WorkOrderActivityMapper::fromPartTransaction);
        Stream<WorkOrderActivityResponse> dispatch = dispatchEvents.stream()
                .filter(event -> "RESCHEDULE".equals(event.getAction()))
                .map(WorkOrderActivityMapper::fromDispatchAudit);

        return Stream.of(statuses, dispatch, parts)
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(WorkOrderActivityResponse::createdAt))
                .toList();
    }

    private static boolean isTechnicianConsumption(InventoryTransaction transaction) {
        return transaction.getTransactionType() == InventoryTransactionType.CONSUME
                && "TECHNICIAN".equals(transaction.getActorRole());
    }

    private static WorkOrderActivityResponse fromStatusHistory(WorkOrderStatusHistory history) {
        return new WorkOrderActivityResponse(
                history.getId(),
                WorkOrderActivityType.STATUS_CHANGE,
                history.getToStatus(),
                history.getNote(),
                history.getChangedBy(),
                history.getActorDisplayName(),
                history.getActorRole(),
                history.getDiagnosisSnapshot(),
                history.getResolutionSnapshot(),
                null,
                null,
                null,
                null,
                null,
                history.getCreatedAt()
        );
    }

    private static WorkOrderActivityResponse fromDispatchAudit(AuditLog audit) {
        return new WorkOrderActivityResponse(
                audit.getId(),
                WorkOrderActivityType.DISPATCH_UPDATED,
                null,
                audit.getDetails(),
                audit.getActorUsername(),
                audit.getActorDisplayName(),
                audit.getActorRole(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                audit.getCreatedAt()
        );
    }

    private static WorkOrderActivityResponse fromPartTransaction(InventoryTransaction transaction) {
        var part = transaction.getSparePart();
        return new WorkOrderActivityResponse(
                transaction.getId(),
                WorkOrderActivityType.PART_CONSUMED,
                null,
                transaction.getNote(),
                transaction.getCreatedBy(),
                transaction.getActorDisplayName(),
                transaction.getActorRole(),
                null,
                null,
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                transaction.getQuantity(),
                transaction.getCreatedAt()
        );
    }
}
