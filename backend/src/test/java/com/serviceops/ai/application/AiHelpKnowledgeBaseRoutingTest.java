package com.serviceops.ai.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiHelpKnowledgeBaseRoutingTest {

    @Test
    void dispatcherGenericQuestionReturnsDispatcherOnlyOverview() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Trong vai trò này tôi được làm những gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-orders");
        assertThat(decision.topic().answer())
                .contains("phân công")
                .contains("điều phối lại")
                .contains("không quản trị tài khoản")
                .contains("không thao tác kho");
    }

    @Test
    void dispatcherRedispatchQuestionMapsToScheduleAndExplainsBoundary() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Kỹ thuật viên chưa bắt đầu, tôi muốn đổi kỹ thuật viên và đổi lịch thì làm sao?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/schedule");
        assertThat(decision.topic().answer())
                .contains("Điều phối lại")
                .contains("lý do")
                .contains("đã di chuyển")
                .contains("bắt đầu xử lý")
                .doesNotContain("ON_THE_WAY", "IN_PROGRESS");
    }

    @Test
    void customerServiceGenericQuestionReflectsPaymentReceiptAndClosureOwnership() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("CUSTOMER_SERVICE", "Chăm sóc khách hàng", "/service-requests");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Trong vai trò này tôi được làm những gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("chuyển yêu cầu")
                .contains("không phân công kỹ thuật viên")
                .contains("không ghi nhận khách xác nhận tại hiện trường")
                .contains("phát hành biên nhận")
                .contains("đóng phiếu");
    }

    @Test
    void technicianInventoryManagementQuestionIsBlockedInsteadOfLeakingWarehouseActions() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("TECHNICIAN", "Kỹ thuật viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi muốn sửa ngưỡng tồn tối thiểu và kiểm kê điều chỉnh kho",
                context
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.refusalReason()).contains("ngoài phạm vi");
    }

    @Test
    void technicianPartQuestionOnlyExplainsAssignedJobUsage() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("TECHNICIAN", "Kỹ thuật viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi xem phụ tùng và ghi vật tư dùng cho công việc được giao như thế nào?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-orders");
        assertThat(decision.topic().answer())
                .contains("phiếu công việc được giao")
                .contains("không nhập kho")
                .contains("không sửa ngưỡng tồn");
    }

    @Test
    void technicianGenericQuestionReturnsAssignedWorkOverviewOnly() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("TECHNICIAN", "Kỹ thuật viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi mới làm Kỹ thuật viên, trong vai trò này tôi được làm những gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("công việc được giao")
                .contains("Lịch của tôi")
                .contains("không quản trị người dùng")
                .contains("nghiệp vụ quản trị kho");
    }

    @Test
    void warehouseGenericStartQuestionReturnsWarehouseOverviewInsteadOfDashboardDenial() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("WAREHOUSE_STAFF", "Nhân viên kho", "/part-requests");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi mới làm kho, tôi nên bắt đầu từ đâu?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/part-requests");
        assertThat(decision.topic().answer())
                .contains("Yêu cầu phụ tùng")
                .contains("không sửa số lượng kỹ thuật viên đã yêu cầu")
                .contains("không thao tác công việc hiện trường")
                .doesNotContain("Work Order");
    }

    @Test
    void roleKnowledgeBaseKeepsManagementInstructionsInsideAuthorizedRoles() {
        String ownerKnowledge = AiHelpKnowledgeBase.knowledgeBase("OWNER");
        String technicianKnowledge = AiHelpKnowledgeBase.knowledgeBase("TECHNICIAN");
        String warehouseKnowledge = AiHelpKnowledgeBase.knowledgeBase("WAREHOUSE_STAFF");

        assertThat(ownerKnowledge)
                .contains("Người dùng")
                .contains("Tất cả trạng thái, Hoạt động và Tạm ngưng")
                .contains("Điều phối và xếp lịch")
                .contains("Kiểm kê tồn kho")
                .contains("Nhật ký hệ thống");
        assertThat(technicianKnowledge)
                .contains("Phụ tùng cho công việc được giao (/work-orders)")
                .doesNotContain("Kho phụ tùng (/inventory)")
                .doesNotContain("Tạo hoặc cập nhật tài khoản")
                .doesNotContain("Dùng Sửa ngưỡng");
        assertThat(warehouseKnowledge)
                .contains("Yêu cầu phụ tùng (/part-requests)")
                .contains("Kiểm kê tồn kho")
                .contains("Lịch sử biến động kho")
                .doesNotContain("Người dùng (/users)")
                .doesNotContain("Điều phối và xếp lịch");
    }

    @Test
    void warehousePartRequestQuestionMapsToQueueAndKeepsQuantityOwnershipClear() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("WAREHOUSE_STAFF", "Nhân viên kho", "/part-requests");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Có yêu cầu phụ tùng mới thì tôi xử lý ở đâu, có được sửa số lượng kỹ thuật viên yêu cầu không?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/part-requests");
        assertThat(decision.topic().answer())
                .contains("Yêu cầu mới chỉ ghi nhận nhu cầu")
                .contains("chưa làm giảm tồn kho")
                .contains("không sửa số lượng")
                .contains("xác nhận cấp")
                .contains("Không thể cấp")
                .doesNotContain("REQUEST", "ISSUE");
    }

    @Test
    void dispatcherCustomerAssetQuestionStaysInOperationalContextInsteadOfMasterDataMenu() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi cần xem khách hàng, thiết bị và serial để điều phối kỹ thuật viên",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-orders");
        assertThat(decision.topic().answer())
                .contains("chỉ sử dụng thông tin này để sắp xếp công việc")
                .contains("phiếu công việc")
                .doesNotContain("workspace");
    }

    @Test
    void dispatcherAuditQuestionIsOutsideRoleScope() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi mở Nhật ký hệ thống audit để xem toàn bộ thay đổi ở đâu?",
                context
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.refusalReason()).contains("ngoài phạm vi");
    }

    @Test
    void ownerBankQrQuestionMapsToPaymentSettings() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("OWNER", "Chủ sở hữu", "/");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi cấu hình tài khoản ngân hàng và QR công ty nhận thanh toán ở đâu?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/payment-settings");
        assertThat(decision.topic().answer())
                .contains("Chỉ Chủ sở hữu")
                .contains("QR")
                .contains("công ty");
    }

    @Test
    void technicianTransferQuestionStaysInsideAssignedWorkOrderInsteadOfOwnerSettings() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("TECHNICIAN", "Kỹ thuật viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Khách muốn chuyển khoản thì tôi cho xem QR công ty và ghi nhận thanh toán thế nào?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-orders");
        assertThat(decision.topic().answer())
                .contains("tài khoản và mã QR của công ty")
                .contains("chỉ đọc")
                .contains("không xác minh tiền đã về công ty")
                .contains("Chăm sóc khách hàng")
                .doesNotContain("SETTLED", "CSKH", "snapshot");
    }

    @Test
    void customerServicePaymentQuestionExplainsQueueToWorkOrderReconciliationFlow() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("CUSTOMER_SERVICE", "Chăm sóc khách hàng", "/payments");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Khách báo chuyển khoản rồi thì tôi đối soát và đóng phiếu như thế nào?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/payments");
        assertThat(decision.topic().answer())
                .contains("Đối soát thanh toán")
                .contains("phiếu công việc")
                .contains("chi phí khách đã xác nhận")
                .contains("đã được đối soát")
                .contains("biên nhận")
                .contains("đóng phiếu")
                .doesNotContain("Work Order", "SETTLED");
    }

    @Test
    void closedWorkOrderHistoryQuestionMapsToHistoryRoute() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("CUSTOMER_SERVICE", "Chăm sóc khách hàng", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi xem lại lịch sử phiếu đã đóng và tiến trình thanh toán ở đâu?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-order-history");
        assertThat(decision.topic().answer())
                .contains("đã đóng")
                .contains("Tiến trình")
                .contains("thanh toán")
                .contains("phụ tùng")
                .doesNotContain("CLOSED", "RETURN");
    }

    @Test
    void customerServicePendingClosureQuestionMapsToWorkOrderHistory() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("CUSTOMER_SERVICE", "Chăm sóc khách hàng", "/payments");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Payment đã SETTLED và đối soát rồi nhưng chưa đóng phiếu thì tôi tìm ở đâu?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/work-order-history");
        assertThat(decision.topic().answer())
                .contains("Chờ hoàn tất hồ sơ")
                .contains("khách đã xác nhận")
                .contains("tiền đã đối soát")
                .doesNotContain("CUSTOMER_ACCEPTED", "SETTLED");
    }

    @Test
    void customerServiceReopenGuidanceRequiresBusinessReason() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("CUSTOMER_SERVICE", "Chăm sóc khách hàng", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Khách báo lỗi vẫn còn, tôi mở lại phiếu như thế nào?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("đã hoàn thành")
                .contains("Mở lại")
                .contains("bắt buộc nhập lý do")
                .contains("khách đã xác nhận")
                .doesNotContain("COMPLETED", "REOPENED", "CUSTOMER_ACCEPTED");
    }

}
