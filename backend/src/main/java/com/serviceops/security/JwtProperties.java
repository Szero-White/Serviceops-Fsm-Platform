package com.serviceops.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "serviceops.jwt")
public record JwtProperties(String issuer, long accessTokenMinutes, String secret) {
}
