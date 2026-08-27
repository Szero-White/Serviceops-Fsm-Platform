package com.serviceops.workorder.web;

import com.serviceops.workorder.application.WorkOrderActivityService;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN')")
public class WorkOrderActivityController {
    private final WorkOrderActivityService service;

    @GetMapping("/{workOrderId}/timeline")
    public List<WorkOrderActivityResponse> timeline(@PathVariable UUID workOrderId) {
        return service.timeline(workOrderId);
    }
}
