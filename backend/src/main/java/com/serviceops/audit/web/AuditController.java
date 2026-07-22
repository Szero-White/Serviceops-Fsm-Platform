package com.serviceops.audit.web;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public PageResponse<AuditResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "30") int size) {
        return service.list(page, size);
    }

    public record AuditResponse(UUID id, String actorUsername, String action, String entityType, UUID entityId, String details, Instant createdAt) {
    }
}
