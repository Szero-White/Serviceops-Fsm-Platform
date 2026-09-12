package com.serviceops.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceops.ai.config.AiProperties;
import com.serviceops.ai.infrastructure.GeminiGateway;
import com.serviceops.ai.infrastructure.GeminiProviderException;
import com.serviceops.ai.web.AiDtos.AiResponseSource;
import com.serviceops.ai.web.AiDtos.ServiceRequestDraftRequest;
import com.serviceops.ai.web.AiDtos.ServiceRequestDraftResponse;
import com.serviceops.audit.application.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiProperties properties;
    private GeminiGateway geminiGateway;
    private AiSuggestionService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        geminiGateway = mock(GeminiGateway.class);
        service = new AiSuggestionService(properties, geminiGateway, mock(AuditService.class));
        authenticateCustomerService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void draftContractDoesNotExposeManualPriorityOrChannel() {
        Set<String> fields = Arrays.stream(ServiceRequestDraftResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder("title", "description", "source");
        assertThat(fields).doesNotContain("priority", "channel", "provider", "confidence", "reason");
    }

    @Test
    void localFallbackOnlyNormalizesTitleAndDescription() {
        when(geminiGateway.isConfigured()).thenReturn(false);

        ServiceRequestDraftResponse response = service.draftServiceRequest(
                new ServiceRequestDraftRequest("Máy pha cà phê không hoạt động khi cấp nguồn")
        );

        assertThat(response.title()).isEqualTo("Máy pha cà phê không hoạt động khi cấp nguồn");
        assertThat(response.description()).contains("Máy pha cà phê không hoạt động khi cấp nguồn");
        assertThat(response.source()).isEqualTo(AiResponseSource.LOCAL);
    }

    @Test
    void providerFailureFallsBackInsteadOfFailingTheForm() {
        when(geminiGateway.isConfigured()).thenReturn(true);
        when(geminiGateway.generateJson(
                anyString(),
                anyString(),
                anyString(),
                anyMap(),
                anyDouble(),
                any(Duration.class)
        )).thenThrow(new GeminiProviderException("timeout"));

        ServiceRequestDraftResponse response = service.draftServiceRequest(
                new ServiceRequestDraftRequest("Máy lạnh không đủ lạnh")
        );

        assertThat(response.title()).isEqualTo("Máy lạnh không đủ lạnh");
        assertThat(response.source()).isEqualTo(AiResponseSource.LOCAL);
    }

    @Test
    void geminiResultMapsOnlyDraftContent() throws Exception {
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
                  "title": "Máy pha cà phê không khởi động",
                  "description": "Thiết bị đã được cấp nguồn nhưng không khởi động."
                }
                """));

        ServiceRequestDraftResponse response = service.draftServiceRequest(
                new ServiceRequestDraftRequest("máy cà phê cắm điện nhưng không chạy")
        );

        assertThat(response.title()).isEqualTo("Máy pha cà phê không khởi động");
        assertThat(response.description()).isEqualTo("Thiết bị đã được cấp nguồn nhưng không khởi động.");
        assertThat(response.source()).isEqualTo(AiResponseSource.GEMINI);
    }

    @Test
    void incompleteGeminiDraftFallsBackToUserContent() throws Exception {
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
                  "title": "",
                  "description": ""
                }
                """));

        ServiceRequestDraftResponse response = service.draftServiceRequest(
                new ServiceRequestDraftRequest("Máy lạnh không đủ lạnh")
        );

        assertThat(response.title()).isEqualTo("Máy lạnh không đủ lạnh");
        assertThat(response.description()).contains("Máy lạnh không đủ lạnh");
        assertThat(response.source()).isEqualTo(AiResponseSource.LOCAL);
    }

    private static void authenticateCustomerService() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("customer-service")
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("roles", List.of("CUSTOMER_SERVICE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
