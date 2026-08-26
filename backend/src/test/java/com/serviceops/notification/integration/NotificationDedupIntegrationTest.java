package com.serviceops.notification.integration;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.notification.application.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDedupIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private NotificationService notificationService;

    @Test
    @Transactional
    void sameEventKeyIsInsertedOnlyOncePerRecipient() {
        UserAccount dispatcher = userAccountRepository.findByUsernameIgnoreCase("dispatcher").orElseThrow();
        String eventKey = "WORK_ORDER_OVERDUE:integration:window-1";

        assertThat(notificationService.createUnique(
                dispatcher.getTenantId(),
                dispatcher,
                eventKey,
                "Phiếu đã quá lịch thực hiện",
                "Mở Lịch điều phối để xử lý"
        )).isTrue();

        assertThat(notificationService.createUnique(
                dispatcher.getTenantId(),
                dispatcher,
                eventKey,
                "Phiếu đã quá lịch thực hiện",
                "Mở Lịch điều phối để xử lý"
        )).isFalse();
    }
}
