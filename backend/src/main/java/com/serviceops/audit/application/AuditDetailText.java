package com.serviceops.audit.application;

public final class AuditDetailText {
    private AuditDetailText() {
    }

    public static String namedCode(String name, String code) {
        String normalizedName = trimToNull(name);
        String normalizedCode = trimToNull(code);
        if (normalizedName == null) {
            return normalizedCode == null ? "Không xác định" : normalizedCode;
        }
        return normalizedCode == null ? normalizedName : normalizedName + " (" + normalizedCode + ")";
    }

    public static String account(String displayName, String username) {
        String normalizedName = trimToNull(displayName);
        String normalizedUsername = trimToNull(username);
        if (normalizedUsername == null) {
            return normalizedName == null ? "Không xác định" : normalizedName;
        }
        return normalizedName == null
                ? "@" + normalizedUsername
                : normalizedName + " (@" + normalizedUsername + ")";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
