package com.serviceops.workorder.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserRole;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private static final Set<WorkOrderStatus> TECHNICIAN_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED
    );
    private static final Set<WorkOrderStatus> CUSTOMER_SERVICE_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.CANCELLED
    );

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
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        String normalizedSearch = PageRequestSupport.normalizeSearch(search);
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

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> history(String search, WorkOrderStatus status, int page, int size) {
        if (status != null && status != WorkOrderStatus.CLOSED && status != WorkOrderStatus.CANCELLED) {
            throw BusinessException.badRequest("INVALID_HISTORY_STATUS", "Lịch sử phiếu chỉ lọc trạng thái đã đóng hoặc đã hủy");
        }
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        String normalizedSearch = PageRequestSupport.normalizeSearch(search);
        var result = CurrentUser.hasRole("TECHNICIAN")
                ? repository.searchAssignedHistory(CurrentUser.tenantId(), CurrentUser.userId(), status, normalizedSearch, pageable)
                : repository.searchHistory(CurrentUser.tenantId(), status, normalizedSearch, pageable);
        return PageResponse.from(result.map(w -> toResponse(w, List.of())));
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
            serviceRequest = serviceRequestRepository.findDetailedForUpdate(request.serviceRequestId(), tenantId)
                    .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
            if (serviceRequest.getStatus() != ServiceRequestStatus.OPEN) {
                throw BusinessException.conflict("SERVICE_REQUEST_ALREADY_PROCESSED", "Yêu cầu dịch vụ đã được xử lý");
            }
            if (!serviceRequest.getCustomer().getId().equals(customer.getId())) {
                throw BusinessException.conflict(
                        "SERVICE_REQUEST_CUSTOMER_MISMATCH",
                        "Khách hàng của phiếu công việc không khớp với yêu cầu dịch vụ nguồn"
                );
            }
            if (serviceRequest.getAsset() != null
                    && (asset == null || !serviceRequest.getAsset().getId().equals(asset.getId()))) {
                throw BusinessException.conflict(
                        "SERVICE_REQUEST_ASSET_MISMATCH",
                        "Thiết bị của phiếu công việc không khớp với yêu cầu dịch vụ nguồn"
                );
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
        addHistory(entity, null, WorkOrderStatus.OPEN, "Tạo phiếu công việc");
        auditService.record("CREATE", "WORK_ORDER", entity.getId(), "Tạo " + entity.getCode());
        notificationService.notifyRoles(tenantId, dispatchRoles(), "Phiếu công việc mới: " + entity.getCode(), entity.getSummary());
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
        if (!technician.isActive()
                || !technician.getUser().isActive()
                || technician.getUser().getRole() != UserRole.TECHNICIAN) {
            throw BusinessException.conflict("TECHNICIAN_INACTIVE", "Kỹ thuật viên đang ngừng hoạt động hoặc tài khoản không còn hiệu lực");
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
        notificationService.notifyRoles(tenantId, dispatchRoles(), "Đã phân công " + workOrder.getCode(), technician.getUser().getDisplayName() + " phụ trách: " + workOrder.getSummary());
        return get(id);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, TransitionWorkOrder request) {
        WorkOrder workOrder = require(id);
        ensureTechnicianCanAccess(workOrder);
        ensureRoleCanTransition(request);
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
        notifyStatusChange(workOrder, previous);
        return get(id);
    }

    @Transactional
    public void deleteFromHistory(UUID id) {
        WorkOrder workOrder = require(id);
        try {
            workOrder.softDelete(CurrentUser.username());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("WORK_ORDER_NOT_ARCHIVABLE", ex.getMessage());
        }
        auditService.record("DELETE_HISTORY", "WORK_ORDER", workOrder.getId(), "Xóa khỏi lịch sử " + workOrder.getCode());
        notificationService.notifyRoles(workOrder.getTenantId(), dispatchRoles(), "Đã xóa khỏi lịch sử: " + workOrder.getCode(), workOrder.getSummary());
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

    private static void ensureRoleCanTransition(TransitionWorkOrder request) {
        WorkOrderStatus targetStatus = request.targetStatus();

        if (CurrentUser.hasRole("TECHNICIAN")) {
            if (!TECHNICIAN_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Kỹ thuật viên chỉ được cập nhật tiến độ thực hiện của công việc được phân công"
                );
            }
            return;
        }

        if (CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            if (!CUSTOMER_SERVICE_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Chăm sóc khách hàng chỉ được hủy phiếu công việc"
                );
            }
            if (blankToNull(request.note()) == null) {
                throw BusinessException.badRequest(
                        "WORK_ORDER_CANCELLATION_REASON_REQUIRED",
                        "Phải nhập lý do hủy phiếu công việc"
                );
            }
        }
    }

    private WorkOrder require(UUID id) {
        var workOrder = CurrentUser.hasRole("TECHNICIAN")
                ? repository.findDetailedAssigned(id, CurrentUser.tenantId(), CurrentUser.userId())
                : repository.findDetailed(id, CurrentUser.tenantId());
        return workOrder.orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
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

    private void notifyStatusChange(WorkOrder workOrder, WorkOrderStatus previous) {
        String title = "Cập nhật " + workOrder.getCode() + ": " + previous + " → " + workOrder.getStatus();
        notificationService.notifyRoles(workOrder.getTenantId(), dispatchRoles(), title, workOrder.getSummary());
        if (workOrder.getTechnician() != null && !CurrentUser.userId().equals(workOrder.getTechnician().getUser().getId())) {
            notificationService.create(workOrder.getTenantId(), workOrder.getTechnician().getUser(), title, workOrder.getSummary());
        }
    }

    private static List<UserRole> dispatchRoles() {
        return List.of(UserRole.OWNER, UserRole.DISPATCHER);
    }

    private static WorkOrderHistoryResponse toHistory(WorkOrderStatusHistory h) {
        return new WorkOrderHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedBy(), h.getCreatedAt());
    }

    public static WorkOrderResponse toResponse(WorkOrder w, List<WorkOrderHistoryResponse> history) {
        String assetLabel = w.getAsset() == null ? null : assetLabel(w.getAsset());
        return new WorkOrderResponse(w.getId(), w.getCode(), w.getServiceRequest() == null ? null : w.getServiceRequest().getId(),
                w.getCustomer().getId(), w.getCustomer().getName(), w.getAsset() == null ? null : w.getAsset().getId(), assetLabel,
                w.getTechnician() == null ? null : w.getTechnician().getId(),
                w.getTechnician() == null ? null : w.getTechnician().getUser().getDisplayName(),
                w.getSummary(), w.getDescription(), w.getPriority(), w.getStatus(), w.getScheduledStart(), w.getScheduledEnd(),
                w.getDiagnosis(), w.getResolution(), w.getCompletedAt(), w.getCreatedAt(), history);
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
