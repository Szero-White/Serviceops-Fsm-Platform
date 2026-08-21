package com.serviceops.ai.web;

import com.serviceops.ai.application.AiHelpService;
import com.serviceops.ai.application.AiSuggestionService;
import com.serviceops.ai.web.AiDtos.HelpRequest;
import com.serviceops.ai.web.AiDtos.HelpResponse;
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
public class AiController {
    private final AiSuggestionService suggestionService;
    private final AiHelpService helpService;

    @PostMapping("/service-request-draft")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')")
    public ServiceRequestDraftResponse draftServiceRequest(@Valid @RequestBody ServiceRequestDraftRequest request) {
        return suggestionService.draftServiceRequest(request);
    }

    @PostMapping("/help")
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER','TECHNICIAN','WAREHOUSE_STAFF')")
    public HelpResponse help(@Valid @RequestBody HelpRequest request) {
        return helpService.answer(request);
    }
}
