package com.serviceops.notification.application;

import java.math.BigDecimal;

/** Inventory-specific bell copy. */
final class InventoryNotificationCopy {
    private InventoryNotificationCopy() {
    }

    static NotificationCopy.Copy partRequestCreated(
            String workOrderCode,
            String workOrderSummary,
            String sku,
            String partName,
            BigDecimal quantity,
            String unit,
            String technicianName
    ) {
        return NotificationCopy.copy(
                "Có yêu cầu phụ tùng mới: " + NotificationCopy.fallback(workOrderCode, "Phiếu công việc"),
                "Kỹ thuật viên " + NotificationCopy.fallback(technicianName, "được phân công")
                        + " cần " + NotificationCopy.quantity(quantity) + " " + NotificationCopy.fallback(unit, "") + " phụ tùng \""
                        + NotificationCopy.fallback(partName, sku) + "\" (" + NotificationCopy.fallback(sku, "Chưa có mã phụ tùng") + ") cho phiếu \""
                        + NotificationCopy.limit(NotificationCopy.fallback(workOrderSummary, "Nội dung chưa có tiêu đề"), NotificationCopy.CONTEXT_LIMIT) + "\" ("
                        + NotificationCopy.fallback(workOrderCode, "Phiếu công việc")
                        + "). Mở Yêu cầu phụ tùng để kiểm tra và xác nhận cấp khi đã giao hàng thực tế."
        );
    }

    static NotificationCopy.Copy lowStockAfterIssue(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel,
            String workOrderCode,
            String warehouseActorName
    ) {
        return NotificationCopy.copy(
                "Tồn kho thấp: " + sku,
                "Sau khi nhân viên kho " + NotificationCopy.fallback(warehouseActorName, "phụ trách")
                        + " xác nhận cấp phụ tùng cho " + NotificationCopy.fallback(workOrderCode, "phiếu công việc")
                        + ", phụ tùng \"" + NotificationCopy.fallback(partName, sku) + "\" (" + sku + ") còn "
                        + NotificationCopy.quantity(stockQuantity) + " " + unit + "; ngưỡng tồn tối thiểu là "
                        + NotificationCopy.quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }

    static NotificationCopy.Copy lowStockAfterReorderLevelChange(
            String sku,
            String partName,
            BigDecimal stockQuantity,
            String unit,
            BigDecimal reorderLevel,
            String actorDisplayName
    ) {
        return NotificationCopy.copy(
                "Tồn kho thấp theo ngưỡng mới: " + sku,
                NotificationCopy.fallback(actorDisplayName, "Người phụ trách")
                        + " vừa thay đổi ngưỡng tồn tối thiểu. Phụ tùng \""
                        + NotificationCopy.fallback(partName, sku) + "\" (" + sku + ") còn "
                        + NotificationCopy.quantity(stockQuantity) + " " + unit + "; ngưỡng mới là "
                        + NotificationCopy.quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }

    static NotificationCopy.Copy stocktakeDiscrepancy(
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
        String message = "Phụ tùng \"" + NotificationCopy.fallback(partName, sku) + "\" (" + sku + "): hệ thống "
                + NotificationCopy.quantity(systemQuantity) + " " + unit
                + ", kiểm kê thực tế " + NotificationCopy.quantity(actualQuantity) + " " + unit
                + " (chênh " + NotificationCopy.signedQuantity(difference) + " " + unit + "). "
                + "Người kiểm kê: " + NotificationCopy.fallback(actorDisplayName, "Không xác định") + "."
                + NotificationCopy.optionalReason(reason, "Lý do");
        if (lowStock) {
            message += " Tồn thực tế đang ở mức thấp.";
        }
        message += " Mở Lịch sử biến động để đối chiếu.";
        return NotificationCopy.copy("Kiểm kê có chênh lệch: " + sku, message);
    }

    static NotificationCopy.Copy lowStockAfterStocktake(
            String sku,
            String partName,
            BigDecimal actualQuantity,
            String unit,
            BigDecimal reorderLevel,
            String actorDisplayName
    ) {
        return NotificationCopy.copy(
                "Tồn kho thấp sau kiểm kê: " + sku,
                "Sau kiểm kê của " + NotificationCopy.fallback(actorDisplayName, "nhân viên kho")
                        + ", phụ tùng \"" + NotificationCopy.fallback(partName, sku) + "\" (" + sku + ") còn "
                        + NotificationCopy.quantity(actualQuantity) + " " + unit + "; ngưỡng tồn tối thiểu là "
                        + NotificationCopy.quantity(reorderLevel) + " " + unit
                        + ". Mở Kho phụ tùng để kiểm tra và bổ sung nếu cần."
        );
    }
}
