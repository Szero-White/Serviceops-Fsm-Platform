package com.serviceops.notification.application;

import java.math.BigDecimal;

/** Payment-handoff copy kept separate from general Work Order lifecycle copy. */
final class PaymentNotificationCopy {
    private PaymentNotificationCopy() {
    }

    static NotificationCopy.Copy transferPending(
            NotificationCopy.WorkOrderContext context,
            String technicianName,
            BigDecimal amount
    ) {
        return NotificationCopy.copy(
                "Cần đối soát chuyển khoản: " + context.code(),
                "Kỹ thuật viên " + NotificationCopy.fallback(technicianName, "được phân công")
                        + " ghi nhận khách đã chuyển " + NotificationCopy.money(amount) + " cho "
                        + NotificationCopy.workOrderContext(context) + ". "
                        + "Mở Xử lý thanh toán để kiểm tra tiền thực tế vào tài khoản công ty."
        );
    }

    static NotificationCopy.Copy cashHandoverPending(
            NotificationCopy.WorkOrderContext context,
            String technicianName,
            BigDecimal amount
    ) {
        return NotificationCopy.copy(
                "Cần nhận bàn giao tiền mặt: " + context.code(),
                "Kỹ thuật viên " + NotificationCopy.fallback(technicianName, "được phân công")
                        + " đang giữ " + NotificationCopy.money(amount) + " của "
                        + NotificationCopy.workOrderContext(context) + ". "
                        + "Mở Xử lý thanh toán để nhận bàn giao và đối soát."
        );
    }

    static NotificationCopy.Copy counterCollectionPending(
            NotificationCopy.WorkOrderContext context,
            String technicianName,
            BigDecimal amount
    ) {
        return NotificationCopy.copy(
                "Khách hẹn thanh toán tại quầy: " + context.code(),
                "Kỹ thuật viên " + NotificationCopy.fallback(technicianName, "được phân công")
                        + " ghi nhận khách chưa thanh toán tại hiện trường và sẽ thanh toán "
                        + NotificationCopy.money(amount) + " tại quầy cho "
                        + NotificationCopy.workOrderContext(context) + ". "
                        + "Mở Xử lý thanh toán khi khách đến để thu và đối soát."
        );
    }
}
