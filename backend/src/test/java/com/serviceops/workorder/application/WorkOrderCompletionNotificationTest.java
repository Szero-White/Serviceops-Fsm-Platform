package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.domain.Priority;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.security.CurrentUser;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderCompletionNotificationTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
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
        authenticateTechnician();
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
        workOrder = inProgressWorkOrder();
        when(repository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingCompletionDetailsStopsBeforeHistoryAuditAndNotification() {
        assertThatThrownBy(() -> service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.COMPLETED, "Bàn giao", "Đã kiểm tra", " ")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phải nhập chẩn đoán và giải pháp");

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        verifyNoInteractions(historyRepository, auditService, notificationService);
    }

    @Test
    void successfulCompletionNotifiesCustomerServiceForFollowUpWithoutSpammingOwner() {
        when(repository.findDetailedAssigned(WORK_ORDER_ID, TENANT_ID, USER_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());

        var response = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(
                        WorkOrderStatus.COMPLETED,
                        "Đã bàn giao vận hành",
                        "Tụ khởi động suy giảm",
                        "Thay tụ và chạy thử ổn định"
                )
        );

        assertThat(response.status()).isEqualTo(WorkOrderStatus.COMPLETED);
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                eq("Phiếu đã hoàn thành: WO-UAT-001"),
                contains("Theo dõi phản hồi khách hàng")
        );
        verify(notificationService, never()).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.OWNER)),
                anyString(),
                anyString()
        );
    }

    private WorkOrder inProgressWorkOrder() {
        technicianUser = new UserAccount();
        technicianUser.setId(USER_ID);
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
        entity.setCode("WO-UAT-001");
        entity.setSummary("Kiểm tra máy lạnh không khởi động");
        entity.setPriority(Priority.NORMAL);
        entity.setStatus(WorkOrderStatus.IN_PROGRESS);
        entity.setCustomer(customer);
        entity.setTechnician(technician);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static void authenticateTechnician() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("technician")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", "Phạm Quốc Kỹ thuật")
                .claim("roles", List.of("TECHNICIAN"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
