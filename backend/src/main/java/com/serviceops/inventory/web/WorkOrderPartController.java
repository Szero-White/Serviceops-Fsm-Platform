package com.serviceops.inventory.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.application.WorkOrderPartFulfillmentService;
import com.serviceops.inventory.application.WorkOrderPartQueryService;
import com.serviceops.inventory.application.WorkOrderPartRequestService;
import com.serviceops.inventory.application.WorkOrderPartReturnService;
import com.serviceops.inventory.application.WorkOrderPartUsageService;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestCreateRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestReasonRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestUpdateRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageUpdateRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnPartRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnablePartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkOrderPartController {
    private final WorkOrderPartRequestService requestService;
    private final WorkOrderPartFulfillmentService fulfillmentService;
    private final WorkOrderPartUsageService usageService;
    private final WorkOrderPartReturnService returnService;
    private final WorkOrderPartQueryService queryService;

    @GetMapping("/part-requests")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public PageResponse<PartRequestResponse> partRequests(
            @RequestParam(required = false) WorkOrderPartRequestStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.searchRequests(status, search, page, size);
    }

    @GetMapping("/work-orders/{workOrderId}/part-requests")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN','WAREHOUSE_STAFF')")
    public List<PartRequestResponse> workOrderPartRequests(@PathVariable UUID workOrderId) {
        return queryService.requestsForWorkOrder(workOrderId);
    }

    @PostMapping("/work-orders/{workOrderId}/part-requests")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PartRequestResponse createPartRequest(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody PartRequestCreateRequest request) {
        return requestService.createRequest(workOrderId, request);
    }

    @PatchMapping("/part-requests/{requestId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PartRequestResponse updatePartRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody PartRequestUpdateRequest request) {
        return requestService.updateRequest(requestId, request);
    }

    @PostMapping("/part-requests/{requestId}/cancel")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PartRequestResponse cancelPartRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody PartRequestReasonRequest request) {
        return requestService.cancelRequest(requestId, request);
    }

    @PostMapping("/part-requests/{requestId}/unavailable")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public PartRequestResponse markPartRequestUnavailable(
            @PathVariable UUID requestId,
            @Valid @RequestBody PartRequestReasonRequest request) {
        return fulfillmentService.markUnavailable(requestId, request);
    }

    @PostMapping("/part-requests/{requestId}/issue")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public PartRequestResponse issuePartRequest(@PathVariable UUID requestId) {
        return fulfillmentService.issue(requestId);
    }

    @GetMapping("/work-orders/{workOrderId}/part-usage")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN','WAREHOUSE_STAFF')")
    public List<PartUsageResponse> workOrderPartUsage(@PathVariable UUID workOrderId) {
        return usageService.usageForWorkOrder(workOrderId);
    }

    @PutMapping("/work-orders/{workOrderId}/part-usage")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PartUsageResponse updatePartUsage(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody PartUsageUpdateRequest request) {
        return usageService.updateUsage(workOrderId, request);
    }

    @GetMapping("/work-orders/{workOrderId}/parts/{sparePartId}/returnable")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public ReturnablePartResponse returnable(
            @PathVariable UUID workOrderId,
            @PathVariable UUID sparePartId) {
        return returnService.getReturnablePart(workOrderId, sparePartId);
    }

    @PostMapping("/work-orders/{workOrderId}/parts/{sparePartId}/return")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ReturnablePartResponse returnPart(
            @PathVariable UUID workOrderId,
            @PathVariable UUID sparePartId,
            @Valid @RequestBody ReturnPartRequest request) {
        return returnService.returnPart(workOrderId, sparePartId, request);
    }
}
