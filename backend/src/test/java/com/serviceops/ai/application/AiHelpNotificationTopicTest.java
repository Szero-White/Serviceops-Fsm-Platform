package com.serviceops.ai.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiHelpNotificationTopicTest {
    @Test
    void markUnreadQuestionExplainsNotificationToggle() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("WAREHOUSE_STAFF", "Nhân viên kho", "/part-requests");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi lỡ đánh dấu thông báo đã đọc, làm sao đánh dấu lại chưa đọc?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().name()).isEqualTo("Thông báo");
        assertThat(decision.topic().answer()).contains("Đánh dấu chưa đọc");
    }

    @Test
    void ownerNotificationGuidanceExplainsAttentionOnlyPolicy() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("OWNER", "Chủ sở hữu", "/");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Owner sẽ nhận những thông báo nào?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("CLOSED/CANCELLED")
                .contains("chênh lệch kiểm kê")
                .contains("không nhận routine")
                .contains("Audit");
    }

    @Test
    void dispatcherNotificationGuidanceOnlyDescribesDispatcherAttentionQueue() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Thông báo của tôi dùng để làm gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("cần phân công")
                .contains("chờ phụ tùng")
                .contains("quá hạn")
                .doesNotContain("Xử lý thanh toán")
                .doesNotContain("yêu cầu phụ tùng mới");
    }

    @Test
    void warehouseNotificationGuidanceDoesNotLeakOtherRoleTasks() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("WAREHOUSE_STAFF", "Nhân viên kho", "/part-requests");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi sẽ nhận thông báo gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().route()).isEqualTo("/part-requests");
        assertThat(decision.topic().answer())
                .contains("yêu cầu phụ tùng mới")
                .contains("tồn thấp")
                .contains("không nhận notification về")
                .doesNotContain("SETTLED")
                .doesNotContain("phát hành biên nhận");
    }

}
