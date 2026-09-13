package com.serviceops.workorder.application;

import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class WorkOrderActivityFactory {
    private WorkOrderActivityFactory() {
    }

    static WorkOrderActivityResponse activity(
            String id,
            WorkOrderActivityType type,
            WorkOrderStatus status,
            String note,
            String actor,
            String actorDisplayName,
            String actorRole,
            String diagnosis,
            String resolution,
            UUID sparePartId,
            String sparePartSku,
            String sparePartName,
            String unit,
            BigDecimal quantity,
            BigDecimal amount,
            String paymentMethod,
            String referenceCode,
            Instant createdAt
    ) {
        return new WorkOrderActivityResponse(
                id,
                type,
                status,
                note,
                actor,
                actorDisplayName,
                actorRole,
                diagnosis,
                resolution,
                sparePartId,
                sparePartSku,
                sparePartName,
                unit,
                quantity,
                amount,
                paymentMethod,
                referenceCode,
                createdAt
        );
    }
}
