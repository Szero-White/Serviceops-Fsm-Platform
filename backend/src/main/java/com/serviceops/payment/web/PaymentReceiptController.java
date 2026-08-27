package com.serviceops.payment.web;

import com.serviceops.payment.application.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/receipt")
@RequiredArgsConstructor
public class PaymentReceiptController {
    private final PaymentReceiptService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<byte[]> issue(@PathVariable UUID workOrderId) {
        return response(workOrderId, service.issue(workOrderId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE')")
    public ResponseEntity<byte[]> download(@PathVariable UUID workOrderId) {
        return response(workOrderId, service.download(workOrderId));
    }

    private static ResponseEntity<byte[]> response(UUID workOrderId, byte[] content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("bien-nhan-thanh-toan-" + workOrderId + ".html", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }
}
