package com.serviceops.payment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.payment.domain.CompanyPaymentProfile;
import com.serviceops.payment.domain.CompanyPaymentProfileRepository;
import com.serviceops.payment.web.PaymentDtos.CompanyPaymentProfileRequest;
import com.serviceops.payment.web.PaymentDtos.CompanyPaymentProfileResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyPaymentProfileService {
    public static final String ATTACHMENT_REFERENCE_TYPE = "COMPANY_PAYMENT_PROFILE";

    private final CompanyPaymentProfileRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CompanyPaymentProfileResponse get() {
        requireViewRole();
        return repository.findByTenantId(CurrentUser.tenantId())
                .map(CompanyPaymentProfileService::toResponse)
                .orElse(null);
    }

    @Transactional
    public CompanyPaymentProfileResponse update(CompanyPaymentProfileRequest request) {
        if (!CurrentUser.hasRole("OWNER")) {
            throw BusinessException.forbidden("PAYMENT_PROFILE_UPDATE_DENIED", "Chỉ chủ sở hữu được cấu hình tài khoản nhận thanh toán");
        }
        UUID tenantId = CurrentUser.tenantId();
        UUID qrAttachmentId = request.qrAttachmentId();
        if (qrAttachmentId != null) {
            Attachment attachment = attachmentRepository.findByIdAndTenantId(qrAttachmentId, tenantId)
                    .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Không tìm thấy ảnh QR thanh toán"));
            if (!ATTACHMENT_REFERENCE_TYPE.equals(attachment.getReferenceType())
                    || !tenantId.equals(attachment.getReferenceId())) {
                throw BusinessException.badRequest("INVALID_PAYMENT_QR", "Ảnh QR không thuộc cấu hình thanh toán của doanh nghiệp");
            }
        }

        CompanyPaymentProfile profile = repository.findByTenantId(tenantId).orElseGet(CompanyPaymentProfile::new);
        if (profile.getId() == null) {
            profile.setTenantId(tenantId);
        }
        profile.setBankName(request.bankName().trim());
        profile.setAccountHolder(request.accountHolder().trim());
        profile.setAccountNumber(request.accountNumber().trim());
        profile.setQrAttachmentId(qrAttachmentId);
        profile.setUpdatedByUserId(CurrentUser.userId());
        profile.setUpdatedByUsername(CurrentUser.username());
        profile.setUpdatedByDisplayName(CurrentUser.displayName());
        repository.save(profile);
        auditService.record("UPDATE_PAYMENT_PROFILE", "TENANT", tenantId, "Cập nhật tài khoản ngân hàng nhận thanh toán");
        return toResponse(profile);
    }

    public CompanyPaymentProfile requireConfigured() {
        return repository.findByTenantId(CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.conflict(
                        "PAYMENT_PROFILE_NOT_CONFIGURED",
                        "Chủ sở hữu chưa cấu hình tài khoản ngân hàng của công ty"
                ));
    }

    private static void requireViewRole() {
        if (!CurrentUser.hasRole("OWNER") && !CurrentUser.hasRole("CUSTOMER_SERVICE") && !CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden("PAYMENT_PROFILE_ACCESS_DENIED", "Bạn không có quyền xem thông tin nhận thanh toán");
        }
    }

    private static CompanyPaymentProfileResponse toResponse(CompanyPaymentProfile profile) {
        return new CompanyPaymentProfileResponse(
                profile.getId(),
                profile.getTenantId(),
                profile.getBankName(),
                profile.getAccountHolder(),
                profile.getAccountNumber(),
                profile.getQrAttachmentId(),
                profile.getUpdatedByDisplayName(),
                profile.getUpdatedAt()
        );
    }
}
