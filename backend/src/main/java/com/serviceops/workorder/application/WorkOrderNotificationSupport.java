package com.serviceops.workorder.application;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** User-facing Work Order notification orchestration kept separate from lifecycle persistence. */
final class WorkOrderNotificationSupport {
    private WorkOrderNotificationSupport() {
    }

    static void notifyNeedsDispatch(NotificationService notifications, WorkOrder workOrder) {
        NotificationCopy.Copy copy = NotificationCopy.workOrderNeedsDispatch(context(workOrder), currentActorLabel());
        notifications.notifyRoles(workOrder.getTenantId(), List.of(UserRole.DISPATCHER), copy.title(), copy.message());
    }

    static void notifyInitialAssignment(
            NotificationService notifications,
            UUID tenantId,
            TechnicianProfile technician,
            WorkOrder workOrder
    ) {
        create(notifications, tenantId, technician.getUser(),
                NotificationCopy.technicianAssigned(context(workOrder), currentActorLabel()));
    }

    static void notifyRedispatch(
            NotificationService notifications,
            UUID tenantId,
            WorkOrder workOrder,
            TechnicianProfile previousTechnician,
            TechnicianProfile technician,
            boolean technicianChanged,
            Instant previousStart,
            Instant previousEnd,
            Instant newStart,
            Instant newEnd,
            String reason
    ) {
        String actor = currentActorLabel();
        if (technicianChanged && previousTechnician != null) {
            create(notifications, tenantId, previousTechnician.getUser(),
                    NotificationCopy.technicianTransferredAway(context(workOrder), technician.getUser().getDisplayName(), actor));
            create(notifications, tenantId, technician.getUser(),
                    NotificationCopy.technicianTransferredTo(context(workOrder), actor));
            return;
        }
        create(notifications, tenantId, technician.getUser(),
                NotificationCopy.technicianScheduleChanged(
                        context(workOrder), actor, previousStart, previousEnd, newStart, newEnd, reason));
    }

    static void notifyStatusChange(
            NotificationService notifications,
            WorkOrder workOrder,
            String note,
            WorkOrderStatusHistory statusHistory
    ) {
        UUID tenantId = workOrder.getTenantId();
        NotificationCopy.WorkOrderContext context = context(workOrder);
        String actor = currentActorLabel();

        switch (workOrder.getStatus()) {
            case WAITING_FOR_PARTS -> notifyRoles(notifications, tenantId, List.of(UserRole.DISPATCHER),
                    NotificationCopy.workOrderWaitingForParts(context, assignedTechnicianName(workOrder), note));
            case REOPENED -> {
                notifyRoles(notifications, tenantId, List.of(UserRole.DISPATCHER),
                        NotificationCopy.workOrderReopenedAttention(context, actor, note));
                if (!CurrentUser.hasRole("CUSTOMER_SERVICE")) {
                    notifyRoles(notifications, tenantId, List.of(UserRole.CUSTOMER_SERVICE),
                            NotificationCopy.workOrderReopenedForCustomerService(context, actor, note));
                }
                notifyAssignedTechnician(notifications, workOrder,
                        NotificationCopy.workOrderReopenedForTechnician(context, actor, note));
            }
            case COMPLETED -> {
                NotificationCopy.Copy copy = NotificationCopy.workOrderCompletedForCustomerService(
                        context, assignedTechnicianName(workOrder));
                notifications.notifyRolesUnique(
                        tenantId,
                        List.of(UserRole.CUSTOMER_SERVICE),
                        completionNotificationEventKey(statusHistory),
                        copy.title(),
                        copy.message()
                );
            }
            case CLOSED -> {
                notifyRoles(notifications, tenantId, List.of(UserRole.OWNER),
                        NotificationCopy.workOrderClosedForOwner(context, actor));
                notifyAssignedTechnician(notifications, workOrder,
                        NotificationCopy.workOrderClosedForTechnician(context, actor));
            }
            case CANCELLED -> {
                notifyRoles(notifications, tenantId, List.of(UserRole.OWNER),
                        NotificationCopy.workOrderCancelledForOwner(context, actor, note));
                if (!CurrentUser.hasRole("CUSTOMER_SERVICE")) {
                    notifyRoles(notifications, tenantId, List.of(UserRole.CUSTOMER_SERVICE),
                            NotificationCopy.workOrderCancelledForCustomerService(context, actor, note));
                }
                notifyAssignedTechnician(notifications, workOrder,
                        NotificationCopy.workOrderCancelledForTechnician(context, actor, note));
            }
            case CUSTOMER_ACCEPTED, ON_THE_WAY, IN_PROGRESS -> {
                // Expected operational steps stay in the Work Order timeline instead of the bell.
            }
            default -> {
                // Scheduling/assignment has dedicated notifications.
            }
        }
    }

    static String currentActorLabel() {
        String role = CurrentUser.primaryRole();
        String roleLabel = switch (role == null ? "" : role) {
            case "OWNER" -> "Chủ sở hữu";
            case "DISPATCHER" -> "Điều phối viên";
            case "CUSTOMER_SERVICE" -> "Chăm sóc khách hàng";
            case "TECHNICIAN" -> "Kỹ thuật viên";
            case "WAREHOUSE_STAFF" -> "Nhân viên kho";
            default -> "Người dùng";
        };
        return roleLabel + " " + CurrentUser.displayName();
    }

    private static NotificationCopy.WorkOrderContext context(WorkOrder workOrder) {
        String customerName = workOrder.getCustomer() == null ? null : workOrder.getCustomer().getName();
        return new NotificationCopy.WorkOrderContext(workOrder.getCode(), workOrder.getSummary(), customerName);
    }

    private static String assignedTechnicianName(WorkOrder workOrder) {
        if (workOrder.getTechnician() == null || workOrder.getTechnician().getUser() == null) {
            return null;
        }
        return workOrder.getTechnician().getUser().getDisplayName();
    }

    private static String completionNotificationEventKey(WorkOrderStatusHistory statusHistory) {
        if (statusHistory == null
                || statusHistory.getToStatus() != WorkOrderStatus.COMPLETED
                || statusHistory.getId() == null) {
            throw new IllegalStateException("Completed work order notification requires persisted completion history");
        }
        return "WORK_ORDER_COMPLETED:" + statusHistory.getId();
    }

    private static void notifyAssignedTechnician(
            NotificationService notifications,
            WorkOrder workOrder,
            NotificationCopy.Copy copy
    ) {
        if (workOrder.getTechnician() == null
                || CurrentUser.userId().equals(workOrder.getTechnician().getUser().getId())) {
            return;
        }
        create(notifications, workOrder.getTenantId(), workOrder.getTechnician().getUser(), copy);
    }

    private static void notifyRoles(
            NotificationService notifications,
            UUID tenantId,
            List<UserRole> roles,
            NotificationCopy.Copy copy
    ) {
        notifications.notifyRoles(tenantId, roles, copy.title(), copy.message());
    }

    private static void create(
            NotificationService notifications,
            UUID tenantId,
            UserAccount recipient,
            NotificationCopy.Copy copy
    ) {
        notifications.create(tenantId, recipient, copy.title(), copy.message());
    }
}
