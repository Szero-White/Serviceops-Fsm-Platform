package com.serviceops.servicerequest.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.servicerequest.domain.RequestChannel;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ServiceRequestDtos {
    private ServiceRequestDtos() {
    }

    public record CreateServiceRequest(
            @NotNull UUID customerId,
            UUID assetId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull Priority priority,
            @NotNull RequestChannel channel
    ) {
    }

    public record ServiceRequestResponse(
            UUID id,
            UUID customerId,
            String customerName,
            UUID assetId,
            String assetLabel,
            String title,
            String description,
            Priority priority,
            RequestChannel channel,
            ServiceRequestStatus status,
            String createdBy,
            Instant createdAt
    ) {
    }
}
