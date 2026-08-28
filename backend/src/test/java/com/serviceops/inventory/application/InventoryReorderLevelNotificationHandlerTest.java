package com.serviceops.inventory.application;

import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryReorderLevelNotificationHandlerTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private NotificationService notificationService;

    @Test
    void thresholdCrossingIntoLowStockNotifiesWarehouseWithoutEchoingActor() {
        var handler = new InventoryReorderLevelNotificationHandler(notificationService);

        handler.onReorderLevelChanged(event("5", "3", "6", true));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                eq(ACTOR_ID),
                eq("Tồn kho thấp theo ngưỡng mới: PART-TEST"),
                message.capture()
        );
        assertThat(message.getValue())
                .contains("Đặng Nam Kho")
                .contains("còn 5 cái")
                .contains("ngưỡng mới là 6 cái")
                .contains("Kho phụ tùng");
    }

    @Test
    void healthyThresholdChangeDoesNotCreatePersistentAlert() {
        var handler = new InventoryReorderLevelNotificationHandler(notificationService);

        handler.onReorderLevelChanged(event("5", "3", "4", true));

        verify(notificationService, never()).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                eq(ACTOR_ID),
                anyString(),
                anyString()
        );
    }

    @Test
    void alreadyLowStockThresholdChangeDoesNotDuplicateAlert() {
        var handler = new InventoryReorderLevelNotificationHandler(notificationService);

        handler.onReorderLevelChanged(event("5", "6", "7", true));

        verify(notificationService, never()).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                eq(ACTOR_ID),
                anyString(),
                anyString()
        );
    }

    private static InventoryReorderLevelChangedEvent event(String stock, String previous, String next, boolean active) {
        return new InventoryReorderLevelChangedEvent(
                TENANT_ID, UUID.randomUUID(), "PART-TEST", "Tụ điện 35uF", "cái",
                new BigDecimal(stock), new BigDecimal(previous), new BigDecimal(next), active,
                ACTOR_ID, "Đặng Nam Kho"
        );
    }
}
