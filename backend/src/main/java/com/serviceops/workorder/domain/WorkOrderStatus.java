package com.serviceops.workorder.domain;

import java.util.List;

public enum WorkOrderStatus {
    DRAFT,
    OPEN,
    SCHEDULED,
    ASSIGNED,
    ON_THE_WAY,
    IN_PROGRESS,
    WAITING_FOR_PARTS,
    COMPLETED,
    CUSTOMER_ACCEPTED,
    CLOSED,
    CANCELLED,
    REOPENED;

    private static final List<WorkOrderStatus> OPERATIONAL_STATUSES = List.of(
            DRAFT,
            OPEN,
            SCHEDULED,
            ASSIGNED,
            ON_THE_WAY,
            IN_PROGRESS,
            WAITING_FOR_PARTS,
            COMPLETED,
            CUSTOMER_ACCEPTED,
            REOPENED
    );

    private static final List<WorkOrderStatus> HISTORY_STATUSES = List.of(
            CUSTOMER_ACCEPTED,
            CLOSED,
            CANCELLED
    );

    public static List<WorkOrderStatus> operationalStatuses() {
        return OPERATIONAL_STATUSES;
    }

    public static List<WorkOrderStatus> historyStatuses() {
        return HISTORY_STATUSES;
    }
}
