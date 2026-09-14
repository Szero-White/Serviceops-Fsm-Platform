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


    public String displayName() {
        return switch (this) {
            case DRAFT -> "Nháp";
            case OPEN -> "Đang mở";
            case SCHEDULED -> "Đã lên lịch";
            case ASSIGNED -> "Đã phân công";
            case ON_THE_WAY -> "Đang di chuyển";
            case IN_PROGRESS -> "Đang thực hiện";
            case WAITING_FOR_PARTS -> "Chờ phụ tùng";
            case COMPLETED -> "Đã hoàn thành";
            case CUSTOMER_ACCEPTED -> "Khách đã xác nhận";
            case CLOSED -> "Đã đóng";
            case CANCELLED -> "Đã hủy";
            case REOPENED -> "Mở lại để xử lý";
        };
    }

    public static List<WorkOrderStatus> operationalStatuses() {
        return OPERATIONAL_STATUSES;
    }

    public static List<WorkOrderStatus> historyStatuses() {
        return HISTORY_STATUSES;
    }
}
