package com.serviceops.ai.application;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Final presentation guard for AI Help.
 *
 * <p>The domain model keeps stable enums, role codes and technical identifiers internally. AI
 * output is normalized here before it reaches an operational user so a provider response cannot
 * accidentally expose those identifiers as interface language.</p>
 */
final class AiUserFacingLanguage {
    private static final List<Replacement> CODE_REPLACEMENTS = List.of(
            exactCode("TRANSFER_PENDING_VERIFICATION", "Chờ xác minh chuyển khoản"),
            exactCode("COUNTER_PAYMENT_PENDING", "Chờ thanh toán tại quầy"),
            exactCode("CASH_PENDING_HANDOVER", "Chờ bàn giao tiền mặt"),
            exactCode("CUSTOMER_ACCEPTED", "Khách đã xác nhận"),
            exactCode("WAITING_FOR_PARTS", "Chờ phụ tùng"),
            exactCode("ADJUSTMENT_OUT", "Điều chỉnh giảm"),
            exactCode("ADJUSTMENT_IN", "Điều chỉnh tăng"),
            exactCode("WAREHOUSE_STAFF", "Nhân viên kho"),
            exactCode("CUSTOMER_SERVICE", "Chăm sóc khách hàng"),
            exactCode("ON_THE_WAY", "Đang di chuyển"),
            exactCode("IN_PROGRESS", "Đang thực hiện"),
            exactCode("REQUESTED", "Chờ cấp phụ tùng"),
            exactCode("COMPLETED", "Đã hoàn thành"),
            exactCode("CANCELLED", "Đã hủy"),
            exactCode("REOPENED", "Mở lại để xử lý"),
            exactCode("SCHEDULED", "Đã lên lịch"),
            exactCode("ASSIGNED", "Đã phân công"),
            exactCode("TECHNICIAN", "Kỹ thuật viên"),
            exactCode("DISPATCHER", "Điều phối viên"),
            exactCode("SETTLED", "Đã đối soát"),
            exactCode("ISSUED", "Đã cấp phụ tùng"),
            exactCode("CONSUME", "sử dụng phụ tùng"),
            exactCode("RETURN", "hoàn trả"),
            exactCode("REQUEST", "yêu cầu phụ tùng"),
            exactCode("ISSUE", "cấp phụ tùng"),
            exactCode("USED", "đã sử dụng"),
            exactCode("WALK_IN", "Khách đến trực tiếp"),
            exactCode("INTERNAL", "Nội bộ"),
            exactCode("WEBSITE", "Trang web"),
            exactCode("PHONE", "Điện thoại"),
            exactCode("EMAIL", "Email"),
            exactCode("OWNER", "Chủ sở hữu"),
            exactCode("CLOSED", "Đã đóng"),
            exactCode("OPEN", "Đang mở")
    );

    private static final List<Replacement> PHRASE_REPLACEMENTS = List.of(
            phrase("Customer Service", "Chăm sóc khách hàng"),
            phrase("Warehouse Staff", "Nhân viên kho"),
            phrase("User Management", "quản lý người dùng"),
            phrase("Customer/Asset", "khách hàng và thiết bị"),
            phrase("operational cancellation", "hủy công việc theo quy trình"),
            phrase("knowledge base", "nội dung hướng dẫn"),
            phrase("Work Order", "phiếu công việc"),
            phrase("Service Request", "yêu cầu dịch vụ"),
            phrase("payment settlement", "đối soát thanh toán"),
            phrase("customer acceptance", "khách xác nhận"),
            phrase("field progress", "tiến độ hiện trường"),
            phrase("field work", "công việc hiện trường"),
            phrase("audit trail", "nhật ký thay đổi"),
            phrase("actual-used", "số lượng thực tế đã sử dụng"),
            phrase("actual-use", "số lượng thực tế đã sử dụng"),
            phrase("public demo", "bản dùng thử công khai"),
            phrase("notification queue", "danh sách thông báo"),
            phrase("Dashboard", "màn tổng quan"),
            phrase("notification", "thông báo"),
            phrase("workflow", "quy trình"),
            phrase("workspace", "màn hình làm việc"),
            phrase("billing", "chi phí"),
            phrase("payment", "thanh toán"),
            phrase("closure", "đóng phiếu"),
            phrase("outstanding", "số lượng còn đang giữ"),
            phrase("ledger", "lịch sử biến động"),
            phrase("actor", "người thực hiện"),
            phrase("catalog", "danh mục"),
            phrase("commit", "ghi nhận"),
            phrase("broadcast", "gửi thông báo rộng"),
            phrase("reopen", "mở lại"),
            phrase("overdue", "quá hạn"),
            phrase("low-stock", "tồn kho thấp"),
            phrase("part-request", "yêu cầu phụ tùng"),
            phrase("runtime", "dữ liệu đang vận hành"),
            phrase("policy", "quy định"),
            phrase("routine", "thông thường"),
            phrase("username", "tên đăng nhập"),
            phrase("serial", "số sê-ri"),
            phrase("CSV", "tệp dữ liệu"),
            phrase("SKU", "mã phụ tùng"),
            phrase("CRUD", "các thao tác quản lý dữ liệu"),
            phrase("API", "kết nối hệ thống"),
            phrase("CSKH", "chăm sóc khách hàng"),
            phrase("KTV", "kỹ thuật viên"),
            phrase("Owner", "Chủ sở hữu"),
            phrase("Dispatcher", "Điều phối viên"),
            phrase("Technician", "Kỹ thuật viên"),
            phrase("Warehouse", "Nhân viên kho"),
            phrase("Audit", "Nhật ký hệ thống")
    );

    private AiUserFacingLanguage() {
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String result = value;
        for (Replacement replacement : CODE_REPLACEMENTS) {
            result = replacement.apply(result);
        }
        for (Replacement replacement : PHRASE_REPLACEMENTS) {
            result = replacement.apply(result);
        }
        return result;
    }

    static List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(AiUserFacingLanguage::sanitize).toList();
    }

    private static Replacement exactCode(String value, String replacement) {
        return new Replacement(boundaryPattern(value, 0), replacement);
    }

    private static Replacement phrase(String value, String replacement) {
        return new Replacement(boundaryPattern(value, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), replacement);
    }

    private static Pattern boundaryPattern(String value, int flags) {
        return Pattern.compile(
                "(?<![\\p{L}\\p{N}_-])" + Pattern.quote(value) + "(?![\\p{L}\\p{N}_-])",
                flags
        );
    }

    private record Replacement(Pattern pattern, String replacement) {
        String apply(String source) {
            return pattern.matcher(source).replaceAll(replacement);
        }
    }
}
