package com.serviceops.notification.application;

import java.time.Instant;

/** Dispatch, assignment and overdue notification copy. */
final class WorkOrderDispatchNotificationCopy {
    private WorkOrderDispatchNotificationCopy() {
    }

    static NotificationCopy.Copy workOrderNeedsDispatch(NotificationCopy.WorkOrderContext context, String actorLabel) {
        return NotificationCopy.copy(
                "Cần phân công kỹ thuật viên: " + context.code(),
                NotificationCopy.actor(actorLabel) + " đã chuyển " + NotificationCopy.workOrderContext(context)
                        + " sang bộ phận điều phối. Mở Lịch điều phối để chọn kỹ thuật viên và thời gian thực hiện."
        );
    }


    static NotificationCopy.Copy technicianAssigned(NotificationCopy.WorkOrderContext context, String actorLabel) {
        return NotificationCopy.copy(
                "Bạn có công việc mới: " + context.code(),
                NotificationCopy.actor(actorLabel) + " đã giao cho bạn " + NotificationCopy.workOrderContext(context)
                        + ". Mở Lịch của tôi để xem lịch và bắt đầu công việc."
        );
    }


    static NotificationCopy.Copy technicianTransferredAway(
            NotificationCopy.WorkOrderContext context,
            String newTechnicianName,
            String actorLabel
    ) {
        return NotificationCopy.copy(
                "Bạn không còn phụ trách: " + context.code(),
                NotificationCopy.actor(actorLabel) + " đã chuyển " + NotificationCopy.workOrderContext(context)
                        + " cho kỹ thuật viên " + NotificationCopy.fallback(newTechnicianName, "khác")
                        + ". Bạn không cần tiếp tục phiếu này; kiểm tra Lịch của tôi để cập nhật kế hoạch."
        );
    }


    static NotificationCopy.Copy technicianTransferredTo(NotificationCopy.WorkOrderContext context, String actorLabel) {
        return NotificationCopy.copy(
                "Bạn có công việc mới: " + context.code(),
                NotificationCopy.actor(actorLabel) + " đã chuyển cho bạn " + NotificationCopy.workOrderContext(context)
                        + ". Mở Lịch của tôi để xem lịch mới và nội dung công việc."
        );
    }


    static NotificationCopy.Copy technicianScheduleChanged(
            NotificationCopy.WorkOrderContext context,
            String actorLabel,
            Instant previousStart,
            Instant previousEnd,
            Instant newStart,
            Instant newEnd,
            String reason
    ) {
        String message = NotificationCopy.limit(NotificationCopy.actor(actorLabel), NotificationCopy.ACTOR_LIMIT)
                + " đã đổi lịch " + context.code()
                + " - \"" + NotificationCopy.limit(context.summary(), NotificationCopy.RESCHEDULE_CONTEXT_LIMIT) + "\" của khách "
                + NotificationCopy.limit(context.customerName(), NotificationCopy.RESCHEDULE_CONTEXT_LIMIT) + ". "
                + "Lịch cũ: " + NotificationCopy.scheduleRange(previousStart, previousEnd) + ". "
                + "Lịch mới: " + NotificationCopy.scheduleRange(newStart, newEnd) + "."
                + NotificationCopy.optionalReason(reason, "Lý do", NotificationCopy.RESCHEDULE_REASON_LIMIT)
                + " Mở Lịch của tôi để xem lịch mới.";
        return NotificationCopy.copy("Lịch của bạn đã thay đổi: " + context.code(), message);
    }


    static NotificationCopy.Copy workOrderOverdueForDispatcher(
            NotificationCopy.WorkOrderContext context,
            String technicianName,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return NotificationCopy.copy(
                "Phiếu đã quá lịch thực hiện: " + context.code(),
                NotificationCopy.workOrderContext(context) + " đã quá lịch " + NotificationCopy.scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng công việc chưa bắt đầu. Kỹ thuật viên: "
                        + NotificationCopy.fallback(technicianName, "chưa xác định")
                        + ". Mở Lịch điều phối để kiểm tra và điều chỉnh lịch."
        );
    }


    static NotificationCopy.Copy workOrderOverdueForTechnician(
            NotificationCopy.WorkOrderContext context,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return NotificationCopy.copy(
                "Công việc đã quá lịch: " + context.code(),
                NotificationCopy.workOrderContext(context) + " đã quá lịch " + NotificationCopy.scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng chưa được bắt đầu. Mở Lịch của tôi để kiểm tra và liên hệ điều phối nếu cần đổi lịch."
        );
    }


    static NotificationCopy.Copy workOrderOverdueForCustomerService(
            NotificationCopy.WorkOrderContext context,
            String technicianName,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return NotificationCopy.copy(
                "Khách hàng có thể cần được liên hệ: " + context.code(),
                NotificationCopy.workOrderContext(context) + " đã quá lịch hẹn " + NotificationCopy.scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng kỹ thuật viên chưa bắt đầu công việc. Kỹ thuật viên: "
                        + NotificationCopy.fallback(technicianName, "chưa xác định")
                        + ". Mở Phiếu công việc để kiểm tra tình trạng và chủ động liên hệ khách hàng nếu cần."
        );
    }

}
