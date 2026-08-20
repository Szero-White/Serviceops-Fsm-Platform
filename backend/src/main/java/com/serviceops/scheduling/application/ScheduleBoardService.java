package com.serviceops.scheduling.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.scheduling.web.ScheduleBoardDtos.DispatchQueueItemResponse;
import com.serviceops.scheduling.web.ScheduleBoardDtos.ScheduleAppointmentResponse;
import com.serviceops.scheduling.web.ScheduleBoardDtos.ScheduleBoardResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleBoardService {
    private static final Duration MAX_RANGE = Duration.ofDays(31);
    private static final int DISPATCH_QUEUE_LIMIT = 100;

    private final AppointmentRepository appointmentRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional(readOnly = true)
    public ScheduleBoardResponse getBoard(Instant rangeStart, Instant rangeEnd) {
        validateRange(rangeStart, rangeEnd);
        UUID tenantId = CurrentUser.tenantId();

        List<ScheduleAppointmentResponse> appointments = appointmentRepository
                .findBoardRange(tenantId, rangeStart, rangeEnd, AppointmentStatus.ACTIVE)
                .stream()
                .map(ScheduleBoardService::toAppointmentResponse)
                .toList();

        List<DispatchQueueItemResponse> dispatchQueue = workOrderRepository
                .findDispatchQueue(tenantId, PageRequest.of(0, DISPATCH_QUEUE_LIMIT))
                .stream()
                .map(ScheduleBoardService::toDispatchQueueResponse)
                .toList();

        return new ScheduleBoardResponse(
                rangeStart,
                rangeEnd,
                appointments,
                dispatchQueue,
                workOrderRepository.countDispatchQueue(tenantId)
        );
    }

    private static void validateRange(Instant rangeStart, Instant rangeEnd) {
        if (rangeStart == null || rangeEnd == null || !rangeEnd.isAfter(rangeStart)) {
            throw BusinessException.badRequest("INVALID_SCHEDULE_RANGE", "Khoảng thời gian điều phối không hợp lệ");
        }
        if (Duration.between(rangeStart, rangeEnd).compareTo(MAX_RANGE) > 0) {
            throw BusinessException.badRequest("SCHEDULE_RANGE_TOO_LARGE", "Bảng điều phối chỉ hỗ trợ tối đa 31 ngày mỗi lần tải");
        }
    }

    private static ScheduleAppointmentResponse toAppointmentResponse(Appointment appointment) {
        WorkOrder workOrder = appointment.getWorkOrder();
        return new ScheduleAppointmentResponse(
                appointment.getId(),
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getCustomer().getName(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                appointment.getTechnician().getId(),
                appointment.getTechnician().getUser().getDisplayName(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );
    }

    private static DispatchQueueItemResponse toDispatchQueueResponse(WorkOrder workOrder) {
        return new DispatchQueueItemResponse(
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getCustomer().getName(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                workOrder.getCreatedAt()
        );
    }
}
