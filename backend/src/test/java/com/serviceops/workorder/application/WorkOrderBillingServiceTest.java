package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingItemRepository;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshotRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderBillingServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private WorkOrderBillingSnapshotRepository snapshotRepository;
    @Mock private WorkOrderBillingItemRepository itemRepository;
    @Mock private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerAcceptanceSnapshotUsesActualUsedQuantityAndCatalogPrice() {
        authenticate("TECHNICIAN", USER_ID);
        WorkOrder workOrder = workOrder();
        workOrder.setLaborFee(new BigDecimal("250000"));
        workOrder.setIncidentalFee(new BigDecimal("50000"));
        workOrder.setIncidentalReason("Phí di chuyển ngoài khu vực");

        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setTenantId(TENANT_ID);
        part.setSku("DW-VALVE-220V-01");
        part.setName("Van cấp nước");
        part.setUnit("cái");
        part.setUnitPrice(new BigDecimal("485000"));

        WorkOrderPartUsage usage = new WorkOrderPartUsage();
        usage.setTenantId(TENANT_ID);
        usage.setWorkOrder(workOrder);
        usage.setSparePart(part);
        usage.setUsedQuantity(new BigDecimal("2"));

        when(snapshotRepository.findByWorkOrder(TENANT_ID, workOrder.getId())).thenReturn(Optional.empty());
        when(usageRepository.findDetailedByWorkOrder(TENANT_ID, workOrder.getId())).thenReturn(List.of(usage));

        WorkOrderBillingService service = new WorkOrderBillingService(
                workOrderRepository,
                usageRepository,
                snapshotRepository,
                itemRepository,
                auditService
        );
        var snapshot = service.freezeForCustomerAcceptance(workOrder);

        assertThat(snapshot.getPartsTotal()).isEqualByComparingTo("970000.00");
        assertThat(snapshot.getTotalAmount()).isEqualByComparingTo("1270000.00");
        assertThat(snapshot.getAcceptedByUserId()).isEqualTo(USER_ID);

        var itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
    }

    private static WorkOrder workOrder() {
        UserAccount user = new UserAccount();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername("technician");
        user.setDisplayName("Trịnh Quốc Tiến");
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
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén");
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setTechnician(technician);
        return workOrder;
    }

    private static void authenticate(String role, UUID userId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("technician")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", userId.toString())
                .claim("displayName", "Trịnh Quốc Tiến")
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
