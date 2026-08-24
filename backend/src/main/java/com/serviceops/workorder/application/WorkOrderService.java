package com.serviceops.workorder.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private static final Set<WorkOrderStatus> TECHNICIAN_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CUSTOMER_ACCEPTED,
            WorkOrderStatus.CLOSED,
            WorkOrderStatus.REOPENED
    );
    private static final Set<WorkOrderStatus> OWNER_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.CUSTOMER_ACCEPTED,
            WorkOrderStatus.CLOSED,
            WorkOrderStatus.REOPENED,
            WorkOrderStatus.CANCELLED
    );
    private static final Set<WorkOrderStatus> CUSTOMER_SERVICE_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.REOPENED,
            WorkOrderStatus.CANCELLED
    );
    private static final Set<WorkOrderStatus> DISPATCHER_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.CANCELLED
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
        UUID tenantId = CurrentUser.tenantId();
        List<WorkOrderStatusHistory> statusHistory = historyRepository
                .findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(tenantId, id);
        List<InventoryTransaction> partTransactions = inventoryTransactionRepository
                .findPartUsageForWorkOrder(tenantId, id);
        List<WorkOrderHistoryResponse> history = statusHistory.stream()
                .map(WorkOrderService::toHistory)
                .toList();
        var dispatchEvents = auditService.findEntityEvents(id, "WORK_ORDER", List.of("RESCHEDULE"));
        List<WorkOrderActivityResponse> activities = WorkOrderActivityMapper.merge(
                statusHistory,
                partTransactions,
                dispatchEvents
        );
        return toResponse(workOrder, history, activities);
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
        notificationService.notifyRoles(
                tenantId,
                dispatcherRoles(),
                "Phiếu mới cần điều phối: " + entity.getCode(),
                "Mở Phiếu công việc để sắp lịch và phân công kỹ thuật viên."
        );
        return toResponse(entity, List.of());
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
            String dispatchActor = dispatchActorLabel();
            String details = technicianChanged
                    ? dispatchActor + " đã điều phối lại kỹ thuật viên từ " + previousTechnicianName + " sang " + technicianName
                            + ". Lý do: " + reason
                    : dispatchActor + " đã điều chỉnh lịch thực hiện cho " + technicianName + ". Lý do: " + reason;
            auditService.record("RESCHEDULE", "WORK_ORDER", workOrder.getId(), details);

            if (technicianChanged && previousTechnician != null) {
                notificationService.create(
                        tenantId,
                        previousTechnician.getUser(),
                        "Công việc đã được điều chuyển: " + workOrder.getCode(),
                        "Phiếu đã được chuyển sang " + technicianName
                                + ". Bạn không cần tiếp tục xử lý phiếu này; kiểm tra Lịch của tôi để cập nhật kế hoạch."
                );
                notificationService.create(
                        tenantId,
                        technician.getUser(),
                        "Bạn được phân công tiếp nhận: " + workOrder.getCode(),
                        dispatchActor
                                + " đã chuyển phiếu này cho bạn. Mở phiếu để xem khách hàng, nội dung và lịch thực hiện mới."
                );
            } else {
                notificationService.create(
                        tenantId,
                        technician.getUser(),
                        "Lịch thực hiện đã được cập nhật: " + workOrder.getCode(),
                        dispatchActor
                                + " đã cập nhật lịch của phiếu. Mở Lịch của tôi để xem thời gian thực hiện mới."
                );
            }
        } else {
            auditService.record(
                    "ASSIGN",
                    "WORK_ORDER",
                    workOrder.getId(),
                    "Phân công " + workOrder.getCode() + " cho " + technicianName
            );
            notificationService.create(
                    tenantId,
                    technician.getUser(),
                    "Bạn được giao công việc mới: " + workOrder.getCode(),
                    dispatchActorLabel()
                            + " đã chuyển thông tin phiếu đến bạn. Mở phiếu để xem khách hàng, nội dung và thời gian thực hiện."
            );
        }

        return get(id);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, TransitionWorkOrder request) {
        WorkOrder workOrder = requireForUpdate(id);
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
        notifyStatusChange(workOrder);
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

        if (targetStatus == WorkOrderStatus.CANCELLED && blankToNull(request.note()) == null) {
            throw BusinessException.badRequest(
                    "WORK_ORDER_CANCELLATION_REASON_REQUIRED",
                    "Phải nhập lý do hủy phiếu công việc"
            );
        }

        if (CurrentUser.hasRole("OWNER")) {
            if (!OWNER_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Owner có quyền quản trị bước xác nhận khách, đóng/mở lại và hủy phiếu; tiến độ hiện trường vẫn thuộc kỹ thuật viên"
                );
            }
            return;
        }

        if (CurrentUser.hasRole("TECHNICIAN")) {
            if (!TECHNICIAN_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Kỹ thuật viên chỉ được cập nhật tiến độ, ghi nhận khách xác nhận, đóng hoặc mở lại công việc được phân công"
                );
            }
            return;
        }

        if (CurrentUser.hasRole("DISPATCHER")) {
            if (!DISPATCHER_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Điều phối viên chỉ được hủy phiếu công việc theo nghiệp vụ điều phối"
                );
            }
            return;
        }

        if (CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            if (!CUSTOMER_SERVICE_ALLOWED_TRANSITIONS.contains(targetStatus)) {
                throw BusinessException.forbidden(
                        "WORK_ORDER_TRANSITION_FORBIDDEN",
                        "Chăm sóc khách hàng chỉ mở lại hoặc hủy phiếu khi tiếp nhận yêu cầu thay đổi từ khách hàng"
                );
            }
        }
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

    private static String dispatchActorLabel() {
        String roleLabel = CurrentUser.hasRole("OWNER") ? "Chủ sở hữu" : "Điều phối viên";
        return roleLabel + " " + CurrentUser.displayName();
    }

    private void notifyStatusChange(WorkOrder workOrder) {
        UUID tenantId = workOrder.getTenantId();
        String code = workOrder.getCode();

        switch (workOrder.getStatus()) {
            case COMPLETED -> notificationService.notifyRoles(
                    tenantId,
                    List.of(UserRole.OWNER),
                    "Chờ khách xác nhận: " + code,
                    "Kỹ thuật viên đã hoàn thành công việc. Kỹ thuật viên được giao hoặc Owner có thể mở phiếu và bấm Khách xác nhận sau khi khách đồng ý kết quả."
            );
            case WAITING_FOR_PARTS -> notificationService.notifyRoles(
                    tenantId,
                    List.of(UserRole.DISPATCHER),
                    "Cần xử lý phụ tùng: " + code,
                    "Kỹ thuật viên đang chờ vật tư để tiếp tục. Kiểm tra phiếu và phối hợp xử lý phụ tùng."
            );
            case REOPENED -> {
                notificationService.notifyRoles(
                        tenantId,
                        List.of(UserRole.DISPATCHER),
                        "Cần điều phối xử lý lại: " + code,
                        "Khách yêu cầu xử lý lại. Kiểm tra lý do và sắp xếp kỹ thuật viên hoặc lịch phù hợp."
                );
                notifyAssignedTechnician(
                        workOrder,
                        "Công việc cần xử lý lại: " + code,
                        "Phiếu đã được mở lại. Mở phiếu để xem lý do và tiếp tục xử lý theo phân công."
                );
            }
            case CLOSED -> {
                notificationService.notifyRoles(
                        tenantId,
                        List.of(UserRole.OWNER),
                        "Phiếu đã đóng: " + code,
                        "Khách đã xác nhận kết quả và phiếu đã được đóng. Mở Lịch sử phiếu nếu cần đối soát."
                );
                notifyAssignedTechnician(
                        workOrder,
                        "Phiếu đã đóng: " + code,
                        "Khách đã xác nhận kết quả và phiếu đã được đóng. Không cần tiếp tục thao tác trên công việc này."
                );
            }
            case CANCELLED -> {
                notificationService.notifyRoles(
                        tenantId,
                        List.of(UserRole.OWNER),
                        "Phiếu đã hủy: " + code,
                        "Phiếu đã được hủy. Mở Lịch sử phiếu nếu cần kiểm tra lý do và người thực hiện."
                );
                notifyAssignedTechnician(
                        workOrder,
                        "Công việc đã hủy: " + code,
                        "Phiếu này đã bị hủy. Bạn không cần tiếp tục thực hiện công việc này."
                );
            }
            case CUSTOMER_ACCEPTED, ON_THE_WAY, IN_PROGRESS -> {
                // These are expected operational steps. The actor already sees success
                // feedback and the current status is visible in the Work Order screen.
            }
            default -> {
                // Scheduling/assignment has dedicated notifications. Avoid generic
                // "STATUS_A -> STATUS_B" messages that expose internal enum wording.
            }
        }
    }

    private void notifyAssignedTechnician(WorkOrder workOrder, String title, String message) {
        if (workOrder.getTechnician() == null
                || CurrentUser.userId().equals(workOrder.getTechnician().getUser().getId())) {
            return;
        }
        notificationService.create(
                workOrder.getTenantId(),
                workOrder.getTechnician().getUser(),
                title,
                message
        );
    }

    private static List<UserRole> dispatcherRoles() {
        return List.of(UserRole.DISPATCHER);
    }

    private static WorkOrderHistoryResponse toHistory(WorkOrderStatusHistory h) {
        return new WorkOrderHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedBy(), h.getCreatedAt());
    }

    public static WorkOrderResponse toResponse(WorkOrder w, List<WorkOrderHistoryResponse> history) {
        return toResponse(w, history, List.of());
    }

    private static WorkOrderResponse toResponse(
            WorkOrder w,
            List<WorkOrderHistoryResponse> history,
            List<WorkOrderActivityResponse> activities
    ) {
        String assetLabel = w.getAsset() == null ? null : assetLabel(w.getAsset());
        return new WorkOrderResponse(w.getId(), w.getCode(), w.getServiceRequest() == null ? null : w.getServiceRequest().getId(),
                w.getCustomer().getId(), w.getCustomer().getName(), w.getAsset() == null ? null : w.getAsset().getId(), assetLabel,
                w.getTechnician() == null ? null : w.getTechnician().getId(),
                w.getTechnician() == null ? null : w.getTechnician().getUser().getDisplayName(),
                w.getSummary(), w.getDescription(), w.getPriority(), w.getStatus(), w.getScheduledStart(), w.getScheduledEnd(),
                w.getDiagnosis(), w.getResolution(), w.getCompletedAt(), w.getCreatedAt(), history, activities);
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
