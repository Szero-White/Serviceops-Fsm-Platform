package com.serviceops.audit.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDetailTextTest {
    @Test
    void namedCodeKeepsBusinessNameReadableAndCodeTraceable() {
        assertThat(AuditDetailText.namedCode("Van cấp nước máy rửa chén 220V", "PT-20260914-003"))
                .isEqualTo("Van cấp nước máy rửa chén 220V (PT-20260914-003)");
    }

    @Test
    void accountPrefersDisplayNameAndKeepsUsernameForTraceability() {
        assertThat(AuditDetailText.account("Trần Quốc Bảo", "ktv-bao"))
                .isEqualTo("Trần Quốc Bảo (@ktv-bao)");
    }

    @Test
    void helpersRemainSafeWhenOptionalDisplayValuesAreMissing() {
        assertThat(AuditDetailText.namedCode(null, "PT-20260914-003")).isEqualTo("PT-20260914-003");
        assertThat(AuditDetailText.account(null, "owner")).isEqualTo("@owner");
    }
}
