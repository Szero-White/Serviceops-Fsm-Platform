package com.serviceops.inventory.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    public record SparePartRequest(
            @NotBlank @Size(max = 60) String sku,
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 30) String unit,
            @NotNull @DecimalMin("0.0") BigDecimal initialStock,
            @NotNull @DecimalMin("0.0") BigDecimal reorderLevel,
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
            Boolean active
    ) {
    }

    public record StockAdjustmentRequest(
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String note
    ) {
    }

    public record ConsumePartRequest(
            @NotNull UUID sparePartId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @Size(max = 300) String note
    ) {
    }

    public record SparePartResponse(
            UUID id,
            String sku,
            String name,
            String unit,
            BigDecimal stockQuantity,
            BigDecimal reorderLevel,
            BigDecimal unitPrice,
            boolean lowStock,
            boolean active,
            Instant updatedAt
    ) {
    }
}
