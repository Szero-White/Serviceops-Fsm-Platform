package com.serviceops.common.businesscode;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCodeGeneratorTest {
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 13);

    @Test
    void formatsCanonicalBusinessCodesWithDailySequence() {
        assertThat(BusinessCodeGenerator.format(BusinessCodeType.CUSTOMER, BUSINESS_DATE, 1))
                .isEqualTo("KH-20260913-001");
        assertThat(BusinessCodeGenerator.format(BusinessCodeType.WORK_ORDER, BUSINESS_DATE, 12))
                .isEqualTo("WO-20260913-012");
        assertThat(BusinessCodeGenerator.format(BusinessCodeType.PAYMENT_RECEIPT, BUSINESS_DATE, 123))
                .isEqualTo("BN-20260913-123");
        assertThat(BusinessCodeGenerator.format(BusinessCodeType.SPARE_PART, BUSINESS_DATE, 1000))
                .isEqualTo("PT-20260913-1000");
    }
}
