package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.domain.Priority;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderCustomerServiceLifecycleTest {
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
        workOrder = completedWorkOrder();
        when(repository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerServiceCannotRecordCustomerAcceptanceOrCloseTheWorkOrder() {
        authenticate(UserRole.CUSTOMER_SERVICE, "customer-service", "Lê Thu CSKH");

        assertThatThrownBy(() -> service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CUSTOMER_ACCEPTED, null, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chăm sóc khách hàng chỉ mở lại hoặc hủy phiếu");

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        verifyNoInteractions(historyRepository, auditService, notificationService);
    }

    @Test
    void customerServiceCanReopenBeforeClosureWhenCustomerReportsTheIssuePersists() {
        authenticate(UserRole.CUSTOMER_SERVICE, "customer-service", "Lê Thu CSKH");
        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());

        var reopened = service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.REOPENED, "Khách phản hồi lỗi vẫn còn", null, null)
        );

        assertThat(reopened.status()).isEqualTo(WorkOrderStatus.REOPENED);
        var expectedNotification = NotificationCopy.workOrderReopenedAttention(
                new NotificationCopy.WorkOrderContext(
                        "WO-UAT-CS-001",
                        "Kiểm tra máy lạnh",
                        "Khách hàng UAT"
                ),
                "Chăm sóc khách hàng Lê Thu CSKH",
                "Khách phản hồi lỗi vẫn còn"
        );
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.OWNER, UserRole.DISPATCHER)),
                eq(expectedNotification.title()),
                eq(expectedNotification.message())
        );
        verify(notificationService, never()).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );

        ArgumentCaptor<WorkOrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("customer-service");
        assertThat(historyCaptor.getValue().getActorDisplayName()).isEqualTo("Lê Thu CSKH");
        assertThat(historyCaptor.getValue().getActorRole()).isEqualTo("CUSTOMER_SERVICE");
    }

    @Test
    void reopenByAnotherRoleCreatesCustomerServiceFollowUpNotification() {
        authenticate(UserRole.OWNER, "owner", "Nguyễn An Owner");
        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());

        service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.REOPENED, "Khách phản hồi lỗi vẫn còn", null, null)
        );

        var expected = NotificationCopy.workOrderReopenedForCustomerService(
                new NotificationCopy.WorkOrderContext("WO-UAT-CS-001", "Kiểm tra máy lạnh", "Khách hàng UAT"),
                "Chủ sở hữu Nguyễn An Owner",
                "Khách phản hồi lỗi vẫn còn"
        );
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                eq(expected.title()),
                eq(expected.message())
        );
    }

    @Test
    void cancellationByDispatcherCreatesCustomerServiceCustomerCommunicationNotification() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        authenticate(UserRole.DISPATCHER, "dispatcher", "Lê Thu Điều phối");
        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());

        service.transition(
                WORK_ORDER_ID,
                new TransitionWorkOrder(WorkOrderStatus.CANCELLED, "Khách yêu cầu hủy lịch", null, null)
        );

        var expected = NotificationCopy.workOrderCancelledForCustomerService(
                new NotificationCopy.WorkOrderContext("WO-UAT-CS-001", "Kiểm tra máy lạnh", "Khách hàng UAT"),
                "Điều phối viên Lê Thu Điều phối",
                "Khách yêu cầu hủy lịch"
        );
        verify(notificationService).notifyRoles(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                eq(expected.title()),
                eq(expected.message())
        );
    }

    private static WorkOrder completedWorkOrder() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("CUS-UAT");
        customer.setName("Khách hàng UAT");

        WorkOrder entity = new WorkOrder();
        entity.setId(WORK_ORDER_ID);
        entity.setTenantId(TENANT_ID);
        entity.setCode("WO-UAT-CS-001");
        entity.setSummary("Kiểm tra máy lạnh");
        entity.setPriority(Priority.NORMAL);
        entity.setStatus(WorkOrderStatus.COMPLETED);
        entity.setCustomer(customer);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static void authenticate(UserRole role, String username, String displayName) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", displayName)
                .claim("roles", List.of(role.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
