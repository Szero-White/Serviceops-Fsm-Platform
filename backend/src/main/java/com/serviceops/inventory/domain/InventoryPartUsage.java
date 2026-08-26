package com.serviceops.inventory.domain;

import java.math.BigDecimal;

public record InventoryPartUsage(
        SparePart sparePart,
        BigDecimal quantity
) {
}
