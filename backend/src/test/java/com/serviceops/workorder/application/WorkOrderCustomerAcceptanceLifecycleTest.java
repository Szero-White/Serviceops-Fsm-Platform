package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderCustomerAcceptanceLifecycleTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_USER_ID = UUID.randomUUID();
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
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
    private WorkOrder workOrder;
    private UserAccount technicianUser;

    @BeforeEach
    void setUp() {
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
        workOrder = completedAssignedWorkOrder();
        when(repository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignedTechnicianCanRecordCustomerAcceptanceAndClose() {
        authenticate(UserRole.TECHNICIAN, TECHNICIAN_USER_ID, "technician", "Phạm Quốc Kỹ thuật");
        when(repository.findDetailedAssigned(WORK_ORDER_ID, TENANT_ID, TECHNICIAN_USER_ID)).thenReturn(Optional.of(workOrder));

        var accepted = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CUSTOMER_ACCEPTED, "Khách đã kiểm tra và đồng ý", null, null)
        );
        assertThat(accepted.status()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);

        var closed = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CLOSED, "Đã bàn giao và kết thúc công việc", null, null)
        );
        assertThat(closed.status()).isEqualTo(WorkOrderStatus.CLOSED);
        verifyNoInteractions(notificationService);
    }

    @Test
    void assignedTechnicianCanReopenBeforeClosureWhenSameIssuePersists() {
        authenticate(UserRole.TECHNICIAN, TECHNICIAN_USER_ID, "technician", "Phạm Quốc Kỹ thuật");
        when(repository.findDetailedAssigned(WORK_ORDER_ID, TENANT_ID, TECHNICIAN_USER_ID)).thenReturn(Optional.of(workOrder));

        var reopened = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.REOPENED, "Khách báo lỗi vẫn còn trước khi đóng", null, null)
        );

        assertThat(reopened.status()).isEqualTo(WorkOrderStatus.REOPENED);
        var expectedNotification = NotificationCopy.workOrderReopenedAttention("WO-UAT-ACCEPT-001");
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.OWNER, UserRole.DISPATCHER)),
                eq(expectedNotification.title()),
                eq(expectedNotification.message())
        );
    }

    @Test
    void ownerCanRecordCustomerAcceptanceAndCloseAsAdminOverride() {
        authenticate(UserRole.OWNER, OWNER_USER_ID, "owner", "Quản trị hệ thống");
        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));

        var accepted = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CUSTOMER_ACCEPTED, "Owner ghi nhận khách đã đồng ý", null, null)
        );
        assertThat(accepted.status()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);

        var closed = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CLOSED, "Owner đóng phiếu", null, null)
        );
        assertThat(closed.status()).isEqualTo(WorkOrderStatus.CLOSED);
        verify(notificationService).create(
                eq(TENANT_ID),
                eq(technicianUser),
                eq("Phiếu đã đóng: WO-UAT-ACCEPT-001"),
                contains("không cần thao tác thêm")
        );
    }

    private WorkOrder completedAssignedWorkOrder() {
        technicianUser = new UserAccount();
        technicianUser.setId(TECHNICIAN_USER_ID);
        technicianUser.setTenantId(TENANT_ID);
        technicianUser.setUsername("technician");
        technicianUser.setDisplayName("Phạm Quốc Kỹ thuật");
        technicianUser.setRole(UserRole.TECHNICIAN);
        technicianUser.setActive(true);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(technicianUser);
        technician.setActive(true);

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("CUS-UAT");
        customer.setName("Khách hàng UAT");

        WorkOrder entity = new WorkOrder();
        entity.setId(WORK_ORDER_ID);
        entity.setTenantId(TENANT_ID);
        entity.setCode("WO-UAT-ACCEPT-001");
        entity.setSummary("Kiểm tra máy lạnh");
        entity.setPriority(Priority.NORMAL);
        entity.setStatus(WorkOrderStatus.COMPLETED);
        entity.setCustomer(customer);
        entity.setTechnician(technician);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static void authenticate(UserRole role, UUID userId, String username, String displayName) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", userId.toString())
                .claim("displayName", displayName)
                .claim("roles", List.of(role.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
