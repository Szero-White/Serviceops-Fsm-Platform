package com.serviceops.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceops.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiGateway {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public boolean isConfigured() {
        return "gemini".equalsIgnoreCase(properties.getProvider())
                && properties.getGeminiApiKey() != null
                && !properties.getGeminiApiKey().isBlank();
    }

    public JsonNode generateJson(
            String operation,
            String systemInstruction,
            String userText,
            Map<String, Object> responseSchema,
            double temperature,
            Duration readTimeout
    ) {
        if (!isConfigured()) {
            throw new GeminiProviderException("Gemini provider is not configured");
        }

        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userText)))),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );

        try {
            // Keep the HTTP transport deliberately byte-oriented. The gateway owns JSON
            // serialization/deserialization so Spring message converters cannot turn a
            // provider response into a generic RestClientException before we can classify it.
            byte[] requestBody = objectMapper.writeValueAsBytes(payload);
            byte[] responseBody = restClient(readTimeout).post()
                    .uri("/models/{model}:generateContent", properties.getGeminiModel())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

            if (responseBody == null || responseBody.length == 0) {
                throw new GeminiProviderException("Gemini returned an empty response");
            }

            JsonNode response = objectMapper.readTree(responseBody);
            String json = findText(response)
                    .orElseThrow(() -> new GeminiProviderException("Gemini returned an empty response"));
            return objectMapper.readTree(json);
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Gemini request failed: operation={}, status={}, model={}",
                    operation,
                    ex.getStatusCode().value(),
                    properties.getGeminiModel()
            );
            throw new GeminiProviderException("Gemini HTTP request failed", ex);
        } catch (ResourceAccessException ex) {
            String failure = isTimeout(ex) ? "timeout" : "network";
            log.warn(
                    "Gemini request unavailable: operation={}, failure={}, model={}",
                    operation,
                    failure,
                    properties.getGeminiModel()
            );
            throw new GeminiProviderException("Gemini request unavailable", ex);
        } catch (IOException ex) {
            log.warn("Gemini returned invalid JSON: operation={}, model={}", operation, properties.getGeminiModel());
            throw new GeminiProviderException("Gemini returned invalid JSON", ex);
        } catch (RestClientException ex) {
            log.warn(
                    "Gemini client failure: operation={}, type={}, rootType={}, model={}",
                    operation,
                    ex.getClass().getSimpleName(),
                    rootCauseType(ex),
                    properties.getGeminiModel()
            );
            throw new GeminiProviderException("Gemini client failure", ex);
        } catch (GeminiProviderException ex) {
            log.warn("Gemini returned no usable content: operation={}, model={}", operation, properties.getGeminiModel());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Gemini client failure: operation={}, type={}, model={}",
                    operation,
                    ex.getClass().getSimpleName(),
                    properties.getGeminiModel()
            );
            throw new GeminiProviderException("Gemini client failure", ex);
        }
    }

    private RestClient restClient(Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(readTimeout);

        // RestClient.Builder is mutable. Clone the injected Boot-configured template so
        // concurrent draft/help requests never race while applying different timeouts.
        return restClientBuilder.clone()
                .baseUrl(properties.getGeminiBaseUrl())
                .defaultHeaders(headers -> headers.set("x-goog-api-key", properties.getGeminiApiKey()))
                .requestFactory(requestFactory)
                .build();
    }

    private static String rootCauseType(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName();
    }

    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static Optional<String> findText(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.has("output_text") && node.get("output_text").isTextual()) {
            return Optional.of(node.get("output_text").asText());
        }
        if (node.has("text") && node.get("text").isTextual()) {
            return Optional.of(node.get("text").asText());
        }
        if (node.isContainerNode()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                Optional<String> value = findText(iterator.next());
                if (value.isPresent()) {
                    return value;
                }
            }
        }
        return Optional.empty();
    }
}
