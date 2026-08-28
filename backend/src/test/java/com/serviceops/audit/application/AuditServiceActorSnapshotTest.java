package com.serviceops.audit.application;

import com.serviceops.audit.domain.AuditLog;
import com.serviceops.audit.domain.AuditLogRepository;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceActorSnapshotTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuditLogRepository repository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordSnapshotsCurrentActorDisplayNameAndRole() {
        authenticate("dispatcher", "Lê Thu Điều phối", "DISPATCHER");
        AuditService service = new AuditService(repository);

        service.record("RESCHEDULE", "WORK_ORDER", UUID.randomUUID(), "Đổi lịch thực hiện");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorUsername()).isEqualTo("dispatcher");
        assertThat(captor.getValue().getActorDisplayName()).isEqualTo("Lê Thu Điều phối");
        assertThat(captor.getValue().getActorRole()).isEqualTo("DISPATCHER");
    }

    private static void authenticate(String username, String displayName, String role) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", displayName)
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
