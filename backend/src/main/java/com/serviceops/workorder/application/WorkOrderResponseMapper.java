package com.serviceops.workorder.application;

import com.serviceops.asset.application.AssetDisplay;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderHistoryResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;

import java.util.List;

final class WorkOrderResponseMapper {
    private WorkOrderResponseMapper() {
    }

    static WorkOrderHistoryResponse toHistory(WorkOrderStatusHistory history) {
        return new WorkOrderHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNote(),
                history.getChangedBy(),
                history.getActorDisplayName(),
                history.getActorRole(),
                history.getDiagnosisSnapshot(),
                history.getResolutionSnapshot(),
                history.getCreatedAt()
        );
    }

    static WorkOrderResponse toResponse(WorkOrder workOrder, List<WorkOrderHistoryResponse> history) {
        return toResponse(workOrder, history, List.of());
    }

    static WorkOrderResponse toResponse(
            WorkOrder workOrder,
            List<WorkOrderHistoryResponse> history,
            List<WorkOrderActivityResponse> activities
    ) {
        String assetLabel = workOrder.getAsset() == null ? null : AssetDisplay.label(workOrder.getAsset());
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getServiceRequest() == null ? null : workOrder.getServiceRequest().getId(),
                workOrder.getCustomer().getId(),
                workOrder.getCustomer().getName(),
                workOrder.getAsset() == null ? null : workOrder.getAsset().getId(),
                assetLabel,
                workOrder.getTechnician() == null ? null : workOrder.getTechnician().getId(),
                workOrder.getTechnician() == null ? null : workOrder.getTechnician().getUser().getDisplayName(),
                workOrder.getSummary(),
                workOrder.getDescription(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                workOrder.getScheduledStart(),
                workOrder.getScheduledEnd(),
                workOrder.getDiagnosis(),
                workOrder.getResolution(),
                workOrder.getCompletedAt(),
                workOrder.getCreatedAt(),
                history,
                activities
        );
    }

}
