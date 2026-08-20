package com.serviceops.identity.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.security.DemoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoAccountProtectionPolicyTest {

    private static final String[] REQUIRED_DEMO_USERNAMES = {
            "owner",
            "dispatcher",
            "customer-service",
            "technician",
            "technician-2",
            "warehouse"
    };

    @Test
    void protectsRequiredDemoAccountsWhenDemoModeIsEnabled() {
        DemoAccountProtectionPolicy policy =
                new DemoAccountProtectionPolicy(new DemoProperties(true, "Demo@2026"));

        for (String username : REQUIRED_DEMO_USERNAMES) {
            UserAccount user = user(username);

            assertThatThrownBy(() -> policy.guardMutation(user))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getCode()).isEqualTo("DEMO_ACCOUNT_PROTECTED");
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    });
        }
    }

    @Test
    void allowsNormalCrudForUsersCreatedDuringDemo() {
        DemoAccountProtectionPolicy policy =
                new DemoAccountProtectionPolicy(new DemoProperties(true, "Demo@2026"));

        assertThatCode(() -> policy.guardMutation(user("recruiter-test")))
                .doesNotThrowAnyException();
    }

    @Test
    void protectionIsDisabledOutsideDemoMode() {
        DemoAccountProtectionPolicy policy =
                new DemoAccountProtectionPolicy(new DemoProperties(false, "123456"));

        for (String username : REQUIRED_DEMO_USERNAMES) {
            assertThatCode(() -> policy.guardMutation(user(username)))
                    .doesNotThrowAnyException();
        }
    }

    private static UserAccount user(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        return user;
    }
}