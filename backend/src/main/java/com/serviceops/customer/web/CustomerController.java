package com.serviceops.customer.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.application.CustomerService;
import com.serviceops.customer.web.CustomerDtos.CustomerRequest;
import com.serviceops.customer.web.CustomerDtos.CustomerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class CustomerController {
    private final CustomerService service;

    @GetMapping
    public PageResponse<CustomerResponse> search(@RequestParam(defaultValue = "") String search,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(search, page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }
}
