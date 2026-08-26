package com.serviceops.inventory.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkOrderPartWorkflowPolicy {
    private static final Set<WorkOrderStatus> REQUEST_ALLOWED_STATUSES = EnumSet.of(
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.REOPENED
    );
    private static final Set<WorkOrderStatus> USAGE_ALLOWED_STATUSES = EnumSet.of(
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.REOPENED
    );
    private static final Set<WorkOrderStatus> REQUEST_EXPIRY_STATUSES = EnumSet.of(
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CUSTOMER_ACCEPTED,
            WorkOrderStatus.CLOSED,
            WorkOrderStatus.CANCELLED
    );

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderPartRequestRepository requestRepository;

    public WorkOrder requireAssignedWorkOrderForRequest(UUID workOrderId) {
        requireRole("TECHNICIAN", "Chỉ kỹ thuật viên được phân công mới được thao tác yêu cầu phụ tùng");
        WorkOrder workOrder = requireWorkOrderForUpdate(workOrderId);
        requireAssignedTechnician(workOrder);
        ensureRequestStatusAllowed(
                workOrder,
                "PART_WORKFLOW_NOT_ALLOWED",
                "Phiếu công việc chưa ở trạng thái cho phép thao tác phụ tùng"
        );
        return workOrder;
    }

    public WorkOrder requireAssignedWorkOrderForUsage(UUID workOrderId) {
        requireRole("TECHNICIAN", "Chỉ kỹ thuật viên được phân công mới được xác nhận phụ tùng thực tế đã sử dụng");
        WorkOrder workOrder = requireWorkOrderForUpdate(workOrderId);
        requireAssignedTechnician(workOrder);
        if (!USAGE_ALLOWED_STATUSES.contains(workOrder.getStatus())) {
            throw BusinessException.conflict(
                    "PART_WORKFLOW_NOT_ALLOWED",
                    "Phiếu công việc chưa ở trạng thái cho phép xác nhận phụ tùng thực tế sử dụng"
            );
        }
        return workOrder;
    }

    public WorkOrderPartRequest requireTechnicianPendingRequestForUpdate(UUID requestId) {
        requireRole("TECHNICIAN", "Chỉ kỹ thuật viên được phân công mới được sửa hoặc hủy yêu cầu phụ tùng");
        WorkOrderPartRequest request = requirePendingRequestForUpdate(requestId);
        requireAssignedTechnician(request.getWorkOrder());
        return request;
    }

    public WorkOrderPartRequest requireWarehousePendingRequestForUpdate(UUID requestId) {
        requireRole("WAREHOUSE_STAFF", "Chỉ nhân viên kho được xử lý yêu cầu phụ tùng");
        return requirePendingRequestForUpdate(requestId);
    }

    public void ensureRequestEditable(WorkOrder workOrder) {
        ensureRequestStatusAllowed(
                workOrder,
                "PART_REQUEST_NOT_ALLOWED",
                "Không thể sửa yêu cầu phụ tùng ở trạng thái hiện tại của phiếu công việc"
        );
    }

    public void ensureRequestIssuable(WorkOrder workOrder) {
        ensureRequestStatusAllowed(
                workOrder,
                "PART_REQUEST_NOT_ISSUABLE",
                "Phiếu công việc không còn ở trạng thái cho phép cấp phụ tùng"
        );
    }

    public WorkOrder requireViewableWorkOrder(UUID workOrderId) {
        WorkOrder workOrder = workOrderRepository.findDetailed(workOrderId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (CurrentUser.hasRole("TECHNICIAN")) {
            requireAssignedTechnician(workOrder);
        }
        return workOrder;
    }

    public WorkOrder requireWorkOrderForUpdate(UUID workOrderId) {
        return workOrderRepository.findForUpdate(workOrderId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
    }

    public boolean shouldExpirePendingRequests(WorkOrderStatus status) {
        return REQUEST_EXPIRY_STATUSES.contains(status);
    }

    public void requireRole(String role, String message) {
        if (!CurrentUser.hasRole(role)) {
            throw BusinessException.forbidden("PART_WORKFLOW_FORBIDDEN", message);
        }
    }

    public void requireAnyRole(Set<String> roles, String message) {
        if (roles.stream().noneMatch(CurrentUser::hasRole)) {
            throw BusinessException.forbidden("PART_WORKFLOW_FORBIDDEN", message);
        }
    }

    private WorkOrderPartRequest requirePendingRequestForUpdate(UUID requestId) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrderPartRequest preview = requestRepository.findDetailed(requestId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("PART_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu phụ tùng"));
        requireWorkOrderForUpdate(preview.getWorkOrder().getId());
        WorkOrderPartRequest request = requestRepository.findForUpdate(requestId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("PART_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu phụ tùng"));
        if (request.getStatus() != WorkOrderPartRequestStatus.REQUESTED) {
            throw BusinessException.conflict(
                    "PART_REQUEST_ALREADY_RESOLVED",
                    "Yêu cầu phụ tùng đã được xử lý và không thể thay đổi"
            );
        }
        return request;
    }

    private static void ensureRequestStatusAllowed(WorkOrder workOrder, String code, String message) {
        if (!REQUEST_ALLOWED_STATUSES.contains(workOrder.getStatus())) {
            throw BusinessException.conflict(code, message);
        }
    }

    private static void requireAssignedTechnician(WorkOrder workOrder) {
        if (workOrder.getTechnician() == null
                || workOrder.getTechnician().getUser() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId())) {
            throw BusinessException.forbidden(
                    "WORK_ORDER_NOT_ASSIGNED",
                    "Bạn chỉ được thao tác phụ tùng cho công việc được phân công cho mình"
            );
        }
    }
}
