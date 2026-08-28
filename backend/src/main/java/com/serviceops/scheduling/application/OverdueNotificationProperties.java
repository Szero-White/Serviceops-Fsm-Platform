package com.serviceops.scheduling.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "serviceops.notifications.overdue-scan")
public class OverdueNotificationProperties {
    private Duration customerServiceGrace = Duration.ofMinutes(15);

    public Duration getCustomerServiceGrace() {
        return customerServiceGrace;
    }

    public void setCustomerServiceGrace(Duration customerServiceGrace) {
        if (customerServiceGrace == null || customerServiceGrace.isNegative()) {
            throw new IllegalArgumentException("customerServiceGrace must be zero or positive");
        }
        this.customerServiceGrace = customerServiceGrace;
    }
}
