package com.serviceops.payment.application;

import com.serviceops.attachment.domain.Attachment;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.payment.web.PaymentDtos.PaymentResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final CompanyPaymentProfileService companyPaymentProfileService;
    private final AuditService auditService;

    @Transactional
    public Payment initializeUnpaid(WorkOrder workOrder, WorkOrderBillingSnapshot snapshot) {
        return repository.findForUpdateByWorkOrder(CurrentUser.tenantId(), workOrder.getId())
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setTenantId(workOrder.getTenantId());
                    payment.setWorkOrder(workOrder);
                    payment.setBillingSnapshot(snapshot);
                    payment.setAmount(snapshot.getTotalAmount());
                    payment.setStatus(PaymentStatus.UNPAID);
                    return repository.save(payment);
                });
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByWorkOrder(UUID workOrderId) {
        Payment payment = repository.findDetailedByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseThrow(() -> BusinessException.notFound("PAYMENT_NOT_FOUND", "Phiếu chưa có thông tin thanh toán"));
        ensureCanView(payment);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> search(PaymentStatus status, String search, int page, int size) {
        requirePaymentQueueRole();
        var pageable = PageRequestSupport.of(page, size, Sort.by("updatedAt").descending());
        return PageResponse.from(repository.search(
                CurrentUser.tenantId(),
                status,
                PageRequestSupport.normalizeSearch(search),
                pageable
        ).map(PaymentService::toResponse));
    }

    @Transactional
    public PaymentResponse reportTransfer(UUID workOrderId, UUID evidenceAttachmentId) {
        requireTechnicianRole();
        Payment payment = requireWorkOrderPaymentForUpdate(workOrderId);
        ensureAssignedTechnician(payment);
        ensureUnpaid(payment);
        ensureCustomerAccepted(payment);
        companyPaymentProfileService.requireConfigured();
        if (evidenceAttachmentId != null) {
            validateEvidenceAttachment(payment, evidenceAttachmentId);
        }
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.TRANSFER_PENDING_VERIFICATION);
        payment.setTransferEvidenceAttachmentId(evidenceAttachmentId);
        payment.setTransferReportedAt(Instant.now());
        auditService.record("REPORT_BANK_TRANSFER", "PAYMENT", payment.getId(), "Khách báo đã chuyển khoản cho " + payment.getWorkOrder().getCode());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse recordCashCollection(UUID workOrderId) {
        requireTechnicianRole();
        Payment payment = requireWorkOrderPaymentForUpdate(workOrderId);
        ensureAssignedTechnician(payment);
        ensureUnpaid(payment);
        ensureCustomerAccepted(payment);
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.CASH_PENDING_HANDOVER);
        payment.setCashCollectedAt(Instant.now());
        payment.setCollectedByUserId(CurrentUser.userId());
        payment.setCollectedByUsername(CurrentUser.username());
        payment.setCollectedByDisplayName(CurrentUser.displayName());
        auditService.record("COLLECT_CASH", "PAYMENT", payment.getId(), "Kỹ thuật viên nhận tiền mặt cho " + payment.getWorkOrder().getCode());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse settleTransfer(UUID paymentId) {
        requireCustomerServiceRole();
        Payment payment = requireForUpdate(paymentId);
        if (payment.getStatus() != PaymentStatus.TRANSFER_PENDING_VERIFICATION) {
            throw BusinessException.conflict("PAYMENT_NOT_TRANSFER_PENDING", "Khoản thanh toán không ở trạng thái chờ xác minh chuyển khoản");
        }
        return settle(payment, "VERIFY_BANK_TRANSFER", "Đã xác minh tiền chuyển khoản vào công ty");
    }

    @Transactional
    public PaymentResponse settleCash(UUID paymentId) {
        requireCustomerServiceRole();
        Payment payment = requireForUpdate(paymentId);
        if (payment.getStatus() != PaymentStatus.CASH_PENDING_HANDOVER) {
            throw BusinessException.conflict("PAYMENT_NOT_CASH_PENDING", "Khoản thanh toán không ở trạng thái chờ bàn giao tiền mặt");
        }
        return settle(payment, "CONFIRM_CASH_HANDOVER", "Đã nhận tiền mặt bàn giao từ kỹ thuật viên");
    }

    private PaymentResponse settle(Payment payment, String action, String details) {
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setSettledAt(Instant.now());
        payment.setSettledByUserId(CurrentUser.userId());
        payment.setSettledByUsername(CurrentUser.username());
        payment.setSettledByDisplayName(CurrentUser.displayName());
        auditService.record(action, "PAYMENT", payment.getId(), details + " · " + payment.getWorkOrder().getCode());
        return toResponse(payment);
    }

    private Payment requireWorkOrderPaymentForUpdate(UUID workOrderId) {
        return repository.findForUpdateByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseThrow(() -> BusinessException.notFound("PAYMENT_NOT_FOUND", "Phiếu chưa có thông tin thanh toán"));
    }

    private Payment requireForUpdate(UUID paymentId) {
        return repository.findForUpdate(CurrentUser.tenantId(), paymentId)
                .orElseThrow(() -> BusinessException.notFound("PAYMENT_NOT_FOUND", "Không tìm thấy khoản thanh toán"));
    }

    private static void ensureUnpaid(Payment payment) {
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw BusinessException.conflict("PAYMENT_ALREADY_REPORTED", "Phương thức thanh toán của phiếu này đã được ghi nhận");
        }
    }

    private static void ensureCustomerAccepted(Payment payment) {
        if (payment.getWorkOrder().getStatus() != WorkOrderStatus.CUSTOMER_ACCEPTED) {
            throw BusinessException.conflict("CUSTOMER_ACCEPTANCE_REQUIRED", "Phải ghi nhận khách xác nhận trước khi ghi nhận thanh toán");
        }
    }

    private void validateEvidenceAttachment(Payment payment, UUID attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "Không tìm thấy ảnh giao dịch"));
        if (!"WORK_ORDER".equals(attachment.getReferenceType())
                || !payment.getWorkOrder().getId().equals(attachment.getReferenceId())) {
            throw BusinessException.badRequest("INVALID_PAYMENT_EVIDENCE", "Ảnh giao dịch phải thuộc đúng phiếu công việc");
        }
    }

    private static void ensureAssignedTechnician(Payment payment) {
        if (payment.getWorkOrder().getTechnician() == null
                || payment.getWorkOrder().getTechnician().getUser() == null
                || !CurrentUser.userId().equals(payment.getWorkOrder().getTechnician().getUser().getId())) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được ghi nhận thanh toán cho công việc được phân công cho mình");
        }
    }

    private static void ensureCanView(Payment payment) {
        if (CurrentUser.hasRole("OWNER") || CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            return;
        }
        if (CurrentUser.hasRole("TECHNICIAN")) {
            ensureAssignedTechnician(payment);
            return;
        }
        throw BusinessException.forbidden("PAYMENT_ACCESS_DENIED", "Bạn không có quyền xem thông tin thanh toán");
    }

    private static void requirePaymentQueueRole() {
        if (!CurrentUser.hasRole("OWNER") && !CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            throw BusinessException.forbidden("PAYMENT_QUEUE_ACCESS_DENIED", "Bạn không có quyền xem hàng đợi thanh toán");
        }
    }

    private static void requireTechnicianRole() {
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden("PAYMENT_REPORT_DENIED", "Chỉ kỹ thuật viên được ghi nhận phương thức khách đã thanh toán tại hiện trường");
        }
    }

    private static void requireCustomerServiceRole() {
        if (!CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            throw BusinessException.forbidden("PAYMENT_SETTLEMENT_DENIED", "Chỉ chăm sóc khách hàng được đối soát tiền về công ty");
        }
    }

    public static PaymentResponse toResponse(Payment payment) {
        WorkOrder workOrder = payment.getWorkOrder();
        String technicianName = workOrder.getTechnician() == null || workOrder.getTechnician().getUser() == null
                ? null
                : workOrder.getTechnician().getUser().getDisplayName();
        return new PaymentResponse(
                payment.getId(),
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getStatus(),
                workOrder.getCustomer() == null ? null : workOrder.getCustomer().getName(),
                technicianName,
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransferEvidenceAttachmentId(),
                payment.getTransferReportedAt(),
                payment.getCashCollectedAt(),
                payment.getCollectedByDisplayName(),
                payment.getSettledAt(),
                payment.getSettledByDisplayName(),
                payment.getUpdatedAt()
        );
    }
}
