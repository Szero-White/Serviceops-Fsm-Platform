package com.serviceops.security;

import com.serviceops.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static UUID tenantId() {
        Jwt jwt = jwt();
        String tenantId = jwt.getClaimAsString("tenantId");
        if (tenantId == null) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Không xác định được doanh nghiệp hiện tại", HttpStatus.UNAUTHORIZED);
        }
        return UUID.fromString(tenantId);
    }

    public static UUID userId() {
        Jwt jwt = jwt();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    public static String username() {
        return jwt().getSubject();
    }

    public static boolean hasRole(String role) {
        List<String> roles = jwt().getClaimAsStringList("roles");
        return roles != null && roles.contains(role);
    }

    private static Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("UNAUTHENTICATED", "Phiên đăng nhập không hợp lệ", HttpStatus.UNAUTHORIZED);
        }
        return jwt;
    }
}
