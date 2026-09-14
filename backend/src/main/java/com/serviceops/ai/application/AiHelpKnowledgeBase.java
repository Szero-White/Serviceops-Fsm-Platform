package com.serviceops.ai.application;

import com.serviceops.security.CurrentUser;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

final class AiHelpKnowledgeBase {
    private AiHelpKnowledgeBase() {
    }

    static UserGuideContext currentContext(String currentPath) {
        String role = currentRole();
        return new UserGuideContext(role, roleLabel(role), sanitizePath(currentPath));
    }

    static ScopeDecision scopeDecision(String question, UserGuideContext context) {
        String normalizedQuestion = normalize(question);

        if (isSensitiveSecurityRequest(normalizedQuestion)) {
            return ScopeDecision.denied(
                    defaultTopic(context.role()),
                    "Tôi không thể cung cấp thông tin bảo mật, thông tin truy cập hoặc cấu hình nội bộ của hệ thống. "
                            + "Bạn có thể hỏi về quy trình và chức năng ServiceOps thuộc phạm vi vai trò hiện tại."
            );
        }

        if (isRoleOverviewRequest(normalizedQuestion)) {
            return ScopeDecision.allowed(AiHelpTopicCatalog.roleOverview(context.role()));
        }

        // User/role administration is an OWNER-only capability. Keep this as an
        // explicit backend guard so English wording or mixed-language prompts cannot
        // be reinterpreted by Gemini as technician assignment or another allowed topic.
        if (!"OWNER".equals(context.role()) && isUserAdministrationRequest(normalizedQuestion)) {
            return ScopeDecision.denied(
                    defaultTopic(context.role()),
                    "Nội dung quản lý tài khoản và phân quyền nằm ngoài phạm vi của vai trò "
                            + context.roleLabel() + "."
            );
        }

        if (!List.of("OWNER", "WAREHOUSE_STAFF").contains(context.role())
                && isInventoryAdministrationRequest(normalizedQuestion)) {
            return ScopeDecision.denied(
                    defaultTopic(context.role()),
                    "Nội dung quản trị kho nằm ngoài phạm vi của vai trò " + context.roleLabel()
                            + ". Kỹ thuật viên chỉ được xem phụ tùng và ghi nhận vật tư cho phiếu công việc được giao."
            );
        }

        int strongestAllowedScore = AiHelpTopicCatalog.topics().stream()
                .filter(topic -> topic.allowedRoles().contains(context.role()))
                .mapToInt(topic -> topic.keywordScore(normalizedQuestion))
                .max()
                .orElse(0);

        int strongestDisallowedScore = AiHelpTopicCatalog.topics().stream()
                .filter(topic -> !topic.allowedRoles().contains(context.role()))
                .mapToInt(topic -> topic.keywordScore(normalizedQuestion))
                .max()
                .orElse(0);

        // Deny only when the question clearly points more strongly to a domain the
        // current role cannot use. If allowed and disallowed topics tie, keep the
        // request inside the role-filtered knowledge base instead of false-blocking it.
        if (strongestDisallowedScore > 0 && strongestDisallowedScore > strongestAllowedScore) {
            return ScopeDecision.denied(
                    defaultTopic(context.role()),
                    "Nội dung này nằm ngoài phạm vi hướng dẫn của vai trò " + context.roleLabel()
                            + ". Tôi chỉ hỗ trợ các nghiệp vụ mà vai trò hiện tại được phép sử dụng trong ServiceOps."
            );
        }

        return ScopeDecision.allowed(bestTopic(question, context));
    }

