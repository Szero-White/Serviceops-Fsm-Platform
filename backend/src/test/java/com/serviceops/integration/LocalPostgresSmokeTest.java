package com.serviceops.integration;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.tenant.domain.TenantRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalPostgresSmokeTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("serviceops")
            .withUsername("serviceops")
            .withPassword("serviceops");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void shouldRunFlywaySeedDataAndLogin() {
        String token = login("owner", "123456");

        assertThat(token).isNotBlank();
    }

    @Test
    void ownerShouldReachCriticalReadOnlyModules() {
        String token = login("owner", "123456");

        for (String path : new String[]{
                "/api/v1/dashboard",
                "/api/v1/customers",
                "/api/v1/assets",
                "/api/v1/service-requests",
                "/api/v1/work-orders",
                "/api/v1/technicians",
                "/api/v1/spare-parts",
                "/api/v1/audit-logs",
                "/api/v1/notifications"
        }) {
            ResponseEntity<String> response = exchangeGet(path, token);
            assertThat(response.getStatusCode())
                    .as("GET %s", path)
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void assetsShouldBeFilterableByCustomerForDependentSelectors() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer firstCustomer = testCustomer(owner.getTenantId(), "FILTER-CUST-A", "Asset Filter Customer A");
        Customer secondCustomer = testCustomer(owner.getTenantId(), "FILTER-CUST-B", "Asset Filter Customer B");
        customerRepository.saveAllAndFlush(List.of(firstCustomer, secondCustomer));

        Asset firstAsset = testAsset(owner.getTenantId(), firstCustomer, "FILTER-ASSET-A");
        Asset secondAsset = testAsset(owner.getTenantId(), secondCustomer, "FILTER-ASSET-B");
        assetRepository.saveAllAndFlush(List.of(firstAsset, secondAsset));

        ResponseEntity<Map> response = exchangeGetMap(
                "/api/v1/assets?customerId=" + firstCustomer.getId() + "&size=100",
                login("customer-service", "123456")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content)
                .extracting(item -> item.get("id"))
                .contains(firstAsset.getId().toString())
                .doesNotContain(secondAsset.getId().toString());
    }

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
    void aiHelpShouldSupportEveryRoleThatSeesTheAssistant() {
        for (String username : List.of("technician", "warehouse")) {
            ResponseEntity<String> response = postJson(
                    "/api/v1/ai/help",
                    login(username, "123456"),
                    Map.of("question", "Tôi nên bắt đầu ở đâu?", "currentPath", "/")
            );

            assertThat(response.getStatusCode())
                    .as("AI help for %s", username)
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void tenantACannotReadTenantBCustomerButTenantBCan() {
        Tenant tenantB = new Tenant();
        tenantB.setCode("TENANT-B");
        tenantB.setName("Tenant B Isolation Test");
        tenantB.setActive(true);
        tenantRepository.save(tenantB);

        UserAccount tenantBOwner = new UserAccount();
        tenantBOwner.setTenantId(tenantB.getId());
        tenantBOwner.setUsername("tenant-b-owner");
        tenantBOwner.setDisplayName("Tenant B Owner");
        tenantBOwner.setPasswordHash(passwordEncoder.encode("tenant-b-password"));
        tenantBOwner.setRole(UserRole.OWNER);
        tenantBOwner.setActive(true);
        userAccountRepository.save(tenantBOwner);

        Customer tenantBCustomer = new Customer();
        tenantBCustomer.setTenantId(tenantB.getId());
        tenantBCustomer.setCode("B-CUSTOMER-001");
        tenantBCustomer.setName("Tenant B Customer");
        tenantBCustomer.setActive(true);
        customerRepository.save(tenantBCustomer);

        String tenantAToken = login("owner", "123456");
        String tenantBToken = login("tenant-b-owner", "tenant-b-password");

        ResponseEntity<String> crossTenant = exchangeGet("/api/v1/customers/" + tenantBCustomer.getId(), tenantAToken);
        ResponseEntity<String> ownTenant = exchangeGet("/api/v1/customers/" + tenantBCustomer.getId(), tenantBToken);

        assertThat(crossTenant.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ownTenant.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void coreServiceFlowShouldRunAcrossBusinessRoles() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> owner.getTenantId().equals(c.getTenantId()))
                .findFirst()
                .orElseThrow();
        var technician = technicianRepository.findByTenantIdAndUserId(owner.getTenantId(), technicianUser.getId())
                .orElseThrow();

        String customerServiceToken = login("customer-service", "123456");
        ResponseEntity<Map> createdRequest = postJsonMap(
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

        ResponseEntity<Map> converted = postJsonMap(
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
        ResponseEntity<Map> scheduled = postJsonMap(
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
        assertTransition(workOrderId, technicianToken, "COMPLETED", Map.of(
                "note", "Service completed",
                "diagnosis", "Integration test diagnosis",
                "resolution", "Integration test resolution"
        ));

        String ownerToken = login("owner", "123456");
        assertTransition(workOrderId, ownerToken, "CUSTOMER_ACCEPTED", Map.of("note", "Customer accepted result"));
        ResponseEntity<Map> closed = assertTransition(workOrderId, ownerToken, "CLOSED", Map.of("note", "Workflow closed"));
        assertThat(closed.getBody()).isNotNull();
        assertThat(closed.getBody().get("status")).isEqualTo("CLOSED");
    }

    @Test
    void dispatcherShouldReadScheduleBoardAndTechnicianShouldBeDenied() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> owner.getTenantId().equals(c.getTenantId()))
                .findFirst()
                .orElseThrow();
        var technician = technicianRepository.findActive(owner.getTenantId()).get(0);

        WorkOrder scheduledWorkOrder = testWorkOrder(owner.getTenantId(), customer, "TEST-BOARD-SCHEDULED");
        WorkOrder queuedWorkOrder = testWorkOrder(owner.getTenantId(), customer, "TEST-BOARD-QUEUE");
        workOrderRepository.saveAllAndFlush(List.of(scheduledWorkOrder, queuedWorkOrder));

        Instant start = Instant.now().plus(120, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String dispatcherToken = login("dispatcher", "123456");
        ResponseEntity<Map> scheduled = postJsonMap(
                "/api/v1/work-orders/" + scheduledWorkOrder.getId() + "/schedule",
                dispatcherToken,
                Map.of(
                        "technicianId", technician.getId(),
                        "startTime", start.toString(),
                        "endTime", end.toString()
                )
        );
        assertThat(scheduled.getStatusCode()).isEqualTo(HttpStatus.OK);

        String boardPath = "/api/v1/schedule-board?from="
                + start.minus(1, ChronoUnit.DAYS)
                + "&to="
                + end.plus(1, ChronoUnit.DAYS);
        ResponseEntity<Map> board = exchangeGetMap(boardPath, dispatcherToken);

        assertThat(board.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(board.getBody()).isNotNull();
        List<Map<String, Object>> appointments = (List<Map<String, Object>>) board.getBody().get("appointments");
        List<Map<String, Object>> dispatchQueue = (List<Map<String, Object>>) board.getBody().get("dispatchQueue");
        assertThat(appointments).anySatisfy(item ->
                assertThat(item.get("workOrderId")).isEqualTo(scheduledWorkOrder.getId().toString()));
        assertThat(dispatchQueue).anySatisfy(item ->
                assertThat(item.get("workOrderId")).isEqualTo(queuedWorkOrder.getId().toString()));

        ResponseEntity<Map> technicianAccess = exchangeGetMap(boardPath, login("technician", "123456"));
        assertThat(technicianAccess.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void technicianMyScheduleShouldUseAuthenticatedIdentity() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        UserAccount technician2User = userAccountRepository.findByUsernameIgnoreCase("technician-2").orElseThrow();
        var technician = technicianRepository.findByTenantIdAndUserId(owner.getTenantId(), technicianUser.getId()).orElseThrow();
        var technician2 = technicianRepository.findByTenantIdAndUserId(owner.getTenantId(), technician2User.getId()).orElseThrow();
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> owner.getTenantId().equals(c.getTenantId()))
                .findFirst()
                .orElseThrow();

        WorkOrder first = testWorkOrder(owner.getTenantId(), customer, "TEST-MY-SCHEDULE-A");
        WorkOrder second = testWorkOrder(owner.getTenantId(), customer, "TEST-MY-SCHEDULE-B");
        workOrderRepository.saveAllAndFlush(List.of(first, second));

        Instant start = Instant.now().plus(210, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String dispatcherToken = login("dispatcher", "123456");
        assertThat(postJsonMap(
                "/api/v1/work-orders/" + first.getId() + "/schedule",
                dispatcherToken,
                Map.of("technicianId", technician.getId(), "startTime", start.toString(), "endTime", end.toString())
        ).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postJsonMap(
                "/api/v1/work-orders/" + second.getId() + "/schedule",
                dispatcherToken,
                Map.of("technicianId", technician2.getId(), "startTime", start.toString(), "endTime", end.toString())
        ).getStatusCode()).isEqualTo(HttpStatus.OK);

        String path = "/api/v1/my-schedule?from="
                + start.minus(1, ChronoUnit.DAYS)
                + "&to="
                + end.plus(1, ChronoUnit.DAYS);

        ResponseEntity<Map> firstSchedule = exchangeGetMap(path, login("technician", "123456"));
        assertThat(firstSchedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstSchedule.getBody()).isNotNull();
        assertThat(firstSchedule.getBody().get("technicianId")).isEqualTo(technician.getId().toString());
        List<Map<String, Object>> firstAppointments = (List<Map<String, Object>>) firstSchedule.getBody().get("appointments");
        assertThat(firstAppointments)
                .extracting(item -> item.get("workOrderId"))
                .contains(first.getId().toString())
                .doesNotContain(second.getId().toString());

        ResponseEntity<Map> secondSchedule = exchangeGetMap(path, login("technician-2", "123456"));
        assertThat(secondSchedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondSchedule.getBody()).isNotNull();
        assertThat(secondSchedule.getBody().get("technicianId")).isEqualTo(technician2.getId().toString());
        List<Map<String, Object>> secondAppointments = (List<Map<String, Object>>) secondSchedule.getBody().get("appointments");
        assertThat(secondAppointments)
                .extracting(item -> item.get("workOrderId"))
                .contains(second.getId().toString())
                .doesNotContain(first.getId().toString());

        assertThat(exchangeGetMap(path, dispatcherToken).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void inactiveTechnicianIdentityCannotBeReactivatedOrScheduledThroughWorkforceProfile() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        UserAccount user = new UserAccount();
        user.setTenantId(owner.getTenantId());
        user.setUsername("inactive-tech-" + UUID.randomUUID());
        user.setDisplayName("Inactive Technician Identity");
        user.setPasswordHash(passwordEncoder.encode("Technician-Test1!"));
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(false);
        userAccountRepository.saveAndFlush(user);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setTenantId(owner.getTenantId());
        technician.setUser(user);
        technician.setPhone("0900000000");
        technician.setSkills("Integration test");
        technician.setActive(true);
        technicianRepository.saveAndFlush(technician);

        String dispatcherToken = login("dispatcher", "123456");
        ResponseEntity<String> reactivate = putJson(
                "/api/v1/technicians/" + technician.getId(),
                dispatcherToken,
                Map.of("phone", "0900000000", "skills", "Integration test", "active", true)
        );
        assertThat(reactivate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Customer customer = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(),
                "INACTIVE-TECH-CUST-" + UUID.randomUUID().toString().substring(0, 8),
                "Inactive Technician Customer"
        ));
        WorkOrder workOrder = workOrderRepository.saveAndFlush(testWorkOrder(
                owner.getTenantId(),
                customer,
                "WO-INACTIVE-TECH-" + UUID.randomUUID().toString().substring(0, 8)
        ));

        ResponseEntity<String> schedule = postJson(
                "/api/v1/work-orders/" + workOrder.getId() + "/schedule",
                dispatcherToken,
                Map.of(
                        "technicianId", technician.getId(),
                        "startTime", Instant.now().plus(2, ChronoUnit.DAYS),
                        "endTime", Instant.now().plus(2, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS)
                )
        );
        assertThat(schedule.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void assetCustomerCannotChangeAfterOperationalHistoryExists() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer first = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(), "ASSET-HISTORY-A-" + UUID.randomUUID().toString().substring(0, 8), "Asset History Customer A"
        ));
        Customer second = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(), "ASSET-HISTORY-B-" + UUID.randomUUID().toString().substring(0, 8), "Asset History Customer B"
        ));
        Asset asset = assetRepository.saveAndFlush(testAsset(
                owner.getTenantId(), first, "ASSET-HISTORY-" + UUID.randomUUID()
        ));

        String customerServiceToken = login("customer-service", "123456");
        ResponseEntity<Map> serviceRequest = postJsonMap(
                "/api/v1/service-requests",
                customerServiceToken,
                Map.of(
                        "customerId", first.getId(),
                        "assetId", asset.getId(),
                        "title", "Asset ownership history test",
                        "description", "Create history before trying to move the asset",
                        "priority", "NORMAL",
                        "channel", "PHONE"
                )
        );
        assertThat(serviceRequest.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> update = putJson(
                "/api/v1/assets/" + asset.getId(),
                login("owner", "123456"),
                Map.of(
                        "customerId", second.getId(),
                        "category", asset.getCategory(),
                        "serialNumber", asset.getSerialNumber(),
                        "status", asset.getStatus().name()
                )
        );
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(assetRepository.findDetailed(asset.getId(), owner.getTenantId()).orElseThrow().getCustomer().getId())
                .isEqualTo(first.getId());
    }

    @Test
    void serviceRequestConversionMustPreserveCustomerAssetAndAllowOnlyOneConcurrentConversion() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer first = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(), "SR-WO-A-" + UUID.randomUUID().toString().substring(0, 8), "Service Request Customer A"
        ));
        Customer second = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(), "SR-WO-B-" + UUID.randomUUID().toString().substring(0, 8), "Service Request Customer B"
        ));
        Asset firstAsset = assetRepository.saveAndFlush(testAsset(
                owner.getTenantId(), first, "SR-WO-ASSET-A-" + UUID.randomUUID()
        ));
        Asset secondAsset = assetRepository.saveAndFlush(testAsset(
                owner.getTenantId(), second, "SR-WO-ASSET-B-" + UUID.randomUUID()
        ));

        String customerServiceToken = login("customer-service", "123456");
        ResponseEntity<Map> createdRequest = postJsonMap(
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

    @Test
    void inactiveSparePartCannotBeConsumed() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = customerRepository.saveAndFlush(testCustomer(
                owner.getTenantId(), "INACTIVE-PART-CUST-" + UUID.randomUUID().toString().substring(0, 8), "Inactive Part Customer"
        ));
        WorkOrder workOrder = workOrderRepository.saveAndFlush(testWorkOrder(
                owner.getTenantId(), customer, "WO-INACTIVE-PART-" + UUID.randomUUID().toString().substring(0, 8)
        ));

        SparePart part = new SparePart();
        part.setTenantId(owner.getTenantId());
        part.setSku("INACTIVE-PART-" + UUID.randomUUID());
        part.setName("Inactive Integration Part");
        part.setUnit("cái");
        part.setStockQuantity(new BigDecimal("5"));
        part.setReorderLevel(BigDecimal.ONE);
        part.setUnitPrice(new BigDecimal("10000"));
        part.setActive(false);
        sparePartRepository.saveAndFlush(part);

        ResponseEntity<String> response = postJson(
                "/api/v1/work-orders/" + workOrder.getId() + "/parts/consume",
                login("owner", "123456"),
                Map.of("sparePartId", part.getId(), "quantity", new BigDecimal("1"), "note", "Must be rejected")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId()).orElseThrow().getStockQuantity())
                .isEqualByComparingTo("5");
    }

    @Test
    void concurrentInventoryConsumptionShouldNeverCreateNegativeStock() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        WorkOrder workOrder = workOrderRepository.findAll().stream()
                .filter(w -> owner.getTenantId().equals(w.getTenantId()))
                .filter(w -> w.getStatus() != WorkOrderStatus.CLOSED && w.getStatus() != WorkOrderStatus.CANCELLED)
                .findFirst()
                .orElseThrow();

        SparePart part = new SparePart();
        part.setTenantId(owner.getTenantId());
        part.setSku("CONC-STOCK-001");
        part.setName("Concurrency Test Part");
        part.setUnit("cai");
        part.setStockQuantity(new BigDecimal("5.000"));
        part.setReorderLevel(BigDecimal.ZERO);
        part.setUnitPrice(BigDecimal.ONE);
        part.setActive(true);
        sparePartRepository.saveAndFlush(part);

        String token = login("owner", "123456");
        Map<String, Object> body = Map.of(
                "sparePartId", part.getId(),
                "quantity", new BigDecimal("4.000"),
                "note", "concurrency integration test"
        );

        List<Integer> statuses = runTwoConcurrentPosts(
                "/api/v1/work-orders/" + workOrder.getId() + "/parts/consume",
                token,
                body
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        SparePart reloaded = sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualByComparingTo("1.000");
    }

    @Test
    void concurrentOverlappingSchedulingShouldAllowOnlyOneWorkOrder() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> owner.getTenantId().equals(c.getTenantId()))
                .findFirst()
                .orElseThrow();
        var technician = technicianRepository.findActive(owner.getTenantId()).get(0);

        WorkOrder first = testWorkOrder(owner.getTenantId(), customer, "TEST-SCHED-A");
        WorkOrder second = testWorkOrder(owner.getTenantId(), customer, "TEST-SCHED-B");
        workOrderRepository.saveAndFlush(first);
        workOrderRepository.saveAndFlush(second);

        Instant start = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(30));
        Instant end = start.plusSeconds(TimeUnit.HOURS.toSeconds(2));
        Map<String, Object> body = Map.of(
                "technicianId", technician.getId(),
                "startTime", start.toString(),
                "endTime", end.toString()
        );
        String token = login("owner", "123456");

        List<Integer> statuses = runConcurrentPosts(
                "/api/v1/work-orders/" + first.getId() + "/schedule",
                "/api/v1/work-orders/" + second.getId() + "/schedule",
                token,
                body
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        long createdAppointments = List.of(first, second).stream()
                .filter(w -> appointmentRepository.findByTenantIdAndWorkOrderId(owner.getTenantId(), w.getId()).isPresent())
                .count();
        assertThat(createdAppointments).isEqualTo(1);
    }


    @Test
    void concurrentOwnersCannotDisableEachOtherAndLeaveTenantWithoutActiveOwner() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setCode("OWNER-RACE");
        tenant.setName("Owner Invariant Concurrency Test");
        tenant.setActive(true);
        tenantRepository.save(tenant);

        UserAccount ownerA = testOwner(tenant, "owner-race-a", "Owner Race A");
        UserAccount ownerB = testOwner(tenant, "owner-race-b", "Owner Race B");
        userAccountRepository.saveAndFlush(ownerA);
        userAccountRepository.saveAndFlush(ownerB);

        String tokenA = login(ownerA.getUsername(), "Owner-Race-Password1!");
        String tokenB = login(ownerB.getUsername(), "Owner-Race-Password1!");

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
                () -> putJson("/api/v1/users/" + ownerB.getId(), tokenA, disableB).getStatusCode().value(),
                () -> putJson("/api/v1/users/" + ownerA.getId(), tokenB, disableA).getStatusCode().value()
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(userAccountRepository.countByTenantIdAndRoleAndActiveTrue(tenant.getId(), UserRole.OWNER))
                .isEqualTo(1);
    }


    private Customer testCustomer(UUID tenantId, String code, String name) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setCode(code);
        customer.setName(name);
        customer.setActive(true);
        return customer;
    }

    private Asset testAsset(UUID tenantId, Customer customer, String serialNumber) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomer(customer);
        asset.setCategory("Laptop");
        asset.setSerialNumber(serialNumber);
        asset.setStatus(AssetStatus.ACTIVE);
        return asset;
    }

    private UserAccount testOwner(Tenant tenant, String username, String displayName) {
        UserAccount owner = new UserAccount();
        owner.setTenantId(tenant.getId());
        owner.setUsername(username);
        owner.setDisplayName(displayName);
        owner.setPasswordHash(passwordEncoder.encode("Owner-Race-Password1!"));
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        return owner;
    }

    private WorkOrder testWorkOrder(java.util.UUID tenantId, Customer customer, String code) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTenantId(tenantId);
        workOrder.setCustomer(customer);
        workOrder.setCode(code);
        workOrder.setSummary("Concurrency scheduling test");
        workOrder.setPriority(Priority.NORMAL);
        workOrder.setStatus(WorkOrderStatus.OPEN);
        return workOrder;
    }

    private List<Integer> runTwoConcurrentPosts(String path, String token, Map<String, Object> body) throws Exception {
        return runConcurrentPosts(path, path, token, body);
    }

    private List<Integer> runConcurrentPosts(String firstPath, String secondPath, String token, Map<String, Object> body) throws Exception {
        return runConcurrentRequests(
                () -> postJson(firstPath, token, body).getStatusCode().value(),
                () -> postJson(secondPath, token, body).getStatusCode().value()
        );
    }

    private List<Integer> runConcurrentRequests(java.util.concurrent.Callable<Integer> firstRequest,
                                                java.util.concurrent.Callable<Integer> secondRequest) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> {
                start.await();
                return firstRequest.call();
            });
            Future<Integer> second = executor.submit(() -> {
                start.await();
                return secondRequest.call();
            });
            start.countDown();
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<Map> assertTransition(String workOrderId, String token, String targetStatus, Map<String, Object> fields) {
        HashMap<String, Object> body = new HashMap<>(fields);
        body.put("targetStatus", targetStatus);
        ResponseEntity<Map> response = postJsonMap("/api/v1/work-orders/" + workOrderId + "/transition", token, body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(targetStatus);
        return response;
    }

    private ResponseEntity<Map> postJsonMap(String path, String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<String> postJson(String path, String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }


    private ResponseEntity<String> putJson(String path, String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of("username", username, "password", password),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return String.valueOf(response.getBody().get("accessToken"));
    }

    private ResponseEntity<String> exchangeGet(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
    }

    private ResponseEntity<Map> exchangeGetMap(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null, headers), Map.class);
    }
}
