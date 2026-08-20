package com.serviceops.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceops.common.web.RequestCorrelationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DemoProtectionFilter extends OncePerRequestFilter {
    private final DemoProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled() || !isBlockedMutation(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://serviceops.local/problems/demo_write_protected");
        body.put("title", "DEMO_WRITE_PROTECTED");
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put(
                "detail",
                "Public demo đang bảo vệ thao tác quản trị hoặc xóa dữ liệu. Chức năng vẫn khả dụng khi DEMO_MODE=false."
        );
        body.put("code", "DEMO_WRITE_PROTECTED");
        body.put("timestamp", Instant.now());
        body.put("path", request.getRequestURI());

        Object requestId = request.getAttribute(RequestCorrelationFilter.MDC_KEY);
        if (requestId != null) {
            body.put("requestId", requestId);
        }

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static boolean isBlockedMutation(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // User Management remains usable in public demo.
        // DemoAccountProtectionPolicy protects only the required seeded identities.
        if (matchesPathOrChild(path, "/api/v1/users")) {
            return false;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            return true;
        }

        if (matchesPathOrChild(path, "/api/v1/service-channels")) {
            return "POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method)
                    || "DELETE".equalsIgnoreCase(method);
        }

        return false;
    }

    private static boolean matchesPathOrChild(String requestPath, String protectedPath) {
        return requestPath.equals(protectedPath)
                || requestPath.startsWith(protectedPath + "/");
    }
}
