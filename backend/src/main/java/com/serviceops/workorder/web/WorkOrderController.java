package com.serviceops.workorder.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.workorder.application.WorkOrderInvoiceService;
import com.serviceops.workorder.application.WorkOrderService;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.ScheduleWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.TransitionWorkOrder;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService service;
    private final WorkOrderInvoiceService invoiceService;

    @GetMapping
    public PageResponse<WorkOrderResponse> search(@RequestParam(defaultValue = "") String search,
                                                  @RequestParam(required = false) WorkOrderStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.search(search, status, page, size);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN')")
    public PageResponse<WorkOrderResponse> history(@RequestParam(defaultValue = "") String search,
                                                   @RequestParam(required = false) WorkOrderStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.history(search, status, page, size);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable UUID id) {
        WorkOrderResponse workOrder = service.get(id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("hoa-don-dich-vu-" + workOrder.code() + ".html", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(invoiceService.exportInvoice(workOrder));
    }

    @PostMapping("/from-service-request/{serviceRequestId}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public WorkOrderResponse convert(@PathVariable UUID serviceRequestId) {
        return service.convertServiceRequest(serviceRequestId);
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public WorkOrderResponse schedule(@PathVariable UUID id, @Valid @RequestBody ScheduleWorkOrder request) {
        return service.schedule(id, request);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN')")
    public WorkOrderResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionWorkOrder request) {
        return service.transition(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public void deleteFromHistory(@PathVariable UUID id) {
        service.deleteFromHistory(id);
    }
}
