package com.serviceops.technician.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.application.DemoAccountProtectionPolicy;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.technician.web.TechnicianController.TechnicianProfileRequest;
import com.serviceops.workorder.domain.WorkOrderRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceLifecycleTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_ID = UUID.randomUUID();

    @Mock private TechnicianRepository repository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private DemoAccountProtectionPolicy demoAccountProtectionPolicy;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileCannotBePausedWhileOperationalAssignmentsRemain() {
        authenticateOwner();
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setTenantId(TENANT_ID);
        user.setUsername("field-tech");
        user.setDisplayName("Field Technician");
        user.setPasswordHash("hash");
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(true);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(TECHNICIAN_ID);
        technician.setTenantId(TENANT_ID);
        technician.setUser(user);
        technician.setActive(true);

        when(repository.findForUpdate(TECHNICIAN_ID, TENANT_ID)).thenReturn(Optional.of(technician));
        when(workOrderRepository.existsActiveAssignment(TENANT_ID, TECHNICIAN_ID)).thenReturn(true);

        TechnicianService service = new TechnicianService(
                repository,
                workOrderRepository,
                auditService,
                notificationService,
                demoAccountProtectionPolicy
        );

        assertThatThrownBy(() -> service.updateProfile(
                TECHNICIAN_ID,
                new TechnicianProfileRequest(null, null, false)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TECHNICIAN_ACTIVE_ASSIGNMENTS");

        verify(repository, never()).save(technician);
    }

    private static void authenticateOwner() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("owner")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", OWNER_ID.toString())
                .claim("roles", List.of("OWNER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
