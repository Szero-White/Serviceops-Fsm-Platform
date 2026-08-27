package com.serviceops.payment.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.payment.application.CompanyPaymentProfileService;
import com.serviceops.payment.application.PaymentService;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.payment.web.PaymentDtos.CompanyPaymentProfileRequest;
import com.serviceops.payment.web.PaymentDtos.CompanyPaymentProfileResponse;
import com.serviceops.payment.web.PaymentDtos.PaymentResponse;
import com.serviceops.payment.web.PaymentDtos.TransferReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final CompanyPaymentProfileService companyPaymentProfileService;

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public PageResponse<PaymentResponse> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.search(status, search, page, size);
    }

    @GetMapping("/work-orders/{workOrderId}/payment")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')")
    public PaymentResponse workOrderPayment(@PathVariable UUID workOrderId) {
        return paymentService.getByWorkOrder(workOrderId);
    }

    @PostMapping("/work-orders/{workOrderId}/payment/report-transfer")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PaymentResponse reportTransfer(@PathVariable UUID workOrderId,
                                          @RequestBody(required = false) TransferReportRequest request) {
        return paymentService.reportTransfer(workOrderId, request == null ? null : request.evidenceAttachmentId());
    }

    @PostMapping("/work-orders/{workOrderId}/payment/collect-cash")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PaymentResponse collectCash(@PathVariable UUID workOrderId) {
        return paymentService.recordCashCollection(workOrderId);
    }

    @PostMapping("/payments/{paymentId}/settle-transfer")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public PaymentResponse settleTransfer(@PathVariable UUID paymentId) {
        return paymentService.settleTransfer(paymentId);
    }

    @PostMapping("/payments/{paymentId}/settle-cash")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public PaymentResponse settleCash(@PathVariable UUID paymentId) {
        return paymentService.settleCash(paymentId);
    }

    @GetMapping("/company-payment-profile")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')")
    public CompanyPaymentProfileResponse paymentProfile() {
        return companyPaymentProfileService.get();
    }

    @PutMapping("/company-payment-profile")
    @PreAuthorize("hasRole('OWNER')")
    public CompanyPaymentProfileResponse updatePaymentProfile(@Valid @RequestBody CompanyPaymentProfileRequest request) {
        return companyPaymentProfileService.update(request);
    }
}
