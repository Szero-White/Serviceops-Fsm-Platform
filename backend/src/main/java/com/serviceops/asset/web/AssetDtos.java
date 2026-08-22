package com.serviceops.asset.web;

import com.serviceops.asset.domain.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AssetDtos {
    private AssetDtos() {
    }

    public record AssetRequest(
            @NotNull UUID customerId,
            @NotBlank @Size(max = 80) String category,
            @Size(max = 100) String brand,
            @Size(max = 100) String model,
            @Size(max = 120) String serialNumber,
            LocalDate installedAt,
            LocalDate warrantyUntil,
            AssetStatus status,
            @Size(max = 2000) String notes
    ) {
    }

    public record AssetResponse(
            UUID id,
            UUID customerId,
            String customerName,
            String category,
            String brand,
            String model,
            String serialNumber,
            LocalDate installedAt,
            LocalDate warrantyUntil,
            boolean underWarranty,
            AssetStatus status,
            String notes,
            Instant createdAt
    ) {
    }

    public record AssetImportResult(
            int totalRows,
            int validRows,
            int errorRows,
            int importedRows,
            boolean committed,
            List<AssetImportRowResult> rows
    ) {
    }

    public record AssetImportRowResult(
            int rowNumber,
            String serialNumber,
            String customerCode,
            boolean valid,
            String message
    ) {
    }
}
