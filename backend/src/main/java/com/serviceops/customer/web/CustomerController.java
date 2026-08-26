package com.serviceops.customer.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.application.CustomerService;
import com.serviceops.customer.web.CustomerDtos.CustomerImportResult;
import com.serviceops.customer.web.CustomerDtos.CustomerRequest;
import com.serviceops.customer.web.CustomerDtos.CustomerResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class CustomerController {
    private final CustomerService service;

    @GetMapping
    public PageResponse<CustomerResponse> search(@RequestParam(defaultValue = "") String search,
                                                 @RequestParam(required = false) Boolean active,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(search, active, page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "") String search) {
        return csv("serviceops-customers.csv", service.exportCustomers(search));
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public ResponseEntity<byte[]> importTemplate() {
        return csv("serviceops-customers-template.csv", service.customerImportTemplate());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public CustomerImportResult importCsv(@RequestParam MultipartFile file,
                                          @RequestParam(defaultValue = "false") boolean commit) {
        return service.importCustomers(file, commit);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private static ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(content);
    }
}
