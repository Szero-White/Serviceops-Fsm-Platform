package com.serviceops.attachment.application;

import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.attachment.domain.AttachmentPurpose;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentLifecycleTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock private AttachmentRepository repository;
    @Mock private FileStorageService storageService;
    @Mock private AuditService auditService;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;

    private AttachmentService service;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(
                repository,
                storageService,
                auditService,
                workOrderRepository,
                assetRepository,
                serviceRequestRepository
        );
        authenticateTechnician();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void workEvidenceCannotBeUploadedAfterCustomerAcceptance() {
        WorkOrder accepted = workOrder(WorkOrderStatus.CUSTOMER_ACCEPTED);
        allowTechnicianAccess(accepted);
        MockMultipartFile image = png("after-acceptance.png");

        assertThatThrownBy(() -> service.upload(
                "WORK_ORDER",
                WORK_ORDER_ID,
                AttachmentPurpose.WORK_EVIDENCE,
                image
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("WORK_EVIDENCE_FROZEN");

        verify(storageService, never()).store(any(), any());
    }

    @Test
    void paymentEvidenceCanBePreparedAfterCustomerAcceptanceAndStartsUnlocked() {
        WorkOrder accepted = workOrder(WorkOrderStatus.CUSTOMER_ACCEPTED);
        allowTechnicianAccess(accepted);
        MockMultipartFile image = png("bank-transfer.png");
        when(storageService.store(eq(image), any()))
                .thenReturn(new FileStorageService.StoredFile(
                        "tenant/work_order/bank-transfer.png",
                        "bank-transfer.png",
                        "image/png",
                        image.getSize()
                ));

        var response = service.upload(
                "WORK_ORDER",
                WORK_ORDER_ID,
                AttachmentPurpose.PAYMENT_EVIDENCE,
                image
        );

        assertThat(response.purpose()).isEqualTo(AttachmentPurpose.PAYMENT_EVIDENCE);
        assertThat(response.locked()).isFalse();
        assertThat(response.manageable()).isTrue();
        verify(repository).save(any());
    }

    @Test
    void paymentEvidenceRejectsPdfEvenThoughWorkDocumentsMayUsePdf() {
        WorkOrder accepted = workOrder(WorkOrderStatus.CUSTOMER_ACCEPTED);
        allowTechnicianAccess(accepted);
        MockMultipartFile pdf = new MockMultipartFile(
                "file",
                "transfer.pdf",
                "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}
        );

        assertThatThrownBy(() -> service.upload(
                "WORK_ORDER",
                WORK_ORDER_ID,
                AttachmentPurpose.PAYMENT_EVIDENCE,
                pdf
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_EVIDENCE_IMAGE_REQUIRED");

        verify(storageService, never()).store(any(), any());
    }

    private void allowTechnicianAccess(WorkOrder workOrder) {
        when(workOrderRepository.findDetailedAssigned(WORK_ORDER_ID, TENANT_ID, USER_ID))
                .thenReturn(Optional.of(workOrder));
        when(workOrderRepository.findDetailed(WORK_ORDER_ID, TENANT_ID))
                .thenReturn(Optional.of(workOrder));
    }

    private static MockMultipartFile png(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
    }

    private static WorkOrder workOrder(WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setStatus(status);
        return workOrder;
    }

    private static void authenticateTechnician() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("technician")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("roles", List.of("TECHNICIAN"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
