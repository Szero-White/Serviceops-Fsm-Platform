package com.serviceops.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;

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
    void safeSortAcceptsOnlyWhitelistedFieldsAndAddsStableNewestTieBreak() {
        var sort = PageRequestSupport.safeSort(
                "customerName",
                "asc",
                Map.of("customerName", "customer.name", "createdAt", "createdAt"),
                "createdAt",
                Sort.Direction.DESC
        );

        assertEquals(Sort.Direction.ASC, sort.getOrderFor("customer.name").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    void safeSortFallsBackWhenClientRequestsUnknownField() {
        var sort = PageRequestSupport.safeSort(
                "drop table users",
                "asc",
                Map.of("createdAt", "createdAt"),
                "createdAt",
                Sort.Direction.DESC
        );

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    void normalizesSearchText() {
        assertEquals("", PageRequestSupport.normalizeSearch(null));
        assertEquals("WO-2026-001003", PageRequestSupport.normalizeSearch("  WO-2026-001003  "));
    }
}
