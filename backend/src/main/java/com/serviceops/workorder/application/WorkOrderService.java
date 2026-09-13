package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.application.WorkOrderPartRequestService;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
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
import com.serviceops.workorder.web.WorkOrderDtos.ScheduleWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
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
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("code", "code"),
            Map.entry("summary", "summary"),
            Map.entry("customerName", "customer.name"),
            Map.entry("assetLabel", "asset.serialNumber"),
            Map.entry("technicianName", "technician.user.displayName"),
            Map.entry("priority", "priority"),
            Map.entry("status", "status"),
            Map.entry("scheduledStart", "scheduledStart"),
            Map.entry("scheduledEnd", "scheduledEnd"),
            Map.entry("completedAt", "completedAt"),
            Map.entry("createdAt", "createdAt")
    );
    private static final Set<WorkOrderStatus> DISPATCHABLE_STATUSES = EnumSet.of(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.SCHEDULED,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.REOPENED
    );

    private final WorkOrderRepository repository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final WorkOrderPartRequestService workOrderPartRequestService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final TechnicianRepository technicianRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> search(String search, List<WorkOrderStatus> status, int page, int size) {
        return search(search, status, page, size, "createdAt", "desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> search(String search, List<WorkOrderStatus> status, int page, int size, String sortBy, String sortDir) {
        var sort = PageRequestSupport.safeSort(sortBy, sortDir, SORT_FIELDS, "createdAt", Sort.Direction.DESC);
        var pageable = PageRequestSupport.of(page, size, sort);
        String normalizedSearch = PageRequestSupport.normalizeSearch(search);
        List<WorkOrderStatus> statuses = WorkOrderAccessPolicy.activeStatuses(status);
        var result = CurrentUser.hasRole("TECHNICIAN")
                ? repository.searchAssigned(CurrentUser.tenantId(), CurrentUser.userId(), statuses, normalizedSearch, pageable)
                : repository.search(CurrentUser.tenantId(), statuses, normalizedSearch, pageable);
        return PageResponse.from(result.map(w -> WorkOrderResponseMapper.toResponse(w, List.of())));
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(UUID id) {
        WorkOrder workOrder = require(id);
        UUID tenantId = CurrentUser.tenantId();
        List<WorkOrderStatusHistory> statusHistory = historyRepository
                .findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(tenantId, id);
        List<InventoryTransaction> partTransactions = inventoryTransactionRepository
                .findPartUsageForWorkOrder(tenantId, id);
        List<WorkOrderHistoryResponse> history = statusHistory.stream()
                .map(WorkOrderResponseMapper::toHistory)
                .toList();
        var dispatchEvents = auditService.findEntityEvents(id, "WORK_ORDER", List.of("RESCHEDULE"));
        List<WorkOrderActivityResponse> activities = WorkOrderActivityMapper.merge(
                statusHistory,
                partTransactions,
                dispatchEvents
        );
        return WorkOrderResponseMapper.toResponse(workOrder, history, activities);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> history(String search, List<WorkOrderStatus> status, int page, int size) {
        return history(search, status, page, size, "createdAt", "desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> history(String search, List<WorkOrderStatus> status, int page, int size, String sortBy, String sortDir) {
        List<WorkOrderStatus> historyStatuses = WorkOrderAccessPolicy.historyStatuses(status);
        var sort = PageRequestSupport.safeSort(sortBy, sortDir, SORT_FIELDS, "createdAt", Sort.Direction.DESC);
        var pageable = PageRequestSupport.of(page, size, sort);
        String normalizedSearch = PageRequestSupport.normalizeSearch(search);
        var result = CurrentUser.hasRole("TECHNICIAN")
                ? repository.searchAssignedHistory(CurrentUser.tenantId(), CurrentUser.userId(), historyStatuses, normalizedSearch, pageable)
                : repository.searchHistory(CurrentUser.tenantId(), historyStatuses, normalizedSearch, pageable);
        return PageResponse.from(result.map(w -> WorkOrderResponseMapper.toResponse(w, List.of())));
    }

    @Transactional
    public WorkOrderResponse convertServiceRequest(UUID serviceRequestId) {
        UUID tenantId = CurrentUser.tenantId();
        ServiceRequest serviceRequest = serviceRequestRepository.findDetailedForUpdate(serviceRequestId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
        if (serviceRequest.getStatus() != ServiceRequestStatus.OPEN) {
            throw BusinessException.conflict("SERVICE_REQUEST_ALREADY_PROCESSED", "Yêu cầu dịch vụ đã được xử lý");
        }

        serviceRequest.markConverted();

        WorkOrder entity = new WorkOrder();
        entity.setTenantId(tenantId);
        entity.setServiceRequest(serviceRequest);
        entity.setCustomer(serviceRequest.getCustomer());
        entity.setAsset(serviceRequest.getAsset());
        entity.setCode(nextCode());
        entity.setSummary(serviceRequest.getTitle().trim());
        entity.setDescription(blankToNull(serviceRequest.getDescription()));
        entity.setPriority(serviceRequest.getPriority());
        entity.setStatus(WorkOrderStatus.OPEN);
        repository.save(entity);

        addHistory(entity, null, WorkOrderStatus.OPEN, "Tiếp nhận từ yêu cầu dịch vụ");
        auditService.record("CREATE_FROM_SERVICE_REQUEST", "WORK_ORDER", entity.getId(), "Tạo " + entity.getCode() + " từ yêu cầu dịch vụ");
        WorkOrderNotificationSupport.notifyNeedsDispatch(notificationService, entity);
        return WorkOrderResponseMapper.toResponse(entity, List.of());
    }

    @Transactional
    public WorkOrderResponse schedule(UUID id, ScheduleWorkOrder request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw BusinessException.badRequest("INVALID_APPOINTMENT_TIME", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = requireForUpdate(id);
        if (!DISPATCHABLE_STATUSES.contains(workOrder.getStatus())) {
            throw BusinessException.conflict(
                    "WORK_ORDER_ALREADY_STARTED",
                    "Chỉ có thể điều phối lại trước khi kỹ thuật viên bắt đầu di chuyển hoặc thực hiện công việc"
            );
        }

        TechnicianProfile technician = technicianRepository.findForUpdate(request.technicianId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("TECHNICIAN_NOT_FOUND", "Không tìm thấy kỹ thuật viên"));
        if (!technician.isActive()
                || !technician.getUser().isActive()
                || technician.getUser().getRole() != UserRole.TECHNICIAN) {
            throw BusinessException.conflict("TECHNICIAN_INACTIVE", "Kỹ thuật viên đang ngừng hoạt động hoặc tài khoản không còn hiệu lực");
        }

        Appointment existingAppointment = appointmentRepository
                .findByTenantIdAndWorkOrderId(tenantId, workOrder.getId())
                .orElse(null);
        TechnicianProfile previousTechnician = existingAppointment == null
                ? workOrder.getTechnician()
                : existingAppointment.getTechnician();
        Instant previousStart = existingAppointment == null ? workOrder.getScheduledStart() : existingAppointment.getStartTime();
        Instant previousEnd = existingAppointment == null ? workOrder.getScheduledEnd() : existingAppointment.getEndTime();

        boolean previouslyDispatched = previousTechnician != null || previousStart != null || previousEnd != null;
        boolean technicianChanged = previousTechnician != null && !previousTechnician.getId().equals(technician.getId());
        boolean scheduleChanged = !Objects.equals(previousStart, request.startTime())
                || !Objects.equals(previousEnd, request.endTime());
        boolean dispatchChanged = technicianChanged || scheduleChanged;

        if (previouslyDispatched && !dispatchChanged) {
            return get(id);
        }

        String reason = blankToNull(request.reason());
        if (previouslyDispatched && reason == null) {
            throw BusinessException.badRequest(
                    "WORK_ORDER_REDISPATCH_REASON_REQUIRED",
                    "Phải nhập lý do khi điều phối lại kỹ thuật viên hoặc lịch thực hiện"
            );
        }

        boolean overlap = appointmentRepository.existsOverlap(
                tenantId,
                technician.getId(),
                request.startTime(),
                request.endTime(),
                AppointmentStatus.ACTIVE,
                workOrder.getId()
        );
        if (overlap) {
            throw BusinessException.conflict("TECHNICIAN_SCHEDULE_CONFLICT", "Kỹ thuật viên đã có công việc trùng thời gian");
        }

        WorkOrderStatus previousStatus = workOrder.getStatus();
        try {
            workOrder.schedule(technician, request.startTime(), request.endTime());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("INVALID_APPOINTMENT_TIME", ex.getMessage());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INVALID_STATUS_TRANSITION", ex.getMessage());
        }

        Appointment appointment = existingAppointment == null ? new Appointment() : existingAppointment;
        if (appointment.getId() == null) {
            appointment.setTenantId(tenantId);
            appointment.setWorkOrder(workOrder);
        }
        appointment.setTechnician(technician);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setStatus(AppointmentStatus.ACTIVE);
        appointmentRepository.save(appointment);

        String technicianName = technician.getUser().getDisplayName();
        if (previousStatus != workOrder.getStatus()) {
            addHistory(workOrder, previousStatus, workOrder.getStatus(), "Phân công cho " + technicianName);
        }

        if (previouslyDispatched) {
            String previousTechnicianName = previousTechnician == null
                    ? "Chưa phân công"
                    : previousTechnician.getUser().getDisplayName();
            String details = "Điều chỉnh lịch " + workOrder.getCode()
                    + ": " + previousTechnicianName + " [" + previousStart + " - " + previousEnd + "]"
                    + " → " + technicianName + " [" + request.startTime() + " - " + request.endTime() + "]"
                    + ". Lý do: " + reason;
            auditService.record("RESCHEDULE", "WORK_ORDER", workOrder.getId(), details);

            WorkOrderNotificationSupport.notifyRedispatch(
                    notificationService,
                    tenantId,
                    workOrder,
                    previousTechnician,
                    technician,
                    technicianChanged,
                    previousStart,
                    previousEnd,
                    request.startTime(),
                    request.endTime(),
                    reason
            );
        } else {
            auditService.record(
                    "ASSIGN",
                    "WORK_ORDER",
                    workOrder.getId(),
                    "Phân công " + workOrder.getCode() + " cho " + technicianName
            );
            WorkOrderNotificationSupport.notifyInitialAssignment(notificationService, tenantId, technician, workOrder);
        }

        return get(id);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, TransitionWorkOrder request) {
        WorkOrder workOrder = requireForUpdate(id);
        WorkOrderAccessPolicy.ensureTechnicianCanAccess(workOrder);
        WorkOrderAccessPolicy.ensureRoleCanTransition(request);
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
        WorkOrderStatusHistory statusHistory = addHistory(
                workOrder,
                previous,
                workOrder.getStatus(),
                blankToNull(request.note())
        );
        auditService.record("CHANGE_STATUS", "WORK_ORDER", workOrder.getId(), previous.displayName() + " → " + workOrder.getStatus().displayName());
        workOrderPartRequestService.expirePendingRequests(workOrder);
        WorkOrderNotificationSupport.notifyStatusChange(notificationService, workOrder, blankToNull(request.note()), statusHistory);
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
    }

    private WorkOrder requireForUpdate(UUID id) {
        return repository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
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

    private WorkOrderStatusHistory addHistory(WorkOrder workOrder, WorkOrderStatus from, WorkOrderStatus to, String note) {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setTenantId(workOrder.getTenantId());
        history.setWorkOrder(workOrder);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNote(note);
        history.setChangedBy(CurrentUser.username());
        history.setActorDisplayName(CurrentUser.displayName());
        history.setActorRole(CurrentUser.primaryRole());
        if (to == WorkOrderStatus.COMPLETED) {
            history.setDiagnosisSnapshot(workOrder.getDiagnosis());
            history.setResolutionSnapshot(workOrder.getResolution());
        }
        return historyRepository.save(history);
    }

    /**
     * Backward-compatible response mapper entry point retained for callers outside this service.
     * New internal code delegates to {@link WorkOrderResponseMapper}.
     */
    public static WorkOrderResponse toResponse(WorkOrder workOrder, List<WorkOrderHistoryResponse> history) {
        return WorkOrderResponseMapper.toResponse(workOrder, history);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
