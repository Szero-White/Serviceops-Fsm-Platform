package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestReasonRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartFulfillmentService {
    private final SparePartRepository sparePartRepository;
    private final WorkOrderPartWorkflowPolicy workflowPolicy;
    private final WorkOrderPartStockService stockService;
    private final AuditService auditService;

    @Transactional
    public PartRequestResponse markUnavailable(UUID requestId, PartRequestReasonRequest request) {
        WorkOrderPartRequest entity = workflowPolicy.requireWarehousePendingRequestForUpdate(requestId);
        String reason = request.reason().trim();
        resolveByCurrentUser(entity, WorkOrderPartRequestStatus.UNAVAILABLE, reason);
        auditService.record(
                "PART_REQUEST_UNAVAILABLE",
                "WORK_ORDER",
                entity.getWorkOrder().getId(),
                "Kho không thể cấp " + entity.getSparePart().getSku() + " x "
                        + WorkOrderPartStockService.formatQuantity(entity.getRequestedQuantity()) + "; lý do: " + reason
        );
        return WorkOrderPartResponseMapper.toRequestResponse(entity);
    }

    @Transactional
    public PartRequestResponse issue(UUID requestId) {
        WorkOrderPartRequest entity = workflowPolicy.requireWarehousePendingRequestForUpdate(requestId);
        WorkOrder workOrder = entity.getWorkOrder();
        workflowPolicy.ensureRequestIssuable(workOrder);
        if (workOrder.getTechnician() == null || workOrder.getTechnician().getUser() == null) {
            throw BusinessException.conflict(
                    "WORK_ORDER_TECHNICIAN_REQUIRED",
                    "Phiếu công việc chưa có kỹ thuật viên để nhận phụ tùng"
            );
        }

        SparePart part = sparePartRepository.findForUpdate(entity.getSparePart().getId(), CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
        if (!part.isActive()) {
            throw BusinessException.conflict("SPARE_PART_INACTIVE", "Phụ tùng đã ngừng sử dụng và không thể cấp");
        }

        stockService.issue(
                part,
                workOrder,
                entity.getRequestedQuantity(),
                "Cấp theo yêu cầu phụ tùng; mục đích: " + entity.getRequestNote()
        );
        snapshotIssue(entity, workOrder);
        auditService.record(
                "ISSUE_PART",
                "WORK_ORDER",
                workOrder.getId(),
                "Cấp " + WorkOrderPartStockService.formatQuantity(entity.getIssuedQuantity()) + " " + part.getUnit()
                        + " - " + part.getSku() + "; người nhận: " + entity.getReceivedByDisplayName()
        );
        return WorkOrderPartResponseMapper.toRequestResponse(entity);
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

    private static void snapshotIssue(WorkOrderPartRequest entity, WorkOrder workOrder) {
        entity.setStatus(WorkOrderPartRequestStatus.ISSUED);
        entity.setIssuedQuantity(entity.getRequestedQuantity());
        entity.setIssuedByUserId(CurrentUser.userId());
        entity.setIssuedByUsername(CurrentUser.username());
        entity.setIssuedByDisplayName(CurrentUser.displayName());
        entity.setIssuedAt(Instant.now());
        entity.setReceivedByUserId(workOrder.getTechnician().getUser().getId());
        entity.setReceivedByDisplayName(workOrder.getTechnician().getUser().getDisplayName());
    }
}
