package com.serviceops.payment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.payment.domain.CompanyPaymentProfile;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_ID = UUID.randomUUID();
    private static final UUID CS_ID = UUID.randomUUID();

    @Mock private PaymentRepository repository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private CompanyPaymentProfileService companyPaymentProfileService;
    @Mock private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cashCollectionTracksTechnicianCustodyWithoutSettlingCompanyReceipt() {
        authenticate("TECHNICIAN", TECHNICIAN_ID, "technician", "Trịnh Quốc Tiến");
        Payment payment = payment(PaymentStatus.UNPAID);
        when(repository.findForUpdateByWorkOrder(TENANT_ID, payment.getWorkOrder().getId())).thenReturn(Optional.of(payment));

        var service = service();
        var response = service.recordCashCollection(payment.getWorkOrder().getId());

        assertThat(response.status()).isEqualTo(PaymentStatus.CASH_PENDING_HANDOVER);
        assertThat(response.method()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getCollectedByUserId()).isEqualTo(TECHNICIAN_ID);
        assertThat(payment.getSettledAt()).isNull();
    }

    @Test
    void bankTransferKeepsEvidenceAndStaysPendingUntilCustomerServiceVerifiesCompanyReceipt() {
        authenticate("TECHNICIAN", TECHNICIAN_ID, "technician", "Trịnh Quốc Tiến");
        Payment payment = payment(PaymentStatus.UNPAID);
        UUID evidenceId = UUID.randomUUID();
        Attachment evidence = new Attachment();
        evidence.setId(evidenceId);
        evidence.setTenantId(TENANT_ID);
        evidence.setReferenceType("WORK_ORDER");
        evidence.setReferenceId(payment.getWorkOrder().getId());
        when(repository.findForUpdateByWorkOrder(TENANT_ID, payment.getWorkOrder().getId())).thenReturn(Optional.of(payment));
        when(companyPaymentProfileService.requireConfigured()).thenReturn(new CompanyPaymentProfile());
        when(attachmentRepository.findByIdAndTenantId(evidenceId, TENANT_ID)).thenReturn(Optional.of(evidence));

        var service = service();
        var response = service.reportTransfer(payment.getWorkOrder().getId(), evidenceId);

        assertThat(response.status()).isEqualTo(PaymentStatus.TRANSFER_PENDING_VERIFICATION);
        assertThat(response.method()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(response.transferEvidenceAttachmentId()).isEqualTo(evidenceId);
        assertThat(payment.getSettledAt()).isNull();
    }

    @Test
    void customerServiceSettlesCashOnlyAfterHandover() {
        authenticate("CUSTOMER_SERVICE", CS_ID, "customer-service", "Lê Thu CSKH");
        Payment payment = payment(PaymentStatus.CASH_PENDING_HANDOVER);
        when(repository.findForUpdate(TENANT_ID, payment.getId())).thenReturn(Optional.of(payment));

        var service = service();
        var response = service.settleCash(payment.getId());

        assertThat(response.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(payment.getSettledByUserId()).isEqualTo(CS_ID);
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("CONFIRM_CASH_HANDOVER"), org.mockito.ArgumentMatchers.eq("PAYMENT"), org.mockito.ArgumentMatchers.eq(payment.getId()), org.mockito.ArgumentMatchers.contains(payment.getWorkOrder().getCode()));
    }

    @Test
    void customerServiceSettlesTransferOnlyAfterVerification() {
        authenticate("CUSTOMER_SERVICE", CS_ID, "customer-service", "Lê Thu CSKH");
        Payment payment = payment(PaymentStatus.TRANSFER_PENDING_VERIFICATION);
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        when(repository.findForUpdate(TENANT_ID, payment.getId())).thenReturn(Optional.of(payment));

        var response = service().settleTransfer(payment.getId());

        assertThat(response.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(response.method()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(payment.getSettledByUserId()).isEqualTo(CS_ID);
        verify(auditService).record(
                org.mockito.ArgumentMatchers.eq("VERIFY_BANK_TRANSFER"),
                org.mockito.ArgumentMatchers.eq("PAYMENT"),
                org.mockito.ArgumentMatchers.eq(payment.getId()),
                org.mockito.ArgumentMatchers.contains(payment.getWorkOrder().getCode())
        );
    }

    private PaymentService service() {
        return new PaymentService(repository, attachmentRepository, companyPaymentProfileService, auditService);
    }

    private static Payment payment(PaymentStatus status) {
        UserAccount user = new UserAccount();
        user.setId(TECHNICIAN_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername("technician");
        user.setDisplayName("Trịnh Quốc Tiến");
        user.setRole(UserRole.TECHNICIAN);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(user);

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
        workOrder.setStatus(WorkOrderStatus.CUSTOMER_ACCEPTED);
        workOrder.setTechnician(technician);
        workOrder.setCustomer(customer);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTenantId(TENANT_ID);
        payment.setWorkOrder(workOrder);
        payment.setAmount(new BigDecimal("1270000"));
        payment.setStatus(status);
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    private static void authenticate(String role, UUID userId, String username, String displayName) {
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
