package com.serviceops.servicerequest.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.DemoProperties;
import com.serviceops.servicerequest.domain.ServiceChannel;
import com.serviceops.servicerequest.domain.ServiceChannelRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelRequest;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceChannelDemoProtectionTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksSystemDefinedChannelMutationInDemoMode() {
        Fixture fixture = fixture(true, true);

        assertThatThrownBy(() -> fixture.service().update(
                fixture.channel().getId(),
                new ServiceChannelUpdateRequest("Đổi tên", "Mô tả", "blue", 10, true)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("DEMO_SEED_PROTECTED");

        assertThatThrownBy(() -> fixture.service().delete(fixture.channel().getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("DEMO_SEED_PROTECTED");

        verify(fixture.repository(), never()).delete(any());
    }

    @Test
    void allowsCustomChannelCreateInDemoMode() {
        Fixture fixture = fixture(true, false);
        when(fixture.repository().existsByTenantIdAndCodeIgnoreCase(any(), any())).thenReturn(false);

        assertThatCode(() -> fixture.service().create(
                new ServiceChannelRequest("TIKTOK", "TikTok", "Lead mới", "red", 70, true)
        )).doesNotThrowAnyException();

        verify(fixture.repository()).save(any(ServiceChannel.class));
    }

    @Test
    void allowsCustomChannelMutationInDemoMode() {
        Fixture fixture = fixture(true, false);
        when(fixture.serviceRequestRepository().countByTenantIdAndChannel(any(), any())).thenReturn(0L);

        assertThatCode(() -> fixture.service().update(
                fixture.channel().getId(),
                new ServiceChannelUpdateRequest("TikTok", "Lead mới", "red", 70, true)
        )).doesNotThrowAnyException();

        assertThatCode(() -> fixture.service().delete(fixture.channel().getId()))
                .doesNotThrowAnyException();

        verify(fixture.repository()).delete(fixture.channel());
    }

    @Test
    void allowsSystemDefinedChannelMutationOutsideDemoMode() {
        Fixture fixture = fixture(false, true);
        when(fixture.serviceRequestRepository().countByTenantIdAndChannel(any(), any())).thenReturn(0L);

        assertThatCode(() -> fixture.service().update(
                fixture.channel().getId(),
                new ServiceChannelUpdateRequest("Điện thoại", "Cập nhật", "green", 10, true)
        )).doesNotThrowAnyException();

        assertThatCode(() -> fixture.service().delete(fixture.channel().getId()))
                .doesNotThrowAnyException();
    }

    private static Fixture fixture(boolean demoEnabled, boolean systemDefined) {
        UUID tenantId = UUID.randomUUID();
        authenticate(tenantId);

        ServiceChannel channel = new ServiceChannel();
        channel.setId(UUID.randomUUID());
        channel.setTenantId(tenantId);
        channel.setCode(systemDefined ? "PHONE" : "TIKTOK");
        channel.setName(systemDefined ? "Điện thoại" : "TikTok");
        channel.setDescription("Demo");
        channel.setColor("blue");
        channel.setSortOrder(10);
        channel.setActive(true);
        channel.setSystemDefined(systemDefined);

        ServiceChannelRepository repository = mock(ServiceChannelRepository.class);
        ServiceRequestRepository serviceRequestRepository = mock(ServiceRequestRepository.class);
        AuditService auditService = mock(AuditService.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(repository.findByIdAndTenantId(channel.getId(), tenantId)).thenReturn(Optional.of(channel));

        ServiceChannelService service = new ServiceChannelService(
                repository,
                serviceRequestRepository,
                auditService,
                notificationService,
                new DemoProperties(demoEnabled, "Demo@2026")
        );

        return new Fixture(service, channel, repository, serviceRequestRepository);
    }

    private static void authenticate(UUID tenantId) {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
                "test-token",
                now,
                now.plusSeconds(300),
                java.util.Map.of("alg", "none"),
                java.util.Map.of(
                        "sub", "owner",
                        "tenantId", tenantId.toString(),
                        "userId", UUID.randomUUID().toString(),
                        "roles", List.of("OWNER")
                )
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private record Fixture(
            ServiceChannelService service,
            ServiceChannel channel,
            ServiceChannelRepository repository,
            ServiceRequestRepository serviceRequestRepository
    ) {
    }
}
