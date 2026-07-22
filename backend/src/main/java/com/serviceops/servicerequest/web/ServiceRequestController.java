package com.serviceops.servicerequest.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.servicerequest.application.ServiceRequestService;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.servicerequest.web.ServiceRequestDtos.CreateServiceRequest;
import com.serviceops.servicerequest.web.ServiceRequestDtos.ServiceRequestResponse;
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
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class ServiceRequestController {
    private final ServiceRequestService service;

    @GetMapping
    public PageResponse<ServiceRequestResponse> search(@RequestParam(defaultValue = "") String search,
                                                       @RequestParam(required = false) ServiceRequestStatus status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return service.search(search, status, page, size);
    }

    @GetMapping("/{id}")
    public ServiceRequestResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public ServiceRequestResponse create(@Valid @RequestBody CreateServiceRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public ServiceRequestResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
