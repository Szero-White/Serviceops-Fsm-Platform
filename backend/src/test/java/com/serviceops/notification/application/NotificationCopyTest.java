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
    void overdueCopyExplainsScheduleContextAndRoutesRecipientsToTheRightScreen() {
        var dispatcher = NotificationCopy.workOrderOverdueForDispatcher(
                WORK_ORDER,
                "Trịnh Quốc Tiến",
                Instant.parse("2026-08-26T03:11:00Z"),
                Instant.parse("2026-08-26T03:12:00Z")
        );
        var technician = NotificationCopy.workOrderOverdueForTechnician(
                WORK_ORDER,
                Instant.parse("2026-08-26T03:11:00Z"),
                Instant.parse("2026-08-26T03:12:00Z")
        );

        assertThat(dispatcher.title()).isEqualTo("Phiếu đã quá lịch thực hiện: WO-2026-001010");
        assertThat(dispatcher.message())
                .contains("Trịnh Quốc Tiến")
                .contains("26/08/2026 10:11–10:12")
                .contains("Lịch điều phối")
                .doesNotContain("ASSIGNED", "SCHEDULED");

        assertThat(technician.title()).isEqualTo("Công việc đã quá lịch: WO-2026-001010");
        assertThat(technician.message())
                .contains("Máy rửa chén không cấp nước")
                .contains("Trần Minh Anh")
                .contains("26/08/2026 10:11–10:12")
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
    void ownerClosureCopyIsATerminalSummaryRatherThanAnOperationalAlert() {
        var copy = NotificationCopy.workOrderClosedForOwner(WORK_ORDER, "Chăm sóc khách hàng Nguyễn An");

        assertThat(copy.title()).isEqualTo("Phiếu đã hoàn tất: WO-2026-001010");
        assertThat(copy.message())
                .contains("Chăm sóc khách hàng Nguyễn An")
                .contains("Máy rửa chén không cấp nước")
                .contains("Trần Minh Anh")
                .contains("thanh toán đã được đối soát")
                .contains("biên nhận đã được phát hành")
                .contains("hoàn tất toàn bộ quy trình")
                .contains("Lịch sử phiếu");
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
    void partRequestCopyRoutesWarehouseToTheRequestQueue() {
        var copy = NotificationCopy.partRequestCreated(
                "WO-2026-001010",
                "Máy rửa chén không cấp nước",
                "DW-INLET-220V",
                "Van cấp nước máy rửa chén 220V",
                new BigDecimal("2"),
                "cái",
                "Trịnh Quốc Tiến"
        );

        assertThat(copy.title()).isEqualTo("Có yêu cầu phụ tùng mới: WO-2026-001010");
        assertThat(copy.message())
                .contains("Trịnh Quốc Tiến")
                .contains("2 cái")
                .contains("DW-INLET-220V")
                .contains("Yêu cầu phụ tùng")
                .contains("xác nhận cấp")
                .doesNotContain("Mở Kho phụ tùng để kiểm tra và xác nhận cấp");
    }

    @Test
    void issueLowStockCopyKeepsSkuPartNameQuantitiesAndNextAction() {
        var copy = NotificationCopy.lowStockAfterIssue(
                "DW-INLET-220V",
                "Van cấp nước máy rửa chén 220V",
                new BigDecimal("2"),
                "cái",
                new BigDecimal("3"),
                "WO-2026-001010",
                "Đặng Nam Kho"
        );

        assertThat(copy.title()).isEqualTo("Tồn kho thấp: DW-INLET-220V");
        assertThat(copy.message())
                .contains("Đặng Nam Kho")
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
    @Test
    void customerServiceCopiesDescribeOnlyCustomerFacingFollowUpWork() {
        var overdue = NotificationCopy.workOrderOverdueForCustomerService(
                WORK_ORDER,
                "Trịnh Quốc Tiến",
                Instant.parse("2026-08-26T03:11:00Z"),
                Instant.parse("2026-08-26T03:12:00Z")
        );
        var reopened = NotificationCopy.workOrderReopenedForCustomerService(
                WORK_ORDER,
                "Chủ sở hữu Nguyễn An",
                "Khách phản ánh lỗi vẫn còn"
        );
        var cancelled = NotificationCopy.workOrderCancelledForCustomerService(
                WORK_ORDER,
                "Điều phối viên Lê Thu",
                "Khách yêu cầu hủy lịch"
        );

        assertThat(overdue.title()).isEqualTo("Khách hàng có thể cần được liên hệ: WO-2026-001010");
        assertThat(overdue.message())
                .contains("Trịnh Quốc Tiến")
                .contains("26/08/2026 10:11–10:12")
                .contains("chủ động liên hệ khách hàng");
        assertThat(reopened.title()).isEqualTo("Phiếu cần theo dõi lại: WO-2026-001010");
        assertThat(reopened.message())
                .contains("Chủ sở hữu Nguyễn An")
                .contains("Khách phản ánh lỗi vẫn còn")
                .contains("theo dõi khách hàng");
        assertThat(cancelled.title()).isEqualTo("Phiếu đã hủy, cần cập nhật khách hàng: WO-2026-001010");
        assertThat(cancelled.message())
                .contains("Điều phối viên Lê Thu")
                .contains("Khách yêu cầu hủy lịch")
                .contains("liên hệ khách hàng");
    }

}
