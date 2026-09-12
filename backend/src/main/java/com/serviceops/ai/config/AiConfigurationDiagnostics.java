package com.serviceops.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiConfigurationDiagnostics {
    private final AiProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void reportConfiguration() {
        if (!properties.isEnabled()) {
            log.info("AI assistance is disabled");
            return;
        }

        if ("gemini".equalsIgnoreCase(properties.getProvider())) {
            if (properties.getGeminiApiKey() == null || properties.getGeminiApiKey().isBlank()) {
                log.warn("AI assistance is enabled but Gemini credentials are not configured; built-in fallback will be used");
                return;
            }
            log.info("AI assistance is ready with the configured Gemini provider");
            return;
        }

        log.warn("AI assistance is enabled with unsupported provider={}; built-in fallback will be used", properties.getProvider());
    }
}
