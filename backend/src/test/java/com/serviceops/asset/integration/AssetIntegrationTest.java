package com.serviceops.asset.integration;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Map;

import static com.serviceops.integration.support.IntegrationTestFixtures.asset;
import static com.serviceops.integration.support.IntegrationTestFixtures.customer;
import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static org.assertj.core.api.Assertions.assertThat;

class AssetIntegrationTest extends AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = postgresContainer();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registerDatasource(registry, POSTGRES);
    }

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Test
    void assetsShouldBeFilterableByCustomerForDependentSelectors() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer firstCustomer = customer(
                owner.getTenantId(),
                uniqueCode("FILTER-A-"),
                "Asset Filter Customer A"
        );
        Customer secondCustomer = customer(
                owner.getTenantId(),
                uniqueCode("FILTER-B-"),
                "Asset Filter Customer B"
        );
        customerRepository.saveAllAndFlush(List.of(firstCustomer, secondCustomer));

        Asset firstAsset = asset(
                owner.getTenantId(),
                firstCustomer,
                "FILTER-ASSET-A-" + shortId()
        );
        Asset secondAsset = asset(
                owner.getTenantId(),
                secondCustomer,
                "FILTER-ASSET-B-" + shortId()
        );
        assetRepository.saveAllAndFlush(List.of(firstAsset, secondAsset));

        ResponseEntity<Map<String, Object>> response = exchangeGetMap(
                "/api/v1/assets?customerId=" + firstCustomer.getId() + "&size=100",
                login("customer-service", "123456")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<Map<String, Object>> content = mapList(response.getBody(), "content");
        assertThat(content)
                .extracting(item -> item.get("id"))
                .contains(firstAsset.getId().toString())
                .doesNotContain(secondAsset.getId().toString());
    }

    @Test
    void assetCustomerCannotChangeAfterOperationalHistoryExists() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer first = customer(
                owner.getTenantId(),
                uniqueCode("AS-H-A-"),
                "Asset History Customer A"
        );
        Customer second = customer(
                owner.getTenantId(),
                uniqueCode("AS-H-B-"),
                "Asset History Customer B"
        );
        customerRepository.saveAllAndFlush(List.of(first, second));

        Asset persistedAsset = asset(
                owner.getTenantId(),
                first,
                "ASSET-HISTORY-" + shortId()
        );
        assetRepository.saveAndFlush(persistedAsset);

        String customerServiceToken = login("customer-service", "123456");
        ResponseEntity<Map<String, Object>> serviceRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", first.getId(),
                        "assetId", persistedAsset.getId(),
                        "title", "Asset ownership history test",
                        "description", "Create history before trying to move the asset",
                        "priority", "NORMAL",
                        "channel", "PHONE"
                )
        );
        assertThat(serviceRequest.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> update = putJson(
                "/api/v1/assets/" + persistedAsset.getId(),
                login("owner", "123456"),
                Map.of(
                        "customerId", second.getId(),
                        "category", persistedAsset.getCategory(),
                        "serialNumber", persistedAsset.getSerialNumber(),
                        "status", persistedAsset.getStatus().name()
                )
        );

        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(
                assetRepository.findDetailed(persistedAsset.getId(), owner.getTenantId())
                        .orElseThrow()
                        .getCustomer()
                        .getId()
        ).isEqualTo(first.getId());
    }
}
