package com.serviceops.technician.web;

import com.serviceops.technician.application.TechnicianService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<TechnicianResponse> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.list(activeOnly);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TechnicianResponse updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody TechnicianProfileRequest request
    ) {
        return service.updateProfile(id, request);
    }

    public record TechnicianProfileRequest(
            @Size(max = 30) String phone,
            @Size(max = 500) String skills,
            Boolean active
    ) {
    }

    public record TechnicianResponse(
            UUID id,
            UUID userId,
            String name,
            String username,
            String phone,
            String skills,
            boolean active,
            boolean accountActive,
            boolean protectedDemo
    ) {
    }
}
