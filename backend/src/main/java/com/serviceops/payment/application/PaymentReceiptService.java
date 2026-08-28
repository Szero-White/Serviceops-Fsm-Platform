package com.serviceops.payment.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.payment.domain.PaymentReceiptRepository;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrderBillingItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {
    private final PaymentRepository paymentRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final WorkOrderBillingItemRepository billingItemRepository;
    private final PaymentReceiptHtmlRenderer renderer;
    private final AuditService auditService;

    @Transactional
    public byte[] issue(UUID workOrderId) {
        requireCustomerServiceRole();
        Payment payment = requireSettledPayment(workOrderId);
        PaymentReceipt receipt = receiptRepository.findByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseGet(() -> createReceipt(payment));
        return render(receipt);
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID workOrderId) {
        requireReceiptViewerRole();
        PaymentReceipt receipt = receiptRepository.findByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseThrow(() -> BusinessException.notFound(
                        "PAYMENT_RECEIPT_NOT_FOUND",
                        "Biên nhận chưa được phát hành cho phiếu công việc này"
                ));
        return render(receipt);
    }

    private Payment requireSettledPayment(UUID workOrderId) {
        Payment payment = paymentRepository.findForUpdateByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseThrow(() -> BusinessException.notFound("PAYMENT_NOT_FOUND", "Phiếu chưa có thông tin thanh toán"));
        if (payment.getStatus() != PaymentStatus.SETTLED || payment.getSettledAt() == null || payment.getMethod() == null) {
            throw BusinessException.conflict(
                    "PAYMENT_NOT_SETTLED",
                    "Chỉ được phát hành biên nhận sau khi CSKH đối soát tiền đã về công ty"
            );
        }
        return payment;
    }

    private PaymentReceipt createReceipt(Payment payment) {
        var workOrder = payment.getWorkOrder();
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setTenantId(payment.getTenantId());
        receipt.setWorkOrder(workOrder);
        receipt.setPayment(payment);
        receipt.setBillingSnapshot(payment.getBillingSnapshot());
        receipt.setReceiptCode("BN-" + workOrder.getCode());
        receipt.setWorkOrderCodeSnapshot(workOrder.getCode());
        receipt.setCustomerNameSnapshot(workOrder.getCustomer().getName());
        receipt.setAmount(payment.getAmount());
        receipt.setPaymentMethod(payment.getMethod());
        receipt.setSettledAt(payment.getSettledAt());
        receipt.setSettledByDisplayName(payment.getSettledByDisplayName());
        receipt.setIssuedAt(Instant.now());
        receipt.setIssuedByUserId(CurrentUser.userId());
        receipt.setIssuedByUsername(CurrentUser.username());
        receipt.setIssuedByDisplayName(CurrentUser.displayName());
        receiptRepository.save(receipt);
        auditService.record(
                "ISSUE_PAYMENT_RECEIPT",
                "WORK_ORDER",
                workOrder.getId(),
                "Phát hành biên nhận " + receipt.getReceiptCode() + " cho " + workOrder.getCode()
        );
        return receipt;
    }

    private byte[] render(PaymentReceipt receipt) {
        var items = billingItemRepository.findByTenantIdAndBillingSnapshotIdOrderBySparePartNameAsc(
                CurrentUser.tenantId(), receipt.getBillingSnapshot().getId());
        return renderer.render(receipt, receipt.getBillingSnapshot(), items).getBytes(StandardCharsets.UTF_8);
    }

    private static void requireCustomerServiceRole() {
        if (!CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            throw BusinessException.forbidden(
                    "PAYMENT_RECEIPT_ISSUE_DENIED",
                    "Chỉ chăm sóc khách hàng được phát hành biên nhận thanh toán dịch vụ"
            );
        }
    }

    private static void requireReceiptViewerRole() {
        if (!CurrentUser.hasRole("CUSTOMER_SERVICE") && !CurrentUser.hasRole("OWNER")) {
            throw BusinessException.forbidden(
                    "PAYMENT_RECEIPT_ACCESS_DENIED",
                    "Bạn không có quyền tải biên nhận thanh toán dịch vụ"
            );
        }
    }
}
