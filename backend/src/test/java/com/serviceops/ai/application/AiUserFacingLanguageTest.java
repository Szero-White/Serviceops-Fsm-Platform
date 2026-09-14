package com.serviceops.ai.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiUserFacingLanguageTest {

    @Test
    void translatesInternalCodesAndTechnicalTermsForOperationalUsers() {
        String result = AiUserFacingLanguage.sanitize(
                "TECHNICIAN cập nhật Work Order từ IN_PROGRESS sang COMPLETED; CSKH xem audit trail và SKU."
        );

        assertThat(result)
                .contains("Kỹ thuật viên")
                .contains("phiếu công việc")
                .contains("Đang thực hiện")
                .contains("Đã hoàn thành")
                .contains("chăm sóc khách hàng")
                .contains("nhật ký thay đổi")
                .contains("mã phụ tùng")
                .doesNotContain("TECHNICIAN", "Work Order", "IN_PROGRESS", "COMPLETED", "CSKH", "audit trail", "SKU");
    }

    @Test
    void translatesSpecificCodeBeforeItsPrefix() {
        assertThat(AiUserFacingLanguage.sanitize("ISSUED rồi RETURN"))
                .isEqualTo("Đã cấp phụ tùng rồi hoàn trả");
    }

    @Test
    void translatesRoleChannelAndPresentationJargon() {
        String result = AiUserFacingLanguage.sanitize(
                "OWNER mở public demo, kiểm tra WALK_IN, username, serial và CSV."
        );

        assertThat(result)
                .contains("Chủ sở hữu")
                .contains("bản dùng thử công khai")
                .contains("Khách đến trực tiếp")
                .contains("tên đăng nhập")
                .contains("số sê-ri")
                .contains("tệp dữ liệu")
                .doesNotContain("OWNER", "public demo", "WALK_IN", "username", "serial", "CSV");
    }

    @Test
    void onlyReplacesWholeTechnicalTerms() {
        assertThat(AiUserFacingLanguage.sanitize("CAPITAL và api-client"))
                .isEqualTo("CAPITAL và api-client");
    }

    @Test
    void sanitizesStepListsWithoutChangingOrder() {
        assertThat(AiUserFacingLanguage.sanitize(List.of(
                "Mở Dashboard",
                "Kiểm tra notification",
                "Xem Work Order"
        ))).containsExactly(
                "Mở màn tổng quan",
                "Kiểm tra thông báo",
                "Xem phiếu công việc"
        );
    }
}
