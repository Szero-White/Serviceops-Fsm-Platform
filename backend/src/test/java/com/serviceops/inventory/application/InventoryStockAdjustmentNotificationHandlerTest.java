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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryStockAdjustmentNotificationHandlerTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private NotificationService notificationService;

    @Test
    void discrepancyAlwaysNotifiesOwnerWithTraceableContext() {
        var handler = new InventoryStockAdjustmentNotificationHandler(notificationService);

        handler.onStockAdjusted(event("10", "8", "3"));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.OWNER)),
                eq(ACTOR_ID),
                eq("Kiểm kê có chênh lệch: PART-TEST"),
                message.capture()
        );
        assertThat(message.getValue())
                .contains("tồn hệ thống 10 cái")
                .contains("thực tế 8 cái")
                .contains("chênh -2 cái")
                .contains("Đặng Nam Kho")
                .contains("Kiểm kê cuối ca");
    }

    @Test
    void lowStockAfterStocktakeNotifiesWarehouseRole() {
        var handler = new InventoryStockAdjustmentNotificationHandler(notificationService);

        handler.onStockAdjusted(event("10", "2", "3"));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                org.mockito.ArgumentMatchers.isNull(),
                eq("Cần bổ sung tồn kho sau kiểm kê: PART-TEST"),
                message.capture()
        );
        assertThat(message.getValue()).contains("ngưỡng tồn tối thiểu");
    }

    @Test
    void ownerNotificationFailureDoesNotPreventWarehouseLowStockDelivery() {
        var handler = new InventoryStockAdjustmentNotificationHandler(notificationService);
        doThrow(new IllegalStateException("notification database unavailable"))
                .when(notificationService).notifyRolesIndependently(
                        eq(TENANT_ID),
                        eq(List.of(UserRole.OWNER)),
                        eq(ACTOR_ID),
                        anyString(),
                        anyString()
                );

        handler.onStockAdjusted(event("10", "2", "3"));

        verify(notificationService).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                org.mockito.ArgumentMatchers.isNull(),
                eq("Cần bổ sung tồn kho sau kiểm kê: PART-TEST"),
                anyString()
        );
    }

    @Test
    void healthyStockDoesNotBroadcastWarehouseLowStockAlert() {
        var handler = new InventoryStockAdjustmentNotificationHandler(notificationService);

        handler.onStockAdjusted(event("10", "8", "3"));

        verify(notificationService, never()).notifyRolesIndependently(
                eq(TENANT_ID),
                eq(List.of(UserRole.WAREHOUSE_STAFF)),
                org.mockito.ArgumentMatchers.isNull(),
                anyString(),
                anyString()
        );
    }

    private static InventoryStockAdjustedEvent event(String system, String actual, String reorder) {
        return new InventoryStockAdjustedEvent(
                TENANT_ID, UUID.randomUUID(), "PART-TEST", "Tụ điện 35uF", "cái",
                new BigDecimal(system), new BigDecimal(actual), new BigDecimal(reorder),
                ACTOR_ID, "Đặng Nam Kho", "Kiểm kê cuối ca"
        );
    }
}
