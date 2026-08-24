package com.serviceops.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application event emitted when a stocktake changes the physical balance.
 * Notification routing stays outside InventoryService so the policy can evolve independently.
 */
public record InventoryStockAdjustedEvent(
        UUID tenantId,
        UUID sparePartId,
        String sku,
        String partName,
        String unit,
        BigDecimal systemQuantity,
        BigDecimal actualQuantity,
        BigDecimal reorderLevel,
        UUID actorUserId,
        String actorDisplayName,
        String reason
) {
    public BigDecimal difference() {
        return actualQuantity.subtract(systemQuantity);
    }

    public boolean isLowStock() {
        return actualQuantity.compareTo(reorderLevel) <= 0;
    }
}
