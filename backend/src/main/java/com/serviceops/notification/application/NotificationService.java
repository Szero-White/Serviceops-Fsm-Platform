package com.serviceops.notification.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.notification.domain.Notification;
import com.serviceops.notification.domain.NotificationRepository;
import com.serviceops.notification.web.NotificationController.NotificationResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(UUID tenantId, UserAccount recipient, String title, String message) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return PageResponse.from(repository.findByTenantIdAndRecipientId(CurrentUser.tenantId(), CurrentUser.userId(), pageable)
                .map(NotificationService::toResponse));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByTenantIdAndRecipientIdAndReadAtIsNull(CurrentUser.tenantId(), CurrentUser.userId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id) {
        Notification notification = repository.findByIdAndTenantIdAndRecipientId(id, CurrentUser.tenantId(), CurrentUser.userId())
                .orElseThrow(() -> BusinessException.notFound("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo"));
        notification.setReadAt(Instant.now());
        return toResponse(notification);
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getReadAt(), n.getCreatedAt());
    }
}
