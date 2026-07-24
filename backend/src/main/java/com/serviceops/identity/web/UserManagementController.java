package com.serviceops.identity.web;

import com.serviceops.identity.application.UserManagementService;
import com.serviceops.identity.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserManagementController {
    private final UserManagementService service;

    @GetMapping
    public List<UserAccountResponse> list() {
        return service.list();
    }

    @PostMapping
    public UserAccountResponse create(@Valid @RequestBody UserAccountRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UserAccountResponse update(@PathVariable UUID id, @Valid @RequestBody UserAccountRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    public record UserAccountRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 150) String displayName,
            @NotNull UserRole role,
            @Size(min = 6, max = 100) String password,
            Boolean active,
            @Size(max = 30) String phone,
            @Size(max = 500) String skills
    ) {
    }

    public record UserAccountResponse(
            UUID id,
            String username,
            String displayName,
            UserRole role,
            boolean active,
            UUID technicianProfileId,
            String phone,
            String skills,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
