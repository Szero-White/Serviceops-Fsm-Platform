package com.serviceops.inventory.application;

import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnablePartResponse;
import com.serviceops.workorder.domain.WorkOrder;

import java.math.BigDecimal;

final class WorkOrderPartResponseMapper {
    private WorkOrderPartResponseMapper() {
    }

    static PartRequestResponse toRequestResponse(WorkOrderPartRequest request) {
        WorkOrder workOrder = request.getWorkOrder();
        SparePart part = request.getSparePart();
        return new PartRequestResponse(
                request.getId(),
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getSummary(),
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                request.getRequestedQuantity(),
                request.getRequestNote(),
                request.getStatus(),
                request.getRequestedByUserId(),
                request.getRequestedByDisplayName(),
                request.getCreatedAt(),
                request.getIssuedQuantity(),
                request.getIssuedByUserId(),
                request.getIssuedByDisplayName(),
                request.getIssuedAt(),
                request.getReceivedByUserId(),
                request.getReceivedByDisplayName(),
                request.getResolutionReason(),
                request.getResolvedByDisplayName(),
                request.getResolvedAt()
        );
    }

    static PartUsageResponse toUsageResponse(
            WorkOrder workOrder,
            SparePart part,
            BigDecimal issued,
            BigDecimal used,
            BigDecimal returned,
            WorkOrderPartUsage usage
    ) {
        return new PartUsageResponse(
                workOrder.getId(),
                workOrder.getCode(),
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                issued,
                used,
                returned,
                issued.subtract(used).subtract(returned).max(BigDecimal.ZERO),
                usage == null ? null : usage.getUpdatedByDisplayName(),
                usage == null ? null : usage.getUpdatedAt()
        );
    }

    static ReturnablePartResponse toReturnableResponse(WorkOrder workOrder, SparePart part, BigDecimal quantity) {
        return new ReturnablePartResponse(
                workOrder.getId(),
                workOrder.getCode(),
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                quantity
        );
    }
}
