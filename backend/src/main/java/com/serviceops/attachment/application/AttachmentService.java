package com.serviceops.attachment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentPurpose;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.attachment.web.AttachmentController.AttachmentResponse;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {
    private static final Set<String> ALLOWED_REFERENCE_TYPES = Set.of(
            "WORK_ORDER", "ASSET", "SERVICE_REQUEST", "COMPANY_PAYMENT_PROFILE"
    );
    private static final Set<String> PAYMENT_EVIDENCE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<WorkOrderStatus> WORK_EVIDENCE_FROZEN_STATUSES = Set.of(
            WorkOrderStatus.CUSTOMER_ACCEPTED,
            WorkOrderStatus.CLOSED,
            WorkOrderStatus.CANCELLED
    );

    private final AttachmentRepository repository;
    private final FileStorageService storageService;
    private final AuditService auditService;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Transactional
    public AttachmentResponse upload(String referenceType, UUID referenceId, AttachmentPurpose requestedPurpose, MultipartFile file) {
        String normalizedType = normalizeReferenceType(referenceType);
        AttachmentPurpose purpose = normalizePurpose(normalizedType, requestedPurpose);
        UUID tenantId = CurrentUser.tenantId();

        if ("COMPANY_PAYMENT_PROFILE".equals(normalizedType) && !CurrentUser.hasRole("OWNER")) {
            throw BusinessException.forbidden("PAYMENT_PROFILE_UPLOAD_DENIED", "Chỉ chủ sở hữu được tải ảnh QR thanh toán");
        }

        authorizeReference(normalizedType, referenceId, tenantId);
        authorizeUpload(normalizedType, referenceId, purpose, file, tenantId);

        var stored = storageService.store(file, tenantId + "/" + normalizedType.toLowerCase(Locale.ROOT));
        deleteStoredFileIfTransactionRollsBack(stored.storageKey());

        if (purpose == AttachmentPurpose.PAYMENT_EVIDENCE
                && !PAYMENT_EVIDENCE_CONTENT_TYPES.contains(stored.contentType())) {
            storageService.delete(stored.storageKey());
            throw BusinessException.badRequest(
                    "PAYMENT_EVIDENCE_IMAGE_REQUIRED",
                    "Bằng chứng chuyển khoản chỉ nhận ảnh JPG, PNG hoặc WEBP"
            );
        }

        Attachment attachment = new Attachment();
        attachment.setTenantId(tenantId);
        attachment.setOriginalFilename(stored.originalFilename());
        attachment.setStorageKey(stored.storageKey());
        attachment.setContentType(stored.contentType());
        attachment.setFileSize(stored.size());
        attachment.setReferenceType(normalizedType);
        attachment.setReferenceId(referenceId);
        attachment.setUploadedBy(CurrentUser.username());
        attachment.setPurpose(purpose);
        repository.save(attachment);
        auditService.record(
                "UPLOAD_FILE",
                normalizedType,
                referenceId,
                "Tải file " + attachment.getOriginalFilename() + " · " + purpose
        );
        return toResponse(attachment, isLifecycleMutable(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(String referenceType, UUID referenceId) {
        String normalizedType = normalizeReferenceType(referenceType);
        UUID tenantId = CurrentUser.tenantId();
        authorizeReference(normalizedType, referenceId, tenantId);
        List<Attachment> attachments = repository
                .findByTenantIdAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(tenantId, normalizedType, referenceId);

        Boolean workEvidenceMutable = "WORK_ORDER".equals(normalizedType)
                ? isWorkEvidenceMutable(referenceId, tenantId)
                : null;

        return attachments.stream()
                .filter(this::canViewPurpose)
                .map(attachment -> toResponse(
                        attachment,
                        attachment.getPurpose() == AttachmentPurpose.WORK_EVIDENCE
                                ? Boolean.TRUE.equals(workEvidenceMutable)
                                : isLifecycleMutable(attachment)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(UUID id) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Không tìm thấy file đính kèm"));
        authorizeReference(attachment.getReferenceType(), attachment.getReferenceId(), tenantId);
        authorizePurposeView(attachment);
        return new DownloadedAttachment(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                storageService.load(attachment.getStorageKey())
        );
    }

    @Transactional
    public AttachmentResponse rename(UUID id, String originalFilename) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = getAuthorizedAttachment(id, tenantId);
        authorizeManage(attachment);
        String sanitizedFilename = sanitizeFilename(originalFilename);
        if (sanitizedFilename.equals(attachment.getOriginalFilename())) {
            return toResponse(attachment, true);
        }

        String oldFilename = attachment.getOriginalFilename();
        attachment.setOriginalFilename(sanitizedFilename);
        auditService.record(
                "RENAME_FILE",
                attachment.getReferenceType(),
                attachment.getReferenceId(),
                "Đổi tên file " + oldFilename + " thành " + sanitizedFilename
        );
        return toResponse(attachment, true);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = CurrentUser.tenantId();
        Attachment attachment = getAuthorizedAttachment(id, tenantId);
        authorizeManage(attachment);
        repository.delete(attachment);
        deleteStoredFileAfterCommit(attachment.getStorageKey());
        auditService.record(
                "DELETE_FILE",
                attachment.getReferenceType(),
                attachment.getReferenceId(),
                "Xóa file " + attachment.getOriginalFilename()
        );
    }

    private static String normalizeReferenceType(String referenceType) {
        String normalizedType = referenceType == null ? "" : referenceType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_REFERENCE_TYPES.contains(normalizedType)) {
            throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
        return normalizedType;
    }

    private static AttachmentPurpose normalizePurpose(String referenceType, AttachmentPurpose requestedPurpose) {
        AttachmentPurpose purpose = requestedPurpose == null
                ? ("WORK_ORDER".equals(referenceType) ? AttachmentPurpose.WORK_EVIDENCE : AttachmentPurpose.GENERAL)
                : requestedPurpose;

        if ("WORK_ORDER".equals(referenceType)) {
            if (purpose != AttachmentPurpose.WORK_EVIDENCE && purpose != AttachmentPurpose.PAYMENT_EVIDENCE) {
                throw BusinessException.badRequest(
                        "INVALID_ATTACHMENT_PURPOSE",
                        "File phiếu công việc phải là hồ sơ sửa chữa hoặc bằng chứng thanh toán"
                );
            }
            return purpose;
        }

        if (purpose != AttachmentPurpose.GENERAL) {
            throw BusinessException.badRequest(
                    "INVALID_ATTACHMENT_PURPOSE",
                    "Mục đích file không phù hợp với đối tượng đính kèm"
            );
        }
        return purpose;
    }

    private void authorizeUpload(String referenceType,
                                 UUID referenceId,
                                 AttachmentPurpose purpose,
                                 MultipartFile file,
                                 UUID tenantId) {
        if (!"WORK_ORDER".equals(referenceType)) {
            return;
        }

        WorkOrder workOrder = workOrderRepository.findDetailed(referenceId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy phiếu công việc"));

        if (purpose == AttachmentPurpose.WORK_EVIDENCE) {
            if (!hasAnyRole("OWNER", "TECHNICIAN")) {
                throw BusinessException.forbidden(
                        "WORK_EVIDENCE_UPLOAD_DENIED",
                        "Chỉ chủ sở hữu hoặc kỹ thuật viên được phân công mới tải hồ sơ sửa chữa"
                );
            }
            if (WORK_EVIDENCE_FROZEN_STATUSES.contains(workOrder.getStatus())) {
                throw BusinessException.conflict(
                        "WORK_EVIDENCE_FROZEN",
                        "Hình ảnh và tài liệu sửa chữa đã được khóa sau khi hồ sơ hoàn tất"
                );
            }
            return;
        }

        if (!CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden(
                    "PAYMENT_EVIDENCE_UPLOAD_DENIED",
                    "Chỉ kỹ thuật viên được phân công mới tải bằng chứng chuyển khoản"
            );
        }
        if (workOrder.getStatus() != WorkOrderStatus.CUSTOMER_ACCEPTED) {
            throw BusinessException.conflict(
                    "PAYMENT_EVIDENCE_NOT_READY",
                    "Chỉ tải bằng chứng thanh toán sau khi khách đã xác nhận kết quả và chi phí"
            );
        }
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !PAYMENT_EVIDENCE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw BusinessException.badRequest(
                    "PAYMENT_EVIDENCE_IMAGE_REQUIRED",
                    "Bằng chứng chuyển khoản chỉ nhận ảnh JPG, PNG hoặc WEBP"
            );
        }
    }

    private void authorizeReference(String referenceType, UUID referenceId, UUID tenantId) {
        switch (referenceType) {
            case "WORK_ORDER" -> {
                if (CurrentUser.hasRole("TECHNICIAN")) {
                    workOrderRepository.findDetailedAssigned(referenceId, tenantId, CurrentUser.userId())
                            .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy đối tượng đính kèm"));
                } else {
                    if (!hasAnyRole("OWNER", "DISPATCHER", "CUSTOMER_SERVICE")) {
                        throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Bạn không có quyền truy cập file phiếu công việc");
                    }
                    workOrderRepository.findDetailed(referenceId, tenantId)
                            .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy đối tượng đính kèm"));
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
                if (!hasAnyRole("OWNER", "CUSTOMER_SERVICE")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Bạn không có quyền truy cập file yêu cầu dịch vụ");
                }
                serviceRequestRepository.findDetailed(referenceId, tenantId)
                        .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
            }
            case "COMPANY_PAYMENT_PROFILE" -> {
                if (!tenantId.equals(referenceId)) {
                    throw BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy cấu hình thanh toán của doanh nghiệp");
                }
                if (!hasAnyRole("OWNER", "CUSTOMER_SERVICE", "TECHNICIAN")) {
                    throw BusinessException.forbidden("ATTACHMENT_ACCESS_DENIED", "Bạn không có quyền xem ảnh QR thanh toán");
                }
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
                .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Không tìm thấy file đính kèm"));
        authorizeReference(attachment.getReferenceType(), attachment.getReferenceId(), tenantId);
        authorizePurposeView(attachment);
        return attachment;
    }

    private boolean canViewPurpose(Attachment attachment) {
        if (attachment.getPurpose() != AttachmentPurpose.PAYMENT_EVIDENCE) {
            return true;
        }
        return hasAnyRole("OWNER", "CUSTOMER_SERVICE", "TECHNICIAN");
    }

    private void authorizePurposeView(Attachment attachment) {
        if (!canViewPurpose(attachment)) {
            throw BusinessException.forbidden(
                    "PAYMENT_EVIDENCE_ACCESS_DENIED",
                    "Bạn không có quyền xem bằng chứng thanh toán"
            );
        }
    }

    private void authorizeManage(Attachment attachment) {
        if (!isLifecycleMutable(attachment)) {
            String message = attachment.getPurpose() == AttachmentPurpose.PAYMENT_EVIDENCE
                    ? "Bằng chứng thanh toán đã được liên kết và không thể thay đổi"
                    : "Hình ảnh và tài liệu của hồ sơ đã hoàn tất chỉ được xem hoặc tải xuống";
            throw BusinessException.conflict("ATTACHMENT_LOCKED", message);
        }

        if (!canUserManage(attachment)) {
            throw BusinessException.forbidden("ATTACHMENT_MANAGE_DENIED", "Bạn không có quyền chỉnh sửa file đính kèm này");
        }
    }

    private boolean isLifecycleMutable(Attachment attachment) {
        if (attachment.isLocked()) {
            return false;
        }
        if (attachment.getPurpose() == AttachmentPurpose.WORK_EVIDENCE
                && "WORK_ORDER".equals(attachment.getReferenceType())) {
            return isWorkEvidenceMutable(attachment.getReferenceId(), attachment.getTenantId());
        }
        return true;
    }

    private boolean isWorkEvidenceMutable(UUID workOrderId, UUID tenantId) {
        WorkOrder workOrder = workOrderRepository.findDetailed(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("REFERENCE_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        return !WORK_EVIDENCE_FROZEN_STATUSES.contains(workOrder.getStatus());
    }

    private static boolean canUserManage(Attachment attachment) {
        if (attachment.getPurpose() == AttachmentPurpose.PAYMENT_EVIDENCE) {
            return CurrentUser.hasRole("TECHNICIAN") && CurrentUser.username().equals(attachment.getUploadedBy());
        }
        return CurrentUser.hasRole("OWNER") || CurrentUser.username().equals(attachment.getUploadedBy());
    }

    private static String sanitizeFilename(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_REQUIRED", "Tên file không được để trống");
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

    private static AttachmentResponse toResponse(Attachment attachment, boolean lifecycleMutable) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getReferenceType(),
                attachment.getReferenceId(),
                attachment.getUploadedBy(),
                attachment.getPurpose(),
                !lifecycleMutable,
                lifecycleMutable && canUserManage(attachment),
                attachment.getCreatedAt()
        );
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

    public record DownloadedAttachment(String filename, String contentType, Resource resource) {
    }
}
