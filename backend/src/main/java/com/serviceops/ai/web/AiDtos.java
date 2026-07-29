package com.serviceops.ai.web;

import com.serviceops.common.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AiDtos {
    private AiDtos() {
    }

    public record ServiceRequestDraftRequest(
            @NotBlank @Size(max = 3000) String rawText,
            @Size(max = 30) String preferredChannel
    ) {
    }

    public record ServiceRequestDraftResponse(
            String title,
            String description,
            Priority priority,
            String channel,
            double confidence,
            String reason,
            String provider
    ) {
    }
}
