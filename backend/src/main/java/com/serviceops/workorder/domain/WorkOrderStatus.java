package com.serviceops.workorder.domain;

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
    REOPENED
}
