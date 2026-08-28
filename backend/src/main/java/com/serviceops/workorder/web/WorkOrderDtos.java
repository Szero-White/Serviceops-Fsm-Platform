package com.serviceops.workorder.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.workorder.domain.WorkOrderStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WorkOrderDtos {
    private WorkOrderDtos() {
    }

    public record ScheduleWorkOrder(
            @NotNull UUID technicianId,
            @NotNull @Future Instant startTime,
            @NotNull @Future Instant endTime,
            @Size(max = 500) String reason
    ) {
    }

    public record TransitionWorkOrder(
            @NotNull WorkOrderStatus targetStatus,
            @Size(max = 1000) String note,
            @Size(max = 5000) String diagnosis,
            @Size(max = 5000) String resolution
    ) {
    }

    public enum WorkOrderActivityType {
        STATUS_CHANGE,
        DISPATCH_UPDATED,
        PART_REQUESTED,
        PART_REQUEST_CANCELLED,
        PART_UNAVAILABLE,
        PART_REQUEST_EXPIRED,
        PART_ISSUED,
        PART_USED,
        PART_CONSUMED,
        PART_RETURNED,
        PAYMENT_REPORTED,
        PAYMENT_SETTLED,
        RECEIPT_ISSUED
    }

    public record WorkOrderActivityResponse(
            String id,
            WorkOrderActivityType type,
            WorkOrderStatus status,
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
    }

    public record WorkOrderHistoryResponse(
            UUID id,
            WorkOrderStatus fromStatus,
            WorkOrderStatus toStatus,
            String note,
            String changedBy,
            String actorDisplayName,
            String actorRole,
            String diagnosis,
            String resolution,
            Instant createdAt
    ) {
    }

    public record WorkOrderResponse(
            UUID id,
            String code,
            UUID serviceRequestId,
            UUID customerId,
            String customerName,
            UUID assetId,
            String assetLabel,
            UUID technicianId,
            String technicianName,
            String summary,
            String description,
            Priority priority,
            WorkOrderStatus status,
            Instant scheduledStart,
            Instant scheduledEnd,
            String diagnosis,
            String resolution,
            Instant completedAt,
            Instant createdAt,
            List<WorkOrderHistoryResponse> history,
            List<WorkOrderActivityResponse> activities
    ) {
    }
}
