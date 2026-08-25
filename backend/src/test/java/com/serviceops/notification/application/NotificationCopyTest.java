package com.serviceops.notification.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCopyTest {

    @Test
    void workOrderCopyUsesConsistentUserFacingTerminology() {
        assertThat(NotificationCopy.workOrderNeedsDispatch("WO-2026-001010"))
                .satisfies(copy -> {
                    assertThat(copy.title()).isEqualTo("Phiếu mới chờ điều phối: WO-2026-001010");
                    assertThat(copy.message())
                            .contains("Phiếu công việc mới đã sẵn sàng")
                            .contains("Lịch điều phối")
                            .doesNotContain("OPEN", "ASSIGNED", "SCHEDULED");
                });

        assertThat(NotificationCopy.technicianScheduleChanged("WO-2026-001010", "Điều phối viên Lê Thu"))
                .satisfies(copy -> {
                    assertThat(copy.title()).isEqualTo("Lịch làm việc đã thay đổi: WO-2026-001010");
                    assertThat(copy.message()).contains("Lịch của tôi");
                });
    }

    @Test
    void inventoryCopyKeepsSkuButExplainsTheAlertInPlainLanguage() {
        var copy = NotificationCopy.lowStock(
                "SENSOR-TEMP-10K",
                "Cảm biến nhiệt độ 10K",
                new BigDecimal("2"),
                "cái",
                new BigDecimal("3")
        );

        assertThat(copy.title()).isEqualTo("Tồn kho thấp: SENSOR-TEMP-10K");
        assertThat(copy.message())
                .isEqualTo("Cảm biến nhiệt độ 10K còn 2 cái; ngưỡng cảnh báo là 3 cái. Kiểm tra và bổ sung tồn kho.");
    }

    @Test
    void customerServiceCompletionCopyExplainsTheNextActionWithoutClaimingClosureOwnership() {
        var copy = NotificationCopy.workOrderCompletedForCustomerService("WO-2026-001010");

        assertThat(copy.title()).isEqualTo("Phiếu đã hoàn thành: WO-2026-001010");
        assertThat(copy.message())
                .contains("Theo dõi phản hồi khách hàng")
                .contains("mở lại phiếu")
                .doesNotContain("Đóng phiếu", "Khách xác nhận");
    }
}