    static HelpTopic bestTopic(String question, UserGuideContext context) {
        String normalizedQuestion = normalize(question);
        String currentPath = context.currentPath();

        if (isRoleOverviewRequest(normalizedQuestion)) {
            return AiHelpTopicCatalog.roleOverview(context.role());
        }

        if (isPendingClosureHistoryRequest(normalizedQuestion)
                && AiHelpTopicCatalog.topicWorkOrderHistory().allowedRoles().contains(context.role())) {
            return AiHelpTopicCatalog.topicWorkOrderHistory();
        }

        if (isFormFeedbackRequest(normalizedQuestion) && AiHelpTopicCatalog.topicFormFeedback().allowedRoles().contains(context.role())) {
            return AiHelpTopicCatalog.topicFormFeedback();
        }

        if (isNotificationReadStateRequest(normalizedQuestion)) {
            return notificationReadStateTopic(context.role());
        }

        HelpTopic topic = AiHelpTopicCatalog.topics().stream()
                .filter(item -> item.allowedRoles().contains(context.role()))
                .max((left, right) -> Integer.compare(
                        left.score(normalizedQuestion, currentPath),
                        right.score(normalizedQuestion, currentPath)
                ))
                .orElse(defaultTopic(context.role()));

        return topic.score(normalizedQuestion, currentPath) == 0 ? defaultTopic(context.role()) : topic;
    }

    static String knowledgeBase(String role) {
        String roleGuide = AiHelpTopicCatalog.roleGuide(role);
        HelpTopic overview = AiHelpTopicCatalog.roleOverview(role);
        String topics = AiHelpTopicCatalog.topics().stream()
                .filter(topic -> topic.allowedRoles().contains(role))
                .map(topic -> "- " + topic.name() + " (" + topic.route() + "): " + topic.answer()
                        + " Các bước: " + String.join(" > ", topic.steps()))
                .reduce("", (left, right) -> left + "\n" + right);

        return roleGuide
                + "\n- " + overview.name() + " (" + overview.route() + "): " + overview.answer()
                + " Các bước: " + String.join(" > ", overview.steps())
                + "\n" + topics;
    }

    static String safeRoute(String role, String candidateRoute, HelpTopic fallbackTopic) {
        String candidate = sanitizePath(candidateRoute);
        boolean allowed = AiHelpTopicCatalog.topics().stream()
                .filter(topic -> topic.allowedRoles().contains(role))
                .map(HelpTopic::route)
                .anyMatch(route -> route.equals(candidate));

        if (allowed) {
            return candidate;
        }

        if (fallbackTopic != null && fallbackTopic.allowedRoles().contains(role)) {
            return fallbackTopic.route();
        }

        return "/";
    }

    private static String currentRole() {
        for (String role : List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF")) {
            if (CurrentUser.hasRole(role)) {
                return role;
            }
        }
        return "USER";
    }

    private static String roleLabel(String role) {
        return switch (role) {
            case "OWNER" -> "Chủ sở hữu";
            case "DISPATCHER" -> "Điều phối viên";
            case "CUSTOMER_SERVICE" -> "Chăm sóc khách hàng";
            case "TECHNICIAN" -> "Kỹ thuật viên";
            case "WAREHOUSE_STAFF" -> "Nhân viên kho";
            default -> "Người dùng";
        };
    }

    private static HelpTopic defaultTopic(String role) {
        return AiHelpTopicCatalog.roleOverview(role);
    }

