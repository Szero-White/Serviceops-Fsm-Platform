package com.serviceops.inventory.application;

import com.serviceops.common.domain.Priority;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderPartQueryServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID WAREHOUSE_USER_ID = UUID.randomUUID();

    @Mock private WorkOrderPartRequestRepository requestRepository;
    @Mock private WorkOrderPartUsageRepository usageRepository;
    @Mock private WorkOrderPartStockService stockService;
    @Mock private WorkOrderPartWorkflowPolicy workflowPolicy;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void outstandingQueueUsesIssuedMinusActualUsedMinusReturned() {
        authenticateWarehouse();
        WorkOrder workOrder = workOrder();
        SparePart part = part();
        WorkOrderPartRequest request = issuedRequest(workOrder, part);

        WorkOrderPartUsage usage = new WorkOrderPartUsage();
        usage.setTenantId(TENANT_ID);
        usage.setWorkOrder(workOrder);
        usage.setSparePart(part);
        usage.setUsedQuantity(new BigDecimal("2"));

        when(requestRepository.findIssuedForOutstanding(TENANT_ID)).thenReturn(List.of(request));
        when(stockService.totals(TENANT_ID, workOrder.getId(), part.getId()))
                .thenReturn(new WorkOrderPartStockService.PartStockTotals(new BigDecimal("3"), BigDecimal.ZERO));
        when(usageRepository.findByTenantIdAndWorkOrderIdAndSparePartId(TENANT_ID, workOrder.getId(), part.getId()))
                .thenReturn(Optional.of(usage));

        WorkOrderPartQueryService service = new WorkOrderPartQueryService(
                requestRepository,
                usageRepository,
                stockService,
                workflowPolicy
        );
        var result = service.outstandingParts("");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().issuedQuantity()).isEqualByComparingTo("3");
        assertThat(result.getFirst().usedQuantity()).isEqualByComparingTo("2");
        assertThat(result.getFirst().returnedQuantity()).isEqualByComparingTo("0");
        assertThat(result.getFirst().outstandingQuantity()).isEqualByComparingTo("1");
        assertThat(result.getFirst().technicianName()).isEqualTo("Trịnh Quốc Tiến");
    }

    private static WorkOrder workOrder() {
        UserAccount technicianUser = new UserAccount();
        technicianUser.setId(UUID.randomUUID());
        technicianUser.setTenantId(TENANT_ID);
        technicianUser.setUsername("technician");
        technicianUser.setDisplayName("Trịnh Quốc Tiến");
        technicianUser.setRole(UserRole.TECHNICIAN);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(technicianUser);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén");
        workOrder.setPriority(Priority.NORMAL);
        workOrder.setStatus(WorkOrderStatus.CUSTOMER_ACCEPTED);
        workOrder.setTechnician(technician);
        return workOrder;
    }

    private static SparePart part() {
        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setTenantId(TENANT_ID);
        part.setSku("DW-VALVE-220V-01");
        part.setName("Van cấp nước");
        part.setUnit("cái");
        return part;
    }

    private static WorkOrderPartRequest issuedRequest(WorkOrder workOrder, SparePart part) {
        WorkOrderPartRequest request = new WorkOrderPartRequest();
        request.setId(UUID.randomUUID());
        request.setTenantId(TENANT_ID);
        request.setWorkOrder(workOrder);
        request.setSparePart(part);
        request.setRequestedQuantity(new BigDecimal("3"));
        request.setIssuedQuantity(new BigDecimal("3"));
        request.setStatus(WorkOrderPartRequestStatus.ISSUED);
        request.setIssuedAt(Instant.parse("2026-08-27T01:00:00Z"));
        return request;
    }

    private static void authenticateWarehouse() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("warehouse")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", WAREHOUSE_USER_ID.toString())
                .claim("displayName", "Nhân viên kho")
                .claim("roles", List.of("WAREHOUSE_STAFF"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
