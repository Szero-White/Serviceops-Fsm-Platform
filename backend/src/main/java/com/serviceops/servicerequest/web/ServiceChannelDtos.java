package com.serviceops.servicerequest.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ServiceChannelDtos {
    private ServiceChannelDtos() {
    }

    public record ServiceChannelRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Z0-9_]+$") String code,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 240) String description,
            @Size(max = 30) String color,
            Integer sortOrder,
            Boolean active
    ) {
    }

    public record ServiceChannelUpdateRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 240) String description,
            @Size(max = 30) String color,
            Integer sortOrder,
            Boolean active
    ) {
    }

    public record ServiceChannelResponse(
            UUID id,
            String code,
            String name,
            String description,
            String color,
            int sortOrder,
            boolean active,
            boolean systemDefined,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
