package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class WorkOrderActivityDetailTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock private WorkOrderRepository repository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        authenticateOwner();
        service = new WorkOrderService(
                repository,
                historyRepository,
                inventoryTransactionRepository,
                serviceRequestRepository,
                technicianRepository,
                appointmentRepository,
                auditService,
                notificationService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void detailedWorkOrderIncludesPartUsageInActivityTimelineWithoutReplacingStatusHistory() {
        WorkOrder workOrder = workOrder();
        WorkOrderStatusHistory status = statusHistory();
        InventoryTransaction consumed = consumedPart();

        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(List.of(status));
        when(inventoryTransactionRepository.findPartUsageForWorkOrder(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(List.of(consumed));

        var response = service.get(WORK_ORDER_ID);

        assertThat(response.history()).hasSize(1);
        assertThat(response.history().getFirst().toStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(response.history().getFirst().actorDisplayName()).isEqualTo("Phạm Quốc Kỹ thuật");
        assertThat(response.history().getFirst().actorRole()).isEqualTo("TECHNICIAN");
        assertThat(response.activities()).hasSize(2);
        assertThat(response.activities()).extracting(activity -> activity.type()).containsExactly(
                WorkOrderActivityType.STATUS_CHANGE,
                WorkOrderActivityType.PART_CONSUMED
        );
        assertThat(response.activities().get(1).sparePartName()).isEqualTo("Lưới lọc máy lạnh tiêu chuẩn");
        assertThat(response.activities().get(1).quantity()).isEqualByComparingTo("4.000");
    }

    private static WorkOrder workOrder() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("CUS-001");
        customer.setName("Khách hàng UAT");

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCustomer(customer);
        workOrder.setCode("WO-2026-001007");
        workOrder.setSummary("Kiểm tra máy lạnh không khởi động");
        workOrder.setPriority(Priority.NORMAL);
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrder.setCreatedAt(Instant.parse("2026-08-24T03:20:00Z"));
        return workOrder;
    }

    private static WorkOrderStatusHistory statusHistory() {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setId(UUID.randomUUID());
        history.setTenantId(TENANT_ID);
        history.setToStatus(WorkOrderStatus.IN_PROGRESS);
        history.setChangedBy("technician");
        history.setActorDisplayName("Phạm Quốc Kỹ thuật");
        history.setActorRole("TECHNICIAN");
        history.setCreatedAt(Instant.parse("2026-08-24T03:23:00Z"));
        return history;
    }

    private static InventoryTransaction consumedPart() {
        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setTenantId(TENANT_ID);
        part.setSku("FILTER-AC-01");
        part.setName("Lưới lọc máy lạnh tiêu chuẩn");
        part.setUnit("cái");

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setTenantId(TENANT_ID);
        transaction.setSparePart(part);
        transaction.setTransactionType(InventoryTransactionType.CONSUME);
        transaction.setQuantity(new BigDecimal("4.000"));
        transaction.setCreatedBy("technician");
        transaction.setActorDisplayName("Phạm Quốc Kỹ thuật");
        transaction.setActorRole("TECHNICIAN");
        transaction.setNote("Lắp thay cho khách");
        transaction.setCreatedAt(Instant.parse("2026-08-24T03:40:00Z"));
        return transaction;
    }

    private static void authenticateOwner() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("owner")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", UUID.randomUUID().toString())
                .claim("roles", List.of("OWNER"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
