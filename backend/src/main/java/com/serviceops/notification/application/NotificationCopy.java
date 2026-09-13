package com.serviceops.notification.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Central user-facing notification copy.
 *
 * Bell notifications are reserved for cross-role events that require awareness or a next action.
 * The title answers "what happened / what needs attention"; the message adds business context
 * (who / which customer / which work order) and tells the recipient what to do next.
 *
 * Internal enum names, raw timestamps, test identifiers and audit details belong in Timeline/Audit,
 * not in persistent notification copy.
 */
public final class NotificationCopy {
    private static final int TITLE_LIMIT = 180;
    private static final int MESSAGE_LIMIT = 500;
    static final int CONTEXT_LIMIT = 96;
    private static final int REASON_LIMIT = 160;
    static final int RESCHEDULE_CONTEXT_LIMIT = 64;
    static final int RESCHEDULE_REASON_LIMIT = 120;
    static final int ACTOR_LIMIT = 72;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SCHEDULE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(BUSINESS_ZONE);
    private static final DateTimeFormatter SCHEDULE_TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BUSINESS_ZONE);

    private NotificationCopy() {
    }

    public record Copy(String title, String message) {
        public Copy {
            title = limit(normalize(title), TITLE_LIMIT);
            message = limit(normalize(message), MESSAGE_LIMIT);
        }
    }

    /**
     * Lightweight read model used only to compose notification copy. Keeping it in the notification
     * package avoids coupling NotificationCopy to the Work Order domain entity.
     */
    public record WorkOrderContext(String code, String summary, String customerName) {
        public WorkOrderContext {
            code = fallback(code, "Phiếu công việc");
            summary = fallback(summary, "Nội dung chưa có tiêu đề");
            customerName = fallback(customerName, "Khách hàng chưa xác định");
        }
    }

    public static Copy workOrderNeedsDispatch(WorkOrderContext context, String actorLabel) {
        return WorkOrderDispatchNotificationCopy.workOrderNeedsDispatch(context, actorLabel);
    }

    public static Copy technicianAssigned(WorkOrderContext context, String actorLabel) {
        return WorkOrderDispatchNotificationCopy.technicianAssigned(context, actorLabel);
    }

    public static Copy technicianTransferredAway(WorkOrderContext context, String newTechnicianName, String actorLabel) {
        return WorkOrderDispatchNotificationCopy.technicianTransferredAway(context, newTechnicianName, actorLabel);
    }

    public static Copy technicianTransferredTo(WorkOrderContext context, String actorLabel) {
        return WorkOrderDispatchNotificationCopy.technicianTransferredTo(context, actorLabel);
    }

    public static Copy technicianScheduleChanged(
            WorkOrderContext context, String actorLabel, Instant previousStart, Instant previousEnd,
            Instant newStart, Instant newEnd, String reason
    ) {
        return WorkOrderDispatchNotificationCopy.technicianScheduleChanged(
                context, actorLabel, previousStart, previousEnd, newStart, newEnd, reason);
    }

    public static Copy workOrderOverdueForDispatcher(
            WorkOrderContext context, String technicianName, Instant scheduledStart, Instant scheduledEnd
    ) {
        return WorkOrderDispatchNotificationCopy.workOrderOverdueForDispatcher(
                context, technicianName, scheduledStart, scheduledEnd);
    }

    public static Copy workOrderOverdueForTechnician(
            WorkOrderContext context, Instant scheduledStart, Instant scheduledEnd
    ) {
        return WorkOrderDispatchNotificationCopy.workOrderOverdueForTechnician(context, scheduledStart, scheduledEnd);
    }

    public static Copy workOrderOverdueForCustomerService(
            WorkOrderContext context, String technicianName, Instant scheduledStart, Instant scheduledEnd
    ) {
        return WorkOrderDispatchNotificationCopy.workOrderOverdueForCustomerService(
                context, technicianName, scheduledStart, scheduledEnd);
    }

    public static Copy workOrderWaitingForParts(
            WorkOrderContext context,
            String technicianName,
            String note
    ) {
        String detail = optionalReason(note, "Ghi chú kỹ thuật viên");
        return copy(
                "Phiếu đang chờ phụ tùng: " + context.code(),
                "Kỹ thuật viên " + fallback(technicianName, "được phân công")
                        + " đang tạm dừng " + workOrderContext(context) + " vì chờ phụ tùng."
                        + detail + " Mở phiếu để xem tình trạng và phối hợp xử lý."
        );
    }

    public static Copy workOrderReopenedAttention(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Phiếu cần xử lý lại: " + context.code(),
                actor(actorLabel) + " đã mở lại " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Mở phiếu để xem tình trạng và điều phối bước tiếp theo."
        );
    }

    public static Copy workOrderReopenedForTechnician(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Công việc cần xử lý lại: " + context.code(),
                actor(actorLabel) + " đã mở lại " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Mở phiếu để xem tình trạng và tiếp tục theo phân công."
        );
    }

    public static Copy workOrderReopenedForCustomerService(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Phiếu cần theo dõi lại: " + context.code(),
                actor(actorLabel) + " đã mở lại " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Mở Phiếu công việc để theo dõi khách hàng và phối hợp xử lý."
        );
    }

    public static Copy workOrderCompletedForCustomerService(
            WorkOrderContext context,
            String technicianName
    ) {
        return copy(
                "Cần theo dõi khách sau sửa chữa: " + context.code(),
                "Kỹ thuật viên " + fallback(technicianName, "được phân công")
                        + " đã hoàn thành " + workOrderContext(context) + ". "
                        + "Theo dõi phản hồi khách hàng; nếu sự cố còn, mở lại phiếu theo quy trình."
        );
    }

    public static Copy paymentTransferPending(WorkOrderContext context, String technicianName, BigDecimal amount) {
        return PaymentNotificationCopy.transferPending(context, technicianName, amount);
    }

    public static Copy paymentCashHandoverPending(WorkOrderContext context, String technicianName, BigDecimal amount) {
        return PaymentNotificationCopy.cashHandoverPending(context, technicianName, amount);
    }

    public static Copy paymentCounterCollectionPending(WorkOrderContext context, String technicianName, BigDecimal amount) {
        return PaymentNotificationCopy.counterCollectionPending(context, technicianName, amount);
    }

    public static Copy workOrderClosedForOwner(WorkOrderContext context, String actorLabel) {
        return copy(
                "Phiếu đã hoàn tất: " + context.code(),
                actor(actorLabel) + " đã đóng " + workOrderContext(context)
                        + " sau khi thanh toán đã được đối soát và biên nhận đã được phát hành. "
                        + "Phiếu đã hoàn tất toàn bộ quy trình; mở Lịch sử phiếu nếu cần đối soát."
        );
    }

    public static Copy workOrderClosedForTechnician(WorkOrderContext context, String actorLabel) {
        return copy(
                "Phiếu đã đóng: " + context.code(),
                actor(actorLabel) + " đã đóng " + workOrderContext(context)
                        + " sau khi CSKH đối soát thanh toán. Công việc đã kết thúc; bạn không cần thao tác thêm."
        );
    }

    public static Copy workOrderCancelledForOwner(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Phiếu đã hủy: " + context.code(),
                actor(actorLabel) + " đã hủy " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Mở Lịch sử phiếu nếu cần kiểm tra chi tiết."
        );
    }

    public static Copy workOrderCancelledForTechnician(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Công việc đã hủy: " + context.code(),
                actor(actorLabel) + " đã hủy " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Bạn dừng công việc này và kiểm tra Lịch của tôi để cập nhật kế hoạch."
        );
    }

    public static Copy workOrderCancelledForCustomerService(
            WorkOrderContext context,
            String actorLabel,
            String reason
    ) {
        return copy(
                "Phiếu đã hủy, cần cập nhật khách hàng: " + context.code(),
                actor(actorLabel) + " đã hủy " + workOrderContext(context) + "."
                        + optionalReason(reason, "Lý do")
                        + " Mở Lịch sử phiếu để kiểm tra chi tiết và liên hệ khách hàng nếu cần."
        );
    }

    public static Copy partRequestCreated(
            String workOrderCode, String workOrderSummary, String sku, String partName,
            BigDecimal quantity, String unit, String technicianName
    ) {
        return InventoryNotificationCopy.partRequestCreated(
                workOrderCode, workOrderSummary, sku, partName, quantity, unit, technicianName);
    }

    public static Copy lowStockAfterIssue(
            String sku, String partName, BigDecimal stockQuantity, String unit,
            BigDecimal reorderLevel, String workOrderCode, String warehouseActorName
    ) {
        return InventoryNotificationCopy.lowStockAfterIssue(
                sku, partName, stockQuantity, unit, reorderLevel, workOrderCode, warehouseActorName);
    }

    public static Copy lowStockAfterReorderLevelChange(
            String sku, String partName, BigDecimal stockQuantity, String unit,
            BigDecimal reorderLevel, String actorDisplayName
    ) {
        return InventoryNotificationCopy.lowStockAfterReorderLevelChange(
                sku, partName, stockQuantity, unit, reorderLevel, actorDisplayName);
    }

    public static Copy stocktakeDiscrepancy(
            String sku, String partName, BigDecimal systemQuantity, BigDecimal actualQuantity,
            BigDecimal difference, String unit, String actorDisplayName, String reason, boolean lowStock
    ) {
        return InventoryNotificationCopy.stocktakeDiscrepancy(
                sku, partName, systemQuantity, actualQuantity, difference, unit, actorDisplayName, reason, lowStock);
    }

    public static Copy lowStockAfterStocktake(
            String sku, String partName, BigDecimal actualQuantity, String unit,
            BigDecimal reorderLevel, String actorDisplayName
    ) {
        return InventoryNotificationCopy.lowStockAfterStocktake(
                sku, partName, actualQuantity, unit, reorderLevel, actorDisplayName);
    }

    static Copy copy(String title, String message) {
        return new Copy(title, message);
    }

    static String workOrderContext(WorkOrderContext context) {
        return "phiếu \"" + limit(context.summary(), CONTEXT_LIMIT) + "\" (" + context.code() + ") của khách "
                + limit(context.customerName(), CONTEXT_LIMIT);
    }

    static String actor(String actorLabel) {
        return fallback(actorLabel, "Người phụ trách");
    }

    static String optionalReason(String reason, String label) {
        return optionalReason(reason, label, REASON_LIMIT);
    }

    static String optionalReason(String reason, String label, int maxLength) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " " + label + ": " + limit(normalize(reason), maxLength) + ".";
    }

    static String scheduleRange(Instant start, Instant end) {
        if (start == null || end == null) {
            return "chưa xác định";
        }
        String startLabel = SCHEDULE_DATE_TIME.format(start);
        String endLabel = start.atZone(BUSINESS_ZONE).toLocalDate().equals(end.atZone(BUSINESS_ZONE).toLocalDate())
                ? SCHEDULE_TIME.format(end)
                : SCHEDULE_DATE_TIME.format(end);
        return startLabel + "–" + endLabel;
    }

    static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : normalize(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3).trim() + "...";
    }

    static String money(BigDecimal value) {
        if (value == null) {
            return "số tiền chưa xác định";
        }
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(value) + " đ";
    }

    static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    static String signedQuantity(BigDecimal value) {
        String quantity = quantity(value);
        return value.signum() > 0 ? "+" + quantity : quantity;
    }
}
