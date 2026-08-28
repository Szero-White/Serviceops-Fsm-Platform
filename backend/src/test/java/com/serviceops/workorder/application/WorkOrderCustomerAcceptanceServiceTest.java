package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.payment.application.PaymentService;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderBillingDtos.CustomerAcceptanceRequest;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderCustomerAcceptanceServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private WorkOrderBillingService billingService;
    @Mock private PaymentService paymentService;
    @Mock private AuditService auditService;
    @Mock private WorkOrderService workOrderService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptanceFreezesBillingAndCreatesUnpaidPaymentBeforeChangingStatus() {
        authenticateTechnician();
        WorkOrder workOrder = completedAssignedWorkOrder();
        WorkOrderBillingSnapshot snapshot = new WorkOrderBillingSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setTenantId(TENANT_ID);
        snapshot.setWorkOrder(workOrder);

        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(billingService.freezeForCustomerAcceptance(workOrder)).thenReturn(snapshot);

        WorkOrderCustomerAcceptanceService service = new WorkOrderCustomerAcceptanceService(
                workOrderRepository,
                historyRepository,
                billingService,
                paymentService,
                auditService,
                workOrderService
        );
        service.accept(WORK_ORDER_ID, new CustomerAcceptanceRequest("Khách đã kiểm tra và đồng ý"));

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);
        verify(paymentService).initializeUnpaid(workOrder, snapshot);
        ArgumentCaptor<WorkOrderStatusHistory> history = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(history.getValue().getToStatus()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);
        assertThat(history.getValue().getActorRole()).isEqualTo("TECHNICIAN");
    }

    private static WorkOrder completedAssignedWorkOrder() {
        UserAccount user = new UserAccount();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername("technician");
        user.setDisplayName("Trịnh Quốc Tiến");
        user.setRole(UserRole.TECHNICIAN);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(user);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén");
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setTechnician(technician);
        return workOrder;
    }

    private static void authenticateTechnician() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("technician")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", "Trịnh Quốc Tiến")
                .claim("roles", List.of("TECHNICIAN"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
