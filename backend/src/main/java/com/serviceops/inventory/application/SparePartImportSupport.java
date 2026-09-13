package com.serviceops.inventory.application;

import com.serviceops.inventory.application.InventoryCsvService.SparePartCsvRow;

import java.math.BigDecimal;
import java.util.Locale;

/** Pure validation/parsing support for bulk spare-part import. */
final class SparePartImportSupport {
    private SparePartImportSupport() {
    }

    static Candidate validate(SparePartCsvRow row) {
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
                    row, row.name().trim(), row.unit().trim(),
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
        if ("true".equals(normalized) || "có".equals(normalized) || "co".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "không".equals(normalized) || "khong".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Cột Hoạt động chỉ nhận Có hoặc Không");
    }

    record Candidate(
            SparePartCsvRow row,
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
                    row, row.name(), row.unit(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    false, false, message
            );
        }
    }
}
