package com.serviceops.workorder.application;

import com.serviceops.common.domain.Priority;
import com.serviceops.inventory.domain.InventoryPartUsage;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderInvoiceReturnTest {
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WorkOrderInvoiceHtmlRenderer invoiceRenderer;
    @Captor private ArgumentCaptor<List<InventoryPartUsage>> usageCaptor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invoiceUsesNetConsumptionAfterWarehouseReturn() {
        UUID tenantId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        authenticateOwner(tenantId);

        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setSku("CAP-35UF");
        part.setName("Tụ điện 35uF");
        part.setUnit("cái");
        part.setUnitPrice(new BigDecimal("50000"));

        InventoryTransaction consume = usage(part, InventoryTransactionType.CONSUME, "3.000");
        InventoryTransaction returned = usage(part, InventoryTransactionType.RETURN, "1.000");
        WorkOrderResponse workOrder = workOrder(workOrderId);
        when(transactionRepository.findPartUsageForWorkOrder(tenantId, workOrderId)).thenReturn(List.of(consume, returned));
        when(invoiceRenderer.render(eq(workOrder), anyList())).thenReturn("<html></html>");

        WorkOrderInvoiceService service = new WorkOrderInvoiceService(transactionRepository, invoiceRenderer);
        service.exportInvoice(workOrder);

        verify(transactionRepository).findPartUsageForWorkOrder(tenantId, workOrderId);
        verify(invoiceRenderer).render(eq(workOrder), usageCaptor.capture());
        List<InventoryPartUsage> usage = usageCaptor.getValue();
        assertThat(usage).hasSize(1);
        assertThat(usage.getFirst().sparePart()).isSameAs(part);
        assertThat(usage.getFirst().quantity()).isEqualByComparingTo("2.000");
    }

    private static InventoryTransaction usage(SparePart part, InventoryTransactionType type, String quantity) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setSparePart(part);
        transaction.setTransactionType(type);
        transaction.setQuantity(new BigDecimal(quantity));
        return transaction;
    }

    private static WorkOrderResponse workOrder(UUID id) {
        return new WorkOrderResponse(
                id, "WO-INVOICE-RETURN", null, UUID.randomUUID(), "Customer", null, null, null, null,
                "Invoice return test", null, Priority.NORMAL, WorkOrderStatus.CLOSED,
                null, null, null, null, null, Instant.now(), List.of()
        );
    }

    private static void authenticateOwner(UUID tenantId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("owner")
                .claim("tenantId", tenantId.toString())
                .claim("userId", UUID.randomUUID().toString())
                .claim("roles", List.of("OWNER"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
