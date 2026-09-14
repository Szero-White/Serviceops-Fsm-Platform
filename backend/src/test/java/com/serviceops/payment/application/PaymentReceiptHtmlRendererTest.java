package com.serviceops.payment.application;

import com.serviceops.common.businesscode.BusinessCodeProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReceiptHtmlRendererTest {

    @Test
    void formatsReceiptTimestampsUsingConfiguredBusinessTimeZone() {
        PaymentReceiptHtmlRenderer renderer = new PaymentReceiptHtmlRenderer(
                new BusinessCodeProperties("Asia/Ho_Chi_Minh")
        );

        assertThat(renderer.formatDateTime(Instant.parse("2026-09-14T00:00:00Z")))
                .isEqualTo("14/09/2026 07:00");
    }

    @Test
    void followsBusinessTimeZoneConfigurationInsteadOfServerDefault() {
        PaymentReceiptHtmlRenderer renderer = new PaymentReceiptHtmlRenderer(
                new BusinessCodeProperties("UTC")
        );

        assertThat(renderer.formatDateTime(Instant.parse("2026-09-14T00:00:00Z")))
                .isEqualTo("14/09/2026 00:00");
    }
}
