package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.InventoryDtos.ReorderLevelRequest;
import com.serviceops.inventory.web.InventoryDtos.StocktakeRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceWarehouseWorkflowTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private SparePartRepository sparePartRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private InventoryCsvService csvService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        authenticateWarehouse();
        service = new InventoryService(sparePartRepository, transactionRepository, workOrderRepository,
                csvService, auditService, notificationService, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void historySearchUsesConcreteDatabaseParametersWhenFiltersAreEmpty() {
        when(transactionRepository.search(
                eq(TENANT_ID),
                anyList(),
                eq(""),
                any(Instant.class),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.searchTransactions("", null, null, null, 0, 20);

        verify(transactionRepository).search(
                eq(TENANT_ID),
                eq(List.of(InventoryTransactionType.values())),
                eq(""),
                eq(Instant.EPOCH),
                eq(Instant.parse("9999-12-31T23:59:59Z")),
                any(Pageable.class)
        );
    }

    @Test
    void reorderLevelUpdateChangesOnlyThresholdAndPublishesPolicyEvent() {
        SparePart part = part(new BigDecimal("5.000"));
        part.setReorderLevel(new BigDecimal("3.000"));
        when(sparePartRepository.findForUpdate(part.getId(), TENANT_ID)).thenReturn(Optional.of(part));

        var result = service.updateReorderLevel(
                part.getId(),
                new ReorderLevelRequest(new BigDecimal("6.000"))
        );

        assertThat(part.getStockQuantity()).isEqualByComparingTo("5.000");
        assertThat(part.getReorderLevel()).isEqualByComparingTo("6.000");
        assertThat(result.lowStock()).isTrue();
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));

        ArgumentCaptor<InventoryReorderLevelChangedEvent> event = ArgumentCaptor.forClass(InventoryReorderLevelChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().stockQuantity()).isEqualByComparingTo("5.000");
        assertThat(event.getValue().previousReorderLevel()).isEqualByComparingTo("3.000");
        assertThat(event.getValue().newReorderLevel()).isEqualByComparingTo("6.000");
        assertThat(event.getValue().becameLowStock()).isTrue();
        assertThat(event.getValue().actorDisplayName()).isEqualTo("Đặng Nam Kho");
    }

    @Test
    void stocktakeCreatesAdjustmentOutAndReconcilesPhysicalQuantity() {
        SparePart part = part(new BigDecimal("10.000"));
        when(sparePartRepository.findForUpdate(part.getId(), TENANT_ID)).thenReturn(Optional.of(part));

        var result = service.stocktake(part.getId(), new StocktakeRequest(new BigDecimal("8.000"), "Kiểm kê cuối ca"));

        assertThat(part.getStockQuantity()).isEqualByComparingTo("8.000");
        assertThat(result.difference()).isEqualByComparingTo("-2.000");
        assertThat(result.adjustmentType()).isEqualTo(InventoryTransactionType.ADJUSTMENT_OUT);

        ArgumentCaptor<InventoryTransaction> transaction = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.ADJUSTMENT_OUT);
        assertThat(transaction.getValue().getQuantity()).isEqualByComparingTo("2.000");
        assertThat(transaction.getValue().getBalanceAfter()).isEqualByComparingTo("8.000");
        assertThat(transaction.getValue().getCreatedBy()).isEqualTo("warehouse");
        assertThat(transaction.getValue().getActorDisplayName()).isEqualTo("Đặng Nam Kho");
        assertThat(transaction.getValue().getActorRole()).isEqualTo("WAREHOUSE_STAFF");

        ArgumentCaptor<InventoryStockAdjustedEvent> event = ArgumentCaptor.forClass(InventoryStockAdjustedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().systemQuantity()).isEqualByComparingTo("10.000");
        assertThat(event.getValue().actualQuantity()).isEqualByComparingTo("8.000");
        assertThat(event.getValue().actorDisplayName()).isEqualTo("Đặng Nam Kho");
        assertThat(event.getValue().reason()).isEqualTo("Kiểm kê cuối ca");
    }

    @Test
    void stocktakeWithoutDifferenceDoesNotPublishAdjustmentEvent() {
        SparePart part = part(new BigDecimal("10.000"));
        when(sparePartRepository.findForUpdate(part.getId(), TENANT_ID)).thenReturn(Optional.of(part));

        var result = service.stocktake(part.getId(), new StocktakeRequest(new BigDecimal("10.000"), "Kiểm kê khớp"));

        assertThat(result.difference()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.adjustmentType()).isNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static SparePart part(BigDecimal stock) {
        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setTenantId(TENANT_ID);
        part.setSku("PART-TEST");
        part.setName("Test Part");
        part.setUnit("cái");
        part.setStockQuantity(stock);
        part.setReorderLevel(BigDecimal.ONE);
        part.setUnitPrice(new BigDecimal("10000"));
        part.setActive(true);
        return part;
    }

    private static void authenticateWarehouse() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("warehouse")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("roles", List.of("WAREHOUSE_STAFF"))
                .claim("displayName", "Đặng Nam Kho")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
