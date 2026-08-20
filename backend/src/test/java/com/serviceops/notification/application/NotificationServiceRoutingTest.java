package com.serviceops.notification.application;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.domain.Notification;
import com.serviceops.notification.domain.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceRoutingTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private NotificationRepository repository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notifyRolesExcludesCurrentActorByDefault() {
        authenticate(ACTOR_ID, "warehouse", "WAREHOUSE_STAFF");
        UserAccount actor = user(ACTOR_ID, "warehouse", UserRole.WAREHOUSE_STAFF);
        UserAccount owner = user(UUID.randomUUID(), "owner", UserRole.OWNER);
        when(userAccountRepository.findByTenantIdAndRoleInAndActiveTrue(
                TENANT_ID, List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF)))
                .thenReturn(List.of(actor, owner));

        NotificationService service = new NotificationService(repository, userAccountRepository);
        service.notifyRoles(
                TENANT_ID,
                List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF),
                "Đã nhập kho",
                "SENSOR-TEMP-10K"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getRecipient().getId()).isEqualTo(owner.getId());
    }

    @Test
    void includingCurrentUserKeepsActorForActionableAlerts() {
        authenticate(ACTOR_ID, "warehouse", "WAREHOUSE_STAFF");
        UserAccount actor = user(ACTOR_ID, "warehouse", UserRole.WAREHOUSE_STAFF);
        UserAccount owner = user(UUID.randomUUID(), "owner", UserRole.OWNER);
        when(userAccountRepository.findByTenantIdAndRoleInAndActiveTrue(
                TENANT_ID, List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF)))
                .thenReturn(List.of(actor, owner));

        NotificationService service = new NotificationService(repository, userAccountRepository);
        service.notifyRolesIncludingCurrentUser(
                TENANT_ID,
                List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF),
                "Phụ tùng sắp hết",
                "SENSOR-TEMP-10K còn 2 cái"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(notification -> notification.getRecipient().getId())
                .containsExactlyInAnyOrder(actor.getId(), owner.getId());
    }

    private static UserAccount user(UUID id, String username, UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setTenantId(TENANT_ID);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setPasswordHash("test");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private static void authenticate(UUID userId, String username, String role) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", userId.toString())
                .claim("roles", List.of(role))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
