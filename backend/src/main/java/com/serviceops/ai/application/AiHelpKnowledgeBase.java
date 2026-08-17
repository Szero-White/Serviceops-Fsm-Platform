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
        return new UserGuideContext(role, roleLabel(role), currentPath == null || currentPath.isBlank() ? "/" : currentPath);
    }

    static HelpTopic bestTopic(String question, UserGuideContext context) {
        String normalized = normalize(question + " " + context.currentPath());
        HelpTopic topic = TOPICS.stream()
                .filter(item -> item.allowedRoles().contains(context.role()))
                .max((left, right) -> Integer.compare(left.score(normalized), right.score(normalized)))
                .orElse(defaultTopic(context.role()));
        return topic.score(normalized) == 0 ? defaultTopic(context.role()) : topic;
    }

    static String knowledgeBase(String role) {
        return TOPICS.stream()
                .filter(topic -> topic.allowedRoles().contains(role))
                .map(topic -> "- " + topic.name() + " (" + topic.route() + "): " + topic.answer()
                        + " Các bước: " + String.join(" > ", topic.steps()))
                .reduce("", (left, right) -> left + "\n" + right);
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

    private static HelpTopic defaultTopic(String role) {
        return switch (role) {
            case "OWNER" -> topicUsers();
            case "DISPATCHER" -> topicWorkOrders();
            case "CUSTOMER_SERVICE" -> topicServiceRequests();
            case "TECHNICIAN" -> topicTechnicianWork();
            case "WAREHOUSE_STAFF" -> topicInventory();
            default -> topicDashboard();
        };
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static final List<HelpTopic> TOPICS = List.of(
            topicDashboard(), topicServiceRequests(), topicCustomers(), topicAssets(), topicWorkOrders(),
            topicTechnicianWork(), topicTechnicians(), topicInventory(), topicChannels(), topicUsers(), topicAudit()
    );

    private static HelpTopic topicDashboard() {
        return new HelpTopic("Tổng quan", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("tong quan", "dashboard", "bao cao", "hom nay", "can xem gi"),
                "Trang tổng quan giúp xem tình hình vận hành trong ngày, việc đang mở, phiếu đang xử lý và rủi ro tồn kho.",
                List.of("Mở menu Tổng quan", "Xem các chỉ số ưu tiên ở đầu trang", "Kiểm tra danh sách phiếu hoặc cảnh báo cần xử lý", "Đi tới trang chi tiết nếu cần thao tác"));
    }

    private static HelpTopic topicServiceRequests() {
        return new HelpTopic("Yêu cầu dịch vụ", "/service-requests", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("yeu cau", "tiep nhan", "khach bao", "tao yeu cau", "service request", "ai goi y"),
                "Dùng để ghi nhận sự cố khách báo trước khi đủ điều kiện chuyển thành phiếu công việc.",
                List.of("Mở menu Yêu cầu dịch vụ", "Bấm Tiếp nhận yêu cầu", "Chọn khách hàng, thiết bị và kênh tiếp nhận", "Nhập tiêu đề hoặc mô tả, có thể bấm AI gợi ý", "Bấm Đồng ý để lưu", "Khi đủ thông tin, bấm Tạo phiếu"));
    }

    private static HelpTopic topicCustomers() {
        return new HelpTopic("Khách hàng", "/customers", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("khach hang", "tao khach", "so dien thoai", "dia chi", "customer"),
                "Dùng để quản lý hồ sơ khách hàng, thông tin liên hệ và dữ liệu nền cho thiết bị/yêu cầu.",
                List.of("Mở menu Khách hàng", "Bấm Thêm khách hàng", "Nhập mã, tên, số điện thoại, email và địa chỉ", "Lưu hồ sơ", "Sau đó có thể tạo thiết bị hoặc yêu cầu dịch vụ cho khách hàng này"));
    }

    private static HelpTopic topicAssets() {
        return new HelpTopic("Thiết bị", "/assets", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("thiet bi", "serial", "bao hanh", "may lanh", "tu lanh", "asset"),
                "Dùng để theo dõi thiết bị theo khách hàng, serial, bảo hành và trạng thái phục vụ.",
                List.of("Mở menu Thiết bị", "Bấm Thêm thiết bị", "Chọn khách hàng sở hữu thiết bị", "Nhập loại thiết bị, serial, hãng, model và bảo hành", "Lưu để dùng khi tiếp nhận yêu cầu hoặc tạo phiếu"));
    }

    private static HelpTopic topicWorkOrders() {
        return new HelpTopic("Phiếu công việc", "/work-orders", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("phieu", "work order", "phan cong", "xep lich", "trang thai", "ky thuat"),
                "Phiếu công việc là nhiệm vụ thực tế để điều phối kỹ thuật viên, lịch hẹn, trạng thái xử lý và phụ tùng.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu theo mã, khách hàng hoặc thiết bị", "Mở chi tiết phiếu", "Điều phối viên phân công/xếp lịch", "Kỹ thuật viên cập nhật trạng thái, chẩn đoán và giải pháp", "Kho hoặc kỹ thuật viên ghi nhận phụ tùng đã dùng"));
    }

    private static HelpTopic topicTechnicianWork() {
        return new HelpTopic("Công việc kỹ thuật viên", "/work-orders", List.of("TECHNICIAN", "OWNER", "DISPATCHER"),
                List.of("toi la ky thuat", "viec duoc giao", "cap nhat trang thai", "chan doan", "giai phap"),
                "Kỹ thuật viên tập trung vào phiếu được giao, cập nhật trạng thái và ghi nhận kết quả xử lý.",
                List.of("Mở menu Phiếu công việc", "Lọc hoặc tìm phiếu được giao cho bạn", "Mở chi tiết phiếu", "Cập nhật trạng thái theo tiến độ thực tế", "Ghi chẩn đoán, giải pháp và phụ tùng đã dùng", "Đính kèm ảnh/PDF minh chứng nếu có"));
    }

    private static HelpTopic topicTechnicians() {
        return new HelpTopic("Kỹ thuật viên", "/technicians", List.of("OWNER", "DISPATCHER"),
                List.of("ky thuat vien", "nhan su hien truong", "skills", "tay nghe", "technician"),
                "Dùng để quản lý hồ sơ kỹ thuật viên, kỹ năng, số điện thoại và trạng thái hoạt động.",
                List.of("Mở menu Kỹ thuật viên", "Bấm Thêm kỹ thuật viên", "Nhập tài khoản, tên, số điện thoại và kỹ năng", "Lưu hồ sơ", "Dùng hồ sơ này khi phân công phiếu công việc"));
    }

    private static HelpTopic topicInventory() {
        return new HelpTopic("Kho phụ tùng", "/inventory", List.of("OWNER", "WAREHOUSE_STAFF", "TECHNICIAN"),
                List.of("kho", "phu tung", "ton kho", "nhap kho", "het ton", "inventory"),
                "Dùng để quản lý phụ tùng, nhập kho, theo dõi tồn và ghi nhận phụ tùng dùng cho phiếu.",
                List.of("Mở menu Kho phụ tùng", "Kiểm tra phụ tùng sắp hết tồn", "Bấm Thêm phụ tùng hoặc Nhập kho", "Khi xử lý phiếu, ghi nhận phụ tùng đã dùng trong chi tiết phiếu", "Theo dõi tồn kho sau mỗi lần nhập hoặc sử dụng"));
    }

    private static HelpTopic topicChannels() {
        return new HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel"),
                "Dùng để chuẩn hóa nguồn tiếp nhận yêu cầu như điện thoại, website, Zalo hoặc nội bộ.",
                List.of("Mở menu Kênh tiếp nhận", "Bấm Thêm kênh nếu cần", "Nhập mã, tên, mô tả, màu và thứ tự hiển thị", "Tắt kênh không dùng thay vì xoá nếu đã phát sinh dữ liệu"));
    }

    private static HelpTopic topicUsers() {
        return new HelpTopic("Người dùng", "/users", List.of("OWNER"),
                List.of("nguoi dung", "tai khoan", "phan quyen", "role", "mat khau"),
                "OWNER dùng trang này để tạo tài khoản, phân quyền và kiểm soát truy cập của nhân viên.",
                List.of("Mở menu Người dùng", "Bấm Thêm người dùng", "Nhập username, tên hiển thị, mật khẩu tạm và vai trò", "Chọn đúng vai trò theo trách nhiệm", "Tắt tài khoản khi nhân viên nghỉ hoặc không còn quyền truy cập"));
    }

    private static HelpTopic topicAudit() {
        return new HelpTopic("Nhật ký hệ thống", "/audit", List.of("OWNER", "DISPATCHER"),
                List.of("nhat ky", "audit", "ai da sua", "lich su", "truy vet"),
                "Dùng để truy vết ai đã tạo, sửa, xoá hoặc thay đổi dữ liệu quan trọng trong hệ thống.",
                List.of("Mở menu Nhật ký hệ thống", "Xem thời gian, người thao tác, loại dữ liệu và chi tiết", "Dùng khi cần kiểm tra trách nhiệm hoặc rà soát thay đổi bất thường"));
    }

    record UserGuideContext(String role, String roleLabel, String currentPath) {
    }

    record HelpTopic(String name, String route, List<String> allowedRoles, List<String> keywords, String answer, List<String> steps) {
        int score(String normalizedQuestion) {
            int score = normalizedQuestion.contains(route) ? 3 : 0;
            for (String keyword : keywords) {
                if (normalizedQuestion.contains(keyword)) score += 2;
            }
            return score;
        }
    }
}
