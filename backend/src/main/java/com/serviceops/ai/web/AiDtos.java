package com.serviceops.ai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiDtos {
    private AiDtos() {
    }

    public enum AiResponseSource {
        GEMINI,
        LOCAL
    }

    public record ServiceRequestDraftRequest(
            @NotBlank @Size(max = 3000) String rawText
    ) {
    }

    public record ServiceRequestDraftResponse(
            String title,
            String description,
            AiResponseSource source
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
            AiResponseSource source
    ) {
    }
}
