package com.serviceops.asset.web;

import com.serviceops.asset.application.AssetService;
import com.serviceops.asset.web.AssetDtos.AssetImportResult;
import com.serviceops.asset.web.AssetDtos.AssetRequest;
import com.serviceops.asset.web.AssetDtos.AssetResponse;
import com.serviceops.common.web.PageResponse;
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
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class AssetController {
    private final AssetService service;

    @GetMapping
    public PageResponse<AssetResponse> search(@RequestParam(defaultValue = "") String search,
                                              @RequestParam(required = false) UUID customerId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.search(search, customerId, page, size);
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "") String search) {
        return csv("serviceops-assets.csv", service.exportAssets(search));
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public ResponseEntity<byte[]> importTemplate() {
        return csv("serviceops-assets-template.csv", service.assetImportTemplate());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public AssetImportResult importCsv(@RequestParam MultipartFile file,
                                       @RequestParam(defaultValue = "false") boolean commit) {
        return service.importAssets(file, commit);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public AssetResponse create(@Valid @RequestBody AssetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public AssetResponse update(@PathVariable UUID id, @Valid @RequestBody AssetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
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
