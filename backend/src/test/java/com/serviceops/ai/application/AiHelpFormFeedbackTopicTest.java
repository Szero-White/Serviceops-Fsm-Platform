package com.serviceops.ai.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiHelpFormFeedbackTopicTest {
    @Test
    void invalidSubmitQuestionExplainsRequiredFieldFeedback() {
        var context = new AiHelpKnowledgeBase.UserGuideContext("TECHNICIAN", "Kỹ thuật viên", "/work-orders");

        var decision = AiHelpKnowledgeBase.scopeDecision(
                "Tôi bấm Hoàn thành mà không được, nút không chạy thì kiểm tra gì?",
                context
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.topic().name()).isEqualTo("Phản hồi biểu mẫu");
        assertThat(decision.topic().answer()).contains("trường lỗi");
        assertThat(decision.topic().answer()).contains("không gửi request");
    }
}
