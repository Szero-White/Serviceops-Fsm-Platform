package com.serviceops.inventory.web;

import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WorkOrderPartDtos {
    private WorkOrderPartDtos() {
    }

    public record PartRequestCreateRequest(
            @NotNull UUID sparePartId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String note
    ) {
    }

    public record PartRequestUpdateRequest(
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String note
    ) {
    }

    public record PartRequestReasonRequest(
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record PartUsageUpdateRequest(
            @NotNull UUID sparePartId,
            @NotNull @DecimalMin(value = "0.0") BigDecimal usedQuantity
    ) {
    }

    public record ReturnPartRequest(
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String note
    ) {
    }

    public record PartRequestResponse(
            UUID id,
            UUID workOrderId,
            String workOrderCode,
            String workOrderSummary,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal requestedQuantity,
            String note,
            WorkOrderPartRequestStatus status,
            UUID requestedByUserId,
            String requestedByDisplayName,
            Instant requestedAt,
            BigDecimal issuedQuantity,
            UUID issuedByUserId,
            String issuedByDisplayName,
            Instant issuedAt,
            UUID receivedByUserId,
            String receivedByDisplayName,
            String resolutionReason,
            String resolvedByDisplayName,
            Instant resolvedAt
    ) {
    }

    public record PartUsageResponse(
            UUID workOrderId,
            String workOrderCode,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal issuedQuantity,
            BigDecimal usedQuantity,
            BigDecimal returnedQuantity,
            BigDecimal outstandingQuantity,
            String updatedByDisplayName,
            Instant updatedAt
    ) {
    }

    public record ReturnablePartResponse(
            UUID workOrderId,
            String workOrderCode,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal returnableQuantity
    ) {
    }
    public record OutstandingPartResponse(
            UUID workOrderId,
            String workOrderCode,
            String workOrderSummary,
            UUID technicianUserId,
            String technicianName,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal issuedQuantity,
            BigDecimal usedQuantity,
            BigDecimal returnedQuantity,
            BigDecimal outstandingQuantity,
            Instant since
    ) {
    }

}
