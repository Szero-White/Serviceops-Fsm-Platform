package com.serviceops.payment.web;

import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.workorder.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record TransferReportRequest(UUID evidenceAttachmentId) {
    }

    public record PaymentResponse(
            UUID id,
            UUID workOrderId,
            String workOrderCode,
            String workOrderSummary,
            WorkOrderStatus workOrderStatus,
            String customerName,
            String technicianName,
            BigDecimal amount,
            PaymentMethod method,
            PaymentStatus status,
            UUID transferEvidenceAttachmentId,
            Instant transferReportedAt,
            Instant cashCollectedAt,
            String collectedByDisplayName,
            Instant settledAt,
            String settledByDisplayName,
            Instant updatedAt
    ) {
    }

    public record CompanyPaymentProfileRequest(
            @NotBlank @Size(max = 150) String bankName,
            @NotBlank @Size(max = 180) String accountHolder,
            @NotBlank @Size(max = 80) String accountNumber,
            UUID qrAttachmentId
    ) {
    }

    public record CompanyPaymentProfileResponse(
            UUID id,
            UUID tenantId,
            String bankName,
            String accountHolder,
            String accountNumber,
            UUID qrAttachmentId,
            String updatedByDisplayName,
            Instant updatedAt
    ) {
    }
}
