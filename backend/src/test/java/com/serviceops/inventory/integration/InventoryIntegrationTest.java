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
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
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
    private TechnicianRepository technicianRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private WorkOrderPartRequestRepository workOrderPartRequestRepository;

    @Test
    void inactiveSparePartCannotBeRequested() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer customer = customer(
                owner.getTenantId(),
                uniqueCode("IP-CUST-"),
                "Inactive Part Customer"
        );
        customerRepository.saveAndFlush(customer);

        WorkOrder workOrder = assignedInProgressWorkOrder(owner, customer, "IP-WO-");

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
                "/api/v1/work-orders/" + workOrder.getId() + "/part-requests",
                login("technician", "123456"),
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
    void concurrentPartIssueShouldNeverCreateNegativeStock() throws Exception {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();

        Customer firstCustomer = customer(
                owner.getTenantId(),
                uniqueCode("STOCK-CUST-A-"),
                "Stock Concurrency Customer A"
        );
        Customer secondCustomer = customer(
                owner.getTenantId(),
                uniqueCode("STOCK-CUST-B-"),
                "Stock Concurrency Customer B"
        );
        customerRepository.saveAllAndFlush(List.of(firstCustomer, secondCustomer));

        WorkOrder firstWorkOrder = assignedInProgressWorkOrder(owner, firstCustomer, "STOCK-WO-A-");
        WorkOrder secondWorkOrder = assignedInProgressWorkOrder(owner, secondCustomer, "STOCK-WO-B-");

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

        WorkOrderPartRequest firstRequest = requestedPart(owner, firstWorkOrder, part, new BigDecimal("4.000"));
        WorkOrderPartRequest secondRequest = requestedPart(owner, secondWorkOrder, part, new BigDecimal("4.000"));
        String warehouseToken = login("warehouse", "123456");

        List<Integer> statuses = runConcurrentPosts(
                "/api/v1/part-requests/" + firstRequest.getId() + "/issue",
                "/api/v1/part-requests/" + secondRequest.getId() + "/issue",
                warehouseToken,
                Map.of()
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

    @Test
    void warehouseCanUpdateMinimumStockThresholdWithoutChangingPhysicalStock() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        SparePart part = sparePart(owner, "THRESHOLD-", new BigDecimal("5.000"));
        sparePartRepository.saveAndFlush(part);

        ResponseEntity<String> response = exchangeInventory(
                "/api/v1/spare-parts/" + part.getId() + "/reorder-level",
                HttpMethod.PATCH,
                login("warehouse", "123456"),
                Map.of("reorderLevel", new BigDecimal("6.000"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SparePart reloaded = sparePartRepository
                .findByIdAndTenantId(part.getId(), owner.getTenantId())
                .orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualByComparingTo("5.000");
        assertThat(reloaded.getReorderLevel()).isEqualByComparingTo("6.000");
    }

    @Test
    void warehouseCanReconcileReturnAndTraceInventoryWithoutOperationalWorkOrderAccess() {
        UserAccount owner = userAccountRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        Customer customer = customer(owner.getTenantId(), uniqueCode("WH-CUST-"), "Warehouse Workflow Customer");
        customerRepository.saveAndFlush(customer);
        WorkOrder workOrder = assignedInProgressWorkOrder(owner, customer, "WH-WO-");
        SparePart part = sparePart(owner, "WH-PART-", new BigDecimal("5.000"));
        sparePartRepository.saveAndFlush(part);

        WorkOrderPartRequest request = requestedPart(owner, workOrder, part, new BigDecimal("2.000"));
        String warehouseToken = login("warehouse", "123456");
        ResponseEntity<String> issueResponse = postJson(
                "/api/v1/part-requests/" + request.getId() + "/issue",
                warehouseToken,
                Map.of()
        );
        assertThat(issueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> returnResponse = postJson(
                "/api/v1/work-orders/" + workOrder.getId() + "/parts/" + part.getId() + "/return",
                warehouseToken,
                Map.of("quantity", new BigDecimal("1.000"), "note", "Unused part returned")
        );
        assertThat(returnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> stocktakeResponse = postJson(
                "/api/v1/spare-parts/" + part.getId() + "/stocktake",
                warehouseToken,
                Map.of("actualQuantity", new BigDecimal("3.000"), "reason", "Physical count")
        );
        assertThat(stocktakeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> historyResponse = exchangeInventory(
                "/api/v1/inventory-transactions?search=" + part.getSku(),
                HttpMethod.GET,
                warehouseToken,
                null
        );
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).contains("ISSUE", "RETURN", "ADJUSTMENT_OUT", part.getSku());

        ResponseEntity<String> unfilteredHistoryResponse = exchangeInventory(
                "/api/v1/inventory-transactions",
                HttpMethod.GET,
                warehouseToken,
                null
        );
        assertThat(unfilteredHistoryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unfilteredHistoryResponse.getBody()).contains(part.getSku());

        assertThat(sparePartRepository.findByIdAndTenantId(part.getId(), owner.getTenantId()).orElseThrow().getStockQuantity())
                .isEqualByComparingTo("3.000");
    }

    private WorkOrder assignedInProgressWorkOrder(UserAccount owner, Customer customer, String codePrefix) {
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        TechnicianProfile technician = technicianRepository
                .findByTenantIdAndUserId(owner.getTenantId(), technicianUser.getId())
                .orElseThrow();

        WorkOrder workOrder = workOrder(owner.getTenantId(), customer, uniqueCode(codePrefix));
        workOrder.setTechnician(technician);
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        return workOrderRepository.saveAndFlush(workOrder);
    }

    private WorkOrderPartRequest requestedPart(
            UserAccount owner,
            WorkOrder workOrder,
            SparePart part,
            BigDecimal quantity
    ) {
        UserAccount technicianUser = userAccountRepository.findByUsernameIgnoreCase("technician").orElseThrow();
        WorkOrderPartRequest request = new WorkOrderPartRequest();
        request.setTenantId(owner.getTenantId());
        request.setWorkOrder(workOrder);
        request.setSparePart(part);
        request.setRequestedQuantity(quantity);
        request.setRequestNote("Integration test request");
        request.setStatus(WorkOrderPartRequestStatus.REQUESTED);
        request.setRequestedByUserId(technicianUser.getId());
        request.setRequestedByUsername(technicianUser.getUsername());
        request.setRequestedByDisplayName(technicianUser.getDisplayName());
        return workOrderPartRequestRepository.saveAndFlush(request);
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
