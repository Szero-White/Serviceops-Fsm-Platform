package com.serviceops.inventory.application;

import com.serviceops.common.application.CsvFileService;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryCsvService {
    private static final List<String> SPARE_PART_HEADERS = List.of(
            "sku", "name", "unit", "initialStock", "reorderLevel", "unitPrice", "active"
    );
    private final CsvFileService csvFileService;

    public List<SparePartCsvRow> parseSpareParts(MultipartFile file) {
        return csvFileService.parse(file, SPARE_PART_HEADERS, "kho phụ tùng")
                .stream()
                .map(row -> toRow(row.rowNumber(), row.values()))
                .toList();
    }

    public byte[] sparePartTemplate() {
        return csvFileService.write(List.of(
                SPARE_PART_HEADERS,
                List.of("FILTER-AC-02", "Lưới lọc máy lạnh", "cái", "10", "3", "95000", "true")
        ));
    }

    public byte[] exportSpareParts(List<SparePartResponse> parts) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("sku", "name", "unit", "stockQuantity", "reorderLevel", "unitPrice", "lowStock", "active", "updatedAt"));
        for (SparePartResponse part : parts) {
            rows.add(List.of(
                    part.sku(),
                    part.name(),
                    part.unit(),
                    format(part.stockQuantity()),
                    format(part.reorderLevel()),
                    format(part.unitPrice()),
                    Boolean.toString(part.lowStock()),
                    Boolean.toString(part.active()),
                    part.updatedAt() == null ? "" : part.updatedAt().toString()
            ));
        }
        return csvFileService.write(rows);
    }

    private static SparePartCsvRow toRow(int rowNumber, List<String> values) {
        List<String> normalized = new ArrayList<>(values);
        while (normalized.size() < SPARE_PART_HEADERS.size()) {
            normalized.add("");
        }
        return new SparePartCsvRow(
                rowNumber,
                normalized.get(0).trim(),
                normalized.get(1).trim(),
                normalized.get(2).trim(),
                normalized.get(3).trim(),
                normalized.get(4).trim(),
                normalized.get(5).trim(),
                normalized.get(6).trim()
        );
    }

    private static String format(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    public record SparePartCsvRow(
            int rowNumber,
            String sku,
            String name,
            String unit,
            String initialStock,
            String reorderLevel,
            String unitPrice,
            String active
    ) {
    }
}
