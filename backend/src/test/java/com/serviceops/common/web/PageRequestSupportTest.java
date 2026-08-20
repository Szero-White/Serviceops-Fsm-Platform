package com.serviceops.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageRequestSupportTest {
    private static final Sort SORT = Sort.by("createdAt").descending();

    @Test
    void clampsInvalidPageAndSize() {
        var pageable = PageRequestSupport.of(-4, 0, SORT);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PageRequestSupport.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    void capsPageSizeToProtectListEndpoints() {
        var pageable = PageRequestSupport.of(3, 5_000, SORT);

        assertEquals(3, pageable.getPageNumber());
        assertEquals(PageRequestSupport.MAX_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    void normalizesSearchText() {
        assertEquals("", PageRequestSupport.normalizeSearch(null));
        assertEquals("WO-2026-001003", PageRequestSupport.normalizeSearch("  WO-2026-001003  "));
    }
}
