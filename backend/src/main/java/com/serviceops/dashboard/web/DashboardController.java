package com.serviceops.dashboard.web;

import com.serviceops.common.domain.Priority;
import com.serviceops.dashboard.application.DashboardService;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping
    public DashboardResponse get() {
        return service.get();
    }

    public record DashboardResponse(
            long customers,
            long assets,
            long openServiceRequests,
            long activeTechnicians,
            long openWorkOrders,
            long assignedWorkOrders,
            long inProgressWorkOrders,
            long waitingForPartsWorkOrders,
            long completedWorkOrders,
            long closedWorkOrders,
            long lowStockParts,
            List<RecentWorkOrder> recentWorkOrders
    ) {
    }

    public record RecentWorkOrder(
            UUID id,
            String code,
            String summary,
            String customerName,
            String technicianName,
            WorkOrderStatus status,
            Priority priority,
            Instant scheduledStart
    ) {
    }
}
