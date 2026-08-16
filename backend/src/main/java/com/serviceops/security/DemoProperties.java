package com.serviceops.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "serviceops.demo")
public record DemoProperties(boolean enabled, String seedPassword) {
    private static final Set<String> FORBIDDEN_PUBLIC_PASSWORDS = Set.of(
            "123456",
            "CHANGE_ME_DEMO_PASSWORD",
            "change-this-demo-password"
    );

    public String requireSeedPassword() {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException("Demo seed password must be configured");
        }
        if (enabled && (seedPassword.length() < 8 || FORBIDDEN_PUBLIC_PASSWORDS.contains(seedPassword))) {
            throw new IllegalStateException("Public demo seed password must contain at least 8 characters and must not use a default/placeholder value");
        }
        return seedPassword;
    }
}
