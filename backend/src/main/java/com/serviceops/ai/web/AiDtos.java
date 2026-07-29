package com.serviceops.ai.web;

import com.serviceops.common.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

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

    public record HelpRequest(
            @NotBlank @Size(max = 1000) String question,
            @Size(max = 120) String currentPath
    ) {
    }

    public record HelpResponse(
            String answer,
            List<String> steps,
            String relatedRoute,
            String actionLabel,
            String provider
    ) {
    }
}
