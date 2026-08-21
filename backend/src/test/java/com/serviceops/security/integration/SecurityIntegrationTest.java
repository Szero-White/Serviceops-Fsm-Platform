package com.serviceops.security.integration;

import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.UUID;

import static com.serviceops.integration.support.IntegrationTestFixtures.customer;
import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void roleBoundariesShouldProtectRoleSpecificModules() {
        String technicianToken = login("technician", "123456");
        String customerServiceToken = login("customer-service", "123456");
        String warehouseToken = login("warehouse", "123456");

        assertThat(exchangeGet("/api/v1/customers", technicianToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchangeGet("/api/v1/spare-parts", customerServiceToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchangeGet("/api/v1/spare-parts", technicianToken).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(exchangeGet("/api/v1/work-orders/history", warehouseToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchangeGet(
                "/api/v1/attachments?referenceType=ASSET&referenceId=" + UUID.randomUUID(),
                warehouseToken
        ).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tenantACannotReadTenantBCustomerButTenantBCan() {
        String suffix = shortId();

        Tenant tenantB = new Tenant();
        tenantB.setCode("TENANT-B-" + suffix);
        tenantB.setName("Tenant B Isolation Test");
        tenantB.setActive(true);
        tenantRepository.save(tenantB);

        String tenantBUsername = "tenant-b-owner-" + suffix;
        String tenantBPassword = "Tenant-B-Password1!";

        UserAccount tenantBOwner = new UserAccount();
        tenantBOwner.setTenantId(tenantB.getId());
        tenantBOwner.setUsername(tenantBUsername);
        tenantBOwner.setDisplayName("Tenant B Owner");
        tenantBOwner.setPasswordHash(passwordEncoder.encode(tenantBPassword));
        tenantBOwner.setRole(UserRole.OWNER);
        tenantBOwner.setActive(true);
        userAccountRepository.saveAndFlush(tenantBOwner);

        Customer tenantBCustomer = customer(
                tenantB.getId(),
                uniqueCode("B-CUST-"),
                "Tenant B Customer"
        );
        customerRepository.saveAndFlush(tenantBCustomer);

        String tenantAToken = login("owner", "123456");
        String tenantBToken = login(tenantBUsername, tenantBPassword);

        ResponseEntity<String> crossTenant = exchangeGet(
                "/api/v1/customers/" + tenantBCustomer.getId(),
                tenantAToken
        );
        ResponseEntity<String> ownTenant = exchangeGet(
                "/api/v1/customers/" + tenantBCustomer.getId(),
                tenantBToken
        );

        assertThat(crossTenant.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownTenant.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
