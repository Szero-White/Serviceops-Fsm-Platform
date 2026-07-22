package com.serviceops.attachment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.attachment.web.AttachmentController.AttachmentResponse;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository repository;
    private final FileStorageService storageService;
    private final AuditService auditService;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Transactional
    public AttachmentResponse upload(String referenceType, UUID referenceId, MultipartFile file) {
        String normalizedType = referenceType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("WORK_ORDER", "ASSET", "SERVICE_REQUEST").contains(normalizedType)) {
            throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
        UUID tenantId = CurrentUser.tenantId();
        authorizeReference(normalizedType, referenceId, tenantId);
        var stored = storageService.store(file, tenantId + "/" + normalizedType.toLowerCase(Locale.ROOT));
        Attachment attachment = new Attachment();
        attachment.setTenantId(tenantId);
        attachment.setOriginalFilename(stored.originalFilename());
        attachment.setStorageKey(stored.storageKey());
        attachment.setContentType(stored.contentType());
        attachment.setFileSize(stored.size());
        attachment.setReferenceType(normalizedType);
        attachment.setReferenceId(referenceId);
        attachment.setUploadedBy(CurrentUser.username());
        repository.save(attachment);
        auditService.record("UPLOAD_FILE", normalizedType, referenceId, "Tải file " + attachment.getOriginalFilename());
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(String referenceType, UUID referenceId) {
        String normalizedType = referenceType.toUpperCase(Locale.ROOT);
        UUID tenantId = CurrentUser.tenantId();
        authorizeReference(normalizedType, referenceId, tenantId);
        return repository.findByTenantIdAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(tenantId, normalizedType, referenceId)
                .stream().map(AttachmentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(UUID id) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Không tìm thấy file đính kèm"));
        authorizeReference(attachment.getReferenceType(), attachment.getReferenceId(), tenantId);
        return new DownloadedAttachment(attachment.getOriginalFilename(), attachment.getContentType(), storageService.load(attachment.getStorageKey()));
    }


    private void authorizeReference(String referenceType, UUID referenceId, UUID tenantId) {
        switch (referenceType) {
            case "WORK_ORDER" -> {
                var workOrder = CurrentUser.hasRole("TECHNICIAN")
                        ? workOrderRepository.findDetailedAssigned(referenceId, tenantId, CurrentUser.userId())
                        : workOrderRepository.findDetailed(referenceId, tenantId);
                if (workOrder.isEmpty()) {
                    throw BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy đối tượng đính kèm");
                }
            }
            case "ASSET" -> {
                if (CurrentUser.hasRole("TECHNICIAN")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Kỹ thuật viên không được truy cập file thiết bị ngoài work order");
                }
                assetRepository.findDetailed(referenceId, tenantId)
                        .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy thiết bị"));
            }
            case "SERVICE_REQUEST" -> {
                if (CurrentUser.hasRole("TECHNICIAN")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Kỹ thuật viên không được truy cập file yêu cầu dịch vụ");
                }
                serviceRequestRepository.findDetailed(referenceId, tenantId)
                        .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
            }
            default -> throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
    }

    private static AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getReferenceType(), a.getReferenceId(), a.getUploadedBy(), a.getCreatedAt());
    }

    public record DownloadedAttachment(String filename, String contentType, Resource resource) {}
}
