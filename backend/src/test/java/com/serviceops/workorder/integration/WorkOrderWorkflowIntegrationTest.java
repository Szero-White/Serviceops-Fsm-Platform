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
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.serviceops.integration.support.IntegrationTestFixtures.asset;
import static com.serviceops.integration.support.IntegrationTestFixtures.customer;
import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderWorkflowIntegrationTest extends AbstractPostgresIntegrationTest {

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

    @Autowired
    private TechnicianRepository technicianRepository;

    @Test
    void coreServiceFlowShouldRunAcrossBusinessRoles() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        Customer customer = customerRepository.findAll().stream()
                .filter(item -> owner.getTenantId().equals(item.getTenantId()))
                .findFirst()
                .orElseThrow();
        var technician = technicianRepository
                .findByTenantIdAndUserId(owner.getTenantId(), technicianUser.getId())
                .orElseThrow();

        String customerServiceToken = login("customer-service", "123456");
        ResponseEntity<Map<String, Object>> createdRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", customer.getId(),
                        "title", "Integration flow service request",
                        "description", "End-to-end role workflow integration test",
                        "priority", "NORMAL",
                        "channel", "PHONE"
                )
        );
        assertThat(createdRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdRequest.getBody()).isNotNull();
        String serviceRequestId = String.valueOf(createdRequest.getBody().get("id"));

        ResponseEntity<Map<String, Object>> converted = postJsonMap(
                "/api/v1/work-orders/from-service-request/" + serviceRequestId,
                customerServiceToken,
                Map.of()
        );
        assertThat(converted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(converted.getBody()).isNotNull();
        assertThat(converted.getBody().get("status")).isEqualTo("OPEN");
        String workOrderId = String.valueOf(converted.getBody().get("id"));
        assertThat(serviceRequestRepository.findById(UUID.fromString(serviceRequestId)).orElseThrow().getStatus())
                .isEqualTo(ServiceRequestStatus.CONVERTED);

        Instant start = Instant.now().plus(90, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        ResponseEntity<Map<String, Object>> scheduled = postJsonMap(
                "/api/v1/work-orders/" + workOrderId + "/schedule",
                login("dispatcher", "123456"),
                Map.of(
                        "technicianId", technician.getId(),
                        "startTime", start.toString(),
                        "endTime", end.toString()
                )
        );
        assertThat(scheduled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scheduled.getBody()).isNotNull();
        assertThat(scheduled.getBody().get("status")).isEqualTo("ASSIGNED");

        String technicianToken = login("technician", "123456");
        assertTransition(workOrderId, technicianToken, "ON_THE_WAY", Map.of("note", "Technician departed"));
        assertTransition(workOrderId, technicianToken, "IN_PROGRESS", Map.of("note", "Technician started service"));
        assertForbiddenTechnicianTransition(workOrderId, technicianToken, "CANCELLED", "IN_PROGRESS");
        assertTransition(workOrderId, technicianToken, "COMPLETED", Map.of(
                "note", "Service completed",
                "diagnosis", "Integration test diagnosis",
                "resolution", "Integration test resolution"
        ));
        assertForbiddenTechnicianTransition(workOrderId, technicianToken, "CUSTOMER_ACCEPTED", "COMPLETED");
        assertForbiddenTechnicianTransition(workOrderId, technicianToken, "REOPENED", "COMPLETED");

        String ownerToken = login("owner", "123456");
        assertTransition(workOrderId, ownerToken, "CUSTOMER_ACCEPTED", Map.of("note", "Customer accepted result"));
        ResponseEntity<Map<String, Object>> closed = assertTransition(
                workOrderId,
                ownerToken,
                "CLOSED",
                Map.of("note", "Workflow closed")
        );
        assertThat(closed.getBody()).isNotNull();
        assertThat(closed.getBody().get("status")).isEqualTo("CLOSED");
    }

    @Test
    void serviceRequestConversionMustPreserveCustomerAssetAndAllowOnlyOneConcurrentConversion() throws Exception {
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
        ResponseEntity<Map<String, Object>> createdRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", first.getId(),
                        "assetId", firstAsset.getId(),
                        "title", "Source consistency test",
                        "description", "Source request must stay linked to the same customer and asset",
                        "priority", "HIGH",
                        "channel", "PHONE"
                )
        );
        assertThat(createdRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdRequest.getBody()).isNotNull();

        UUID serviceRequestId = UUID.fromString(String.valueOf(createdRequest.getBody().get("id")));
        String ownerToken = login("owner", "123456");

        Map<String, Object> mismatchBody = Map.of(
                "serviceRequestId", serviceRequestId,
                "customerId", second.getId(),
                "assetId", secondAsset.getId(),
                "summary", "Mismatched conversion must fail",
                "description", "Integration test",
                "priority", "HIGH"
        );
        assertThat(postJson("/api/v1/work-orders", ownerToken, mismatchBody).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(serviceRequestRepository.findDetailed(serviceRequestId, owner.getTenantId()).orElseThrow().getStatus())
                .isEqualTo(ServiceRequestStatus.OPEN);

        Map<String, Object> validBody = Map.of(
                "serviceRequestId", serviceRequestId,
                "customerId", first.getId(),
                "assetId", firstAsset.getId(),
                "summary", "Concurrent conversion must be single-winner",
                "description", "Integration test",
                "priority", "HIGH"
        );
        List<Integer> statuses = runTwoConcurrentPosts("/api/v1/work-orders", ownerToken, validBody);

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(workOrderRepository.countByTenantIdAndServiceRequestId(owner.getTenantId(), serviceRequestId))
                .isEqualTo(1);
    }

    private void assertForbiddenTechnicianTransition(
            String workOrderId,
            String technicianToken,
            String targetStatus,
            String expectedCurrentStatus
    ) {
        ResponseEntity<Map<String, Object>> response = postJsonMap(
                "/api/v1/work-orders/" + workOrderId + "/transition",
                technicianToken,
                Map.of("targetStatus", targetStatus)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("WORK_ORDER_TRANSITION_FORBIDDEN");
        assertThat(workOrderRepository.findById(UUID.fromString(workOrderId)).orElseThrow().getStatus().name())
                .isEqualTo(expectedCurrentStatus);
    }

    private ResponseEntity<Map<String, Object>> assertTransition(
            String workOrderId,
            String token,
            String targetStatus,
            Map<String, Object> fields
    ) {
        HashMap<String, Object> body = new HashMap<>(fields);
        body.put("targetStatus", targetStatus);

        ResponseEntity<Map<String, Object>> response = postJsonMap(
                "/api/v1/work-orders/" + workOrderId + "/transition",
                token,
                body
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(targetStatus);
        return response;
    }
}
