package com.serviceops.security;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveUserJwtValidatorTest {
    @Mock
    private UserAccountRepository repository;

    @Test
    void activeMatchingAccountKeepsTokenValid() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount account = account(tenantId, userId, true, UserRole.DISPATCHER);
        when(repository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(account));

        OAuth2TokenValidatorResult result = new ActiveUserJwtValidator(repository)
                .validate(jwt(tenantId, userId, "dispatcher", "DISPATCHER"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void inactiveAccountInvalidatesPreviouslyIssuedToken() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount account = account(tenantId, userId, false, UserRole.DISPATCHER);
        when(repository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(account));

        OAuth2TokenValidatorResult result = new ActiveUserJwtValidator(repository)
                .validate(jwt(tenantId, userId, "dispatcher", "DISPATCHER"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void staleRoleClaimInvalidatesToken() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount account = account(tenantId, userId, true, UserRole.CUSTOMER_SERVICE);
        account.setUsername("customer-service");
        when(repository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(account));

        OAuth2TokenValidatorResult result = new ActiveUserJwtValidator(repository)
                .validate(jwt(tenantId, userId, "customer-service", "DISPATCHER"));

        assertThat(result.hasErrors()).isTrue();
    }

    private static UserAccount account(UUID tenantId, UUID userId, boolean active, UserRole role) {
        UserAccount account = new UserAccount();
        account.setId(userId);
        account.setTenantId(tenantId);
        account.setUsername("dispatcher");
        account.setDisplayName("Dispatcher");
        account.setPasswordHash("hash");
        account.setRole(role);
        account.setActive(active);
        return account;
    }

    private static Jwt jwt(UUID tenantId, UUID userId, String username, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(username)
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}
