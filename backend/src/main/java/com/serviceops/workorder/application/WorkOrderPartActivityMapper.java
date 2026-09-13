package com.serviceops.workorder.application;

import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.util.List;

final class WorkOrderPartActivityMapper {
    private WorkOrderPartActivityMapper() {
    }

    static boolean isLegacyVisibleTransaction(InventoryTransaction transaction) {
        return transaction.getTransactionType() == InventoryTransactionType.CONSUME
                && "TECHNICIAN".equals(transaction.getActorRole());
    }

    static boolean isTimelineTransaction(InventoryTransaction transaction) {
        return transaction.getTransactionType() == InventoryTransactionType.ISSUE
                || transaction.getTransactionType() == InventoryTransactionType.RETURN
                || isLegacyVisibleTransaction(transaction);
    }

    static void addRequestActivities(List<WorkOrderActivityResponse> activities, WorkOrderPartRequest request) {
        activities.add(WorkOrderActivityFactory.activity(
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
        activities.add(WorkOrderActivityFactory.activity(
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

    static WorkOrderActivityResponse fromTransaction(InventoryTransaction transaction) {
        var part = transaction.getSparePart();
        WorkOrderActivityType type = switch (transaction.getTransactionType()) {
            case ISSUE -> WorkOrderActivityType.PART_ISSUED;
            case RETURN -> WorkOrderActivityType.PART_RETURNED;
            case CONSUME -> WorkOrderActivityType.PART_CONSUMED;
            default -> throw new IllegalArgumentException(
                    "Unsupported timeline inventory transaction " + transaction.getTransactionType());
        };
        return WorkOrderActivityFactory.activity(
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

    static WorkOrderActivityResponse fromUsage(WorkOrderPartUsage usage) {
        var part = usage.getSparePart();
        return WorkOrderActivityFactory.activity(
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
}
