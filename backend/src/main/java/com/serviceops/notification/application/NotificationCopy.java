package com.serviceops.notification.application;

import java.math.BigDecimal;

/**
 * Central user-facing notification copy.
 *
 * Keep notification text concise and action-oriented. Internal enum names, raw timestamps,
 * test identifiers and audit details belong in the domain/audit layers, not in the bell UI.
 */
public final class NotificationCopy {
    private NotificationCopy() {
    }

    public record Copy(String title, String message) {
    }

    public static Copy workOrderNeedsDispatch(String code) {
        return new Copy(
                "Phiếu mới chờ điều phối: " + code,
                "Phiếu công việc mới đã sẵn sàng. Mở Lịch điều phối để phân công kỹ thuật viên."
        );
    }

    public static Copy technicianAssigned(String code, String actorLabel) {
        return new Copy(
                "Bạn được phân công: " + code,
                actorLabel + " đã phân công phiếu cho bạn. Mở Lịch của tôi để xem lịch và nội dung công việc."
        );
    }

    public static Copy technicianTransferredAway(String code, String newTechnicianName) {
        return new Copy(
                "Bạn không còn được phân công: " + code,
                "Phiếu đã được chuyển cho " + newTechnicianName + ". Kiểm tra Lịch của tôi để cập nhật kế hoạch."
        );
    }

    public static Copy technicianTransferredTo(String code, String actorLabel) {
        return new Copy(
                "Bạn được phân công: " + code,
                actorLabel + " đã chuyển phiếu cho bạn. Mở Lịch của tôi để xem lịch mới và nội dung công việc."
        );
    }

    public static Copy technicianScheduleChanged(String code, String actorLabel) {
        return new Copy(
                "Lịch làm việc đã thay đổi: " + code,
                actorLabel + " đã cập nhật thời gian thực hiện. Mở Lịch của tôi để xem lịch mới."
        );
    }

    public static Copy workOrderWaitingForParts(String code) {
        return new Copy(
                "Phiếu đang chờ phụ tùng: " + code,
                "Kỹ thuật viên tạm dừng vì thiếu vật tư. Kiểm tra phiếu và phối hợp với kho để tiếp tục xử lý."
        );
    }

    public static Copy workOrderReopenedAttention(String code) {
        return new Copy(
                "Phiếu cần xử lý lại: " + code,
                "Phiếu đã được mở lại vì cần xử lý tiếp. Kiểm tra lý do và sắp xếp xử lý phù hợp."
        );
    }

    public static Copy workOrderReopenedForTechnician(String code) {
        return new Copy(
                "Phiếu được mở lại: " + code,
                "Phiếu cần tiếp tục xử lý. Mở phiếu để xem lý do và cập nhật tiến độ theo phân công."
        );
    }

    public static Copy workOrderCompletedForCustomerService(String code) {
        return new Copy(
                "Phiếu đã hoàn thành: " + code,
                "Kỹ thuật viên đã hoàn thành xử lý. Theo dõi phản hồi khách hàng; nếu sự cố còn, mở lại phiếu theo quy trình."
        );
    }

    public static Copy workOrderClosedForTechnician(String code) {
        return new Copy(
                "Phiếu đã đóng: " + code,
                "Khách đã xác nhận kết quả và phiếu đã đóng. Bạn không cần thao tác thêm trên công việc này."
        );
    }

    public static Copy workOrderCancelledForOwner(String code) {
        return new Copy(
                "Phiếu đã hủy: " + code,
                "Phiếu đã được hủy. Mở Lịch sử phiếu để xem lý do và người thực hiện khi cần."
        );
    }

    public static Copy workOrderCancelledForTechnician(String code) {
        return new Copy(
                "Phiếu đã hủy: " + code,
                "Bạn không cần tiếp tục công việc này. Kiểm tra Lịch của tôi để cập nhật kế hoạch."
        );
    }

    public static Copy lowStock(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel
    ) {
        return new Copy(
                "Tồn kho thấp: " + sku,
                partName + " còn " + quantity(stockQuantity) + " " + unit
                        + "; ngưỡng cảnh báo là " + quantity(reorderLevel) + " " + unit
                        + ". Kiểm tra và bổ sung tồn kho."
        );
    }

    public static Copy lowStockAfterReorderLevelChange(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel
    ) {
        return new Copy(
                "Tồn kho thấp: " + sku,
                "Ngưỡng cảnh báo vừa được cập nhật. " + partName + " hiện còn "
                        + quantity(stockQuantity) + " " + unit + "; ngưỡng mới là "
                        + quantity(reorderLevel) + " " + unit + ". Kiểm tra và bổ sung tồn kho."
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
        String message = partName
                + ": hệ thống " + quantity(systemQuantity) + " " + unit
                + ", thực tế " + quantity(actualQuantity) + " " + unit
                + " (chênh " + signedQuantity(difference) + " " + unit + "). "
                + "Người kiểm kê: " + actorDisplayName + ". Lý do: " + reason + ".";
        if (lowStock) {
            message += " Tồn thực tế đang ở mức thấp.";
        }
        return new Copy("Kiểm kê có chênh lệch: " + sku, message);
    }

    public static Copy lowStockAfterStocktake(
            String sku,
            String partName,
            BigDecimal actualQuantity,
            String unit,
            BigDecimal reorderLevel
    ) {
        return new Copy(
                "Tồn kho thấp: " + sku,
                "Sau kiểm kê, " + partName + " còn " + quantity(actualQuantity) + " " + unit
                        + "; ngưỡng cảnh báo là " + quantity(reorderLevel) + " " + unit
                        + ". Kiểm tra và bổ sung tồn kho."
        );
    }


    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signedQuantity(BigDecimal value) {
        String quantity = quantity(value);
        return value.signum() > 0 ? "+" + quantity : quantity;
    }
}
