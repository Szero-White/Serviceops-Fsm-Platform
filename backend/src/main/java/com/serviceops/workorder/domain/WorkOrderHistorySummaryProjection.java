package com.serviceops.workorder.domain;

public interface WorkOrderHistorySummaryProjection {
    long getTotal();
    long getPendingClosure();
    long getClosed();
    long getCancelled();
}
