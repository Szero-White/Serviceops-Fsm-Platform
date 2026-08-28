package com.serviceops.payment.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.payment.domain.PaymentReceiptRepository;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingItemRepository;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
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
import java.nio.charset.StandardCharsets;
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
class PaymentReceiptServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentReceiptRepository receiptRepository;
    @Mock private WorkOrderBillingItemRepository billingItemRepository;
    @Mock private PaymentReceiptHtmlRenderer renderer;
    @Mock private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerServiceIssuesImmutableReceiptSnapshotAfterSettlement() {
        authenticate("CUSTOMER_SERVICE");
        Payment payment = settledPayment();
        UUID workOrderId = payment.getWorkOrder().getId();
        when(paymentRepository.findForUpdateByWorkOrder(TENANT_ID, workOrderId)).thenReturn(Optional.of(payment));
        when(receiptRepository.findByWorkOrder(TENANT_ID, workOrderId)).thenReturn(Optional.empty());
        when(billingItemRepository.findByTenantIdAndBillingSnapshotIdOrderBySparePartNameAsc(TENANT_ID, payment.getBillingSnapshot().getId())).thenReturn(List.of());
        when(renderer.render(any(), any(), any())).thenReturn("<html>receipt</html>");

        byte[] result = service().issue(workOrderId);

        assertThat(new String(result, StandardCharsets.UTF_8)).contains("receipt");
        ArgumentCaptor<PaymentReceipt> receipt = ArgumentCaptor.forClass(PaymentReceipt.class);
        verify(receiptRepository).save(receipt.capture());
        assertThat(receipt.getValue().getAmount()).isEqualByComparingTo("1270000");
        assertThat(receipt.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(receipt.getValue().getCustomerNameSnapshot()).isEqualTo("Công ty An Nhiên");
        assertThat(receipt.getValue().getReceiptCode()).isEqualTo("BN-WO-2026-001234");
    }

    @Test
    void receiptCannotBeIssuedBeforePaymentSettlement() {
        authenticate("CUSTOMER_SERVICE");
        Payment payment = settledPayment();
        payment.setStatus(PaymentStatus.TRANSFER_PENDING_VERIFICATION);
        UUID workOrderId = payment.getWorkOrder().getId();
        when(paymentRepository.findForUpdateByWorkOrder(TENANT_ID, workOrderId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service().issue(workOrderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đối soát tiền");

        verify(receiptRepository, never()).save(any());
    }

    private PaymentReceiptService service() {
        return new PaymentReceiptService(
                paymentRepository,
                receiptRepository,
                billingItemRepository,
                renderer,
                auditService
        );
    }

    private static Payment settledPayment() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("CUS-001");
        customer.setName("Công ty An Nhiên");

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCode("WO-2026-001234");
        workOrder.setSummary("Sửa máy rửa chén");
        workOrder.setCustomer(customer);

        WorkOrderBillingSnapshot snapshot = new WorkOrderBillingSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setTenantId(TENANT_ID);
        snapshot.setWorkOrder(workOrder);
        snapshot.setTotalAmount(new BigDecimal("1270000"));

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTenantId(TENANT_ID);
        payment.setWorkOrder(workOrder);
        payment.setBillingSnapshot(snapshot);
        payment.setAmount(new BigDecimal("1270000"));
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setSettledAt(Instant.now());
        payment.setSettledByDisplayName("Lê Thu CSKH");
        return payment;
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
