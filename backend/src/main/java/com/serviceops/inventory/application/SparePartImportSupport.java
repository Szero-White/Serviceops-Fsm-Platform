package com.serviceops.inventory.application;

import com.serviceops.inventory.application.InventoryCsvService.SparePartCsvRow;
import com.serviceops.inventory.domain.SparePartRepository;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Pure validation/parsing support for bulk spare-part import. */
final class SparePartImportSupport {
    private SparePartImportSupport() {
    }

    static Candidate validate(
            SparePartCsvRow row,
            Set<String> seenSkus,
            UUID tenantId,
            SparePartRepository repository
    ) {
        String sku = row.sku().trim().toUpperCase(Locale.ROOT);
        if (sku.isBlank()) {
            return Candidate.invalid(row, "SKU không được để trống");
        }
        if (sku.length() > 60) {
            return Candidate.invalid(row, "SKU không được vượt quá 60 ký tự");
        }
        if (!seenSkus.add(sku)) {
            return Candidate.invalid(row, "SKU bị trùng trong file import");
        }
        if (repository.existsByTenantIdAndSkuIgnoreCase(tenantId, sku)) {
            return Candidate.invalid(row, "SKU đã tồn tại trong hệ thống");
        }
        if (row.name().isBlank() || row.name().length() > 180) {
            return Candidate.invalid(row, "Tên phụ tùng bắt buộc và tối đa 180 ký tự");
        }
        if (row.unit().isBlank() || row.unit().length() > 30) {
            return Candidate.invalid(row, "Đơn vị bắt buộc và tối đa 30 ký tự");
        }

        try {
            BigDecimal initialStock = parseNonNegative(row.initialStock(), "Tồn ban đầu");
            BigDecimal reorderLevel = parseNonNegative(row.reorderLevel(), "Ngưỡng tồn tối thiểu");
            BigDecimal unitPrice = parseNonNegative(row.unitPrice(), "Đơn giá");
            boolean active = parseBoolean(row.active());
            return new Candidate(
                    row, sku, row.name().trim(), row.unit().trim(),
                    initialStock, reorderLevel, unitPrice, active, true, "Hợp lệ"
            );
        } catch (IllegalArgumentException ex) {
            return Candidate.invalid(row, ex.getMessage());
        }
    }

    private static BigDecimal parseNonNegative(String value, String label) {
        try {
            BigDecimal parsed = new BigDecimal(value == null || value.isBlank() ? "0" : value.trim());
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException(label + " không được âm");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " không đúng định dạng số");
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Cột active chỉ nhận true hoặc false");
    }

    record Candidate(
            SparePartCsvRow row,
            String sku,
            String name,
            String unit,
            BigDecimal initialStock,
            BigDecimal reorderLevel,
            BigDecimal unitPrice,
            boolean active,
            boolean valid,
            String message
    ) {
        static Candidate invalid(SparePartCsvRow row, String message) {
            return new Candidate(
                    row, row.sku(), row.name(), row.unit(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    false, false, message
            );
        }
    }
}
