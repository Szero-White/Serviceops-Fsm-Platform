package com.serviceops.customer.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 180) String name,
            @Size(max = 30) String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 300) String address,
            @Size(max = 2000) String notes,
            Boolean active
    ) {
    }

    public record CustomerResponse(
            UUID id,
            String code,
            String name,
            String phone,
            String email,
            String address,
            String notes,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
