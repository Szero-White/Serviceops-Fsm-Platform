package com.serviceops.notification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByTenantIdAndRecipientId(UUID tenantId, UUID recipientId, Pageable pageable);
    long countByTenantIdAndRecipientIdAndReadAtIsNull(UUID tenantId, UUID recipientId);
    Optional<Notification> findByIdAndTenantIdAndRecipientId(UUID id, UUID tenantId, UUID recipientId);
}
