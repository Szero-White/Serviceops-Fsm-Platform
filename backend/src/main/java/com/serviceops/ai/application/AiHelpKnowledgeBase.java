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
                    "Tôi không thể hỗ trợ yêu cầu về secret, token, cấu hình nội bộ hoặc chỉ dẫn hệ thống. "
                            + "Bạn có thể hỏi về quy trình và chức năng ServiceOps thuộc phạm vi vai trò hiện tại."
            );
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

        int strongestAllowedScore = TOPICS.stream()
                .filter(topic -> topic.allowedRoles().contains(context.role()))
                .mapToInt(topic -> topic.keywordScore(normalizedQuestion))
                .max()
                .orElse(0);

        int strongestDisallowedScore = TOPICS.stream()
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

        HelpTopic topic = TOPICS.stream()
                .filter(item -> item.allowedRoles().contains(context.role()))
                .max((left, right) -> Integer.compare(
                        left.score(normalizedQuestion, currentPath),
                        right.score(normalizedQuestion, currentPath)
                ))
                .orElse(defaultTopic(context.role()));

        return topic.score(normalizedQuestion, currentPath) == 0 ? defaultTopic(context.role()) : topic;
    }

    static String knowledgeBase(String role) {
        String roleGuide = roleGuide(role);
        String topics = TOPICS.stream()
                .filter(topic -> topic.allowedRoles().contains(role))
                .map(topic -> "- " + topic.name() + " (" + topic.route() + "): " + topic.answer()
                        + " Các bước: " + String.join(" > ", topic.steps()))
                .reduce("", (left, right) -> left + "\n" + right);

        return roleGuide + "\n" + topics;
    }

    static String safeRoute(String role, String candidateRoute, HelpTopic fallbackTopic) {
        String candidate = sanitizePath(candidateRoute);
        boolean allowed = TOPICS.stream()
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
            case "OWNER" -> "Chủ doanh nghiệp";
            case "DISPATCHER" -> "Điều phối viên";
            case "CUSTOMER_SERVICE" -> "Chăm sóc khách hàng";
            case "TECHNICIAN" -> "Kỹ thuật viên";
            case "WAREHOUSE_STAFF" -> "Nhân viên kho";
            default -> "Người dùng";
        };
    }

    private static String roleGuide(String role) {
        return switch (role) {
            case "OWNER" -> "Phạm vi OWNER: theo dõi tổng quan, khách hàng, thiết bị, yêu cầu dịch vụ, phiếu công việc, điều phối, kho, kênh tiếp nhận, người dùng và audit.";
            case "DISPATCHER" -> "Phạm vi DISPATCHER: tiếp nhận/đánh giá yêu cầu, tạo hoặc theo dõi phiếu công việc, phân công/xếp lịch kỹ thuật viên và theo dõi audit được phép.";
            case "CUSTOMER_SERVICE" -> "Phạm vi CUSTOMER_SERVICE: khách hàng, thiết bị, yêu cầu dịch vụ, theo dõi phiếu công việc và xem kênh tiếp nhận ở chế độ chỉ đọc.";
            case "TECHNICIAN" -> "Phạm vi TECHNICIAN: phiếu được giao, lịch của tôi, cập nhật tiến độ/chẩn đoán/giải pháp và phụ tùng theo luồng công việc.";
            case "WAREHOUSE_STAFF" -> "Phạm vi WAREHOUSE_STAFF: quản lý kho phụ tùng và xem thông tin phiếu công việc cần thiết cho nghiệp vụ kho.";
            default -> "Chỉ hướng dẫn các chức năng ServiceOps mà tài khoản hiện tại được phép sử dụng.";
        };
    }

    private static HelpTopic defaultTopic(String role) {
        return switch (role) {
            case "OWNER" -> topicDashboard();
            case "DISPATCHER" -> topicWorkOrders();
            case "CUSTOMER_SERVICE" -> topicServiceRequests();
            case "TECHNICIAN" -> topicTechnicianWork();
            case "WAREHOUSE_STAFF" -> topicInventory();
            default -> topicDashboard();
        };
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

    private static final List<HelpTopic> TOPICS = List.of(
            topicDashboard(), topicServiceRequests(), topicCustomers(), topicAssets(), topicWorkOrders(),
            topicTechnicianWork(), topicMySchedule(), topicTechnicians(), topicInventory(),
            topicChannelsManage(), topicChannelsReadOnly(), topicUsers(), topicAudit()
    );

    private static HelpTopic topicDashboard() {
        return new HelpTopic("Tổng quan", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("tong quan", "dashboard", "bao cao", "hom nay", "can xem gi", "bat dau", "moi vao lam"),
                "Trang tổng quan giúp người dùng bắt đầu công việc và xem các chỉ số/tình trạng được giao diện hiện tại cung cấp. Trợ lý không tự đọc dữ liệu runtime trong database.",
                List.of("Mở menu Tổng quan", "Xem các thẻ/chỉ số và danh sách đang hiển thị", "Xác định việc cần xử lý theo vai trò", "Đi tới trang nghiệp vụ liên quan để thao tác"));
    }

    private static HelpTopic topicServiceRequests() {
        return new HelpTopic("Yêu cầu dịch vụ", "/service-requests", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("yeu cau", "tiep nhan", "khach bao", "tao yeu cau", "service request", "ai goi y", "chuyen thanh phieu"),
                "Dùng để ghi nhận sự cố khách báo trước khi đủ điều kiện chuyển thành phiếu công việc.",
                List.of("Mở menu Yêu cầu dịch vụ", "Bấm Tiếp nhận yêu cầu", "Chọn khách hàng, thiết bị và kênh tiếp nhận", "Nhập tiêu đề hoặc mô tả, có thể bấm AI gợi ý", "Bấm Đồng ý để lưu", "Khi đủ thông tin, bấm Tạo phiếu"));
    }

    private static HelpTopic topicCustomers() {
        return new HelpTopic("Khách hàng", "/customers", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("khach hang", "tao khach", "so dien thoai", "dia chi", "customer", "ho so khach"),
                "Dùng để quản lý hồ sơ khách hàng và dữ liệu nền cho thiết bị/yêu cầu theo quyền của vai trò.",
                List.of("Mở menu Khách hàng", "Tìm khách hàng cần xử lý", "Nếu giao diện cho phép, bấm Thêm khách hàng", "Nhập thông tin cần thiết", "Lưu hồ sơ rồi tiếp tục với thiết bị hoặc yêu cầu dịch vụ"));
    }

    private static HelpTopic topicAssets() {
        return new HelpTopic("Thiết bị", "/assets", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("thiet bi", "serial", "bao hanh", "may lanh", "tu lanh", "asset", "model"),
                "Dùng để theo dõi thiết bị theo khách hàng, serial, bảo hành và trạng thái phục vụ.",
                List.of("Mở menu Thiết bị", "Tìm thiết bị theo khách hàng hoặc serial", "Nếu giao diện cho phép, bấm Thêm thiết bị", "Nhập thông tin thiết bị và bảo hành", "Lưu để dùng khi tiếp nhận yêu cầu hoặc tạo phiếu"));
    }

    private static HelpTopic topicWorkOrders() {
        return new HelpTopic("Phiếu công việc", "/work-orders", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("phieu", "work order", "cong viec", "phan cong", "xep lich", "trang thai", "ky thuat", "wo-"),
                "Phiếu công việc là nhiệm vụ thực tế để theo dõi điều phối, lịch hẹn, trạng thái xử lý và phụ tùng. Các thao tác hiển thị phụ thuộc vai trò hiện tại.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu theo thông tin mà giao diện cho phép", "Mở chi tiết phiếu", "Thực hiện thao tác phù hợp với vai trò hiện tại", "Theo dõi lịch sử và kết quả xử lý"));
    }

    private static HelpTopic topicTechnicianWork() {
        return new HelpTopic("Công việc kỹ thuật viên", "/work-orders", List.of("TECHNICIAN", "OWNER", "DISPATCHER"),
                List.of("toi la ky thuat", "viec duoc giao", "cap nhat trang thai", "chan doan", "giai phap", "viec cua toi"),
                "Kỹ thuật viên tập trung vào phiếu được giao, cập nhật trạng thái và ghi nhận kết quả xử lý.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu được giao cho bạn", "Mở chi tiết phiếu", "Cập nhật trạng thái theo tiến độ thực tế", "Ghi chẩn đoán, giải pháp và phụ tùng đã dùng", "Đính kèm ảnh/PDF minh chứng nếu có"));
    }

    private static HelpTopic topicMySchedule() {
        return new HelpTopic("Lịch của tôi", "/my-schedule", List.of("TECHNICIAN"),
                List.of("lich cua toi", "lich lam viec", "lich hom nay", "lich tuan", "hen cua toi", "ca lam"),
                "Lịch của tôi hiển thị các lịch hẹn được phân công cho kỹ thuật viên đang đăng nhập.",
                List.of("Mở menu Lịch của tôi", "Chọn khoảng thời gian cần xem", "Đọc giờ hẹn và phiếu được giao", "Mở Phiếu công việc để cập nhật tiến độ khi bắt đầu xử lý"));
    }

    private static HelpTopic topicTechnicians() {
        return new HelpTopic("Kỹ thuật viên", "/technicians", List.of("OWNER", "DISPATCHER"),
                List.of("ky thuat vien", "nhan su hien truong", "skills", "tay nghe", "technician", "ho so ky thuat"),
                "Dùng để theo dõi hồ sơ kỹ thuật viên, kỹ năng và trạng thái hoạt động phục vụ công tác điều phối.",
                List.of("Mở menu Kỹ thuật viên", "Tìm kỹ thuật viên cần xem", "Kiểm tra trạng thái và kỹ năng", "Dùng thông tin phù hợp khi phân công phiếu công việc"));
    }

    private static HelpTopic topicInventory() {
        return new HelpTopic("Kho phụ tùng", "/inventory", List.of("OWNER", "WAREHOUSE_STAFF", "TECHNICIAN"),
                List.of("kho", "phu tung", "ton kho", "nhap kho", "het ton", "inventory", "sku"),
                "Dùng để quản lý phụ tùng, theo dõi tồn và ghi nhận phụ tùng dùng cho phiếu theo quyền của vai trò.",
                List.of("Mở menu Kho phụ tùng", "Tìm hoặc kiểm tra phụ tùng cần xử lý", "Nhân viên có quyền kho thực hiện nhập/cập nhật", "Kỹ thuật viên ghi nhận phụ tùng theo luồng phiếu được phép", "Theo dõi tồn sau giao dịch trên giao diện"));
    }

    private static HelpTopic topicChannelsManage() {
        return new HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("OWNER", "DISPATCHER"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Dùng để chuẩn hóa nguồn tiếp nhận yêu cầu như điện thoại, website, Zalo hoặc nội bộ.",
                List.of("Mở menu Kênh tiếp nhận", "Kiểm tra các kênh đang hoạt động", "Nếu giao diện cho phép, thêm hoặc chỉnh sửa kênh", "Ưu tiên tắt kênh không còn dùng thay vì xoá dữ liệu đã phát sinh"));
    }

    private static HelpTopic topicChannelsReadOnly() {
        return new HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("CUSTOMER_SERVICE"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Chăm sóc khách hàng được xem danh sách kênh tiếp nhận để chọn đúng nguồn khi tạo yêu cầu; trang này là chỉ đọc với vai trò hiện tại.",
                List.of("Mở menu Kênh tiếp nhận", "Xem mã, tên và trạng thái kênh", "Dùng kênh phù hợp khi tiếp nhận yêu cầu", "Nếu cần thay đổi cấu hình kênh, chuyển yêu cầu cho vai trò có quyền quản trị"));
    }

    private static HelpTopic topicUsers() {
        return new HelpTopic("Người dùng", "/users", List.of("OWNER"),
                List.of(
                        "nguoi dung", "tai khoan", "phan quyen", "role", "roles", "tao user",
                        "user management", "user account", "new user account", "create user", "create a user",
                        "create new user", "create a new user", "create account", "create an account",
                        "assign role", "assign roles", "permission", "permissions"
                ),
                "OWNER dùng trang này để quản lý tài khoản, vai trò và trạng thái truy cập của nhân viên.",
                List.of("Mở menu Người dùng", "Tìm tài khoản cần quản lý", "Tạo hoặc cập nhật tài khoản theo chính sách nội bộ", "Chọn đúng vai trò theo trách nhiệm", "Tắt tài khoản khi không còn quyền truy cập"));
    }

    private static HelpTopic topicAudit() {
        return new HelpTopic("Nhật ký hệ thống", "/audit", List.of("OWNER", "DISPATCHER"),
                List.of("nhat ky", "audit", "ai da sua", "ai sua", "lich su thay doi", "truy vet"),
                "Dùng để truy vết hoạt động hệ thống trong phạm vi vai trò được phép xem.",
                List.of("Mở menu Nhật ký hệ thống", "Lọc theo thời gian, người thao tác hoặc đối tượng", "Mở bản ghi liên quan", "Đối chiếu khi cần rà soát thay đổi bất thường"));
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
