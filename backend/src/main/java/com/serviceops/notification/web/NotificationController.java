package com.serviceops.notification.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return service.markRead(id);
    }

    public record NotificationResponse(UUID id, String title, String message, Instant readAt, Instant createdAt) {
    }
}
