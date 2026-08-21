package com.serviceops.ai.integration;

import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiHelpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    @Test
    void aiHelpShouldSupportEveryRoleThatSeesTheAssistant() {
        for (String username : List.of("technician", "warehouse")) {
            ResponseEntity<String> response = postJson(
                    "/api/v1/ai/help",
                    login(username, "123456"),
                    Map.of("question", "Tôi nên bắt đầu ở đâu?", "currentPath", "/")
            );

            assertThat(response.getStatusCode())
                    .as("AI help for %s", username)
                    .isEqualTo(HttpStatus.OK);
        }
    }
}
