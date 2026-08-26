package com.serviceops.notification.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
    private static final int CONTEXT_LIMIT = 96;
    private static final int REASON_LIMIT = 160;
    private static final int RESCHEDULE_CONTEXT_LIMIT = 64;
    private static final int RESCHEDULE_REASON_LIMIT = 120;
    private static final int ACTOR_LIMIT = 72;
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
        return copy(
                "Cần phân công kỹ thuật viên: " + context.code(),
                actor(actorLabel) + " đã chuyển " + workOrderContext(context)
                        + " sang bộ phận điều phối. Mở Lịch điều phối để chọn kỹ thuật viên và thời gian thực hiện."
        );
    }

    public static Copy technicianAssigned(WorkOrderContext context, String actorLabel) {
        return copy(
                "Bạn có công việc mới: " + context.code(),
                actor(actorLabel) + " đã giao cho bạn " + workOrderContext(context)
                        + ". Mở Lịch của tôi để xem lịch và bắt đầu công việc."
        );
    }

    public static Copy technicianTransferredAway(
            WorkOrderContext context,
            String newTechnicianName,
            String actorLabel
    ) {
        return copy(
                "Bạn không còn phụ trách: " + context.code(),
                actor(actorLabel) + " đã chuyển " + workOrderContext(context)
                        + " cho kỹ thuật viên " + fallback(newTechnicianName, "khác")
                        + ". Bạn không cần tiếp tục phiếu này; kiểm tra Lịch của tôi để cập nhật kế hoạch."
        );
    }

    public static Copy technicianTransferredTo(WorkOrderContext context, String actorLabel) {
        return copy(
                "Bạn có công việc mới: " + context.code(),
                actor(actorLabel) + " đã chuyển cho bạn " + workOrderContext(context)
                        + ". Mở Lịch của tôi để xem lịch mới và nội dung công việc."
        );
    }

    public static Copy technicianScheduleChanged(
            WorkOrderContext context,
            String actorLabel,
            Instant previousStart,
            Instant previousEnd,
            Instant newStart,
            Instant newEnd,
            String reason
    ) {
        String message = limit(actor(actorLabel), ACTOR_LIMIT)
                + " đã đổi lịch " + context.code()
                + " - \"" + limit(context.summary(), RESCHEDULE_CONTEXT_LIMIT) + "\" của khách "
                + limit(context.customerName(), RESCHEDULE_CONTEXT_LIMIT) + ". "
                + "Lịch cũ: " + scheduleRange(previousStart, previousEnd) + ". "
                + "Lịch mới: " + scheduleRange(newStart, newEnd) + "."
                + optionalReason(reason, "Lý do", RESCHEDULE_REASON_LIMIT)
                + " Mở Lịch của tôi để xem lịch mới.";
        return copy("Lịch của bạn đã thay đổi: " + context.code(), message);
    }

    public static Copy workOrderOverdueForDispatcher(
            WorkOrderContext context,
            String technicianName,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return copy(
                "Phiếu đã quá lịch thực hiện: " + context.code(),
                workOrderContext(context) + " đã quá lịch " + scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng công việc chưa bắt đầu. Kỹ thuật viên: "
                        + fallback(technicianName, "chưa xác định")
                        + ". Mở Lịch điều phối để kiểm tra và điều chỉnh lịch."
        );
    }

    public static Copy workOrderOverdueForTechnician(
            WorkOrderContext context,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return copy(
                "Công việc đã quá lịch: " + context.code(),
                workOrderContext(context) + " đã quá lịch " + scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng chưa được bắt đầu. Mở Lịch của tôi để kiểm tra và liên hệ điều phối nếu cần đổi lịch."
        );
    }

    public static Copy workOrderOverdueForCustomerService(
            WorkOrderContext context,
            String technicianName,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        return copy(
                "Khách hàng có thể cần được liên hệ: " + context.code(),
                workOrderContext(context) + " đã quá lịch hẹn " + scheduleRange(scheduledStart, scheduledEnd)
                        + " nhưng kỹ thuật viên chưa bắt đầu công việc. Kỹ thuật viên: "
                        + fallback(technicianName, "chưa xác định")
                        + ". Mở Phiếu công việc để kiểm tra tình trạng và chủ động liên hệ khách hàng nếu cần."
        );
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

    public static Copy workOrderClosedForOwner(WorkOrderContext context, String actorLabel) {
        return copy(
                "Phiếu đã hoàn tất: " + context.code(),
                actor(actorLabel) + " đã đóng " + workOrderContext(context)
                        + " sau khi khách xác nhận. Phiếu đã hoàn tất toàn bộ quy trình; mở Lịch sử phiếu nếu cần đối soát."
        );
    }

    public static Copy workOrderClosedForTechnician(WorkOrderContext context, String actorLabel) {
        return copy(
                "Phiếu đã đóng: " + context.code(),
                actor(actorLabel) + " đã đóng " + workOrderContext(context)
                        + " sau khi khách xác nhận. Công việc đã kết thúc; bạn không cần thao tác thêm."
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
                        + " Mở Phiếu công việc để kiểm tra và liên hệ khách hàng nếu cần."
        );
    }

    public static Copy lowStock(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel,
            String workOrderCode,
            String technicianName
    ) {
        return copy(
                "Tồn kho thấp: " + sku,
                "Sau khi kỹ thuật viên " + fallback(technicianName, "được phân công")
                        + " ghi nhận sử dụng cho " + fallback(workOrderCode, "phiếu công việc")
                        + ", phụ tùng \"" + fallback(partName, sku) + "\" (" + sku + ") còn "
                        + quantity(stockQuantity) + " " + unit + "; ngưỡng tồn tối thiểu là "
                        + quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }

    public static Copy lowStockAfterReorderLevelChange(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel,
            String actorDisplayName
    ) {
        return copy(
                "Tồn kho thấp theo ngưỡng mới: " + sku,
                fallback(actorDisplayName, "Người phụ trách") + " vừa thay đổi ngưỡng tồn tối thiểu. Phụ tùng \""
                        + fallback(partName, sku) + "\" (" + sku + ") còn "
                        + quantity(stockQuantity) + " " + unit + "; ngưỡng mới là "
                        + quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }

    public static Copy stocktakeDiscrepancy(
            String sku,
            String partName,
            BigDecimal systemQuantity,
            BigDecimal actualQuantity,
            BigDecimal difference,
            String unit,
            String actorDisplayName,
            String reason,
            boolean lowStock
    ) {
        String message = "Phụ tùng \"" + fallback(partName, sku) + "\" (" + sku + "): hệ thống "
                + quantity(systemQuantity) + " " + unit
                + ", kiểm kê thực tế " + quantity(actualQuantity) + " " + unit
                + " (chênh " + signedQuantity(difference) + " " + unit + "). "
                + "Người kiểm kê: " + fallback(actorDisplayName, "Không xác định") + "."
                + optionalReason(reason, "Lý do");
        if (lowStock) {
            message += " Tồn thực tế đang ở mức thấp.";
        }
        message += " Mở Lịch sử biến động để đối chiếu.";
        return copy("Kiểm kê có chênh lệch: " + sku, message);
    }

    public static Copy lowStockAfterStocktake(
            String sku,
            String partName,
            BigDecimal actualQuantity,
            String unit,
            BigDecimal reorderLevel,
            String actorDisplayName
    ) {
        return copy(
                "Tồn kho thấp sau kiểm kê: " + sku,
                "Sau kiểm kê của " + fallback(actorDisplayName, "nhân viên kho")
                        + ", phụ tùng \"" + fallback(partName, sku) + "\" (" + sku + ") còn "
                        + quantity(actualQuantity) + " " + unit + "; ngưỡng tồn tối thiểu là "
                        + quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }

    private static Copy copy(String title, String message) {
        return new Copy(title, message);
    }

    private static String workOrderContext(WorkOrderContext context) {
        return "phiếu \"" + limit(context.summary(), CONTEXT_LIMIT) + "\" (" + context.code() + ") của khách "
                + limit(context.customerName(), CONTEXT_LIMIT);
    }

    private static String actor(String actorLabel) {
        return fallback(actorLabel, "Người phụ trách");
    }

    private static String optionalReason(String reason, String label) {
        return optionalReason(reason, label, REASON_LIMIT);
    }

    private static String optionalReason(String reason, String label, int maxLength) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " " + label + ": " + limit(normalize(reason), maxLength) + ".";
    }

    private static String scheduleRange(Instant start, Instant end) {
        if (start == null || end == null) {
            return "chưa xác định";
        }
        String startLabel = SCHEDULE_DATE_TIME.format(start);
        String endLabel = start.atZone(BUSINESS_ZONE).toLocalDate().equals(end.atZone(BUSINESS_ZONE).toLocalDate())
                ? SCHEDULE_TIME.format(end)
                : SCHEDULE_DATE_TIME.format(end);
        return startLabel + "–" + endLabel;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : normalize(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3).trim() + "...";
    }

    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signedQuantity(BigDecimal value) {
        String quantity = quantity(value);
        return value.signum() > 0 ? "+" + quantity : quantity;
    }
}
