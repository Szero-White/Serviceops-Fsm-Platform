package com.serviceops.identity.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.security.DemoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DemoAccountProtectionPolicy {

    private static final Set<String> REQUIRED_DEMO_USERNAMES = Set.of(
            "owner",
            "dispatcher",
            "customer-service",
            "technician",
            "technician-2",
            "warehouse"
    );

    private final DemoProperties demoProperties;

    public boolean isProtected(String username) {
        return demoProperties.enabled()
                && username != null
                && REQUIRED_DEMO_USERNAMES.contains(
                        username.trim().toLowerCase(Locale.ROOT)
                );
    }

    public void guardMutation(UserAccount user) {
        if (!isProtected(user.getUsername())) {
            return;
        }

        throw BusinessException.forbidden(
                "DEMO_ACCOUNT_PROTECTED",
                "Tài khoản demo cố định được bảo vệ để bảo đảm trải nghiệm public demo luôn hoạt động"
        );
    }
}