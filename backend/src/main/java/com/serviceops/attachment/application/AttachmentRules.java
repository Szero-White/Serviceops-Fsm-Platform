package com.serviceops.attachment.application;

import com.serviceops.attachment.domain.AttachmentPurpose;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.workorder.domain.WorkOrderStatus;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** Pure attachment validation/normalization rules shared by lifecycle operations. */
final class AttachmentRules {
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

    private AttachmentRules() {
    }

    static String normalizeReferenceType(String referenceType) {
        String normalizedType = referenceType == null ? "" : referenceType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_REFERENCE_TYPES.contains(normalizedType)) {
            throw BusinessException.badRequest("INVALID_REFERENCE_TYPE", "Loại đối tượng đính kèm không hợp lệ");
        }
        return normalizedType;
    }

    static AttachmentPurpose normalizePurpose(String referenceType, AttachmentPurpose requestedPurpose) {
        AttachmentPurpose purpose = requestedPurpose == null
                ? ("WORK_ORDER".equals(referenceType) ? AttachmentPurpose.WORK_EVIDENCE : AttachmentPurpose.GENERAL)
                : requestedPurpose;

        if ("WORK_ORDER".equals(referenceType)) {
            if (purpose != AttachmentPurpose.WORK_EVIDENCE && purpose != AttachmentPurpose.PAYMENT_EVIDENCE) {
                throw BusinessException.badRequest(
                        "INVALID_ATTACHMENT_PURPOSE",
                        "Tệp của phiếu công việc phải là hồ sơ sửa chữa hoặc bằng chứng thanh toán"
                );
            }
            return purpose;
        }

        if (purpose != AttachmentPurpose.GENERAL) {
            throw BusinessException.badRequest(
                    "INVALID_ATTACHMENT_PURPOSE",
                    "Mục đích tệp không phù hợp với đối tượng đính kèm"
            );
        }
        return purpose;
    }

    static boolean isPaymentEvidenceContentType(String contentType) {
        return contentType != null && PAYMENT_EVIDENCE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }

    static boolean isWorkEvidenceFrozen(WorkOrderStatus status) {
        return WORK_EVIDENCE_FROZEN_STATUSES.contains(status);
    }

    static String sanitizeFilename(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_REQUIRED", "Tên tệp không được để trống");
        }
        final String normalized;
        try {
            normalized = Path.of(raw).getFileName().toString();
        } catch (RuntimeException ex) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_INVALID", "Tên tệp không hợp lệ");
        }
        if (normalized.length() > 255) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_TOO_LONG", "Tên tệp không được vượt quá 255 ký tự");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw BusinessException.badRequest("ATTACHMENT_FILENAME_INVALID", "Tên tệp không được chứa ký tự điều khiển");
        }
        return normalized;
    }
}
