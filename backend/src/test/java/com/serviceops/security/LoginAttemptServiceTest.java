package com.serviceops.security;

import com.serviceops.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterConfiguredFailuresAndClearsAfterSuccess() {
        LoginAttemptService service = new LoginAttemptService(new AuthRateLimitProperties(3, 60));
        String ip = "127.0.0.1";
        String username = "owner";

        assertThatCode(() -> service.ensureAllowed(ip, username)).doesNotThrowAnyException();
        service.recordFailure(ip, username);
        service.recordFailure(ip, username);
        service.recordFailure(ip, username);

        assertThatThrownBy(() -> service.ensureAllowed(ip, username))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quá nhiều");

        service.recordSuccess(ip, username);
        assertThatCode(() -> service.ensureAllowed(ip, username)).doesNotThrowAnyException();
    }

    @Test
    void rateLimitIsScopedByIpAndUsername() {
        LoginAttemptService service = new LoginAttemptService(new AuthRateLimitProperties(1, 60));
        service.recordFailure("10.0.0.1", "owner");

        assertThatThrownBy(() -> service.ensureAllowed("10.0.0.1", "owner"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> service.ensureAllowed("10.0.0.2", "owner")).doesNotThrowAnyException();
        assertThatCode(() -> service.ensureAllowed("10.0.0.1", "dispatcher")).doesNotThrowAnyException();
    }


    @Test
    void blocksAccountAfterFailuresAreDistributedAcrossDifferentIps() {
        LoginAttemptService service = new LoginAttemptService(new AuthRateLimitProperties(1, 60));
        service.recordFailure("10.0.0.1", "owner");
        service.recordFailure("10.0.0.2", "owner");
        service.recordFailure("10.0.0.3", "owner");

        assertThatThrownBy(() -> service.ensureAllowed("10.0.0.4", "owner"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void blocksIpAfterFailuresAreDistributedAcrossDifferentUsernames() {
        LoginAttemptService service = new LoginAttemptService(new AuthRateLimitProperties(1, 60));
        for (int i = 0; i < 6; i++) {
            service.recordFailure("10.0.0.1", "user-" + i);
        }

        assertThatThrownBy(() -> service.ensureAllowed("10.0.0.1", "another-user"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void usernameMatchingIsCaseInsensitiveAndWhitespaceNormalized() {
        LoginAttemptService service = new LoginAttemptService(new AuthRateLimitProperties(1, 60));
        service.recordFailure("10.0.0.1", " Owner ");

        assertThatThrownBy(() -> service.ensureAllowed("10.0.0.1", "owner"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void invalidConfigurationFallsBackToSafeDefaults() {
        AuthRateLimitProperties properties = new AuthRateLimitProperties(0, 0);

        assertThatCode(() -> {
            LoginAttemptService service = new LoginAttemptService(properties);
            for (int i = 0; i < 4; i++) {
                service.recordFailure("10.0.0.1", "owner");
            }
            service.ensureAllowed("10.0.0.1", "owner");
        }).doesNotThrowAnyException();
    }
}
