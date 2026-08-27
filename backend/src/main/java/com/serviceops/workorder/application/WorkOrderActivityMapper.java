package com.serviceops.workorder.application;

import com.serviceops.audit.domain.AuditLog;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Builds the Work Order business timeline without duplicating source-of-truth rows.
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
        return Stream.of(
                        statusHistory.stream().map(WorkOrderActivityMapper::fromStatusHistory),
                        dispatchEvents.stream().filter(event -> "RESCHEDULE".equals(event.getAction())).map(WorkOrderActivityMapper::fromDispatchAudit),
                        partTransactions.stream().filter(WorkOrderActivityMapper::isLegacyVisibleTransaction).map(WorkOrderActivityMapper::fromPartTransaction)
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
        partRequests.forEach(request -> addRequestActivities(activities, request));
        partTransactions.stream()
                .filter(WorkOrderActivityMapper::isTimelineTransaction)
                .map(WorkOrderActivityMapper::fromPartTransaction)
                .forEach(activities::add);
        partUsages.stream()
                .filter(usage -> usage.getUsedQuantity() != null && usage.getUsedQuantity().signum() > 0)
                .map(WorkOrderActivityMapper::fromPartUsage)
                .forEach(activities::add);
        addPaymentActivities(activities, payment);
        if (receipt != null) {
            activities.add(fromReceipt(receipt));
        }
        return activities.stream()
                .filter(activity -> activity.createdAt() != null)
                .sorted(Comparator.comparing(WorkOrderActivityResponse::createdAt))
                .toList();
    }

    private static boolean isLegacyVisibleTransaction(InventoryTransaction transaction) {
        return transaction.getTransactionType() == InventoryTransactionType.CONSUME
                && "TECHNICIAN".equals(transaction.getActorRole());
    }

    private static boolean isTimelineTransaction(InventoryTransaction transaction) {
        return transaction.getTransactionType() == InventoryTransactionType.ISSUE
                || transaction.getTransactionType() == InventoryTransactionType.RETURN
                || isLegacyVisibleTransaction(transaction);
    }

    private static WorkOrderActivityResponse fromStatusHistory(WorkOrderStatusHistory history) {
        return activity(
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
        return activity(
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

    private static void addRequestActivities(List<WorkOrderActivityResponse> activities, WorkOrderPartRequest request) {
        activities.add(activity(
                "request:" + request.getId() + ":requested",
                WorkOrderActivityType.PART_REQUESTED,
                null,
                request.getRequestNote(),
                request.getRequestedByUsername(),
                request.getRequestedByDisplayName(),
                "TECHNICIAN",
                null,
                null,
                request.getSparePart().getId(),
                request.getSparePart().getSku(),
                request.getSparePart().getName(),
                request.getSparePart().getUnit(),
                request.getRequestedQuantity(),
                null,
                null,
                null,
                request.getCreatedAt()
        ));

        WorkOrderActivityType terminalType = switch (request.getStatus()) {
            case CANCELLED -> WorkOrderActivityType.PART_REQUEST_CANCELLED;
            case UNAVAILABLE -> WorkOrderActivityType.PART_UNAVAILABLE;
            case EXPIRED -> WorkOrderActivityType.PART_REQUEST_EXPIRED;
            default -> null;
        };
        if (terminalType == null || request.getResolvedAt() == null) {
            return;
        }
        String actorRole = switch (request.getStatus()) {
            case CANCELLED -> "TECHNICIAN";
            case UNAVAILABLE -> "WAREHOUSE_STAFF";
            case EXPIRED -> "SYSTEM";
            default -> null;
        };
        activities.add(activity(
                "request:" + request.getId() + ":resolved",
                terminalType,
                null,
                request.getResolutionReason(),
                request.getResolvedByUsername(),
                request.getResolvedByDisplayName(),
                actorRole,
                null,
                null,
                request.getSparePart().getId(),
                request.getSparePart().getSku(),
                request.getSparePart().getName(),
                request.getSparePart().getUnit(),
                request.getRequestedQuantity(),
                null,
                null,
                null,
                request.getResolvedAt()
        ));
    }

    private static WorkOrderActivityResponse fromPartTransaction(InventoryTransaction transaction) {
        var part = transaction.getSparePart();
        WorkOrderActivityType type = switch (transaction.getTransactionType()) {
            case ISSUE -> WorkOrderActivityType.PART_ISSUED;
            case RETURN -> WorkOrderActivityType.PART_RETURNED;
            case CONSUME -> WorkOrderActivityType.PART_CONSUMED;
            default -> throw new IllegalArgumentException("Unsupported timeline inventory transaction " + transaction.getTransactionType());
        };
        return activity(
                "inventory:" + transaction.getId(),
                type,
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
                null,
                null,
                null,
                transaction.getCreatedAt()
        );
    }

    private static WorkOrderActivityResponse fromPartUsage(WorkOrderPartUsage usage) {
        var part = usage.getSparePart();
        return activity(
                "usage:" + usage.getId(),
                WorkOrderActivityType.PART_USED,
                null,
                null,
                usage.getUpdatedByUsername(),
                usage.getUpdatedByDisplayName(),
                "TECHNICIAN",
                null,
                null,
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                usage.getUsedQuantity(),
                null,
                null,
                null,
                usage.getUpdatedAt()
        );
    }

    private static void addPaymentActivities(List<WorkOrderActivityResponse> activities, Payment payment) {
        if (payment == null) {
            return;
        }
        if (payment.getTransferReportedAt() != null) {
            String username = null;
            String displayName = null;
            if (payment.getWorkOrder().getTechnician() != null && payment.getWorkOrder().getTechnician().getUser() != null) {
                username = payment.getWorkOrder().getTechnician().getUser().getUsername();
                displayName = payment.getWorkOrder().getTechnician().getUser().getDisplayName();
            }
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":reported",
                    WorkOrderActivityType.PAYMENT_REPORTED,
                    "Khách báo đã chuyển khoản",
                    username,
                    displayName,
                    "TECHNICIAN",
                    payment,
                    payment.getTransferReportedAt()
            ));
        } else if (payment.getCashCollectedAt() != null) {
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":reported",
                    WorkOrderActivityType.PAYMENT_REPORTED,
                    "Đã nhận tiền mặt từ khách; chờ bàn giao về công ty",
                    payment.getCollectedByUsername(),
                    payment.getCollectedByDisplayName(),
                    "TECHNICIAN",
                    payment,
                    payment.getCashCollectedAt()
            ));
        }
        if (payment.getSettledAt() != null) {
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":settled",
                    WorkOrderActivityType.PAYMENT_SETTLED,
                    "CSKH đã đối soát tiền về công ty",
                    payment.getSettledByUsername(),
                    payment.getSettledByDisplayName(),
                    "CUSTOMER_SERVICE",
                    payment,
                    payment.getSettledAt()
            ));
        }
    }

    private static WorkOrderActivityResponse paymentActivity(
            String id,
            WorkOrderActivityType type,
            String note,
            String actor,
            String actorDisplayName,
            String actorRole,
            Payment payment,
            Instant createdAt
    ) {
        return activity(
                id,
                type,
                null,
                note,
                actor,
                actorDisplayName,
                actorRole,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                payment.getAmount(),
                paymentMethod(payment.getMethod()),
                null,
                createdAt
        );
    }

    private static WorkOrderActivityResponse fromReceipt(PaymentReceipt receipt) {
        return activity(
                "receipt:" + receipt.getId(),
                WorkOrderActivityType.RECEIPT_ISSUED,
                null,
                "Biên nhận thanh toán dịch vụ đã được phát hành",
                receipt.getIssuedByUsername(),
                receipt.getIssuedByDisplayName(),
                "CUSTOMER_SERVICE",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                receipt.getAmount(),
                paymentMethod(receipt.getPaymentMethod()),
                receipt.getReceiptCode(),
                receipt.getIssuedAt()
        );
    }

    private static String paymentMethod(PaymentMethod method) {
        return method == null ? null : method.name();
    }

    private static WorkOrderActivityResponse activity(
            String id,
            WorkOrderActivityType type,
            com.serviceops.workorder.domain.WorkOrderStatus status,
            String note,
            String actor,
            String actorDisplayName,
            String actorRole,
            String diagnosis,
            String resolution,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal quantity,
            BigDecimal amount,
            String paymentMethod,
            String referenceCode,
            Instant createdAt
    ) {
        return new WorkOrderActivityResponse(
                id,
                type,
                status,
                note,
                actor,
                actorDisplayName,
                actorRole,
                diagnosis,
                resolution,
                sparePartId,
                sparePartSku,
                sparePartName,
                unit,
                quantity,
                amount,
                paymentMethod,
                referenceCode,
                createdAt
        );
    }
}
