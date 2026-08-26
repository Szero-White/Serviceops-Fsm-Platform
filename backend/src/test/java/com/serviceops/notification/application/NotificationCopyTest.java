package com.serviceops.notification.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCopyTest {

    private static final NotificationCopy.WorkOrderContext WORK_ORDER =
            new NotificationCopy.WorkOrderContext(
                    "WO-2026-001010",
                    "Máy rửa chén không cấp nước",
                    "Trần Minh Anh"
            );

    @Test
    void dispatcherCopySaysWhichCustomerWhichJobAndWhatToDoNext() {
        var copy = NotificationCopy.workOrderNeedsDispatch(WORK_ORDER, "Chăm sóc khách hàng Nguyễn An");

        assertThat(copy.title()).isEqualTo("Cần phân công kỹ thuật viên: WO-2026-001010");
        assertThat(copy.message())
                .contains("Chăm sóc khách hàng Nguyễn An")
                .contains("Máy rửa chén không cấp nước")
                .contains("Trần Minh Anh")
                .contains("Lịch điều phối")
                .doesNotContain("OPEN", "ASSIGNED", "SCHEDULED");
    }

    @Test
    void technicianAssignmentAndScheduleCopyIdentifyActorAndBusinessContext() {
        var assigned = NotificationCopy.technicianAssigned(WORK_ORDER, "Điều phối viên Lê Thu");
        var rescheduled = NotificationCopy.technicianScheduleChanged(
                WORK_ORDER,
                "Điều phối viên Lê Thu",
                Instant.parse("2026-08-25T02:00:00Z"),
                Instant.parse("2026-08-25T04:00:00Z"),
                Instant.parse("2026-08-28T05:07:00Z"),
                Instant.parse("2026-08-28T06:39:00Z"),
                "Khách hàng yêu cầu dời lịch"
        );

        assertThat(assigned.title()).isEqualTo("Bạn có công việc mới: WO-2026-001010");
        assertThat(assigned.message())
                .contains("Điều phối viên Lê Thu")
                .contains("Máy rửa chén không cấp nước")
                .contains("Trần Minh Anh")
                .contains("Lịch của tôi");

        assertThat(rescheduled.title()).isEqualTo("Lịch của bạn đã thay đổi: WO-2026-001010");
        assertThat(rescheduled.message())
                .contains("Điều phối viên Lê Thu")
                .contains("Máy rửa chén không cấp nước")
                .contains("Trần Minh Anh")
                .contains("Lịch cũ: 25/08/2026 09:00–11:00")
                .contains("Lịch mới: 28/08/2026 12:07–13:39")
                .contains("Lý do: Khách hàng yêu cầu dời lịch")
                .contains("Lịch của tôi");
    }

    @Test
    void reopenAndCancellationCopyKeepsReasonWithoutBecomingAuditDump() {
        var reopen = NotificationCopy.workOrderReopenedAttention(
                WORK_ORDER,
                "Chăm sóc khách hàng Nguyễn An",
                "Khách báo máy vẫn chưa cấp nước sau khi chạy thử"
        );
        var cancelled = NotificationCopy.workOrderCancelledForTechnician(
                WORK_ORDER,
                "Chăm sóc khách hàng Nguyễn An",
                "Khách đã chuyển lịch sửa chữa sang đơn vị khác"
        );

        assertThat(reopen.message())
                .contains("Chăm sóc khách hàng Nguyễn An")
                .contains("Lý do: Khách báo máy vẫn chưa cấp nước")
                .contains("điều phối bước tiếp theo");
        assertThat(cancelled.message())
                .contains("Khách đã chuyển lịch sửa chữa sang đơn vị khác")
                .contains("dừng công việc này")
                .contains("Lịch của tôi");
    }

    @Test
    void customerServiceCompletionCopyNamesTechnicianCustomerAndNextAction() {
        var copy = NotificationCopy.workOrderCompletedForCustomerService(WORK_ORDER, "Trịnh Quốc Tiến");

        assertThat(copy.title()).isEqualTo("Cần theo dõi khách sau sửa chữa: WO-2026-001010");
        assertThat(copy.message())
                .contains("Kỹ thuật viên Trịnh Quốc Tiến")
                .contains("Trần Minh Anh")
                .contains("Theo dõi phản hồi khách hàng")
                .contains("mở lại phiếu")
                .doesNotContain("Đóng phiếu", "Khách xác nhận");
    }

    @Test
    void inventoryCopyKeepsSkuPartNameQuantitiesAndNextAction() {
        var copy = NotificationCopy.lowStock(
                "DW-INLET-220V",
                "Van cấp nước máy rửa chén 220V",
                new BigDecimal("2"),
                "cái",
                new BigDecimal("3"),
                "WO-2026-001010",
                "Trịnh Quốc Tiến"
        );

        assertThat(copy.title()).isEqualTo("Tồn kho thấp: DW-INLET-220V");
        assertThat(copy.message())
                .contains("Trịnh Quốc Tiến")
                .contains("WO-2026-001010")
                .contains("Van cấp nước máy rửa chén 220V")
                .contains("còn 2 cái")
                .contains("ngưỡng tồn tối thiểu là 3 cái")
                .contains("Kho phụ tùng");
    }

    @Test
    void copyAlwaysFitsPersistenceLimitsEvenWithLongBusinessText() {
        String longText = "Nội dung rất dài ".repeat(100);
        var copy = NotificationCopy.workOrderCancelledForOwner(
                new NotificationCopy.WorkOrderContext("WO-2026-999999", longText, longText),
                longText,
                longText
        );

        assertThat(copy.title().length()).isLessThanOrEqualTo(180);
        assertThat(copy.message().length()).isLessThanOrEqualTo(500);
    }
}
