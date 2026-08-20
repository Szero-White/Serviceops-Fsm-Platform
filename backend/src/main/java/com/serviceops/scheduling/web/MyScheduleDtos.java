package com.serviceops.scheduling.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.workorder.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MyScheduleDtos {
    private MyScheduleDtos() {
    }

    public record MyScheduleResponse(
            UUID technicianId,
            String technicianName,
            Instant rangeStart,
            Instant rangeEnd,
            List<MyScheduleItemResponse> appointments
    ) {
    }

    public record MyScheduleItemResponse(
            UUID appointmentId,
            UUID workOrderId,
            String workOrderCode,
            String summary,
            String customerName,
            String customerAddress,
            String assetLabel,
            Priority priority,
            WorkOrderStatus status,
            Instant startTime,
            Instant endTime
    ) {
    }
}