    private static boolean isRoleOverviewRequest(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                "vai tro nay",
                "quyen cua toi",
                "toi duoc lam gi",
                "toi duoc lam nhung gi",
                "toi co the lam gi",
                "toi co the lam nhung gi",
                "toi co the quan ly",
                "quan ly nhung chuc nang",
                "chuc nang nao",
                "chuc nang cua toi",
                "pham vi cua toi",
                "toi nen bat dau",
                "bat dau tu dau",
                "moi lam"
        );
    }

    private static boolean isFormFeedbackRequest(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                "bam dong y khong duoc",
                "bam hoan thanh khong duoc",
                "bam luu khong duoc",
                "nut khong chay",
                "khong co phan hoi",
                "truong bat buoc",
                "validation",
                "form loi"
        );
    }

    private static boolean isSensitiveSecurityRequest(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                "system prompt",
                "system instruction",
                "developer message",
                "gemini_api_key",
                "gemini api key",
                "api key",
                "jwt secret",
                "access token",
                "refresh token",
                "database password",
                "db password",
                "mat khau database",
                "bien moi truong",
                ".env",
                "ignore previous instructions",
                "ignore all previous",
                "bo qua chi dan truoc",
                "bo qua huong dan truoc"
        );
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUserAdministrationRequest(String normalizedQuestion) {
        return containsAnyKeyword(
                normalizedQuestion,
                "quan ly nguoi dung",
                "tao tai khoan",
                "them tai khoan",
                "phan quyen",
                "tao user",
                "them user",
                "user management",
                "user account",
                "new user account",
                "create user",
                "create a user",
                "create new user",
                "create a new user",
                "create account",
                "create an account",
                "staff account",
                "employee account",
                "assign role",
                "assign roles",
                "change role",
                "change roles",
                "permission",
                "permissions"
        );
    }

    private static boolean isInventoryAdministrationRequest(String normalizedQuestion) {
        return containsAnyKeyword(
                normalizedQuestion,
                "nhap kho",
                "kiem ke",
                "stocktake",
                "dieu chinh ton",
                "nguong ton toi thieu",
                "reorder level",
                "sua nguong ton",
                "cap nhat nguong ton",
                "hoan tra phu tung",
                "return part"
        );
    }

    private static boolean containsAnyKeyword(String text, String... keywords) {
        for (String keyword : keywords) {
            if (containsKeyword(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String candidate = value.trim();
        return candidate.startsWith("/") && candidate.length() <= 120 ? candidate : "/";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsKeyword(String normalizedText, String keyword) {
        String textTokens = normalizedText
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String keywordTokens = normalize(keyword)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return !keywordTokens.isBlank()
                && (" " + textTokens + " ").contains(" " + keywordTokens + " ");
    }

    private static boolean isPendingClosureHistoryRequest(String normalizedQuestion) {
        return (normalizedQuestion.contains("settled") && normalizedQuestion.contains("chua dong"))
                || (normalizedQuestion.contains("doi soat") && normalizedQuestion.contains("chua dong phieu"))
                || normalizedQuestion.contains("cho hoan tat ho so");
    }

    private static boolean isNotificationReadStateRequest(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                "danh dau chua doc",
                "danh dau lai chua doc",
                "bam nham da doc",
                "mark unread",
                "mark as unread"
        );
    }

    private static HelpTopic notificationReadStateTopic(String role) {
        return new HelpTopic(
                "Thông báo",
                "/",
                List.of(role),
                List.of(),
                "Nếu lỡ đánh dấu một thông báo đã đọc, bấm nút Đánh dấu chưa đọc ở cuối dòng để đưa nó về hàng chờ của bạn. Thao tác này chỉ thay đổi trạng thái đọc/chưa đọc, không thay đổi nghiệp vụ hoặc quyền của vai trò.",
                List.of("Mở chuông thông báo", "Tìm thông báo đã lỡ đánh dấu đọc", "Bấm Đánh dấu chưa đọc", "Kiểm tra thông báo trở lại hàng chờ")
        );
    }

    record UserGuideContext(String role, String roleLabel, String currentPath) {
    }

    record ScopeDecision(boolean allowed, HelpTopic topic, String refusalReason) {
        static ScopeDecision allowed(HelpTopic topic) {
            return new ScopeDecision(true, topic, "");
        }

        static ScopeDecision denied(HelpTopic topic, String reason) {
            return new ScopeDecision(false, topic, reason);
        }
    }

    record HelpTopic(String name, String route, List<String> allowedRoles, List<String> keywords, String answer, List<String> steps) {
        int keywordScore(String normalizedQuestion) {
            int score = 0;
            for (String keyword : keywords) {
                if (containsKeyword(normalizedQuestion, keyword)) {
                    score += 2;
                }
            }
            return score;
        }

        int score(String normalizedQuestion, String currentPath) {
            int score = keywordScore(normalizedQuestion);
            if (route.equals(currentPath)) {
                score += 3;
            } else if (!"/".equals(route) && currentPath.startsWith(route + "/")) {
                score += 3;
            }
            return score;
        }
    }
}
