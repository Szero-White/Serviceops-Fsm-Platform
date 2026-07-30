package com.serviceops.inventory.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.application.InventoryService;
import com.serviceops.inventory.web.InventoryDtos.ConsumePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;

    @GetMapping("/spare-parts")
    public PageResponse<SparePartResponse> search(@RequestParam(defaultValue = "") String search,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.search(search, page, size);
    }

    @PostMapping("/spare-parts")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public SparePartResponse create(@Valid @RequestBody SparePartRequest request) {
        return service.create(request);
    }

    @GetMapping("/spare-parts/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "") String search) {
        return csv("serviceops-spare-parts.csv", service.exportSpareParts(search));
    }

    @GetMapping("/spare-parts/import-template")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public ResponseEntity<byte[]> importTemplate() {
        return csv("serviceops-spare-parts-template.csv", service.sparePartImportTemplate());
    }

    @PostMapping(value = "/spare-parts/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public SparePartImportResult importCsv(@RequestParam MultipartFile file,
                                           @RequestParam(defaultValue = "false") boolean commit) {
        return service.importSpareParts(file, commit);
    }

    @PostMapping("/spare-parts/{id}/import")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF')")
    public SparePartResponse importStock(@PathVariable UUID id, @Valid @RequestBody StockAdjustmentRequest request) {
        return service.importStock(id, request);
    }

    @PostMapping("/work-orders/{workOrderId}/parts/consume")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE_STAFF','TECHNICIAN')")
    public SparePartResponse consume(@PathVariable UUID workOrderId, @Valid @RequestBody ConsumePartRequest request) {
        return service.consume(workOrderId, request);
    }

    private static ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(content);
    }
}
