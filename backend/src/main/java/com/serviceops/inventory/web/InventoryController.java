package com.serviceops.inventory.web;

import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.application.InventoryService;
import com.serviceops.inventory.web.InventoryDtos.ConsumePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
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
}
