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

        if (isRoleOverviewRequest(normalizedQuestion)) {
            return ScopeDecision.allowed(roleOverview(context.role()));
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
                            + ". Kỹ thuật viên chỉ được xem phụ tùng và ghi nhận vật tư cho Work Order được giao."
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

        if (isRoleOverviewRequest(normalizedQuestion)) {
            return roleOverview(context.role());
        }

        if (isFormFeedbackRequest(normalizedQuestion) && topicFormFeedback().allowedRoles().contains(context.role())) {
            return topicFormFeedback();
        }

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
        HelpTopic overview = roleOverview(role);
        String topics = TOPICS.stream()
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
            case "OWNER" -> "Quản trị hệ thống";
            case "DISPATCHER" -> "Điều phối viên";
            case "CUSTOMER_SERVICE" -> "Chăm sóc khách hàng";
            case "TECHNICIAN" -> "Kỹ thuật viên";
            case "WAREHOUSE_STAFF" -> "Nhân viên kho";
            default -> "Người dùng";
        };
    }

    private static String roleGuide(String role) {
        return switch (role) {
            case "OWNER" -> "Phạm vi OWNER: quản trị và giám sát toàn hệ thống trong các màn hình dành cho Owner. Owner có thể quản lý người dùng, dữ liệu nền, yêu cầu dịch vụ, điều phối, kho và audit; với Work Order có admin override cho điều phối và hậu xử lý. Owner không giả lập tiến độ hiện trường, không consume phụ tùng thay Technician và không dùng Lịch của tôi của một Technician.";
            case "DISPATCHER" -> "Phạm vi DISPATCHER: đọc dữ liệu khách hàng/thiết bị cần cho điều phối, theo dõi Work Order, phân công hoặc điều phối lại kỹ thuật viên/lịch trước khi field work bắt đầu, operational cancellation và audit. Không quản trị tài khoản, không tiếp nhận Service Request và không thao tác kho.";
            case "CUSTOMER_SERVICE" -> "Phạm vi CUSTOMER_SERVICE: khách hàng, thiết bị, yêu cầu dịch vụ, chuyển yêu cầu đủ thông tin sang Work Order và tiếp nhận phản hồi sau dịch vụ; có thể mở lại hoặc hủy Work Order khi khách thay đổi nhu cầu, nhưng không phân công kỹ thuật viên, không ghi nhận Khách xác nhận/Đóng phiếu và không quản trị kho/người dùng.";
            case "TECHNICIAN" -> "Phạm vi TECHNICIAN: chỉ Work Order được giao, Lịch của tôi, tiến độ/chẩn đoán/giải pháp, bằng chứng và phụ tùng dùng cho chính job; sau khi hoàn thành có thể ghi nhận Khách xác nhận và Đóng phiếu cho job được giao, hoặc Mở lại trước khi đóng nếu cùng sự cố còn tồn tại. Không quản trị người dùng, khách hàng, điều phối hoặc nghiệp vụ quản trị kho.";
            case "WAREHOUSE_STAFF" -> "Phạm vi WAREHOUSE_STAFF: danh mục phụ tùng, nhập kho, ngưỡng tồn, kiểm kê/điều chỉnh, hoàn trả và lịch sử biến động. Không có Work Order operational, Customer/Asset, User Management hoặc dashboard vận hành.";
            default -> "Chỉ hướng dẫn các chức năng ServiceOps mà tài khoản hiện tại được phép sử dụng.";
        };
    }

    private static HelpTopic defaultTopic(String role) {
        return roleOverview(role);
    }

    private static HelpTopic roleOverview(String role) {
        return switch (role) {
            case "OWNER" -> new HelpTopic(
                    "Phạm vi Chủ sở hữu",
                    "/",
                    List.of("OWNER"),
                    List.of(),
                    "Bạn có thể quản trị người dùng; khách hàng và thiết bị; tiếp nhận/chuyển yêu cầu dịch vụ; cấu hình kênh tiếp nhận; theo dõi, điều phối và hậu xử lý Work Order; quản lý đội ngũ kỹ thuật; quản lý kho, kiểm kê và lịch sử biến động; xem dashboard, notification và audit. Các guard an toàn vẫn được giữ: không tự tạm ngưng/xóa tài khoản đang đăng nhập, không làm mất Owner cuối cùng, không sửa tài khoản demo cố định, không giả lập field progress hoặc consume phụ tùng thay Technician.",
                    List.of("Bắt đầu ở Tổng quan để xem tình trạng vận hành", "Mở Người dùng để quản lý tài khoản và lọc Đang hoạt động/Tạm ngưng", "Dùng Khách hàng, Thiết bị và Yêu cầu dịch vụ cho dữ liệu đầu vào", "Dùng Phiếu công việc/Lịch điều phối để quản trị điều phối và hậu xử lý", "Dùng Kho phụ tùng, Kiểm kê và Lịch sử biến động cho tồn kho", "Dùng Nhật ký hệ thống và chuông thông báo để truy vết việc quan trọng")
            );
            case "DISPATCHER" -> new HelpTopic(
                    "Phạm vi Điều phối viên",
                    "/work-orders",
                    List.of("DISPATCHER"),
                    List.of(),
                    "Bạn tập trung vào Work Order đã chuyển sang điều phối: xem dữ liệu liên quan, phân công kỹ thuật viên, xếp lịch hoặc điều phối lại trước khi công việc bắt đầu, theo dõi lịch và xử lý hủy vận hành khi cần. Bạn không quản trị tài khoản, không tiếp nhận Service Request và không thao tác kho.",
                    List.of("Mở Phiếu công việc để xem hàng việc", "Mở Lịch điều phối để phân công hoặc điều phối lại", "Kiểm tra đội ngũ kỹ thuật trước khi chọn người", "Dùng Nhật ký hệ thống khi cần truy vết thay đổi")
            );
            case "CUSTOMER_SERVICE" -> new HelpTopic(
                    "Phạm vi Chăm sóc khách hàng",
                    "/service-requests",
                    List.of("CUSTOMER_SERVICE"),
                    List.of(),
                    "Bạn quản lý khách hàng và thiết bị, tiếp nhận/cập nhật yêu cầu dịch vụ, chọn kênh tiếp nhận và chuyển yêu cầu đủ thông tin sang Work Order. Sau dịch vụ, bạn tiếp nhận phản hồi để mở lại hoặc hủy theo policy; không phân công kỹ thuật viên, không Khách xác nhận/Đóng phiếu và không quản trị người dùng/kho.",
                    List.of("Kiểm tra hoặc tạo Khách hàng", "Gắn đúng Thiết bị", "Tiếp nhận Yêu cầu dịch vụ", "Chuyển yêu cầu đủ thông tin sang điều phối", "Theo dõi Work Order và tiếp nhận phản hồi sau dịch vụ")
            );
            case "TECHNICIAN" -> new HelpTopic(
                    "Phạm vi Kỹ thuật viên",
                    "/work-orders",
                    List.of("TECHNICIAN"),
                    List.of(),
                    "Bạn chỉ thao tác công việc được giao cho mình: xem Lịch của tôi, cập nhật tiến độ thực tế, ghi bằng chứng/chẩn đoán/giải pháp, ghi phụ tùng đã dùng và hoàn tất hậu xử lý được phép cho chính job. Bạn không quản trị người dùng, khách hàng, điều phối hoặc nghiệp vụ quản trị kho.",
                    List.of("Mở Lịch của tôi để xem lịch hẹn", "Mở Phiếu công việc được giao", "Cập nhật tiến độ đúng thực tế", "Ghi phụ tùng/bằng chứng khi cần", "Nhập chẩn đoán và giải pháp trước khi Hoàn thành", "Thực hiện Khách xác nhận/Đóng phiếu nếu đúng trạng thái và quyền")
            );
            case "WAREHOUSE_STAFF" -> new HelpTopic(
                    "Phạm vi Nhân viên kho",
                    "/inventory",
                    List.of("WAREHOUSE_STAFF"),
                    List.of(),
                    "Bạn quản lý nghiệp vụ kho: danh mục phụ tùng, nhập kho, ngưỡng tồn tối thiểu, kiểm kê/điều chỉnh, hoàn trả theo Work Order và lịch sử biến động. Bạn không có quyền thao tác Work Order hiện trường, Customer/Asset, quản trị người dùng hoặc dashboard vận hành.",
                    List.of("Mở Kho phụ tùng để kiểm tra tồn và ngưỡng", "Nhập kho hoặc cập nhật catalog theo quyền", "Dùng Kiểm kê tồn kho để đối soát", "Dùng Lịch sử biến động để truy vết nhập/dùng/hoàn/điều chỉnh")
            );
            default -> new HelpTopic("Hướng dẫn ServiceOps", "/", List.of("USER"), List.of(), "Chỉ hướng dẫn chức năng mà tài khoản hiện tại được phép sử dụng.", List.of("Mở menu được cấp quyền để bắt đầu"));
        };
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

    private static final List<HelpTopic> TOPICS = List.of(
            topicDashboard(), topicServiceRequests(), topicCustomers(), topicAssets(), topicWorkOrders(), topicDispatch(),
            topicTechnicianWork(), topicTechnicianParts(), topicMySchedule(), topicTechnicians(), topicInventory(),
            topicInventoryStocktake(), topicInventoryMovements(), topicInventoryReturns(),
            topicChannelsManage(), topicChannelsReadOnly(), topicUsers(), topicAudit(), topicFormFeedback(), topicNotifications()
    );

    private static HelpTopic topicDashboard() {
        return new HelpTopic("Tổng quan", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("tong quan", "dashboard", "bao cao", "hom nay", "can xem gi", "bat dau", "moi vao lam"),
                "Trang tổng quan giúp người dùng bắt đầu công việc và xem các chỉ số/tình trạng được giao diện hiện tại cung cấp. Trợ lý không tự đọc dữ liệu runtime trong database.",
                List.of("Mở menu Tổng quan", "Xem các thẻ/chỉ số và danh sách đang hiển thị", "Xác định việc cần xử lý theo vai trò", "Đi tới trang nghiệp vụ liên quan để thao tác"));
    }

    private static HelpTopic topicServiceRequests() {
        return new HelpTopic("Yêu cầu dịch vụ", "/service-requests", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("yeu cau", "tiep nhan", "khach bao", "tao yeu cau", "service request", "ai goi y", "chuyen thanh phieu", "chuyen sang dieu phoi"),
                "Dùng để ghi nhận sự cố khách báo trước khi đủ điều kiện chuyển thành phiếu công việc. OWNER và CUSTOMER_SERVICE đều có thể chuyển một yêu cầu hợp lệ sang Work Order; backend vẫn kiểm tra customer/asset/state để không tạo trùng hoặc sai liên kết.",
                List.of("Mở menu Yêu cầu dịch vụ", "Bấm Tiếp nhận yêu cầu", "Chọn khách hàng, thiết bị và kênh tiếp nhận", "Nhập tiêu đề hoặc mô tả, có thể bấm AI gợi ý", "Bấm Tiếp nhận yêu cầu để lưu", "Khi đủ thông tin, bấm Chuyển sang điều phối"));
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
        return new HelpTopic("Phiếu công việc", "/work-orders", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("phieu", "work order", "cong viec", "phan cong", "xep lich", "trang thai", "ky thuat", "tien trinh", "phu tung da dung", "wo-"),
                "Phiếu công việc là nhiệm vụ thực tế để theo dõi điều phối, lịch hẹn, trạng thái xử lý và phụ tùng. Tab Tiến trình ghép lịch sử trạng thái với các giao dịch CONSUME/RETURN của chính Work Order, nên có thể xem kỹ thuật viên đã dùng phụ tùng gì, số lượng bao nhiêu, ai thao tác và lúc nào mà không phải chờ đến hóa đơn. Các thao tác hiển thị phụ thuộc vai trò hiện tại.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu theo thông tin mà giao diện cho phép", "Mở chi tiết phiếu", "Mở tab Tiến trình để xem trạng thái và phụ tùng đã dùng/hoàn trả theo thời gian", "Thực hiện thao tác phù hợp với vai trò hiện tại", "Đối chiếu kết quả xử lý trước khi đóng phiếu"));
    }

    private static HelpTopic topicDispatch() {
        return new HelpTopic("Điều phối và xếp lịch", "/schedule", List.of("OWNER", "DISPATCHER"),
                List.of("phan cong", "xep lich", "dieu phoi", "dieu phoi lai", "doi ky thuat vien", "doi lich", "chua bat dau", "reschedule", "reassign", "trung lich"),
                "Dùng để phân công kỹ thuật viên và thời gian thực hiện. Sau lần phân công đầu, OWNER/DISPATCHER có thể điều phối lại kỹ thuật viên hoặc lịch khi Work Order vẫn ở OPEN, SCHEDULED, ASSIGNED hoặc REOPENED. Điều phối lại phải có lý do; nếu đổi người, kỹ thuật viên cũ và mới đều nhận thông báo phù hợp và Tiến trình ghi nhận thay đổi. Khi đã ON_THE_WAY hoặc IN_PROGRESS thì không điều phối lại bằng schedule endpoint.",
                List.of("Mở Lịch điều phối hoặc chi tiết Phiếu công việc", "Chọn kỹ thuật viên và thời gian phù hợp", "Kiểm tra cảnh báo trùng lịch", "Nếu phiếu đã phân công nhưng chưa bắt đầu, dùng Điều phối lại", "Nhập lý do khi đổi kỹ thuật viên hoặc lịch", "Kiểm tra notification và tab Tiến trình sau khi lưu"));
    }

    private static HelpTopic topicTechnicianWork() {
        return new HelpTopic("Công việc kỹ thuật viên", "/work-orders", List.of("TECHNICIAN"),
                List.of("toi la ky thuat", "viec duoc giao", "cap nhat trang thai", "chan doan", "giai phap", "viec cua toi", "dung phu tung", "phu tung da dung", "tien trinh xu ly"),
                "Kỹ thuật viên tập trung vào phiếu được giao, cập nhật trạng thái và ghi nhận kết quả xử lý. Phụ tùng chỉ được ghi nhận khi phiếu đang ở ASSIGNED, ON_THE_WAY, IN_PROGRESS, WAITING_FOR_PARTS hoặc REOPENED; sau COMPLETED/CUSTOMER_ACCEPTED không được phát sinh CONSUME mới. Sau khi bấm Dùng phụ tùng, giao dịch được lưu ở sổ biến động kho và đồng thời xuất hiện trong tab Tiến trình của Work Order với tên/SKU, số lượng, người thao tác, thời gian và ghi chú. Khi hoàn thành, Chẩn đoán / nguyên nhân và Giải pháp đã thực hiện là bắt buộc; mỗi lần hoàn thành lưu một snapshot riêng trong Tiến trình, gồm chẩn đoán, giải pháp và ghi chú bàn giao nếu có. Nếu phiếu được Mở lại, form Hoàn thành điền sẵn chẩn đoán/giải pháp gần nhất để kỹ thuật viên giữ nguyên hoặc cập nhật, còn Tổng quan luôn hiển thị kết quả đã lưu mới nhất. Sau COMPLETED, kỹ thuật viên được giao hoặc Owner mở chi tiết phiếu và bấm Khách xác nhận khi khách đồng ý; trạng thái sau đó cho phép Đóng phiếu. Nếu khách báo lỗi lại trước khi đóng, dùng Mở lại xử lý. CLOSED/CANCELLED là trạng thái kết thúc và sự cố phát sinh sau CLOSED phải đi qua yêu cầu/phiếu mới.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu được giao cho bạn", "Mở chi tiết phiếu", "Cập nhật trạng thái theo tiến độ thực tế", "Khi dùng phụ tùng, mở tab Tiến trình để kiểm tra tên/SKU, số lượng và thời điểm đã ghi nhận", "Khi bấm Hoàn thành, nhập đủ Chẩn đoán / nguyên nhân và Giải pháp đã thực hiện", "Sau khi khách đồng ý, bấm Khách xác nhận cạnh Tải ảnh / PDF", "Bấm Đóng phiếu để chuyển sang Lịch sử phiếu", "Nếu khách báo lỗi trước khi đóng, dùng Mở lại xử lý", "Đính kèm ảnh/PDF minh chứng nếu có"));
    }

    private static HelpTopic topicTechnicianParts() {
        return new HelpTopic("Phụ tùng cho công việc được giao", "/inventory", List.of("TECHNICIAN"),
                List.of("phu tung", "ton kho", "inventory", "sku", "dung phu tung", "consume part", "vat tu"),
                "Kỹ thuật viên được xem danh mục/tồn phụ tùng cần thiết cho công việc và ghi nhận CONSUME từ Work Order được giao khi trạng thái cho phép. Kỹ thuật viên không nhập kho, không sửa ngưỡng tồn, không kiểm kê/điều chỉnh và không hoàn trả thay Warehouse.",
                List.of("Mở Work Order được giao", "Kiểm tra phụ tùng cần dùng và tồn hiện tại", "Dùng thao tác Dùng phụ tùng trong trạng thái được phép", "Nhập số lượng và mục đích sử dụng", "Mở tab Tiến trình để kiểm tra giao dịch đã ghi"));
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
        return new HelpTopic("Kho phụ tùng", "/inventory", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kho", "phu tung", "ton kho", "nhap kho", "het ton", "inventory", "sku", "muc dat hang", "nguong ton toi thieu", "reorder level"),
                "Dùng để quản lý danh mục phụ tùng và tồn hiện tại. Ngưỡng tồn tối thiểu là mốc cảnh báo tồn thấp, không phải số lượng đặt mua. OWNER/WAREHOUSE_STAFF có thể quản lý catalog, nhập kho và chỉnh ngưỡng; nếu ngưỡng mới làm tồn hiện tại chuyển sang trạng thái tồn thấp, hệ thống phát cảnh báo sau commit cho Warehouse liên quan; Owner theo dõi tồn kho qua workspace thay vì nhận bell vận hành.",
                List.of("Mở menu Kho phụ tùng", "Tìm phụ tùng theo SKU hoặc tên", "Kiểm tra tồn hiện tại và ngưỡng tồn tối thiểu", "Tạo/cập nhật catalog hoặc nhập kho theo quyền", "Dùng Sửa ngưỡng khi cần thay đổi mốc cảnh báo", "Nếu tồn chạm hoặc thấp hơn ngưỡng, kiểm tra cảnh báo tồn thấp", "Dùng Kiểm kê tồn kho hoặc Lịch sử biến động khi cần đối soát"));
    }

    private static HelpTopic topicInventoryStocktake() {
        return new HelpTopic("Kiểm kê tồn kho", "/inventory-stocktake", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kiem ke", "stocktake", "doi soat ton", "chenh lech ton", "ton thuc te", "dieu chinh ton", "adjustment", "thong bao kiem ke", "ai nhan thong bao"),
                "Dùng để đối chiếu số lượng hệ thống với số đếm thực tế. Chênh lệch được ghi thành ADJUSTMENT_IN hoặc ADJUSTMENT_OUT kèm lý do và audit trail. Sau khi giao dịch commit, OWNER được thông báo về chênh lệch; nếu tồn sau kiểm kê chạm hoặc thấp hơn ngưỡng tồn tối thiểu thì Warehouse nhận cảnh báo tồn thấp. TECHNICIAN không nhận broadcast kiểm kê vì hệ thống hiện chưa có part-request/reservation để xác định Work Order nào thực sự bị thiếu vật tư.",
                List.of("Mở menu Kiểm kê tồn kho", "Tìm SKU cần kiểm kê", "Bấm Kiểm kê", "Nhập số lượng thực tế và lý do", "Xác nhận điều chỉnh", "OWNER nhận thông báo nếu có chênh lệch", "Nếu tồn thấp, Warehouse nhận cảnh báo", "Mở Lịch sử biến động để kiểm tra giao dịch điều chỉnh và tồn sau"));
    }

    private static HelpTopic topicInventoryMovements() {
        return new HelpTopic("Lịch sử biến động kho", "/inventory-movements", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("lich su bien dong kho", "lich su bien dong", "bien dong kho", "giao dich kho", "inventory movement", "inventory transaction", "ledger", "receive", "consume", "adjustment"),
                "Dùng để truy vết các thay đổi tồn kho như nhập kho, sử dụng cho Work Order, hoàn trả và điều chỉnh kiểm kê; hiển thị actor, thời gian, số lượng, tồn sau và mã Work Order khi có.",
                List.of("Mở menu Lịch sử biến động", "Tìm theo SKU, tên, mã WO, người thực hiện hoặc ghi chú", "Lọc theo loại giao dịch hoặc khoảng ngày", "Đối chiếu số lượng biến động và tồn sau", "Nếu một giao dịch CONSUME còn số lượng chưa dùng, Warehouse/Owner có thể mở thao tác Hoàn trả"));
    }

    private static HelpTopic topicInventoryReturns() {
        return new HelpTopic("Hoàn trả phụ tùng theo Work Order", "/inventory-movements", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("hoan tra", "tra lai phu tung", "return part", "part return", "phu tung chua dung", "khong dung het"),
                "Dùng để xác nhận phụ tùng đã ghi nhận CONSUME cho Work Order nhưng thực tế chưa sử dụng hết được trả lại kho. Tổng RETURN không được vượt lượng net consumed còn lại và không nhận return mới khi Work Order đã CLOSED/CANCELLED.",
                List.of("Mở menu Lịch sử biến động", "Tìm giao dịch CONSUME của Work Order và phụ tùng cần trả", "Bấm Hoàn trả", "Kiểm tra số lượng tối đa có thể hoàn", "Nhập số lượng và lý do", "Xác nhận; tồn kho tăng và ledger ghi RETURN", "Đối chiếu lại tồn và số lượng còn có thể hoàn"));
    }

    private static HelpTopic topicChannelsManage() {
        return new HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("OWNER"),
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
                "OWNER dùng trang này để quản lý tài khoản, vai trò và trạng thái truy cập của nhân viên. Bộ lọc Tất cả trạng thái / Hoạt động / Tạm ngưng kết hợp với ô tìm kiếm để rà nhanh tài khoản. Username và role được cố định sau khi tạo để giữ audit/ownership; Owner vẫn được đổi họ tên, mật khẩu và trạng thái theo guard an toàn.",
                List.of("Mở menu Người dùng", "Dùng ô tìm kiếm và bộ lọc trạng thái để tìm tài khoản", "Tạo hoặc cập nhật tài khoản theo chính sách nội bộ", "Chọn đúng vai trò khi tạo mới", "Tạm ngưng tài khoản khi không còn quyền truy cập", "Không tắt chính tài khoản đang đăng nhập hoặc Owner cuối cùng"));
    }

    private static HelpTopic topicAudit() {
        return new HelpTopic("Nhật ký hệ thống", "/audit", List.of("OWNER", "DISPATCHER"),
                List.of("nhat ky", "audit", "ai da sua", "ai sua", "lich su thay doi", "truy vet"),
                "Dùng để truy vết hoạt động hệ thống trong phạm vi vai trò được phép xem.",
                List.of("Mở menu Nhật ký hệ thống", "Lọc theo thời gian, người thao tác hoặc đối tượng", "Mở bản ghi liên quan", "Đối chiếu khi cần rà soát thay đổi bất thường"));
    }

    private static HelpTopic topicFormFeedback() {
        return new HelpTopic("Phản hồi biểu mẫu", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("bam dong y khong duoc", "bam hoan thanh khong duoc", "bam luu khong duoc", "nut khong chay", "khong co phan hoi", "thieu thong tin", "truong bat buoc", "validation", "form loi"),
                "Nếu bấm Lưu/Hoàn thành mà dữ liệu bắt buộc còn thiếu, ServiceOps không gửi request: trường lỗi được đánh dấu, trang cuộn tới lỗi đầu tiên và hiện cảnh báo ngắn để người dùng biết cần bổ sung gì. Nếu request đã gửi nhưng API hoặc dữ liệu phụ trợ lỗi, giao diện hiển thị thông báo lỗi và nút Thử lại ở các màn hình tải dữ liệu chính.",
                List.of("Đọc cảnh báo vừa hiện", "Tìm trường có dấu bắt buộc hoặc viền lỗi", "Bổ sung dữ liệu theo thông báo", "Bấm lại nút hành động có tên cụ thể như Hoàn thành công việc hoặc Lưu thay đổi", "Nếu là lỗi tải dữ liệu, bấm Thử lại hoặc kiểm tra backend"));
    }

    private static HelpTopic topicNotifications() {
        return new HelpTopic("Thông báo", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("thong bao", "notification", "chua doc", "da doc", "danh dau chua doc", "mark unread", "mark as unread", "bam nham da doc"),
                "Mở biểu tượng chuông ở góc phải để xem các việc thực sự cần chú ý hoặc hành động. Dispatcher nhận “Cần phân công kỹ thuật viên”, “Phiếu đang chờ phụ tùng” và “Phiếu cần xử lý lại”; Customer Service nhận “Cần theo dõi khách sau sửa chữa”; Technician nhận “Bạn có công việc mới”, “Lịch của bạn đã thay đổi”, chuyển giao, mở lại, hủy hoặc đóng phiếu khi sự kiện do người khác thực hiện; Warehouse nhận cảnh báo tồn kho; Owner chỉ nhận kết quả cuối của Work Order (đóng/hủy) và ngoại lệ quản trị kiểm kê có chênh lệch. Mỗi notification Work Order phải cho biết mã phiếu, nội dung công việc, khách hàng và người thực hiện khi có hành động của người khác; reopen/cancel giữ lý do nghiệp vụ; body luôn nói bước tiếp theo. CRUD thường ngày, hồ sơ/danh mục, attachment, import và các bước tiến độ bình thường không tạo bell để tránh spam vì đã có workspace/Timeline/Audit. Các bell row routine cũ được migration dọn khỏi notification feed; actionable legacy còn lại được giao diện chuyển sang cách đọc thân thiện. Nếu lỡ đánh dấu một thông báo là đã đọc, bấm biểu tượng ở bên phải và chọn Đánh dấu chưa đọc.",
                List.of("Đọc tiêu đề để biết việc cần chú ý", "Đọc dòng mô tả để biết bước tiếp theo", "Bấm một thông báo chưa đọc để đánh dấu đã đọc", "Nếu lỡ đánh dấu đã đọc, bấm nút ngoài cùng bên phải để chuyển lại Chưa đọc", "Dùng mã WO/SKU trong thông báo để tìm đúng phiếu hoặc phụ tùng khi cần"));
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
