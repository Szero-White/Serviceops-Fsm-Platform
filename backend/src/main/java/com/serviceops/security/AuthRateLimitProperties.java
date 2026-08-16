package com.serviceops.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "serviceops.auth")
public record AuthRateLimitProperties(int loginMaxFailures, long loginWindowSeconds) {
    public AuthRateLimitProperties {
        if (loginMaxFailures < 1) {
            loginMaxFailures = 5;
        }
        if (loginWindowSeconds < 1) {
            loginWindowSeconds = 60;
        }
    }
}
