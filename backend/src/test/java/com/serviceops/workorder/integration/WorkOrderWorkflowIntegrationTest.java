package com.serviceops.workorder.integration;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
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

    @Autowired
    private AppointmentRepository appointmentRepository;

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
    void customerServiceCanCancelAssignedWorkOrderButCannotPerformOtherTransitions() {
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
                        "title", "Customer cancellation after assignment",
                        "description", "Customer reports the equipment is working again after a technician was assigned",
                        "priority", "NORMAL",
                        "channel", "PHONE"
                )
        );
        assertThat(createdRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdRequest.getBody()).isNotNull();

        String serviceRequestId = String.valueOf(createdRequest.getBody().get("id"));
        ResponseEntity<Map<String, Object>> created = postJsonMap(
                "/api/v1/work-orders/from-service-request/" + serviceRequestId,
                customerServiceToken,
                Map.of()
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();

        String workOrderId = String.valueOf(created.getBody().get("id"));
        String workOrderCode = String.valueOf(created.getBody().get("code"));

        Instant start = Instant.now().plus(120, ChronoUnit.DAYS);
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
        assertThat(appointmentRepository
                .findByTenantIdAndWorkOrderId(owner.getTenantId(), UUID.fromString(workOrderId))
                .orElseThrow()
                .getStatus()).isEqualTo(AppointmentStatus.ACTIVE);

        ResponseEntity<Map<String, Object>> forbidden = postJsonMap(
                "/api/v1/work-orders/" + workOrderId + "/transition",
                customerServiceToken,
                Map.of("targetStatus", "SCHEDULED")
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody()).isNotNull();
        assertThat(forbidden.getBody().get("code")).isEqualTo("WORK_ORDER_TRANSITION_FORBIDDEN");
        assertThat(workOrderRepository.findById(UUID.fromString(workOrderId)).orElseThrow().getStatus().name())
                .isEqualTo("ASSIGNED");

        ResponseEntity<Map<String, Object>> missingReason = postJsonMap(
                "/api/v1/work-orders/" + workOrderId + "/transition",
                customerServiceToken,
                Map.of("targetStatus", "CANCELLED")
        );
        assertThat(missingReason.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingReason.getBody()).isNotNull();
        assertThat(missingReason.getBody().get("code")).isEqualTo("WORK_ORDER_CANCELLATION_REASON_REQUIRED");

        ResponseEntity<Map<String, Object>> cancelled = assertTransition(
                workOrderId,
                customerServiceToken,
                "CANCELLED",
                Map.of("note", "Khách hàng báo thiết bị đã hoạt động bình thường và không cần kỹ thuật viên đến")
        );
        assertThat(cancelled.getBody()).isNotNull();
        assertThat(appointmentRepository
                .findByTenantIdAndWorkOrderId(owner.getTenantId(), UUID.fromString(workOrderId))
                .orElseThrow()
                .getStatus()).isEqualTo(AppointmentStatus.CANCELLED);

        ResponseEntity<Map<String, Object>> activeSearch = exchangeGetMap(
                "/api/v1/work-orders?search=" + workOrderCode,
                customerServiceToken
        );
        assertThat(activeSearch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activeSearch.getBody()).isNotNull();
        assertThat(mapList(activeSearch.getBody(), "content"))
                .extracting(item -> item.get("id"))
                .doesNotContain(workOrderId);

        ResponseEntity<Map<String, Object>> historySearch = exchangeGetMap(
                "/api/v1/work-orders/history?status=CANCELLED&search=" + workOrderCode,
                customerServiceToken
        );
        assertThat(historySearch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historySearch.getBody()).isNotNull();
        assertThat(mapList(historySearch.getBody(), "content"))
                .extracting(item -> item.get("id"))
                .contains(workOrderId);
    }

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
