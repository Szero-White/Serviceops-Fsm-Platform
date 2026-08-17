package com.serviceops.integration;

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
    void technicianShouldBeDeniedCustomerAdministration() {
        String token = login("technician", "123456");

        ResponseEntity<String> response = exchangeGet("/api/v1/customers", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
}
