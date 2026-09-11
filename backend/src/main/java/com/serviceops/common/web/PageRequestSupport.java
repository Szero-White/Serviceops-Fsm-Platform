package com.serviceops.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

public final class PageRequestSupport {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PageRequestSupport() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    public static Sort safeSort(String sortBy,
                                String sortDir,
                                Map<String, String> allowedFields,
                                String defaultField,
                                Sort.Direction defaultDirection) {
        String requestedField = sortBy == null ? "" : sortBy.trim();
        boolean validRequestedField = allowedFields.containsKey(requestedField);
        String externalField = validRequestedField ? requestedField : defaultField;
        String entityField = allowedFields.getOrDefault(externalField, allowedFields.get(defaultField));
        Sort.Direction direction = validRequestedField ? parseDirection(sortDir, defaultDirection) : defaultDirection;
        Sort sort = Sort.by(direction, entityField);
        if (!"createdAt".equals(entityField)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return sort;
    }

    public static String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private static Sort.Direction parseDirection(String sortDir, Sort.Direction fallback) {
        if (sortDir == null || sortDir.isBlank()) {
            return fallback;
        }
        return switch (sortDir.trim().toLowerCase(Locale.ROOT)) {
            case "asc", "ascend" -> Sort.Direction.ASC;
            case "desc", "descend" -> Sort.Direction.DESC;
            default -> fallback;
        };
    }
}
