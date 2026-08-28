package com.serviceops.notification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByTenantIdAndRecipientId(UUID tenantId, UUID recipientId, Pageable pageable);
    Page<Notification> findByTenantIdAndRecipientIdAndReadAtIsNull(UUID tenantId, UUID recipientId, Pageable pageable);
    long countByTenantIdAndRecipientId(UUID tenantId, UUID recipientId);
    long countByTenantIdAndRecipientIdAndReadAtIsNull(UUID tenantId, UUID recipientId);
    Optional<Notification> findByIdAndTenantIdAndRecipientId(UUID id, UUID tenantId, UUID recipientId);

    @Modifying
    @Query(value = """
            insert into notifications (
                id, tenant_id, recipient_user_id, title, message, event_key, read_at, created_at, updated_at, version
            ) values (
                :id, :tenantId, :recipientId, :title, :message, :eventKey, null, :createdAt, :createdAt, 0
            )
            on conflict (tenant_id, recipient_user_id, event_key) do nothing
            """, nativeQuery = true)
    int insertUnique(@Param("id") UUID id,
                     @Param("tenantId") UUID tenantId,
                     @Param("recipientId") UUID recipientId,
                     @Param("title") String title,
                     @Param("message") String message,
                     @Param("eventKey") String eventKey,
                     @Param("createdAt") Instant createdAt);
}
