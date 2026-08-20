package com.serviceops.notification.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final UserAccountRepository userAccountRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(UUID tenantId, UserAccount recipient, String title, String message) {
        save(tenantId, recipient, title, message);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifyRoles(UUID tenantId, List<UserRole> roles, String title, String message) {
        notifyRolesInternal(tenantId, roles, title, message, CurrentUser.userId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifyRolesIncludingCurrentUser(UUID tenantId, List<UserRole> roles, String title, String message) {
        notifyRolesInternal(tenantId, roles, title, message, null);
    }

    private void notifyRolesInternal(UUID tenantId, List<UserRole> roles, String title, String message, UUID excludedUserId) {
        Set<UUID> notifiedUserIds = new HashSet<>();
        userAccountRepository.findByTenantIdAndRoleInAndActiveTrue(tenantId, roles).forEach(recipient -> {
            if ((excludedUserId == null || !excludedUserId.equals(recipient.getId())) && notifiedUserIds.add(recipient.getId())) {
                save(tenantId, recipient, title, message);
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifyCurrentUser(String title, String message) {
        UUID tenantId = CurrentUser.tenantId();
        UserAccount recipient = userAccountRepository.findByIdAndTenantId(CurrentUser.userId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Không tìm thấy người dùng hiện tại"));
        save(tenantId, recipient, title, message);
    }

    private void save(UUID tenantId, UserAccount recipient, String title, String message) {
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
