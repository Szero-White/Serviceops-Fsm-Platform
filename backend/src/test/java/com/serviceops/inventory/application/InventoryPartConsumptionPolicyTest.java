package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.InventoryDtos.ConsumePartRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryPartConsumptionPolicyTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_USER_ID = UUID.randomUUID();
    private static final UUID SPARE_PART_ID = UUID.randomUUID();

    @Mock private SparePartRepository sparePartRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private InventoryCsvService csvService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        authenticate("technician", TECHNICIAN_USER_ID, "TECHNICIAN");
        service = new InventoryService(
                sparePartRepository, transactionRepository, workOrderRepository,
                csvService, auditService, notificationService, eventPublisher
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completedAndAcceptedWorkOrdersRejectNewConsumptionBeforeStockIsTouched() {
        for (WorkOrderStatus status : List.of(WorkOrderStatus.COMPLETED, WorkOrderStatus.CUSTOMER_ACCEPTED)) {
            WorkOrder workOrder = assignedWorkOrder(status, TECHNICIAN_USER_ID);
            when(workOrderRepository.findForUpdate(workOrder.getId(), TENANT_ID)).thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> service.consume(
                    workOrder.getId(),
                    new ConsumePartRequest(SPARE_PART_ID, BigDecimal.ONE, "Không được phát sinh sau hoàn thành")
            )).isInstanceOfSatisfying(BusinessException.class, ex -> {
                assertThat(ex.getStatus().value()).isEqualTo(409);
                assertThat(ex.getCode()).isEqualTo("WORK_ORDER_PART_CONSUMPTION_NOT_ALLOWED");
            });
        }

        verify(sparePartRepository, never()).findForUpdate(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void nonTechnicianCannotBypassConsumptionPolicyByCallingServiceDirectly() {
        authenticate("owner", UUID.randomUUID(), "OWNER");
        WorkOrder workOrder = assignedWorkOrder(WorkOrderStatus.IN_PROGRESS, TECHNICIAN_USER_ID);
        when(workOrderRepository.findForUpdate(workOrder.getId(), TENANT_ID)).thenReturn(Optional.of(workOrder));

        assertThatThrownBy(() -> service.consume(
                workOrder.getId(),
                new ConsumePartRequest(SPARE_PART_ID, BigDecimal.ONE, "Direct service call")
        )).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getStatus().value()).isEqualTo(403);
            assertThat(ex.getCode()).isEqualTo("WORK_ORDER_PART_CONSUMPTION_FORBIDDEN");
        });

        verify(sparePartRepository, never()).findForUpdate(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    private static WorkOrder assignedWorkOrder(WorkOrderStatus status, UUID userId) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setTenantId(TENANT_ID);
        user.setUsername("technician");
        user.setDisplayName("Kỹ thuật viên test");
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(true);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(user);
        technician.setActive(true);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-CONSUME-POLICY");
        workOrder.setTechnician(technician);
        workOrder.setStatus(status);
        return workOrder;
    }

    private static void authenticate(String username, UUID userId, String role) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", userId.toString())
                .claim("roles", List.of(role))
                .claim("displayName", username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
