package com.serviceops.workorder.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderInvoiceService {
    private final InventoryTransactionRepository transactionRepository;
    private final WorkOrderInvoiceHtmlRenderer invoiceRenderer;

    public byte[] exportInvoice(WorkOrderResponse workOrder) {
        if (workOrder.status() != WorkOrderStatus.CLOSED) {
            throw BusinessException.conflict(
                    "WORK_ORDER_INVOICE_NOT_AVAILABLE",
                    "Chỉ có thể xuất hóa đơn cho phiếu công việc đã đóng"
            );
        }

        List<InventoryTransaction> consumedParts = transactionRepository.findConsumedPartsForWorkOrder(
                CurrentUser.tenantId(), workOrder.id());
        return invoiceRenderer.render(workOrder, consumedParts).getBytes(StandardCharsets.UTF_8);
    }
}
