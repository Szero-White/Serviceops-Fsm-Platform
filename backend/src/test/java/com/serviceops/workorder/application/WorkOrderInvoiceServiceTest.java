package com.serviceops.workorder.application;

import com.serviceops.common.domain.Priority;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WorkOrderInvoiceServiceTest {

    private final InventoryTransactionRepository transactionRepository = mock(InventoryTransactionRepository.class);
    private final WorkOrderInvoiceHtmlRenderer invoiceRenderer = mock(WorkOrderInvoiceHtmlRenderer.class);
    private final WorkOrderInvoiceService service = new WorkOrderInvoiceService(transactionRepository, invoiceRenderer);

    @Test
    void invoiceExportShouldRejectEveryNonClosedStatusBeforeLoadingBillingData() {
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            if (status == WorkOrderStatus.CLOSED) {
                continue;
            }

            assertThatThrownBy(() -> service.exportInvoice(workOrder(status)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Chỉ có thể xuất hóa đơn cho phiếu công việc đã đóng");
        }

        verifyNoInteractions(transactionRepository, invoiceRenderer);
    }

    private static WorkOrderResponse workOrder(WorkOrderStatus status) {
        return new WorkOrderResponse(
                UUID.randomUUID(),
                "WO-INVOICE-TEST",
                null,
                UUID.randomUUID(),
                "Invoice Test Customer",
                null,
                null,
                null,
                null,
                "Invoice policy test",
                null,
                Priority.NORMAL,
                status,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                List.of()
        );
    }
}