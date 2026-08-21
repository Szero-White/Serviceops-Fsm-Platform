package com.serviceops.integration.smoke;

import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceOpsSmokeIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    @Test
    void shouldRunFlywaySeedDataAndLogin() {
        String token = login("owner", "123456");

        assertThat(token).isNotBlank();
    }

    @Test
    void ownerShouldReachCriticalReadOnlyModules() {
        String token = login("owner", "123456");

        for (String path : new String[]{
                "/api/v1/dashboard",
                "/api/v1/customers",
                "/api/v1/assets",
                "/api/v1/service-requests",
                "/api/v1/work-orders",
                "/api/v1/technicians",
                "/api/v1/spare-parts",
                "/api/v1/audit-logs",
                "/api/v1/notifications"
        }) {
            ResponseEntity<String> response = exchangeGet(path, token);
            assertThat(response.getStatusCode())
                    .as("GET %s", path)
                    .isEqualTo(HttpStatus.OK);
        }
    }
}
