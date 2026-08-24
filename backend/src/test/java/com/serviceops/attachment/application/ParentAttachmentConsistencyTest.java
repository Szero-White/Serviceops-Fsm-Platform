package com.serviceops.attachment.application;

import com.serviceops.asset.application.AssetCsvService;
import com.serviceops.asset.application.AssetService;
import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.servicerequest.application.ServiceChannelService;
import com.serviceops.servicerequest.application.ServiceRequestService;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentAttachmentConsistencyTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private AssetRepository assetRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AssetCsvService assetCsvService;
    @Mock private ServiceChannelService serviceChannelService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assetCannotBeHardDeletedWhileAttachmentsStillReferenceIt() {
        authenticateCustomerService();
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setTenantId(TENANT_ID);

        when(assetRepository.findDetailed(assetId, TENANT_ID)).thenReturn(Optional.of(asset));
        when(attachmentRepository.existsByTenantIdAndReferenceTypeAndReferenceId(TENANT_ID, "ASSET", assetId))
                .thenReturn(true);

        AssetService service = new AssetService(
                assetRepository,
                customerRepository,
                serviceRequestRepository,
                workOrderRepository,
                attachmentRepository,
                assetCsvService,
                auditService,
                notificationService
        );

        assertThatThrownBy(() -> service.delete(assetId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ASSET_HAS_ATTACHMENTS");

        verify(assetRepository, never()).delete(asset);
    }

    @Test
    void serviceRequestCannotBeHardDeletedWhileAttachmentsStillReferenceIt() {
        authenticateCustomerService();
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = new ServiceRequest();
        request.setId(requestId);
        request.setTenantId(TENANT_ID);
        request.setStatus(ServiceRequestStatus.OPEN);

        when(serviceRequestRepository.findDetailed(requestId, TENANT_ID)).thenReturn(Optional.of(request));
        when(attachmentRepository.existsByTenantIdAndReferenceTypeAndReferenceId(TENANT_ID, "SERVICE_REQUEST", requestId))
                .thenReturn(true);

        ServiceRequestService service = new ServiceRequestService(
                serviceRequestRepository,
                customerRepository,
                assetRepository,
                serviceChannelService,
                workOrderRepository,
                attachmentRepository,
                auditService,
                notificationService
        );

        assertThatThrownBy(() -> service.delete(requestId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("SERVICE_REQUEST_HAS_ATTACHMENTS");

        verify(serviceRequestRepository, never()).delete(request);
    }

    private static void authenticateCustomerService() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("customer-service")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("roles", List.of("CUSTOMER_SERVICE"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
