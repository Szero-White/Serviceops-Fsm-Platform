package com.serviceops.asset.application;

import com.serviceops.asset.web.AssetDtos.AssetResponse;
import com.serviceops.common.application.CsvFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetCsvService {
    private static final List<String> ASSET_HEADERS = List.of(
            "customerCode", "category", "brand", "model", "serialNumber", "installedAt", "warrantyUntil", "status", "notes"
    );

    private final CsvFileService csvFileService;

    public List<AssetCsvRow> parseAssets(MultipartFile file) {
        return csvFileService.parse(file, ASSET_HEADERS, "thiet bi")
                .stream()
                .map(row -> new AssetCsvRow(
                        row.rowNumber(),
                        row.value(0),
                        row.value(1),
                        row.value(2),
                        row.value(3),
                        row.value(4),
                        row.value(5),
                        row.value(6),
                        row.value(7),
                        row.value(8)
                ))
                .toList();
    }

    public byte[] assetTemplate() {
        return csvFileService.write(List.of(
                ASSET_HEADERS,
                List.of("KH-0001", "May lanh", "Daikin", "FTKC35", "DK-FTKC35-0001", "2026-01-15", "2028-01-15", "ACTIVE", "Lap dat tai phong hop")
        ));
    }

    public byte[] exportAssets(List<AssetResponse> assets) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("customerName", "category", "brand", "model", "serialNumber", "installedAt", "warrantyUntil", "underWarranty", "status", "notes", "createdAt"));
        for (AssetResponse asset : assets) {
            rows.add(List.of(
                    cell(asset.customerName()),
                    cell(asset.category()),
                    cell(asset.brand()),
                    cell(asset.model()),
                    cell(asset.serialNumber()),
                    asset.installedAt() == null ? "" : asset.installedAt().toString(),
                    asset.warrantyUntil() == null ? "" : asset.warrantyUntil().toString(),
                    Boolean.toString(asset.underWarranty()),
                    asset.status() == null ? "" : asset.status().name(),
                    cell(asset.notes()),
                    asset.createdAt() == null ? "" : asset.createdAt().toString()
            ));
        }
        return csvFileService.write(rows);
    }

    private static String cell(String value) {
        return value == null ? "" : value;
    }

    public record AssetCsvRow(
            int rowNumber,
            String customerCode,
            String category,
            String brand,
            String model,
            String serialNumber,
            String installedAt,
            String warrantyUntil,
            String status,
            String notes
    ) {
    }
}
