package com.serviceops.security;

import com.serviceops.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int ACCOUNT_LIMIT_MULTIPLIER = 3;
    private static final int IP_LIMIT_MULTIPLIER = 6;

    private final AuthRateLimitProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, FailureWindow> failures = new ConcurrentHashMap<>();

    public void ensureAllowed(String remoteAddress, String username) {
        Instant now = clock.instant();
        String ip = normalizeIp(remoteAddress);
        String user = normalizeUser(username);

        ensureKeyAllowed(pairKey(ip, user), properties.loginMaxFailures(), now);
        ensureKeyAllowed(accountKey(user), scaledLimit(properties.loginMaxFailures(), ACCOUNT_LIMIT_MULTIPLIER), now);
        ensureKeyAllowed(ipKey(ip), scaledLimit(properties.loginMaxFailures(), IP_LIMIT_MULTIPLIER), now);
    }

    public void recordFailure(String remoteAddress, String username) {
        Instant now = clock.instant();
        String ip = normalizeIp(remoteAddress);
        String user = normalizeUser(username);

        increment(pairKey(ip, user), now);
        increment(accountKey(user), now);
        increment(ipKey(ip), now);
    }

    public void recordSuccess(String remoteAddress, String username) {
        String ip = normalizeIp(remoteAddress);
        String user = normalizeUser(username);
        failures.remove(pairKey(ip, user));
        failures.remove(accountKey(user));
        // Keep the IP-wide failure history: a successful login for one account must not erase
        // suspicious failures against other usernames from the same source address.
    }

    private void ensureKeyAllowed(String key, int limit, Instant now) {
        FailureWindow window = failures.get(key);
        if (window == null) {
            ensureCapacity(now);
            return;
        }
        if (isExpired(window, now)) {
            failures.remove(key, window);
            return;
        }
        if (window.count() >= limit) {
            throw rateLimited();
        }
    }

    private void increment(String key, Instant now) {
        if (!failures.containsKey(key)) {
            ensureCapacity(now);
        }
        failures.compute(key, (ignored, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                return new FailureWindow(1, now);
            }
            return new FailureWindow(existing.count() + 1, existing.windowStartedAt());
        });
    }

    private void ensureCapacity(Instant now) {
        if (failures.size() < MAX_TRACKED_KEYS) {
            return;
        }
        removeExpired(now);
        if (failures.size() >= MAX_TRACKED_KEYS) {
            throw rateLimited();
        }
    }

    private boolean isExpired(FailureWindow window, Instant now) {
        return now.isAfter(window.windowStartedAt().plusSeconds(properties.loginWindowSeconds()));
    }

    private void removeExpired(Instant now) {
        failures.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private static int scaledLimit(int base, int multiplier) {
        return base > Integer.MAX_VALUE / multiplier ? Integer.MAX_VALUE : base * multiplier;
    }

    private static BusinessException rateLimited() {
        return new BusinessException(
                "LOGIN_RATE_LIMITED",
                "Đăng nhập thất bại quá nhiều lần. Vui lòng thử lại sau.",
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    private static String normalizeIp(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
    }

    private static String normalizeUser(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String pairKey(String ip, String user) {
        return "pair|" + ip + '|' + user;
    }

    private static String accountKey(String user) {
        return "account|" + user;
    }

    private static String ipKey(String ip) {
        return "ip|" + ip;
    }

    private record FailureWindow(int count, Instant windowStartedAt) {
    }
}
