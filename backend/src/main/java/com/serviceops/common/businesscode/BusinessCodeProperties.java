package com.serviceops.common.businesscode;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "serviceops.business-code")
public record BusinessCodeProperties(String timeZone) {
    private static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public ZoneId zoneId() {
        String configured = timeZone == null || timeZone.isBlank() ? DEFAULT_TIME_ZONE : timeZone.trim();
        return ZoneId.of(configured);
    }
}
