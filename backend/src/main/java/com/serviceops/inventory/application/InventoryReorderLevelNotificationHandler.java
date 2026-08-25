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
public class InventoryReorderLevelNotificationHandler {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReorderLevelChanged(InventoryReorderLevelChangedEvent event) {
        if (!event.becameLowStock()) {
            return;
        }

        try {
            var copy = NotificationCopy.lowStockAfterReorderLevelChange(
                    event.sku(),
                    event.partName(),
                    event.stockQuantity(),
                    event.unit(),
                    event.newReorderLevel()
            );
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF),
                    event.actorUserId(),
                    copy.title(),
                    copy.message()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver low-stock notification after reorder-level change for sparePartId={}",
                    event.sparePartId(), ex);
        }
    }

}
