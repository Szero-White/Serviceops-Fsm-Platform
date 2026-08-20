package com.serviceops.attachment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.attachment.web.AttachmentController.AttachmentResponse;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {
    private final AttachmentRepository repository;
    private final FileStorageService storageService;
    private final AuditService auditService;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NotificationService notificationService;

    @Transactional
    public AttachmentResponse upload(String referenceType, UUID referenceId, MultipartFile file) {
        String normalizedType = referenceType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("WORK_ORDER", "ASSET", "SERVICE_REQUEST").contains(normalizedType)) {
            throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
        UUID tenantId = CurrentUser.tenantId();
        authorizeReference(normalizedType, referenceId, tenantId);
        var stored = storageService.store(file, tenantId + "/" + normalizedType.toLowerCase(Locale.ROOT));
        deleteStoredFileIfTransactionRollsBack(stored.storageKey());

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
        notificationService.notifyRoles(tenantId, notificationRoles(normalizedType), "Tệp đính kèm mới", attachment.getOriginalFilename());
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(String referenceType, UUID referenceId) {
        String normalizedType = referenceType.trim().toUpperCase(Locale.ROOT);
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

    @Transactional
    public AttachmentResponse rename(UUID id, String originalFilename) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = getAuthorizedAttachment(id, tenantId);
        authorizeManage(attachment);
        String sanitizedFilename = sanitizeFilename(originalFilename);
        if (sanitizedFilename.equals(attachment.getOriginalFilename())) {
            return toResponse(attachment);
        }

        String oldFilename = attachment.getOriginalFilename();
        attachment.setOriginalFilename(sanitizedFilename);
        auditService.record("RENAME_FILE", attachment.getReferenceType(), attachment.getReferenceId(),
                "Đổi tên file " + oldFilename + " thành " + sanitizedFilename);
        return toResponse(attachment);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = getAuthorizedAttachment(id, tenantId);
        authorizeManage(attachment);
        repository.delete(attachment);
        deleteStoredFileAfterCommit(attachment.getStorageKey());
        auditService.record("DELETE_FILE", attachment.getReferenceType(), attachment.getReferenceId(),
                "Xoá file " + attachment.getOriginalFilename());
    }


    private void deleteStoredFileIfTransactionRollsBack(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    safeDeleteStoredFile(storageKey, "rollback cleanup");
                }
            }
        });
    }

    private void deleteStoredFileAfterCommit(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storageService.delete(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDeleteStoredFile(storageKey, "post-commit delete");
            }
        });
    }

    private void safeDeleteStoredFile(String storageKey, String operation) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException ex) {
            log.error("Attachment storage {} failed for key {}", operation, storageKey, ex);
        }
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
                if (!hasAnyRole("OWNER", "DISPATCHER", "CUSTOMER_SERVICE")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Bạn không có quyền truy cập file thiết bị");
                }
                assetRepository.findDetailed(referenceId, tenantId)
                        .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy thiết bị"));
            }
            case "SERVICE_REQUEST" -> {
                if (!hasAnyRole("OWNER", "DISPATCHER", "CUSTOMER_SERVICE")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Bạn không có quyền truy cập file yêu cầu dịch vụ");
                }
                serviceRequestRepository.findDetailed(referenceId, tenantId)
                        .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
            }
            default -> throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
    }

    private static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (CurrentUser.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    private Attachment getAuthorizedAttachment(UUID id, UUID tenantId) {
        Attachment attachment = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Khong tim thay file dinh kem"));
        authorizeReference(attachment.getReferenceType(), attachment.getReferenceId(), tenantId);
        return attachment;
    }

    private void authorizeManage(Attachment attachment) {
        if (CurrentUser.hasRole("OWNER") || CurrentUser.username().equals(attachment.getUploadedBy())) {
            return;
        }
        throw BusinessException.forbidden("ATTACHMENT_MANAGE_DENIED", "Bạn không có quyền chỉnh sửa file đính kèm này");
    }

    private static String sanitizeFilename(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_REQUIRED", "Ten file khong duoc de trong");
        }
        final String normalized;
        try {
            normalized = Path.of(raw).getFileName().toString();
        } catch (RuntimeException ex) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_INVALID", "Tên file không hợp lệ");
        }
        if (normalized.length() > 255) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_TOO_LONG", "Tên file không được vượt quá 255 ký tự");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_INVALID", "Tên file không được chứa ký tự điều khiển");
        }
        return normalized;
    }

    private static List<UserRole> notificationRoles(String referenceType) {
        return switch (referenceType) {
            case "SERVICE_REQUEST" -> List.of(UserRole.OWNER, UserRole.DISPATCHER, UserRole.CUSTOMER_SERVICE);
            case "WORK_ORDER" -> List.of(UserRole.OWNER, UserRole.DISPATCHER);
            default -> List.of(UserRole.OWNER);
        };
    }

    private static AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getReferenceType(), a.getReferenceId(), a.getUploadedBy(), a.getCreatedAt());
    }

    public record DownloadedAttachment(String filename, String contentType, Resource resource) {}
}
