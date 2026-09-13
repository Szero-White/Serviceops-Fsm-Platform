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
                .contains("ON_THE_WAY")
                .contains("IN_PROGRESS");
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
                .contains("Work Order được giao")
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
                .contains("không thao tác Work Order hiện trường");
    }

    @Test
    void roleKnowledgeBaseKeepsManagementInstructionsInsideAuthorizedRoles() {
        String ownerKnowledge = AiHelpKnowledgeBase.knowledgeBase("OWNER");
        String technicianKnowledge = AiHelpKnowledgeBase.knowledgeBase("TECHNICIAN");
        String warehouseKnowledge = AiHelpKnowledgeBase.knowledgeBase("WAREHOUSE_STAFF");

        assertThat(ownerKnowledge)
                .contains("Người dùng")
                .contains("Tất cả trạng thái / Hoạt động / Tạm ngưng")
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
                .contains("REQUEST")
                .contains("không làm giảm tồn kho")
                .contains("không sửa số lượng")
                .contains("ISSUE")
                .contains("Không thể cấp");
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
                .contains("không phải workspace quản lý dữ liệu chính")
                .contains("Phiếu công việc");
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
                .contains("tài khoản/QR công ty")
                .contains("chỉ đọc")
                .contains("không SETTLED")
                .contains("CSKH")
                .doesNotContain("snapshot");
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
                .contains("Work Order")
                .contains("chi phí khách đã xác nhận")
                .contains("SETTLED")
                .contains("biên nhận")
                .contains("đóng Work Order");
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
                .contains("CLOSED")
                .contains("Tiến trình")
                .contains("thanh toán")
                .contains("RETURN");
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
                .contains("CUSTOMER_ACCEPTED")
                .contains("SETTLED");
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
                .contains("COMPLETED")
                .contains("REOPENED")
                .contains("bắt buộc nhập lý do")
                .contains("CUSTOMER_ACCEPTED");
    }

}
