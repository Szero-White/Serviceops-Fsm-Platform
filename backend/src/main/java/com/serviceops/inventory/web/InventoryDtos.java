package com.serviceops.inventory.web;

import com.serviceops.inventory.domain.InventoryTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    public record ReorderLevelRequest(
            @NotNull @DecimalMin(value = "0.0") BigDecimal reorderLevel
    ) {
    }

    public record StockAdjustmentRequest(
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String note
    ) {
    }

    public record StocktakeRequest(
            @NotNull @DecimalMin(value = "0.0") BigDecimal actualQuantity,
            @NotBlank @Size(max = 300) String reason
    ) {
    }

    public record SparePartStatusRequest(@NotNull Boolean active) {
    }

    public record SparePartResponse(
            UUID id, String sku, String name, String unit, BigDecimal stockQuantity,
            BigDecimal reorderLevel, BigDecimal unitPrice, boolean lowStock, boolean active, Instant updatedAt
    ) {
    }

    public record StocktakeResponse(
            SparePartResponse sparePart,
            BigDecimal systemQuantity,
            BigDecimal actualQuantity,
            BigDecimal difference,
            InventoryTransactionType adjustmentType
    ) {
    }

    public record InventoryTransactionResponse(
            UUID id,
            InventoryTransactionType type,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            UUID workOrderId,
            String workOrderCode,
            String workOrderSummary,
            String note,
            String createdBy,
            UUID recipientUserId,
            String recipientDisplayName,
            String actorDisplayName,
            String actorRole,
            Instant createdAt
    ) {
    }

    public record SparePartImportResult(
            int totalRows, int validRows, int errorRows, int importedRows, boolean committed, List<SparePartImportRowResult> rows
    ) {
    }

    public record SparePartImportRowResult(
            int rowNumber, String sku, String name, boolean valid, String message
    ) {
    }
}
