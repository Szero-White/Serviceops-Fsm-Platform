package com.serviceops.payment.application;

import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.payment.domain.CompanyPaymentProfile;
import com.serviceops.payment.domain.CompanyPaymentProfileRepository;
import com.serviceops.payment.web.PaymentDtos.CompanyPaymentProfileRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyPaymentProfileServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private CompanyPaymentProfileRepository repository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onlyOwnerCanUpdateCompanyPaymentProfile() {
        authenticate("TECHNICIAN", "technician", "Trịnh Quốc Tiến");
        CompanyPaymentProfileService service = service();

        assertThatThrownBy(() -> service.update(new CompanyPaymentProfileRequest(
                "Vietcombank",
                "CONG TY SERVICEOPS",
                "0123456789",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ chủ sở hữu");

        verifyNoInteractions(repository, attachmentRepository, auditService);
    }

    @Test
    void ownerStoresCompanyBankAccountWithActorSnapshot() {
        authenticate("OWNER", "owner", "Nguyễn An Owner");
        when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        CompanyPaymentProfileService service = service();

        service.update(new CompanyPaymentProfileRequest(
                "Vietcombank",
                "CONG TY SERVICEOPS",
                "0123456789",
                null
        ));

        ArgumentCaptor<CompanyPaymentProfile> captor = ArgumentCaptor.forClass(CompanyPaymentProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getBankName()).isEqualTo("Vietcombank");
        assertThat(captor.getValue().getUpdatedByUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getUpdatedByDisplayName()).isEqualTo("Nguyễn An Owner");
    }

    private CompanyPaymentProfileService service() {
        return new CompanyPaymentProfileService(repository, attachmentRepository, auditService);
    }

    private static void authenticate(String role, String username, String displayName) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", displayName)
                .claim("roles", List.of(role))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
