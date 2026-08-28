package com.serviceops.workorder.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WorkOrderBillingDtos {
    private WorkOrderBillingDtos() {
    }

    public record BillingDraftRequest(
            @DecimalMin(value = "0.0") BigDecimal laborFee,
            @DecimalMin(value = "0.0") BigDecimal incidentalFee,
            @Size(max = 500) String incidentalReason
    ) {
    }

    public record BillingItemResponse(
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record BillingResponse(
            UUID workOrderId,
            String workOrderCode,
            boolean frozen,
            List<BillingItemResponse> items,
            BigDecimal partsTotal,
            BigDecimal laborFee,
            BigDecimal incidentalFee,
            String incidentalReason,
            BigDecimal totalAmount,
            String acceptedByDisplayName,
            Instant acceptedAt
    ) {
    }

    public record CustomerAcceptanceRequest(
            @Size(max = 1000) String note
    ) {
    }
}
