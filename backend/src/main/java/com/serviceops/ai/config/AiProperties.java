package com.serviceops.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "serviceops.ai")
public class AiProperties {
    private boolean enabled = true;
    private String provider = "gemini";
    private String geminiApiKey;
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String geminiModel = "gemini-3.6-flash";
    private Duration timeout = Duration.ofSeconds(20);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiBaseUrl() {
        return geminiBaseUrl;
    }

    public void setGeminiBaseUrl(String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
