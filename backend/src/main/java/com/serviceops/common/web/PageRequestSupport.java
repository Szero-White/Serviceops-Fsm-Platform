package com.serviceops.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    public static String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
