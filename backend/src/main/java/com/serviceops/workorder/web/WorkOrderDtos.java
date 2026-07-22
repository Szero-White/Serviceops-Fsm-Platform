package com.serviceops.workorder.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.workorder.domain.WorkOrderStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WorkOrderDtos {
    private WorkOrderDtos() {
    }

    public record CreateWorkOrder(
            UUID serviceRequestId,
            @NotNull UUID customerId,
            UUID assetId,
            @NotBlank @Size(max = 200) String summary,
            @Size(max = 5000) String description,
            @NotNull Priority priority
    ) {
    }

    public record ScheduleWorkOrder(
            @NotNull UUID technicianId,
            @NotNull @Future Instant startTime,
            @NotNull @Future Instant endTime
    ) {
    }

    public record TransitionWorkOrder(
            @NotNull WorkOrderStatus targetStatus,
            @Size(max = 1000) String note,
            @Size(max = 5000) String diagnosis,
            @Size(max = 5000) String resolution
    ) {
    }

    public record WorkOrderHistoryResponse(
            UUID id,
            WorkOrderStatus fromStatus,
            WorkOrderStatus toStatus,
            String note,
            String changedBy,
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
            List<WorkOrderHistoryResponse> history
    ) {
    }
}
