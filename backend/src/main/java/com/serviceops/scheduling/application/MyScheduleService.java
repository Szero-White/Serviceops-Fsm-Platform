package com.serviceops.scheduling.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.scheduling.web.MyScheduleDtos.MyScheduleItemResponse;
import com.serviceops.scheduling.web.MyScheduleDtos.MyScheduleResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyScheduleService {
    private static final Duration MAX_RANGE = Duration.ofDays(31);

    private final TechnicianRepository technicianRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public MyScheduleResponse get(Instant rangeStart, Instant rangeEnd) {
        validateRange(rangeStart, rangeEnd);
        UUID tenantId = CurrentUser.tenantId();
        TechnicianProfile technician = technicianRepository
                .findByTenantIdAndUserId(tenantId, CurrentUser.userId())
                .orElseThrow(() -> BusinessException.notFound(
                        "TECHNICIAN_PROFILE_NOT_FOUND",
                        "Tài khoản kỹ thuật viên chưa được liên kết với hồ sơ nhân sự"
                ));

        var appointments = appointmentRepository
                .findTechnicianRange(tenantId, technician.getId(), rangeStart, rangeEnd, AppointmentStatus.ACTIVE)
                .stream()
                .map(MyScheduleService::toResponse)
                .toList();

        return new MyScheduleResponse(
                technician.getId(),
                technician.getUser().getDisplayName(),
                rangeStart,
                rangeEnd,
                appointments
        );
    }

    private static void validateRange(Instant rangeStart, Instant rangeEnd) {
        if (rangeStart == null || rangeEnd == null || !rangeEnd.isAfter(rangeStart)) {
            throw BusinessException.badRequest("INVALID_MY_SCHEDULE_RANGE", "Khoảng thời gian lịch cá nhân không hợp lệ");
        }
        if (Duration.between(rangeStart, rangeEnd).compareTo(MAX_RANGE) > 0) {
            throw BusinessException.badRequest("MY_SCHEDULE_RANGE_TOO_LARGE", "Lịch cá nhân chỉ hỗ trợ tối đa 31 ngày mỗi lần tải");
        }
    }

    private static MyScheduleItemResponse toResponse(Appointment appointment) {
        WorkOrder workOrder = appointment.getWorkOrder();
        String assetLabel = workOrder.getAsset() == null ? null : assetLabel(workOrder.getAsset());

        return new MyScheduleItemResponse(
                appointment.getId(),
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getCustomer().getName(),
                workOrder.getCustomer().getAddress(),
                assetLabel,
                workOrder.getPriority(),
                workOrder.getStatus(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );
    }

    private static String assetLabel(Asset asset) {
        String equipmentName = ((asset.getBrand() == null ? "" : asset.getBrand() + " ")
                + (asset.getModel() == null ? "" : asset.getModel())).trim();
        if (equipmentName.isBlank()) {
            equipmentName = asset.getCategory();
        }
        String serial = asset.getSerialNumber() == null ? "Chưa xác định serial" : asset.getSerialNumber();
        return equipmentName + " (" + serial + ")";
    }
}
