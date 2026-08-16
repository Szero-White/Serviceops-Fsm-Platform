package com.serviceops.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoPropertiesTest {

    @Test
    void keepsLegacyLocalSeedPasswordWhenDemoProtectionIsDisabled() {
        assertThat(new DemoProperties(false, "123456").requireSeedPassword()).isEqualTo("123456");
    }

    @Test
    void acceptsExplicitStrongEnoughPublicDemoPassword() {
        assertThat(new DemoProperties(true, "Recruiter-Demo-2026!").requireSeedPassword())
                .isEqualTo("Recruiter-Demo-2026!");
    }

    @Test
    void rejectsBlankAndShortPublicDemoPasswords() {
        assertThatThrownBy(() -> new DemoProperties(true, " ").requireSeedPassword())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DemoProperties(true, "short").requireSeedPassword())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsKnownPublicDemoPlaceholderPasswords() {
        for (String password : new String[]{"123456", "CHANGE_ME_DEMO_PASSWORD", "change-this-demo-password"}) {
            assertThatThrownBy(() -> new DemoProperties(true, password).requireSeedPassword())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("placeholder");
        }
    }
}
