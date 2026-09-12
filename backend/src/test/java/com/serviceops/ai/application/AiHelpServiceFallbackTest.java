package com.serviceops.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceops.ai.config.AiProperties;
import com.serviceops.ai.infrastructure.GeminiGateway;
import com.serviceops.ai.infrastructure.GeminiProviderException;
import com.serviceops.ai.web.AiDtos.AiResponseSource;
import com.serviceops.ai.web.AiDtos.HelpRequest;
import com.serviceops.ai.web.AiDtos.HelpResponse;
import com.serviceops.audit.application.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiHelpServiceFallbackTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private GeminiGateway geminiGateway;
    private AiHelpService service;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        geminiGateway = mock(GeminiGateway.class);
        service = new AiHelpService(properties, geminiGateway, mock(AuditService.class));
        authenticateCustomerService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void providerFailureFallsBackToRoleKnowledgeBase() {
        when(geminiGateway.isConfigured()).thenReturn(true);
        when(geminiGateway.generateJson(
                anyString(),
                anyString(),
                anyString(),
                anyMap(),
                anyDouble(),
                any(Duration.class)
        )).thenThrow(new GeminiProviderException("timeout"));

        HelpResponse response = service.answer(new HelpRequest(
                "Tôi tiếp nhận yêu cầu và chuyển sang điều phối như thế nào?",
                "/service-requests"
        ));

        assertThat(response.answer()).contains("Chăm sóc khách hàng");
        assertThat(response.relatedRoute()).isEqualTo("/service-requests");
        assertThat(response.steps()).isNotEmpty();
        assertThat(response.source()).isEqualTo(AiResponseSource.LOCAL);
    }

    @Test
    void successfulProviderResponseIsMarkedAsGemini() throws Exception {
        when(geminiGateway.isConfigured()).thenReturn(true);
        when(geminiGateway.generateJson(
                anyString(),
                anyString(),
                anyString(),
                anyMap(),
                anyDouble(),
                any(Duration.class)
        )).thenReturn(objectMapper.readTree("""
                {
                  "answer": "Mở Yêu cầu dịch vụ và tạo yêu cầu mới.",
                  "steps": ["Mở Yêu cầu dịch vụ", "Chọn Tiếp nhận yêu cầu"],
                  "relatedRoute": "/service-requests",
                  "actionLabel": "Mở Yêu cầu dịch vụ"
                }
                """));

        HelpResponse response = service.answer(new HelpRequest(
                "Tôi tiếp nhận yêu cầu như thế nào?",
                "/service-requests"
        ));

        assertThat(response.source()).isEqualTo(AiResponseSource.GEMINI);
        assertThat(response.answer()).contains("Yêu cầu dịch vụ");
    }

    private static void authenticateCustomerService() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("customer-service")
                .claim("tenantId", UUID.randomUUID().toString())
                .claim("userId", UUID.randomUUID().toString())
                .claim("roles", List.of("CUSTOMER_SERVICE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
