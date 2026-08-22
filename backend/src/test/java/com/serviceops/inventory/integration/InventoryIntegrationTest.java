package com.serviceops.inventory.integration;

import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.integration.support.AbstractPostgresIntegrationTest;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

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

    @Test
    void unusedZeroStockSparePartCanBeDeleted() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        SparePart part = sparePart(owner, "DELETE-UNUSED-", BigDecimal.ZERO);
        sparePartRepository.saveAndFlush(part);

        ResponseEntity<String> response = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId(),
                HttpMethod.DELETE,
                login("owner", "123456"),
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId())).isEmpty();
    }

    @Test
    void sparePartWithHistoryCannotBeDeletedAndCanBeDiscontinued() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        SparePart part = sparePart(owner, "DELETE-HISTORY-", BigDecimal.ZERO);
        sparePartRepository.saveAndFlush(part);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTenantId(owner.getTenantId());
        transaction.setSparePart(part);
        transaction.setTransactionType(InventoryTransactionType.IMPORT);
        transaction.setQuantity(BigDecimal.ONE);
        transaction.setBalanceAfter(BigDecimal.ZERO);
        transaction.setNote("Historical transaction for safe-delete test");
        transaction.setCreatedBy("integration-test");
        inventoryTransactionRepository.saveAndFlush(transaction);

        String ownerToken = login("owner", "123456");

        ResponseEntity<String> deleteResponse = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId(),
                HttpMethod.DELETE,
                ownerToken,
                null
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deleteResponse.getBody()).contains("SPARE_PART_HAS_HISTORY");
        assertThat(sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId())).isPresent();

        ResponseEntity<String> discontinueResponse = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId() + "/active",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("active", false)
        );

        assertThat(discontinueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void positiveStockSparePartCannotBeDeletedButCanBeDiscontinued() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        SparePart part = sparePart(owner, "STOCK-GUARD-", new BigDecimal("2.000"));
        sparePartRepository.saveAndFlush(part);

        String ownerToken = login("owner", "123456");

        ResponseEntity<String> deleteResponse = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId(),
                HttpMethod.DELETE,
                ownerToken,
                null
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> discontinueResponse = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId() + "/active",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("active", false)
        );
        assertThat(discontinueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        SparePart reloaded = sparePartRepository
                .findByIdAndTenantId(part.getId(), owner.getTenantId())
                .orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualByComparingTo("2.000");
        assertThat(reloaded.isActive()).isFalse();
    }

    private SparePart sparePart(UserAccount owner, String skuPrefix, BigDecimal stock) {
        SparePart part = new SparePart();
        part.setTenantId(owner.getTenantId());
        part.setSku(skuPrefix + shortId());
        part.setName("Inventory lifecycle test part");
        part.setUnit("cái");
        part.setStockQuantity(stock);
        part.setReorderLevel(BigDecimal.ZERO);
        part.setUnitPrice(BigDecimal.ONE);
        part.setActive(true);
        return part;
    }

    private ResponseEntity<String> exchangeInventory(
            String path,
            HttpMethod method,
            String token,
            Map<String, Object> body
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
