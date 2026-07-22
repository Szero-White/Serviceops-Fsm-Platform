package com.serviceops.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "serviceops.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
