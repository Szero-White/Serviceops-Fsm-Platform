package com.serviceops.workorder.application;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.technician.domain.TechnicianProfile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class WorkOrderDispatchTestFixtures {

    private WorkOrderDispatchTestFixtures() {
    }

    static TechnicianProfile technician(UUID tenantId, String displayName) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setUsername(displayName.replace(" ", ".").toLowerCase());
        user.setDisplayName(displayName);
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(true);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(tenantId);
        technician.setUser(user);
        technician.setActive(true);
        return technician;
    }

    static void authenticateDispatcher(UUID tenantId, UUID dispatcherId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("dispatcher")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claims(claims -> claims.putAll(Map.of(
                        "tenantId", tenantId.toString(),
                        "userId", dispatcherId.toString(),
                        "displayName", "Lê Thu Điều phối",
                        "roles", List.of("DISPATCHER")
                )))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
