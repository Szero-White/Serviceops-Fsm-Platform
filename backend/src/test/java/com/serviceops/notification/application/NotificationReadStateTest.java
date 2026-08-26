package com.serviceops.notification.application;

import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.notification.domain.Notification;
import com.serviceops.notification.domain.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReadStateTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private NotificationRepository repository;
    @Mock private UserAccountRepository userAccountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markUnreadClearsReadAtForOwnedNotification() {
        authenticate();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setTenantId(TENANT_ID);
        notification.setTitle("Test");
        notification.setMessage("Test message");
        notification.setReadAt(Instant.now());
        when(repository.findByIdAndTenantIdAndRecipientId(notificationId, TENANT_ID, USER_ID))
                .thenReturn(Optional.of(notification));

        var response = new NotificationService(repository, userAccountRepository).markUnread(notificationId);

        assertThat(notification.getReadAt()).isNull();
        assertThat(response.readAt()).isNull();
    }

    @Test
    void markReadAndUnreadUseSameRecipientScopedLookup() {
        authenticate();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setTenantId(TENANT_ID);
        notification.setTitle("Test");
        notification.setMessage("Test message");
        when(repository.findByIdAndTenantIdAndRecipientId(notificationId, TENANT_ID, USER_ID))
                .thenReturn(Optional.of(notification));

        NotificationService service = new NotificationService(repository, userAccountRepository);
        assertThat(service.markRead(notificationId).readAt()).isNotNull();
        assertThat(service.markUnread(notificationId).readAt()).isNull();
    }

    private static void authenticate() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("warehouse")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("roles", List.of("WAREHOUSE_STAFF"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
