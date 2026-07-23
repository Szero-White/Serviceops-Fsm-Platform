package com.serviceops.technician.web;

import com.serviceops.technician.application.TechnicianService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public TechnicianResponse create(@Valid @RequestBody TechnicianRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TechnicianResponse update(@PathVariable UUID id, @Valid @RequestBody TechnicianRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    public record TechnicianRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 100) String username,
            @Size(min = 6, max = 100) String password,
            @Size(max = 30) String phone,
            @Size(max = 500) String skills,
            Boolean active
    ) {
    }

    public record TechnicianResponse(UUID id, UUID userId, String name, String username, String phone, String skills, boolean active, boolean accountActive) {
    }
}
