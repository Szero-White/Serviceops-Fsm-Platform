package com.serviceops.scheduling.integration;

import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static com.serviceops.integration.support.IntegrationTestFixtures.workOrder;
import static org.assertj.core.api.Assertions.assertThat;

class SchedulingIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void dispatcherShouldReadScheduleBoardAndTechnicianShouldBeDenied() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = anyCustomerFor(owner);
        var technician = technicianRepository.findActive(owner.getTenantId()).get(0);

        WorkOrder scheduledWorkOrder = workOrder(
                owner.getTenantId(),
                customer,
                uniqueCode("BOARD-A-")
        );
        WorkOrder queuedWorkOrder = workOrder(
                owner.getTenantId(),
                customer,
                uniqueCode("BOARD-Q-")
        );
        workOrderRepository.saveAllAndFlush(List.of(scheduledWorkOrder, queuedWorkOrder));

        Instant start = Instant.now().plus(120, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String dispatcherToken = login("dispatcher", "123456");

        ResponseEntity<Map<String, Object>> scheduled = postJsonMap(
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
        ResponseEntity<Map<String, Object>> board = exchangeGetMap(boardPath, dispatcherToken);

        assertThat(board.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(board.getBody()).isNotNull();

        List<Map<String, Object>> appointments = mapList(board.getBody(), "appointments");
        List<Map<String, Object>> dispatchQueue = mapList(board.getBody(), "dispatchQueue");

        assertThat(appointments).anySatisfy(item ->
                assertThat(item.get("workOrderId")).isEqualTo(scheduledWorkOrder.getId().toString()));
        assertThat(dispatchQueue).anySatisfy(item ->
                assertThat(item.get("workOrderId")).isEqualTo(queuedWorkOrder.getId().toString()));

        ResponseEntity<Map<String, Object>> technicianAccess = exchangeGetMap(
                boardPath,
                login("technician", "123456")
        );
        assertThat(technicianAccess.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void technicianMyScheduleShouldUseAuthenticatedIdentity() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        UserAccount technician2User = userAccountRepository.findByUsernameIgnoreCase("technician-2").orElseThrow();

        var technician = technicianRepository
                .findByTenantIdAndUserId(owner.getTenantId(), technicianUser.getId())
                .orElseThrow();
        var technician2 = technicianRepository
                .findByTenantIdAndUserId(owner.getTenantId(), technician2User.getId())
                .orElseThrow();
        Customer customer = anyCustomerFor(owner);

        WorkOrder first = workOrder(owner.getTenantId(), customer, uniqueCode("MY-SCH-A-"));
        WorkOrder second = workOrder(owner.getTenantId(), customer, uniqueCode("MY-SCH-B-"));
        workOrderRepository.saveAllAndFlush(List.of(first, second));

        Instant start = Instant.now().plus(210, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String dispatcherToken = login("dispatcher", "123456");

        assertThat(postJsonMap(
                "/api/v1/work-orders/" + first.getId() + "/schedule",
                dispatcherToken,
                Map.of(
                        "technicianId", technician.getId(),
                        "startTime", start.toString(),
                        "endTime", end.toString()
                )
        ).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(postJsonMap(
                "/api/v1/work-orders/" + second.getId() + "/schedule",
                dispatcherToken,
                Map.of(
                        "technicianId", technician2.getId(),
                        "startTime", start.toString(),
                        "endTime", end.toString()
                )
        ).getStatusCode()).isEqualTo(HttpStatus.OK);

        String path = "/api/v1/my-schedule?from="
                + start.minus(1, ChronoUnit.DAYS)
                + "&to="
                + end.plus(1, ChronoUnit.DAYS);

        ResponseEntity<Map<String, Object>> firstSchedule = exchangeGetMap(
                path,
                login("technician", "123456")
        );
        assertThat(firstSchedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstSchedule.getBody()).isNotNull();
        assertThat(firstSchedule.getBody().get("technicianId"))
                .isEqualTo(technician.getId().toString());

        List<Map<String, Object>> firstAppointments = mapList(
                firstSchedule.getBody(),
                "appointments"
        );
        assertThat(firstAppointments)
                .extracting(item -> item.get("workOrderId"))
                .contains(first.getId().toString())
                .doesNotContain(second.getId().toString());

        ResponseEntity<Map<String, Object>> secondSchedule = exchangeGetMap(
                path,
                login("technician-2", "123456")
        );
        assertThat(secondSchedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondSchedule.getBody()).isNotNull();
        assertThat(secondSchedule.getBody().get("technicianId"))
                .isEqualTo(technician2.getId().toString());

        List<Map<String, Object>> secondAppointments = mapList(
                secondSchedule.getBody(),
                "appointments"
        );
        assertThat(secondAppointments)
                .extracting(item -> item.get("workOrderId"))
                .contains(second.getId().toString())
                .doesNotContain(first.getId().toString());

        assertThat(exchangeGetMap(path, dispatcherToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void inactiveTechnicianIdentityCannotBeReactivatedOrScheduledThroughWorkforceProfile() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        UserAccount user = new UserAccount();
        user.setTenantId(owner.getTenantId());
        user.setUsername("inactive-tech-" + shortId());
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

        Customer customer = anyCustomerFor(owner);
        WorkOrder workOrder = workOrder(
                owner.getTenantId(),
                customer,
                uniqueCode("INACT-WO-")
        );
        workOrderRepository.saveAndFlush(workOrder);

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        ResponseEntity<String> schedule = postJson(
                "/api/v1/work-orders/" + workOrder.getId() + "/schedule",
                dispatcherToken,
                Map.of(
                        "technicianId", technician.getId(),
                        "startTime", start,
                        "endTime", start.plus(2, ChronoUnit.HOURS)
                )
        );
        assertThat(schedule.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void concurrentOverlappingSchedulingShouldAllowOnlyOneWorkOrder() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = anyCustomerFor(owner);
        var technician = technicianRepository.findActive(owner.getTenantId()).get(0);

        WorkOrder first = workOrder(owner.getTenantId(), customer, uniqueCode("SCHED-A-"));
        WorkOrder second = workOrder(owner.getTenantId(), customer, uniqueCode("SCHED-B-"));
        workOrderRepository.saveAllAndFlush(List.of(first, second));

        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
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
                .filter(workOrder ->
                        appointmentRepository
                                .findByTenantIdAndWorkOrderId(owner.getTenantId(), workOrder.getId())
                                .isPresent()
                )
                .count();
        assertThat(createdAppointments).isEqualTo(1);
    }

    private Customer anyCustomerFor(UserAccount owner) {
        return customerRepository.findAll().stream()
                .filter(customer -> owner.getTenantId().equals(customer.getTenantId()))
                .findFirst()
                .orElseThrow();
    }
}
