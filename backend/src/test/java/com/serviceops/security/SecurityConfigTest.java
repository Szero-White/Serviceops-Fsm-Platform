package com.serviceops.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void shouldAcceptBase64SecretWithAtLeast256Bits() {
        String secret = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

        var key = config.jwtSecretKey(new JwtProperties("issuer", 30, secret));

        assertThat(key.getEncoded()).hasSize(32);
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void shouldRejectBlankJwtSecret() {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("issuer", 30, " ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void shouldRejectNonBase64JwtSecret() {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("issuer", 30, "not-base64***")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");
    }

    @Test
    void shouldRejectShortJwtSecret() {
        String secret = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("issuer", 30, secret)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
