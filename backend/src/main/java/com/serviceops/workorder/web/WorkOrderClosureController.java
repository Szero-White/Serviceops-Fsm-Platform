package com.serviceops.workorder.web;

import com.serviceops.workorder.application.WorkOrderClosureService;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderClosureController {
    private final WorkOrderClosureService service;

    @PostMapping("/{workOrderId}/close")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public WorkOrderResponse close(@PathVariable UUID workOrderId) {
        return service.close(workOrderId);
    }
}
