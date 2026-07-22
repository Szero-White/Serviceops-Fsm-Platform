package com.serviceops.technician.web;

import com.serviceops.technician.application.TechnicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/technicians")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
public class TechnicianController {
    private final TechnicianService service;

    @GetMapping
    public List<TechnicianResponse> list() {
        return service.list();
    }

    public record TechnicianResponse(UUID id, UUID userId, String name, String username, String phone, String skills, boolean active) {
    }
}
