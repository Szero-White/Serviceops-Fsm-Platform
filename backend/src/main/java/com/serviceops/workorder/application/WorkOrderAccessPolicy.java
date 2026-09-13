package com.serviceops.workorder.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Centralizes role and list-scope rules for Work Orders so application services
 * do not duplicate authorization and status-filter semantics.
 */
final class WorkOrderAccessPolicy {
    private static final Set<WorkOrderStatus> TECHNICIAN_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED
    );
    private static final Set<WorkOrderStatus> OWNER_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.CANCELLED
    );
    private static final Set<WorkOrderStatus> CUSTOMER_SERVICE_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.REOPENED,
            WorkOrderStatus.CANCELLED
    );
    private static final Set<WorkOrderStatus> DISPATCHER_ALLOWED_TRANSITIONS = EnumSet.of(
            WorkOrderStatus.CANCELLED
    );

    private WorkOrderAccessPolicy() {
    }

    static List<WorkOrderStatus> activeStatuses(List<WorkOrderStatus> requested) {
        List<WorkOrderStatus> allowed = WorkOrderStatus.operationalStatuses();
        if (requested == null || requested.isEmpty()) {
            return allowed;
        }
        List<WorkOrderStatus> filtered = requested.stream().filter(allowed::contains).distinct().toList();
        if (filtered.size() != requested.stream().distinct().count()) {
            throw BusinessException.badRequest(
                    "INVALID_ACTIVE_WORK_ORDER_STATUS",
                    "Bộ lọc phiếu công việc chứa trạng thái không thuộc danh sách đang vận hành"
            );
        }
        return filtered;
    }

    static List<WorkOrderStatus> historyStatuses(List<WorkOrderStatus> requested) {
        List<WorkOrderStatus> allowed = WorkOrderStatus.historyStatuses();
        if (requested == null || requested.isEmpty()) {
            return allowed;
        }
        List<WorkOrderStatus> filtered = requested.stream().filter(allowed::contains).distinct().toList();
        if (filtered.size() != requested.stream().distinct().count()) {
            throw BusinessException.badRequest(
                    "INVALID_HISTORY_STATUS",
                    "Lịch sử phiếu chỉ lọc hồ sơ chờ hoàn tất, đã đóng hoặc đã hủy"
            );
        }
        return filtered;
    }

    static void ensureTechnicianCanAccess(WorkOrder workOrder) {
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            return;
        }
        if (workOrder.getTechnician() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId())) {
            throw BusinessException.forbidden(
                    "WORK_ORDER_NOT_ASSIGNED",
                    "Bạn chỉ được thao tác công việc được phân công cho mình"
            );
        }
    }

    static void ensureRoleCanTransition(TransitionWorkOrder request) {
        WorkOrderStatus targetStatus = request.targetStatus();

        if (CurrentUser.hasRole("OWNER")) {
            requireAllowed(OWNER_ALLOWED_TRANSITIONS, targetStatus,
                    "Chủ sở hữu giám sát kết quả; thao tác vận hành thông thường thuộc đúng vai trò phụ trách");
            ensureTransitionReason(request);
            return;
        }

        if (CurrentUser.hasRole("TECHNICIAN")) {
            requireAllowed(TECHNICIAN_ALLOWED_TRANSITIONS, targetStatus,
                    "Kỹ thuật viên chỉ được cập nhật tiến độ hiện trường; xác nhận khách và thanh toán dùng thao tác nghiệp vụ riêng");
            return;
        }

        if (CurrentUser.hasRole("DISPATCHER")) {
            requireAllowed(DISPATCHER_ALLOWED_TRANSITIONS, targetStatus,
                    "Điều phối viên chỉ được hủy phiếu công việc theo nghiệp vụ điều phối");
            ensureTransitionReason(request);
            return;
        }

        if (CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            requireAllowed(CUSTOMER_SERVICE_ALLOWED_TRANSITIONS, targetStatus,
                    "Chăm sóc khách hàng chỉ mở lại hoặc hủy phiếu khi tiếp nhận yêu cầu thay đổi từ khách hàng");
            ensureTransitionReason(request);
            return;
        }

        throw BusinessException.forbidden(
                "WORK_ORDER_TRANSITION_FORBIDDEN",
                "Vai trò hiện tại không được phép cập nhật trạng thái phiếu công việc"
        );
    }

    private static void requireAllowed(Set<WorkOrderStatus> allowed, WorkOrderStatus targetStatus, String message) {
        if (!allowed.contains(targetStatus)) {
            throw BusinessException.forbidden("WORK_ORDER_TRANSITION_FORBIDDEN", message);
        }
    }

    private static void ensureTransitionReason(TransitionWorkOrder request) {
        if (hasText(request.note())) {
            return;
        }
        if (request.targetStatus() == WorkOrderStatus.CANCELLED) {
            throw BusinessException.badRequest(
                    "WORK_ORDER_CANCELLATION_REASON_REQUIRED",
                    "Phải nhập lý do hủy phiếu công việc"
            );
        }
        if (request.targetStatus() == WorkOrderStatus.REOPENED) {
            throw BusinessException.badRequest(
                    "WORK_ORDER_REOPEN_REASON_REQUIRED",
                    "Phải nhập lý do mở lại phiếu công việc"
            );
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
