package com.serviceops.inventory.application;

import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryStockAdjustmentNotificationHandler {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockAdjusted(InventoryStockAdjustedEvent event) {
        notifyOwners(event);
        if (event.isLowStock()) {
            notifyWarehouse(event);
        }
    }

    private void notifyOwners(InventoryStockAdjustedEvent event) {
        try {
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.OWNER),
                    event.actorUserId(),
                    "Kiểm kê có chênh lệch: " + event.sku(),
                    ownerMessage(event)
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver stocktake discrepancy notification for sparePartId={}", event.sparePartId(), ex);
        }
    }

    private void notifyWarehouse(InventoryStockAdjustedEvent event) {
        try {
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.WAREHOUSE_STAFF),
                    null,
                    "Cần bổ sung tồn kho sau kiểm kê: " + event.sku(),
                    lowStockMessage(event)
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver low-stock notification after stocktake for sparePartId={}", event.sparePartId(), ex);
        }
    }

    private static String ownerMessage(InventoryStockAdjustedEvent event) {
        String message = event.partName()
                + ": tồn hệ thống " + quantity(event.systemQuantity()) + " " + event.unit()
                + ", thực tế " + quantity(event.actualQuantity()) + " " + event.unit()
                + " (chênh " + signedQuantity(event.difference()) + " " + event.unit() + ")."
                + " Người kiểm kê: " + event.actorDisplayName()
                + ". Lý do: " + event.reason() + ".";
        return event.isLowStock()
                ? message + " Tồn thực tế cũng đang ở mức thấp."
                : message;
    }

    private static String lowStockMessage(InventoryStockAdjustedEvent event) {
        return event.partName() + " còn " + quantity(event.actualQuantity()) + " " + event.unit()
                + "; ngưỡng tồn tối thiểu là " + quantity(event.reorderLevel()) + " " + event.unit()
                + ". Kiểm tra và bổ sung tồn kho nếu cần.";
    }

    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signedQuantity(BigDecimal value) {
        String quantity = quantity(value);
        return value.signum() > 0 ? "+" + quantity : quantity;
    }
}
