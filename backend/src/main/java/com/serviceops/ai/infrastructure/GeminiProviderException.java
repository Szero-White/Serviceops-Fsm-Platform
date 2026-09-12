package com.serviceops.ai.infrastructure;

public class GeminiProviderException extends RuntimeException {
    public GeminiProviderException(String message) {
        super(message);
    }

    public GeminiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
