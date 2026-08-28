package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestCreateRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestReasonRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestUpdateRequest;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartRequestService {
    private final WorkOrderPartRequestRepository requestRepository;
    private final SparePartRepository sparePartRepository;
    private final WorkOrderPartWorkflowPolicy workflowPolicy;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public PartRequestResponse createRequest(UUID workOrderId, PartRequestCreateRequest request) {
        WorkOrder workOrder = workflowPolicy.requireAssignedWorkOrderForRequest(workOrderId);
        UUID tenantId = CurrentUser.tenantId();
        SparePart part = sparePartRepository.findByIdAndTenantId(request.sparePartId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
        if (!part.isActive()) {
            throw BusinessException.conflict(
                    "SPARE_PART_INACTIVE",
                    "Phụ tùng đã ngừng sử dụng và không thể yêu cầu cho công việc mới"
            );
        }
        if (requestRepository.existsByTenantIdAndWorkOrderIdAndSparePartIdAndStatus(
                tenantId, workOrderId, part.getId(), WorkOrderPartRequestStatus.REQUESTED)) {
            throw duplicatePendingRequest(
                    "Phụ tùng này đã có yêu cầu đang chờ cấp; hãy sửa số lượng của yêu cầu hiện tại thay vì tạo thêm"
            );
        }

        WorkOrderPartRequest entity = new WorkOrderPartRequest();
        entity.setTenantId(tenantId);
        entity.setWorkOrder(workOrder);
        entity.setSparePart(part);
        entity.setRequestedQuantity(request.quantity());
        entity.setRequestNote(request.note().trim());
        entity.setStatus(WorkOrderPartRequestStatus.REQUESTED);
        entity.setRequestedByUserId(CurrentUser.userId());
        entity.setRequestedByUsername(CurrentUser.username());
        entity.setRequestedByDisplayName(CurrentUser.displayName());
        try {
            requestRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw duplicatePendingRequest(
                    "Phụ tùng này đã có yêu cầu đang chờ cấp; hãy tải lại phiếu và sửa yêu cầu hiện tại"
            );
        }

        auditService.record(
                "REQUEST_PART",
                "WORK_ORDER",
                workOrder.getId(),
                "Yêu cầu " + WorkOrderPartStockService.formatQuantity(request.quantity()) + " " + part.getUnit()
                        + " - " + part.getSku() + "; mục đích: " + request.note().trim()
        );
        notifyWarehouse(workOrder, part, request);
        return WorkOrderPartResponseMapper.toRequestResponse(entity);
    }

    @Transactional
    public PartRequestResponse updateRequest(UUID requestId, PartRequestUpdateRequest request) {
        WorkOrderPartRequest entity = workflowPolicy.requireTechnicianPendingRequestForUpdate(requestId);
        workflowPolicy.ensureRequestEditable(entity.getWorkOrder());
        var previousQuantity = entity.getRequestedQuantity();
        String previousNote = entity.getRequestNote();
        String nextNote = request.note().trim();
        if (previousQuantity.compareTo(request.quantity()) == 0 && previousNote.equals(nextNote)) {
            return WorkOrderPartResponseMapper.toRequestResponse(entity);
        }

        entity.setRequestedQuantity(request.quantity());
        entity.setRequestNote(nextNote);
        auditService.record(
                "UPDATE_PART_REQUEST",
                "WORK_ORDER",
                entity.getWorkOrder().getId(),
                "Điều chỉnh yêu cầu " + entity.getSparePart().getSku() + ": "
                        + WorkOrderPartStockService.formatQuantity(previousQuantity) + " -> "
                        + WorkOrderPartStockService.formatQuantity(request.quantity()) + " " + entity.getSparePart().getUnit()
                        + (previousNote.equals(nextNote) ? "" : "; cập nhật mục đích sử dụng")
        );
        return WorkOrderPartResponseMapper.toRequestResponse(entity);
    }

    @Transactional
    public PartRequestResponse cancelRequest(UUID requestId, PartRequestReasonRequest request) {
        WorkOrderPartRequest entity = workflowPolicy.requireTechnicianPendingRequestForUpdate(requestId);
        String reason = request.reason().trim();
        resolveByCurrentUser(entity, WorkOrderPartRequestStatus.CANCELLED, reason);
        auditService.record(
                "CANCEL_PART_REQUEST",
                "WORK_ORDER",
                entity.getWorkOrder().getId(),
                "Hủy yêu cầu " + entity.getSparePart().getSku() + " x "
                        + WorkOrderPartStockService.formatQuantity(entity.getRequestedQuantity()) + "; lý do: " + reason
        );
        return WorkOrderPartResponseMapper.toRequestResponse(entity);
    }

    @Transactional
    public void expirePendingRequests(WorkOrder workOrder) {
        if (!workflowPolicy.shouldExpirePendingRequests(workOrder.getStatus())) {
            return;
        }
        List<WorkOrderPartRequest> pending = requestRepository.findDetailedByWorkOrderAndStatus(
                workOrder.getTenantId(), workOrder.getId(), WorkOrderPartRequestStatus.REQUESTED);
        for (WorkOrderPartRequest request : pending) {
            request.setStatus(WorkOrderPartRequestStatus.EXPIRED);
            request.setResolutionReason(
                    "Phiếu công việc đã chuyển sang " + workOrder.getStatus() + " trước khi phụ tùng được cấp"
            );
            request.setResolvedByUsername("system");
            request.setResolvedByDisplayName("Hệ thống");
            request.setResolvedAt(Instant.now());
        }
    }

    private static void resolveByCurrentUser(
            WorkOrderPartRequest request,
            WorkOrderPartRequestStatus status,
            String reason
    ) {
        request.setStatus(status);
        request.setResolutionReason(reason);
        request.setResolvedByUserId(CurrentUser.userId());
        request.setResolvedByUsername(CurrentUser.username());
        request.setResolvedByDisplayName(CurrentUser.displayName());
        request.setResolvedAt(Instant.now());
    }

    private void notifyWarehouse(
            WorkOrder workOrder,
            SparePart part,
            PartRequestCreateRequest request
    ) {
        var copy = NotificationCopy.partRequestCreated(
                workOrder.getCode(),
                workOrder.getSummary(),
                part.getSku(),
                part.getName(),
                request.quantity(),
                part.getUnit(),
                CurrentUser.displayName()
        );
        notificationService.notifyRoles(
                CurrentUser.tenantId(),
                List.of(UserRole.WAREHOUSE_STAFF),
                copy.title(),
                copy.message()
        );
    }

    private static BusinessException duplicatePendingRequest(String message) {
        return BusinessException.conflict("PART_REQUEST_ALREADY_PENDING", message);
    }
}
