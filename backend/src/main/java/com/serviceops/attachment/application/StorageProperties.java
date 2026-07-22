package com.serviceops.attachment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "serviceops.storage")
public record StorageProperties(String root) {
}
