package com.serviceops.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceops.ai.config.AiProperties;
import com.serviceops.ai.web.AiDtos.HelpRequest;
import com.serviceops.ai.web.AiDtos.HelpResponse;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.serviceops.ai.application.AiHelpKnowledgeBase.HelpTopic;
import com.serviceops.ai.application.AiHelpKnowledgeBase.UserGuideContext;

@Service
@RequiredArgsConstructor
public class AiHelpService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final AuditService auditService;

    public HelpResponse answer(HelpRequest request) {
        if (!properties.isEnabled()) {
            throw BusinessException.badRequest("AI_DISABLED", "Tính năng AI đang tắt trong cấu hình hệ thống");
        }

        UserGuideContext context = AiHelpKnowledgeBase.currentContext(request.currentPath());
        HelpResponse fallback = localHelp(request.question(), context);

        try {
            if (isGeminiConfigured()) {
                HelpResponse response = geminiHelp(request.question(), context);
                audit("AI_HELP_GEMINI", request.question());
                return response;
            }
        } catch (RuntimeException ex) {
            audit("AI_HELP_FALLBACK", request.question());
            return fallback;
        }

        audit("AI_HELP_LOCAL", request.question());
        return fallback;
    }

    private boolean isGeminiConfigured() {
        return "gemini".equalsIgnoreCase(properties.getProvider())
                && properties.getGeminiApiKey() != null
                && !properties.getGeminiApiKey().isBlank();
    }

    private HelpResponse geminiHelp(String question, UserGuideContext context) {
        RestClient restClient = restClientBuilder
                .baseUrl(properties.getGeminiBaseUrl())
                .defaultHeader("x-goog-api-key", properties.getGeminiApiKey())
                .requestFactory(aiRequestFactory())
                .build();

        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", helpSystemPrompt(context)))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", question)))),
                "generationConfig", Map.of(
                        "temperature", 0.15,
                        "responseMimeType", "application/json",
                        "responseSchema", helpSchema()
                )
        );

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", properties.getGeminiModel())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String json = findText(response)
                .orElseThrow(() -> BusinessException.badRequest("AI_EMPTY_RESPONSE", "AI không trả về hướng dẫn hợp lệ"));
        return toHelpResponse(parseJson(json), "gemini");
    }

    private HelpResponse localHelp(String question, UserGuideContext context) {
        HelpTopic topic = AiHelpKnowledgeBase.bestTopic(question, context);
        String answer = "Với vai trò " + context.roleLabel() + ", bạn nên bắt đầu ở mục " + topic.name()
                + ". " + topic.answer();
        return new HelpResponse(answer, topic.steps(), topic.route(), "Mở " + topic.name(), "local");
    }

    private SimpleClientHttpRequestFactory aiRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        return requestFactory;
    }

    private String helpSystemPrompt(UserGuideContext context) {
        return """
                Bạn là trợ lý hướng dẫn sử dụng ServiceOps cho nhân viên mới trong doanh nghiệp.
                Trả lời ngắn, thực tế, theo đúng vai trò hiện tại và không bịa tính năng ngoài hệ thống.
                Vai trò hiện tại: %s (%s).
                Đường dẫn hiện tại: %s.

                Knowledge base:
                %s

                Chỉ trả JSON đúng schema. Nếu người dùng hỏi thao tác dữ liệu nhạy cảm như xoá, phân quyền, audit,
                hãy nhắc họ kiểm tra quyền và xác nhận trước khi thực hiện.
                """.formatted(context.role(), context.roleLabel(), context.currentPath(), AiHelpKnowledgeBase.knowledgeBase(context.role()));
    }

    private static Map<String, Object> helpSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "answer", Map.of("type", "string", "description", "Câu trả lời ngắn gọn bằng tiếng Việt"),
                        "steps", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Các bước thao tác"),
                        "relatedRoute", Map.of("type", "string", "description", "Đường dẫn liên quan trong app"),
                        "actionLabel", Map.of("type", "string", "description", "Nhãn nút điều hướng"),
                        "provider", Map.of("type", "string", "enum", List.of("gemini"))
                ),
                "required", List.of("answer", "steps", "relatedRoute", "actionLabel", "provider")
        );
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI_INVALID_JSON", "AI trả về dữ liệu hướng dẫn không đúng định dạng");
        }
    }

    private HelpResponse toHelpResponse(JsonNode node, String provider) {
        List<String> steps = objectMapper.convertValue(
                node.path("steps"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        return new HelpResponse(
                limit(node.path("answer").asText("Tôi chưa có đủ thông tin để hướng dẫn chính xác."), 1200),
                steps == null || steps.isEmpty() ? List.of("Mở trang liên quan", "Kiểm tra dữ liệu", "Thực hiện thao tác theo quyền của bạn") : steps,
                limit(node.path("relatedRoute").asText("/"), 120),
                limit(node.path("actionLabel").asText("Mở trang liên quan"), 80),
                provider
        );
    }

    private Optional<String> findText(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
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

    private static String limit(String value, int limit) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit - 1).trim() + "…";
    }

    private void audit(String action, String question) {
        auditService.recordAs(CurrentUser.tenantId(), CurrentUser.username(), action, "AI", null, "Hỏi trợ lý hướng dẫn: " + limit(question, 180));
    }

}
