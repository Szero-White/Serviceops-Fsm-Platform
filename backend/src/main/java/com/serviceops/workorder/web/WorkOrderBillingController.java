package com.serviceops.workorder.web;

import com.serviceops.workorder.application.WorkOrderBillingService;
import com.serviceops.workorder.application.WorkOrderCustomerAcceptanceService;
import com.serviceops.workorder.web.WorkOrderBillingDtos.BillingDraftRequest;
import com.serviceops.workorder.web.WorkOrderBillingDtos.BillingResponse;
import com.serviceops.workorder.web.WorkOrderBillingDtos.CustomerAcceptanceRequest;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}")
@RequiredArgsConstructor
public class WorkOrderBillingController {
    private final WorkOrderBillingService billingService;
    private final WorkOrderCustomerAcceptanceService acceptanceService;

    @GetMapping("/billing")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')")
    public BillingResponse billing(@PathVariable UUID workOrderId) {
        return billingService.getBilling(workOrderId);
    }

    @PutMapping("/billing")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BillingResponse updateBilling(@PathVariable UUID workOrderId,
                                         @Valid @RequestBody BillingDraftRequest request) {
        return billingService.updateDraft(workOrderId, request);
    }

    @PostMapping("/customer-acceptance")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public WorkOrderResponse customerAcceptance(@PathVariable UUID workOrderId,
                                                @Valid @RequestBody(required = false) CustomerAcceptanceRequest request) {
        return acceptanceService.accept(workOrderId, request == null ? new CustomerAcceptanceRequest(null) : request);
    }
}
