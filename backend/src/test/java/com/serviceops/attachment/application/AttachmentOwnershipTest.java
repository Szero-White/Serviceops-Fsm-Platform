package com.serviceops.attachment.application;

import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.attachment.domain.Attachment;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentOwnershipTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ATTACHMENT_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock
    private AttachmentRepository repository;
    @Mock
    private FileStorageService storageService;
    @Mock
    private AuditService auditService;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

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
        WorkOrder activeWorkOrder = workOrder(WorkOrderStatus.IN_PROGRESS);
        org.mockito.Mockito.lenient().when(workOrderRepository.findDetailed(WORK_ORDER_ID, TENANT_ID))
                .thenReturn(Optional.of(activeWorkOrder));
        org.mockito.Mockito.lenient().when(workOrderRepository.findDetailedAssigned(WORK_ORDER_ID, TENANT_ID, USER_ID))
                .thenReturn(Optional.of(activeWorkOrder));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanRenameAttachmentUploadedByAnotherUser() {
        authenticate("owner", "OWNER");
        Attachment attachment = attachmentUploadedBy("warehouse");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        var response = service.rename(ATTACHMENT_ID, "owner-renamed.pdf");

        assertThat(response.originalFilename()).isEqualTo("owner-renamed.pdf");
    }

    @Test
    void uploaderCanRenameOwnAttachment() {
        authenticate("customer-service", "CUSTOMER_SERVICE");
        Attachment attachment = attachmentUploadedBy("customer-service");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        var response = service.rename(ATTACHMENT_ID, "customer-service-renamed.pdf");

        assertThat(response.originalFilename()).isEqualTo("customer-service-renamed.pdf");
    }

    @Test
    void warehouseCannotRenameOwnersAttachment() {
        authenticate("warehouse", "WAREHOUSE_STAFF");
        Attachment attachment = attachmentUploadedBy("owner");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.rename(ATTACHMENT_ID, "forbidden.pdf"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ATTACHMENT_ACCESS_DENIED");
    }

    @Test
    void dispatcherCannotRenameAnotherUsersAttachment() {
        authenticate("dispatcher", "DISPATCHER");
        Attachment attachment = attachmentUploadedBy("owner");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.rename(ATTACHMENT_ID, "dispatcher-renamed.pdf"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ATTACHMENT_MANAGE_DENIED");
    }

    @Test
    void nonOwnerCannotDeleteAnotherUsersAttachment() {
        authenticate("customer-service", "CUSTOMER_SERVICE");
        Attachment attachment = attachmentUploadedBy("owner");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.delete(ATTACHMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ATTACHMENT_MANAGE_DENIED");

        verify(repository, never()).delete(attachment);
        verify(storageService, never()).delete(attachment.getStorageKey());
    }

    @Test
    void workEvidenceBecomesReadOnlyAfterCustomerAcceptance() {
        authenticate("owner", "OWNER");
        Attachment attachment = attachmentUploadedBy("owner");
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));
        when(workOrderRepository.findDetailed(WORK_ORDER_ID, TENANT_ID))
                .thenReturn(Optional.of(workOrder(WorkOrderStatus.CUSTOMER_ACCEPTED)));

        assertThatThrownBy(() -> service.rename(ATTACHMENT_ID, "cannot-change.pdf"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ATTACHMENT_LOCKED");
    }

    @Test
    void dispatcherCannotDownloadPaymentEvidence() {
        authenticate("dispatcher", "DISPATCHER");
        Attachment attachment = attachmentUploadedBy("technician");
        attachment.setPurpose(AttachmentPurpose.PAYMENT_EVIDENCE);
        attachment.setLockedAt(Instant.now());
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.download(ATTACHMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_EVIDENCE_ACCESS_DENIED");

        verify(storageService, never()).load(attachment.getStorageKey());
    }

    @Test
    void paymentEvidenceCannotBeDeletedAfterItIsLocked() {
        authenticate("technician", "TECHNICIAN");
        Attachment attachment = attachmentUploadedBy("technician");
        attachment.setPurpose(AttachmentPurpose.PAYMENT_EVIDENCE);
        attachment.setLockedAt(Instant.now());
        when(repository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.delete(ATTACHMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ATTACHMENT_LOCKED");

        verify(repository, never()).delete(attachment);
    }

    private static Attachment attachmentUploadedBy(String username) {
        Attachment attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setTenantId(TENANT_ID);
        attachment.setOriginalFilename("evidence.pdf");
        attachment.setStorageKey("tenant/work_order/evidence.pdf");
        attachment.setContentType("application/pdf");
        attachment.setFileSize(1024);
        attachment.setReferenceType("WORK_ORDER");
        attachment.setReferenceId(WORK_ORDER_ID);
        attachment.setUploadedBy(username);
        attachment.setPurpose(AttachmentPurpose.WORK_EVIDENCE);
        attachment.setCreatedAt(Instant.now());
        attachment.setUpdatedAt(Instant.now());
        return attachment;
    }

    private static WorkOrder workOrder(WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        workOrder.setStatus(status);
        return workOrder;
    }

    private static void authenticate(String username, String role) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(username)
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("roles", List.of(role))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
