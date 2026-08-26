package com.serviceops.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when Warehouse or Owner changes the minimum-stock threshold.
 * Stock itself is unchanged; downstream policies can react without coupling catalog updates
 * to notification delivery.
 */
public record InventoryReorderLevelChangedEvent(
        UUID tenantId,
        UUID sparePartId,
        String sku,
        String partName,
        String unit,
        BigDecimal stockQuantity,
        BigDecimal previousReorderLevel,
        BigDecimal newReorderLevel,
        boolean active,
        UUID actorUserId,
        String actorDisplayName
) {
    public boolean wasLowStock() {
        return active && stockQuantity.compareTo(previousReorderLevel) <= 0;
    }

    public boolean isLowStock() {
        return active && stockQuantity.compareTo(newReorderLevel) <= 0;
    }

    public boolean becameLowStock() {
        return !wasLowStock() && isLowStock();
    }
}
