package com.serviceops.workorder.integration;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.workorder.domain.WorkOrderRepository;
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
import java.util.UUID;

import static com.serviceops.integration.support.IntegrationTestFixtures.asset;
import static com.serviceops.integration.support.IntegrationTestFixtures.customer;
import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceRequestConversionIntegrationTest extends AbstractPostgresIntegrationTest {

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

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Test
    void serviceRequestConversionMustPreserveSourceDataAndAllowOnlyOneConcurrentConversion() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer first = customer(
                owner.getTenantId(),
                uniqueCode("SRWO-A-"),
                "Service Request Customer A"
        );
        Customer second = customer(
                owner.getTenantId(),
                uniqueCode("SRWO-B-"),
                "Service Request Customer B"
        );
        customerRepository.saveAllAndFlush(List.of(first, second));

        Asset firstAsset = asset(
                owner.getTenantId(),
                first,
                "SR-WO-ASSET-A-" + shortId()
        );
        Asset secondAsset = asset(
                owner.getTenantId(),
                second,
                "SR-WO-ASSET-B-" + shortId()
        );
        assetRepository.saveAllAndFlush(List.of(firstAsset, secondAsset));

        String customerServiceToken = login("customer-service", "123456");

        ResponseEntity<Map<String, Object>> firstRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", first.getId(),
                        "assetId", firstAsset.getId(),
                        "title", "Source consistency test",
                        "description", "The work order must inherit customer and asset from its source request",
                        "priority", "HIGH",
                        "channel", "PHONE"
                )
        );
        assertThat(firstRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstRequest.getBody()).isNotNull();

        UUID firstRequestId = UUID.fromString(String.valueOf(firstRequest.getBody().get("id")));
        ResponseEntity<Map<String, Object>> converted = postJsonMap(
                "/api/v1/work-orders/from-service-request/" + firstRequestId,
                customerServiceToken,
                Map.of()
        );
        assertThat(converted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(converted.getBody()).isNotNull();
        assertThat(String.valueOf(converted.getBody().get("customerId"))).isEqualTo(first.getId().toString());
        assertThat(String.valueOf(converted.getBody().get("assetId"))).isEqualTo(firstAsset.getId().toString());
        assertThat(serviceRequestRepository.findDetailed(firstRequestId, owner.getTenantId()).orElseThrow().getStatus())
                .isEqualTo(ServiceRequestStatus.CONVERTED);
        assertThat(workOrderRepository.countByTenantIdAndServiceRequestId(owner.getTenantId(), firstRequestId))
                .isEqualTo(1);

        ResponseEntity<Map<String, Object>> secondRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", second.getId(),
                        "assetId", secondAsset.getId(),
                        "title", "Concurrent conversion must be single-winner",
                        "description", "Only one work order may be created from the same service request",
                        "priority", "HIGH",
                        "channel", "PHONE"
                )
        );
        assertThat(secondRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondRequest.getBody()).isNotNull();

        UUID secondRequestId = UUID.fromString(String.valueOf(secondRequest.getBody().get("id")));
        List<Integer> statuses = runTwoConcurrentPosts(
                "/api/v1/work-orders/from-service-request/" + secondRequestId,
                customerServiceToken,
                Map.of()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(workOrderRepository.countByTenantIdAndServiceRequestId(owner.getTenantId(), secondRequestId))
                .isEqualTo(1);
        assertThat(serviceRequestRepository.findDetailed(secondRequestId, owner.getTenantId()).orElseThrow().getStatus())
                .isEqualTo(ServiceRequestStatus.CONVERTED);
    }
}
