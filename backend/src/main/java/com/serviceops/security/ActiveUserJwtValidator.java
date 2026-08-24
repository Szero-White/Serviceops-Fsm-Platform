package com.serviceops.security;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
            "invalid_token",
            "User account is no longer active or the token identity is stale",
            null
    );

    private final UserAccountRepository userAccountRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String userIdClaim = token.getClaimAsString("userId");
        String tenantIdClaim = token.getClaimAsString("tenantId");
        if (userIdClaim == null || tenantIdClaim == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        final UUID userId;
        final UUID tenantId;
        try {
            userId = UUID.fromString(userIdClaim);
            tenantId = UUID.fromString(tenantIdClaim);
        } catch (IllegalArgumentException ex) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        Optional<UserAccount> accountResult = userAccountRepository.findByIdAndTenantId(userId, tenantId);
        if (accountResult.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        UserAccount account = accountResult.get();
        List<String> tokenRoles = token.getClaimAsStringList("roles");
        boolean identityMatches = account.isActive()
                && account.getUsername().equalsIgnoreCase(token.getSubject())
                && tokenRoles != null
                && tokenRoles.size() == 1
                && tokenRoles.contains(account.getRole().name());

        return identityMatches
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
    }
}
