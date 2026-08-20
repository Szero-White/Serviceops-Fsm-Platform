package com.serviceops.scheduling.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.workorder.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ScheduleBoardDtos {
    private ScheduleBoardDtos() {
    }

    public record ScheduleBoardResponse(
            Instant rangeStart,
            Instant rangeEnd,
            List<ScheduleAppointmentResponse> appointments,
            List<DispatchQueueItemResponse> dispatchQueue,
            long dispatchQueueTotal
    ) {
    }

    public record ScheduleAppointmentResponse(
            UUID appointmentId,
            UUID workOrderId,
            String workOrderCode,
            String summary,
            String customerName,
            Priority priority,
            WorkOrderStatus status,
            UUID technicianId,
            String technicianName,
            Instant startTime,
            Instant endTime
    ) {
    }

    public record DispatchQueueItemResponse(
            UUID workOrderId,
            String workOrderCode,
            String summary,
            String customerName,
            Priority priority,
            WorkOrderStatus status,
            Instant createdAt
    ) {
    }
}
