package com.serviceops.servicerequest.web;

import com.serviceops.servicerequest.application.ServiceChannelService;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelRequest;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelResponse;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/service-channels")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class ServiceChannelController {
    private final ServiceChannelService service;

    @GetMapping
    public List<ServiceChannelResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public ServiceChannelResponse create(@Valid @RequestBody ServiceChannelRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public ServiceChannelResponse update(@PathVariable UUID id, @Valid @RequestBody ServiceChannelUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
