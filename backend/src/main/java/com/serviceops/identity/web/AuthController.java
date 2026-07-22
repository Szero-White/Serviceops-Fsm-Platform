package com.serviceops.identity.web;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.security.JwtProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BusinessException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
        }

        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.accessTokenMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("tenantId", user.getTenantId().toString())
                .claim("displayName", user.getDisplayName())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresAt, new CurrentUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole().name(), user.getTenantId()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record CurrentUserResponse(UUID id, String username, String displayName, String role, UUID tenantId) {
    }

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, CurrentUserResponse user) {
    }
}
