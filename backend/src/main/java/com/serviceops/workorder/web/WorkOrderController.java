package com.serviceops.workorder.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.workorder.application.WorkOrderService;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.CreateWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.ScheduleWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService service;

    @GetMapping
    public PageResponse<WorkOrderResponse> search(@RequestParam(defaultValue = "") String search,
                                                  @RequestParam(required = false) WorkOrderStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.search(search, status, page, size);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public WorkOrderResponse create(@Valid @RequestBody CreateWorkOrder request) {
        return service.create(request);
    }

    @PostMapping("/from-service-request/{serviceRequestId}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public WorkOrderResponse convert(@PathVariable UUID serviceRequestId) {
        return service.convertServiceRequest(serviceRequestId);
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public WorkOrderResponse schedule(@PathVariable UUID id, @Valid @RequestBody ScheduleWorkOrder request) {
        return service.schedule(id, request);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER','TECHNICIAN')")
    public WorkOrderResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionWorkOrder request) {
        return service.transition(id, request);
    }
}
