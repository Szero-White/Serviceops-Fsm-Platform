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
                .contains("kết quả cuối của Work Order")
                .contains("đóng/hủy")
                .contains("kiểm kê có chênh lệch")
                .contains("tránh spam")
                .contains("Timeline/Audit");
    }

    @Test
    void notificationGuidanceExplainsRoleRelevantQueuesInsteadOfRoutineCrudSpam() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("DISPATCHER", "Điều phối viên", "/");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Thông báo của từng vai trò dùng để làm gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().answer())
                .contains("Cần phân công kỹ thuật viên")
                .contains("Cần theo dõi khách sau sửa chữa")
                .contains("Bạn có công việc mới")
                .contains("Có yêu cầu phụ tùng mới")
                .contains("Yêu cầu phụ tùng")
                .contains("cảnh báo tồn kho")
                .contains("CRUD thường ngày")
                .contains("khách hàng")
                .contains("người thực hiện")
                .contains("tránh spam");
    }

}
