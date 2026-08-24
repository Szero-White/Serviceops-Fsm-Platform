package com.serviceops.ai.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiHelpNotificationTopicTest {
    @Test
    void markUnreadQuestionExplainsNotificationToggle() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("WAREHOUSE_STAFF", "Nhân viên kho", "/inventory");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi lỡ đánh dấu thông báo đã đọc, làm sao đánh dấu lại chưa đọc?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().name()).isEqualTo("Thông báo");
        assertThat(decision.topic().answer()).contains("đánh dấu lại chưa đọc");
    }
}
