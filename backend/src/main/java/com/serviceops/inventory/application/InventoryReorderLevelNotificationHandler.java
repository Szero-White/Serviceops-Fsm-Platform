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
public class InventoryReorderLevelNotificationHandler {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReorderLevelChanged(InventoryReorderLevelChangedEvent event) {
        if (!event.becameLowStock()) {
            return;
        }

        try {
            notificationService.notifyRolesIndependently(
                    event.tenantId(),
                    List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF),
                    event.actorUserId(),
                    "Cần kiểm tra tồn kho: " + event.sku(),
                    message(event)
            );
        } catch (RuntimeException ex) {
            log.warn("Could not deliver low-stock notification after reorder-level change for sparePartId={}",
                    event.sparePartId(), ex);
        }
    }

    private static String message(InventoryReorderLevelChangedEvent event) {
        return event.partName()
                + " hiện còn " + quantity(event.stockQuantity()) + " " + event.unit()
                + "; ngưỡng tồn tối thiểu mới là " + quantity(event.newReorderLevel()) + " " + event.unit()
                + ". Tồn hiện tại đã chạm hoặc thấp hơn ngưỡng mới; kiểm tra và bổ sung nếu cần.";
    }

    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
