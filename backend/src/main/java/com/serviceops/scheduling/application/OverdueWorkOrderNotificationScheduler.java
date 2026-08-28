package com.serviceops.scheduling.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "serviceops.notifications.overdue-scan.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OverdueWorkOrderNotificationScheduler {
    private final OverdueWorkOrderNotificationService notificationService;

    @Scheduled(
            fixedDelayString = "${serviceops.notifications.overdue-scan.delay-ms:60000}",
            initialDelayString = "${serviceops.notifications.overdue-scan.initial-delay-ms:10000}"
    )
    public void scan() {
        try {
            int created = notificationService.notifyOverdueAppointments(Instant.now());
            if (created > 0) {
                log.info("Created {} overdue work-order notification(s)", created);
            }
        } catch (RuntimeException exception) {
            log.warn("Could not scan overdue work orders for notifications", exception);
        }
    }
}
