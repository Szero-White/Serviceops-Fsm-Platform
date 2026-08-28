package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.payment.application.PaymentReceiptService;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderClosureServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentReceiptService paymentReceiptService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private WorkOrderService workOrderService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerServiceClosesOnlyAfterPaymentIsSettled() {
        authenticate("CUSTOMER_SERVICE");
        WorkOrder workOrder = workOrder();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SETTLED);
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(paymentRepository.findForUpdateByWorkOrder(TENANT_ID, WORK_ORDER_ID)).thenReturn(Optional.of(payment));

        service().close(WORK_ORDER_ID);

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CLOSED);
        ArgumentCaptor<WorkOrderStatusHistory> history = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);
        assertThat(history.getValue().getToStatus()).isEqualTo(WorkOrderStatus.CLOSED);
        verify(paymentReceiptService).issue(WORK_ORDER_ID);
        verify(auditService).record(eq("CLOSE_WORK_ORDER"), eq("WORK_ORDER"), eq(WORK_ORDER_ID), org.mockito.ArgumentMatchers.contains("đối soát"));
    }

    @Test
    void closureIsBlockedWhilePaymentIsStillPending() {
        authenticate("CUSTOMER_SERVICE");
        WorkOrder workOrder = workOrder();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.TRANSFER_PENDING_VERIFICATION);
        when(workOrderRepository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(paymentRepository.findForUpdateByWorkOrder(TENANT_ID, WORK_ORDER_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service().close(WORK_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tiền đã về công ty");

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CUSTOMER_ACCEPTED);
        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private WorkOrderClosureService service() {
        return new WorkOrderClosureService(
                workOrderRepository,
                historyRepository,
                paymentRepository,
                paymentReceiptService,
                auditService,
                notificationService,
                workOrderService
        );
    }

    private static WorkOrder workOrder() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("CUS-001");
        customer.setName("Công ty An Nhiên");

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén");
        workOrder.setCustomer(customer);
        workOrder.setStatus(WorkOrderStatus.CUSTOMER_ACCEPTED);
        return workOrder;
    }

    private static void authenticate(String role) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("customer-service")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", "Lê Thu CSKH")
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
