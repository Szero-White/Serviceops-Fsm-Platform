package com.serviceops.identity.integration;

import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Map;

import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static org.assertj.core.api.Assertions.assertThat;

class OwnerInvariantIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    private static final String OWNER_PASSWORD = "Owner-Race-Password1!";

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void concurrentOwnersCannotDisableEachOtherAndLeaveTenantWithoutActiveOwner() throws Exception {
        String suffix = shortId();

        Tenant tenant = new Tenant();
        tenant.setCode("OWNER-RACE-" + suffix);
        tenant.setName("Owner Invariant Concurrency Test");
        tenant.setActive(true);
        tenantRepository.save(tenant);

        UserAccount ownerA = testOwner(
                tenant,
                "owner-race-a-" + suffix,
                "Owner Race A"
        );
        UserAccount ownerB = testOwner(
                tenant,
                "owner-race-b-" + suffix,
                "Owner Race B"
        );
        userAccountRepository.saveAndFlush(ownerA);
        userAccountRepository.saveAndFlush(ownerB);

        String tokenA = login(ownerA.getUsername(), OWNER_PASSWORD);
        String tokenB = login(ownerB.getUsername(), OWNER_PASSWORD);

        Map<String, Object> disableA = Map.of(
                "username", ownerA.getUsername(),
                "displayName", ownerA.getDisplayName(),
                "role", "OWNER",
                "active", false
        );
        Map<String, Object> disableB = Map.of(
                "username", ownerB.getUsername(),
                "displayName", ownerB.getDisplayName(),
                "role", "OWNER",
                "active", false
        );

        List<Integer> statuses = runConcurrentRequests(
                () -> putJson(
                        "/api/v1/users/" + ownerB.getId(),
                        tokenA,
                        disableB
                ).getStatusCode().value(),
                () -> putJson(
                        "/api/v1/users/" + ownerA.getId(),
                        tokenB,
                        disableA
                ).getStatusCode().value()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(
                userAccountRepository.countByTenantIdAndRoleAndActiveTrue(
                        tenant.getId(),
                        UserRole.OWNER
                )
        ).isEqualTo(1);
    }

    private UserAccount testOwner(Tenant tenant, String username, String displayName) {
        UserAccount owner = new UserAccount();
        owner.setTenantId(tenant.getId());
        owner.setUsername(username);
        owner.setDisplayName(displayName);
        owner.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        return owner;
    }
}
