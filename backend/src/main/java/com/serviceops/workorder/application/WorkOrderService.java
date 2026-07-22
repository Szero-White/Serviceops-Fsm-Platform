package com.serviceops.workorder.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.CreateWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.ScheduleWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderHistoryResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository repository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final TechnicianRepository technicianRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> search(String search, WorkOrderStatus status, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        String normalizedSearch = search == null ? "" : search.trim();
        var result = CurrentUser.hasRole("TECHNICIAN")
                ? repository.searchAssigned(CurrentUser.tenantId(), CurrentUser.userId(), status, normalizedSearch, pageable)
                : repository.search(CurrentUser.tenantId(), status, normalizedSearch, pageable);
        return PageResponse.from(result.map(w -> toResponse(w, List.of())));
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(UUID id) {
        WorkOrder workOrder = require(id);
        List<WorkOrderHistoryResponse> history = historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(CurrentUser.tenantId(), id)
                .stream().map(WorkOrderService::toHistory).toList();
        return toResponse(workOrder, history);
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrder request) {
        UUID tenantId = CurrentUser.tenantId();
        Customer customer = customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
        Asset asset = null;
        if (request.assetId() != null) {
            asset = assetRepository.findDetailed(request.assetId(), tenantId)
                    .orElseThrow(() -> BusinessException.notFound("ASSET_NOT_FOUND", "Không tìm thấy thiết bị"));
            if (!asset.getCustomer().getId().equals(customer.getId())) {
                throw BusinessException.badRequest("ASSET_CUSTOMER_MISMATCH", "Thiết bị không thuộc khách hàng đã chọn");
            }
        }
        ServiceRequest serviceRequest = null;
        if (request.serviceRequestId() != null) {
            serviceRequest = serviceRequestRepository.findDetailed(request.serviceRequestId(), tenantId)
                    .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
            if (serviceRequest.getStatus() != ServiceRequestStatus.OPEN) {
                throw BusinessException.conflict("SERVICE_REQUEST_ALREADY_PROCESSED", "Yêu cầu dịch vụ đã được xử lý");
            }
            serviceRequest.markConverted();
        }

        WorkOrder entity = new WorkOrder();
        entity.setTenantId(tenantId);
        entity.setServiceRequest(serviceRequest);
        entity.setCustomer(customer);
        entity.setAsset(asset);
        entity.setCode(nextCode());
        entity.setSummary(request.summary().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setPriority(request.priority());
        entity.setStatus(WorkOrderStatus.OPEN);
        repository.save(entity);
        addHistory(entity, null, WorkOrderStatus.OPEN, "Tạo work order");
        auditService.record("CREATE", "WORK_ORDER", entity.getId(), "Tạo " + entity.getCode());
        return toResponse(entity, List.of());
    }

    @Transactional
    public WorkOrderResponse convertServiceRequest(UUID serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findDetailed(serviceRequestId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
        return create(new CreateWorkOrder(sr.getId(), sr.getCustomer().getId(), sr.getAsset() == null ? null : sr.getAsset().getId(), sr.getTitle(), sr.getDescription(), sr.getPriority()));
    }

    @Transactional
    public WorkOrderResponse schedule(UUID id, ScheduleWorkOrder request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw BusinessException.badRequest("INVALID_APPOINTMENT_TIME", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = require(id);
        TechnicianProfile technician = technicianRepository.findForUpdate(request.technicianId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("TECHNICIAN_NOT_FOUND", "Không tìm thấy kỹ thuật viên"));
        if (!technician.isActive()) {
            throw BusinessException.conflict("TECHNICIAN_INACTIVE", "Kỹ thuật viên đang ngừng hoạt động");
        }
        boolean overlap = appointmentRepository.existsOverlap(tenantId, technician.getId(), request.startTime(), request.endTime(), AppointmentStatus.ACTIVE, workOrder.getId());
        if (overlap) {
            throw BusinessException.conflict("TECHNICIAN_SCHEDULE_CONFLICT", "Kỹ thuật viên đã có công việc trùng thời gian");
        }

        WorkOrderStatus previous = workOrder.getStatus();
        try {
            workOrder.schedule(technician, request.startTime(), request.endTime());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("INVALID_APPOINTMENT_TIME", ex.getMessage());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INVALID_STATUS_TRANSITION", ex.getMessage());
        }

        Appointment appointment = appointmentRepository.findByTenantIdAndWorkOrderId(tenantId, workOrder.getId()).orElseGet(Appointment::new);
        if (appointment.getId() == null) {
            appointment.setTenantId(tenantId);
            appointment.setWorkOrder(workOrder);
        }
        appointment.setTechnician(technician);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setStatus(AppointmentStatus.ACTIVE);
        appointmentRepository.save(appointment);

        addHistory(workOrder, previous, workOrder.getStatus(), "Phân công cho " + technician.getUser().getDisplayName());
        auditService.record("ASSIGN", "WORK_ORDER", workOrder.getId(), "Phân công " + workOrder.getCode() + " cho " + technician.getUser().getDisplayName());
        notificationService.create(tenantId, technician.getUser(), "Công việc mới: " + workOrder.getCode(), "Bạn được phân công: " + workOrder.getSummary());
        return get(id);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, TransitionWorkOrder request) {
        WorkOrder workOrder = require(id);
        ensureTechnicianCanAccess(workOrder);
        WorkOrderStatus previous = workOrder.getStatus();
        if (request.targetStatus() == WorkOrderStatus.COMPLETED) {
            if (request.diagnosis() == null || request.diagnosis().isBlank() || request.resolution() == null || request.resolution().isBlank()) {
                throw BusinessException.badRequest("COMPLETION_DETAILS_REQUIRED", "Phải nhập chẩn đoán và giải pháp trước khi hoàn thành");
            }
            workOrder.setDiagnosis(request.diagnosis().trim());
            workOrder.setResolution(request.resolution().trim());
        }
        try {
            workOrder.transitionTo(request.targetStatus());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INVALID_STATUS_TRANSITION", ex.getMessage());
        }
        if (request.targetStatus() == WorkOrderStatus.CANCELLED) {
            appointmentRepository.findByTenantIdAndWorkOrderId(CurrentUser.tenantId(), workOrder.getId())
                    .ifPresent(a -> a.setStatus(AppointmentStatus.CANCELLED));
        }
        addHistory(workOrder, previous, workOrder.getStatus(), blankToNull(request.note()));
        auditService.record("CHANGE_STATUS", "WORK_ORDER", workOrder.getId(), previous + " → " + workOrder.getStatus());
        return get(id);
    }


    private static void ensureTechnicianCanAccess(WorkOrder workOrder) {
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            return;
        }
        if (workOrder.getTechnician() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId())) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được thao tác công việc được phân công cho mình");
        }
    }

    private WorkOrder require(UUID id) {
        var workOrder = CurrentUser.hasRole("TECHNICIAN")
                ? repository.findDetailedAssigned(id, CurrentUser.tenantId(), CurrentUser.userId())
                : repository.findDetailed(id, CurrentUser.tenantId());
        return workOrder.orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy work order"));
    }

    private String nextCode() {
        long number = repository.nextNumber();
        int year = Instant.now().atZone(ZoneOffset.UTC).getYear();
        return "WO-%d-%06d".formatted(year, number);
    }

    private void addHistory(WorkOrder workOrder, WorkOrderStatus from, WorkOrderStatus to, String note) {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setTenantId(workOrder.getTenantId());
        history.setWorkOrder(workOrder);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNote(note);
        history.setChangedBy(CurrentUser.username());
        historyRepository.save(history);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static WorkOrderHistoryResponse toHistory(WorkOrderStatusHistory h) {
        return new WorkOrderHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedBy(), h.getCreatedAt());
    }

    public static WorkOrderResponse toResponse(WorkOrder w, List<WorkOrderHistoryResponse> history) {
        String assetLabel = w.getAsset() == null ? null : ((w.getAsset().getBrand() == null ? "" : w.getAsset().getBrand() + " ")
                + (w.getAsset().getModel() == null ? "" : w.getAsset().getModel() + " ")
                + "(" + w.getAsset().getSerialNumber() + ")").trim();
        return new WorkOrderResponse(w.getId(), w.getCode(), w.getServiceRequest() == null ? null : w.getServiceRequest().getId(),
                w.getCustomer().getId(), w.getCustomer().getName(), w.getAsset() == null ? null : w.getAsset().getId(), assetLabel,
                w.getTechnician() == null ? null : w.getTechnician().getId(),
                w.getTechnician() == null ? null : w.getTechnician().getUser().getDisplayName(),
                w.getSummary(), w.getDescription(), w.getPriority(), w.getStatus(), w.getScheduledStart(), w.getScheduledEnd(),
                w.getDiagnosis(), w.getResolution(), w.getCompletedAt(), w.getCreatedAt(), history);
    }
}
