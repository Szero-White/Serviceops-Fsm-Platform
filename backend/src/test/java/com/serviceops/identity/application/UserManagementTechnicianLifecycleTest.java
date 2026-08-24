package com.serviceops.identity.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.identity.web.UserManagementController.UserAccountRequest;
import com.serviceops.notification.domain.NotificationRepository;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.tenant.domain.TenantRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementTechnicianLifecycleTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_ID = UUID.randomUUID();

    @Mock private UserAccountRepository repository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private DemoAccountProtectionPolicy demoAccountProtectionPolicy;

    private UserManagementService service;
    private UserAccount technicianUser;
    private TechnicianProfile technician;

    @BeforeEach
    void setUp() {
        authenticateOwner();
        service = new UserManagementService(
                repository,
                technicianRepository,
                tenantRepository,
                workOrderRepository,
                appointmentRepository,
                notificationRepository,
                auditService,
                passwordEncoder,
                demoAccountProtectionPolicy
        );

        technicianUser = new UserAccount();
        technicianUser.setId(USER_ID);
        technicianUser.setTenantId(TENANT_ID);
        technicianUser.setUsername("field-tech");
        technicianUser.setDisplayName("Field Technician");
        technicianUser.setPasswordHash("hash");
        technicianUser.setRole(UserRole.TECHNICIAN);
        technicianUser.setActive(true);

        technician = new TechnicianProfile();
        technician.setId(TECHNICIAN_ID);
        technician.setTenantId(TENANT_ID);
        technician.setUser(technicianUser);
        technician.setActive(true);

        when(repository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(technicianUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cannotDeactivateTechnicianWhileOperationalAssignmentsRemain() {
        when(technicianRepository.findByTenantIdAndUserIdForUpdate(TENANT_ID, USER_ID)).thenReturn(Optional.of(technician));
        when(workOrderRepository.existsActiveAssignment(TENANT_ID, TECHNICIAN_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(USER_ID, disableRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TECHNICIAN_ACTIVE_ASSIGNMENTS");

        assertThat(technicianUser.isActive()).isTrue();
        assertThat(technician.isActive()).isTrue();
    }

    @Test
    void canDeactivateTechnicianAfterOperationalAssignmentsAreCleared() {
        when(technicianRepository.findByTenantIdAndUserIdForUpdate(TENANT_ID, USER_ID)).thenReturn(Optional.of(technician));
        when(technicianRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(technician));
        when(workOrderRepository.existsActiveAssignment(TENANT_ID, TECHNICIAN_ID)).thenReturn(false);
        when(technicianRepository.save(any(TechnicianProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(USER_ID, disableRequest());

        assertThat(technicianUser.isActive()).isFalse();
        assertThat(technician.isActive()).isFalse();
        verify(auditService).record("UPDATE", "USER_ACCOUNT", USER_ID, "Cập nhật người dùng field-tech");
    }

    @Test
    void usernameCannotChangeAfterAccountCreation() {
        UserAccountRequest request = new UserAccountRequest(
                "renamed-tech",
                "Field Technician",
                UserRole.TECHNICIAN,
                null,
                true,
                null,
                null
        );

        assertThatThrownBy(() -> service.update(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("USER_USERNAME_CHANGE_BLOCKED");

        assertThat(technicianUser.getUsername()).isEqualTo("field-tech");
    }

    private static UserAccountRequest disableRequest() {
        return new UserAccountRequest(
                "field-tech",
                "Field Technician",
                UserRole.TECHNICIAN,
                null,
                false,
                null,
                null
        );
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
