package com.serviceops.inventory.application;

import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
            var copy = NotificationCopy.stocktakeDiscrepancy(
                    event.sku(),
                    event.partName(),
                    event.systemQuantity(),
                    event.actualQuantity(),
                    event.difference(),
                    event.unit(),
                    event.actorDisplayName(),
                    event.reason(),
                    event.isLowStock()
            );
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.OWNER),
                    event.actorUserId(),
                    copy.title(),
                    copy.message()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver stocktake discrepancy notification for sparePartId={}", event.sparePartId(), ex);
        }
    }

    private void notifyWarehouse(InventoryStockAdjustedEvent event) {
        try {
            var copy = NotificationCopy.lowStockAfterStocktake(
                    event.sku(),
                    event.partName(),
                    event.actualQuantity(),
                    event.unit(),
                    event.reorderLevel(),
                    event.actorDisplayName()
            );
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.WAREHOUSE_STAFF),
                    null,
                    copy.title(),
                    copy.message()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver low-stock notification after stocktake for sparePartId={}", event.sparePartId(), ex);
        }
    }

}
