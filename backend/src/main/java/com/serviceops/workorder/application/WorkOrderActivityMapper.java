package com.serviceops.workorder.application;

import com.serviceops.audit.domain.AuditLog;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Builds the Work Order business timeline without duplicating source-of-truth rows. */
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
        return Stream.of(
                        statusHistory.stream().map(WorkOrderActivityMapper::fromStatusHistory),
                        dispatchEvents.stream()
                                .filter(event -> "RESCHEDULE".equals(event.getAction()))
                                .map(WorkOrderActivityMapper::fromDispatchAudit),
                        partTransactions.stream()
                                .filter(WorkOrderPartActivityMapper::isLegacyVisibleTransaction)
                                .map(WorkOrderPartActivityMapper::fromTransaction)
                )
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(WorkOrderActivityResponse::createdAt))
                .toList();
    }

    static List<WorkOrderActivityResponse> mergeComplete(
            List<WorkOrderStatusHistory> statusHistory,
            List<InventoryTransaction> partTransactions,
            List<AuditLog> dispatchEvents,
            List<WorkOrderPartRequest> partRequests,
            List<WorkOrderPartUsage> partUsages,
            Payment payment,
            PaymentReceipt receipt
    ) {
        List<WorkOrderActivityResponse> activities = new ArrayList<>();
        statusHistory.forEach(history -> activities.add(fromStatusHistory(history)));
        dispatchEvents.stream()
                .filter(event -> "RESCHEDULE".equals(event.getAction()))
                .map(WorkOrderActivityMapper::fromDispatchAudit)
                .forEach(activities::add);
        partRequests.forEach(request -> WorkOrderPartActivityMapper.addRequestActivities(activities, request));
        partTransactions.stream()
                .filter(WorkOrderPartActivityMapper::isTimelineTransaction)
                .map(WorkOrderPartActivityMapper::fromTransaction)
                .forEach(activities::add);
        partUsages.stream()
                .filter(usage -> usage.getUsedQuantity() != null && usage.getUsedQuantity().signum() > 0)
                .map(WorkOrderPartActivityMapper::fromUsage)
                .forEach(activities::add);
        WorkOrderPaymentActivityMapper.addPaymentActivities(activities, payment);
        if (receipt != null) {
            activities.add(WorkOrderPaymentActivityMapper.fromReceipt(receipt));
        }
        return activities.stream()
                .filter(activity -> activity.createdAt() != null)
                .sorted(Comparator.comparing(WorkOrderActivityResponse::createdAt))
                .toList();
    }

    private static WorkOrderActivityResponse fromStatusHistory(WorkOrderStatusHistory history) {
        return WorkOrderActivityFactory.activity(
                "status:" + history.getId(),
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
                null,
                null,
                null,
                history.getCreatedAt()
        );
    }

    private static WorkOrderActivityResponse fromDispatchAudit(AuditLog audit) {
        return WorkOrderActivityFactory.activity(
                "dispatch:" + audit.getId(),
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
                null,
                null,
                null,
                audit.getCreatedAt()
        );
    }
}
