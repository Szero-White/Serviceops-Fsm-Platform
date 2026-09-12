package com.serviceops.ai.application;

import com.serviceops.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {
    @Test
    void interactiveAiUsesSeparateTimeoutBudgets() {
        AiProperties properties = new AiProperties();

        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(4));
        assertThat(properties.getSuggestionTimeout()).isEqualTo(Duration.ofSeconds(12));
        assertThat(properties.getHelpTimeout()).isEqualTo(Duration.ofSeconds(18));
    }
}
