package com.serviceops.inventory.integration;

import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.serviceops.integration.support.IntegrationTestFixtures.customer;
import static com.serviceops.integration.support.IntegrationTestFixtures.shortId;
import static com.serviceops.integration.support.IntegrationTestFixtures.uniqueCode;
import static com.serviceops.integration.support.IntegrationTestFixtures.workOrder;
import static org.assertj.core.api.Assertions.assertThat;

class InventoryIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private SparePartRepository sparePartRepository;

    @Test
    void inactiveSparePartCannotBeConsumed() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer customer = customer(
                owner.getTenantId(),
                uniqueCode("IP-CUST-"),
                "Inactive Part Customer"
        );
        customerRepository.saveAndFlush(customer);

        WorkOrder workOrder = workOrder(
                owner.getTenantId(),
                customer,
                uniqueCode("IP-WO-")
        );
        workOrderRepository.saveAndFlush(workOrder);

        SparePart part = new SparePart();
        part.setTenantId(owner.getTenantId());
        part.setSku("INACTIVE-PART-" + shortId());
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
                Map.of(
                        "sparePartId", part.getId(),
                        "quantity", new BigDecimal("1"),
                        "note", "Must be rejected"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(
                sparePartRepository
                        .findByIdAndTenantId(part.getId(), owner.getTenantId())
                        .orElseThrow()
                        .getStockQuantity()
        ).isEqualByComparingTo("5");
    }

    @Test
    void concurrentInventoryConsumptionShouldNeverCreateNegativeStock() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer customer = customer(
                owner.getTenantId(),
                uniqueCode("STOCK-CUST-"),
                "Stock Concurrency Customer"
        );
        customerRepository.saveAndFlush(customer);

        WorkOrder workOrder = workOrder(
                owner.getTenantId(),
                customer,
                uniqueCode("STOCK-WO-")
        );
        workOrderRepository.saveAndFlush(workOrder);

        SparePart part = new SparePart();
        part.setTenantId(owner.getTenantId());
        part.setSku("CONC-STOCK-" + shortId());
        part.setName("Concurrency Test Part");
        part.setUnit("cái");
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
        SparePart reloaded = sparePartRepository
                .findByIdAndTenantId(part.getId(), owner.getTenantId())
                .orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualByComparingTo("1.000");
    }
}
