package com.serviceops.ai.web;

import com.serviceops.ai.application.AiSuggestionService;
import com.serviceops.ai.web.AiDtos.ServiceRequestDraftRequest;
import com.serviceops.ai.web.AiDtos.ServiceRequestDraftResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
public class AiController {
    private final AiSuggestionService service;

    @PostMapping("/service-request-draft")
    public ServiceRequestDraftResponse draftServiceRequest(@Valid @RequestBody ServiceRequestDraftRequest request) {
        return service.draftServiceRequest(request);
    }
}
