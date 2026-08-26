package com.serviceops.inventory.application;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class WorkOrderPartTestFixtures {
    static final UUID TENANT_ID = UUID.randomUUID();
    static final UUID TECHNICIAN_USER_ID = UUID.randomUUID();
    static final UUID WAREHOUSE_USER_ID = UUID.randomUUID();
    static final UUID OWNER_USER_ID = UUID.randomUUID();
    static final UUID WORK_ORDER_ID = UUID.randomUUID();
    static final UUID PART_ID = UUID.randomUUID();
    static final UUID REQUEST_ID = UUID.randomUUID();

    private WorkOrderPartTestFixtures() {
    }

    static WorkOrderPartRequest pendingRequest(String quantity) {
        WorkOrderPartRequest request = new WorkOrderPartRequest();
        request.setId(REQUEST_ID);
        request.setTenantId(TENANT_ID);
        request.setWorkOrder(workOrder(WorkOrderStatus.IN_PROGRESS));
        request.setSparePart(sparePart("10"));
        request.setRequestedQuantity(new BigDecimal(quantity));
        request.setRequestNote("Thay van cấp nước bị lỗi");
        request.setStatus(WorkOrderPartRequestStatus.REQUESTED);
        request.setRequestedByUserId(TECHNICIAN_USER_ID);
        request.setRequestedByUsername("technician");
        request.setRequestedByDisplayName("Trịnh Quốc Tiến");
        request.setCreatedAt(Instant.parse("2026-08-26T02:00:00Z"));
        return request;
    }

    static WorkOrder workOrder(WorkOrderStatus status) {
        UserAccount user = new UserAccount();
        user.setId(TECHNICIAN_USER_ID);
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
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén Bosch không cấp nước");
        workOrder.setTechnician(technician);
        workOrder.setStatus(status);
        return workOrder;
    }

    static SparePart sparePart(String stock) {
        SparePart part = new SparePart();
        part.setId(PART_ID);
        part.setTenantId(TENANT_ID);
        part.setSku("DW-VALVE-220V-01");
        part.setName("Van cấp nước máy rửa chén 220V");
        part.setUnit("cái");
        part.setStockQuantity(new BigDecimal(stock));
        part.setReorderLevel(new BigDecimal("2"));
        part.setUnitPrice(new BigDecimal("485000"));
        part.setActive(true);
        return part;
    }

    static InventoryTransaction transaction(
            SparePart part,
            InventoryTransactionType type,
            String quantity,
            String createdAt
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setTenantId(TENANT_ID);
        transaction.setSparePart(part);
        transaction.setTransactionType(type);
        transaction.setQuantity(new BigDecimal(quantity));
        transaction.setCreatedAt(Instant.parse(createdAt));
        return transaction;
    }

    static WorkOrderPartUsage usage(WorkOrder workOrder, SparePart part, String quantity) {
        WorkOrderPartUsage usage = new WorkOrderPartUsage();
        usage.setId(UUID.randomUUID());
        usage.setTenantId(TENANT_ID);
        usage.setWorkOrder(workOrder);
        usage.setSparePart(part);
        usage.setUsedQuantity(new BigDecimal(quantity));
        usage.setUpdatedByUserId(TECHNICIAN_USER_ID);
        usage.setUpdatedByUsername("technician");
        usage.setUpdatedByDisplayName("Trịnh Quốc Tiến");
        return usage;
    }

    static void authenticateTechnician() {
        authenticate("technician", "Trịnh Quốc Tiến", TECHNICIAN_USER_ID, "TECHNICIAN");
    }

    static void authenticateWarehouse() {
        authenticate("warehouse", "Nguyễn Nhân viên Kho", WAREHOUSE_USER_ID, "WAREHOUSE_STAFF");
    }

    static void authenticateOwner() {
        authenticate("owner", "Chủ sở hữu", OWNER_USER_ID, "OWNER");
    }

    static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String username, String displayName, UUID userId, String role) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", userId.toString())
                .claim("displayName", displayName)
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
