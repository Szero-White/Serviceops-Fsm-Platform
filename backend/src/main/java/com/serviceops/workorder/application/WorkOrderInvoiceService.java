package com.serviceops.workorder.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryPartUsage;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        List<InventoryPartUsage> consumedParts = netPartUsage(transactionRepository.findPartUsageForWorkOrder(
                CurrentUser.tenantId(), workOrder.id()));
        return invoiceRenderer.render(workOrder, consumedParts).getBytes(StandardCharsets.UTF_8);
    }

    private static List<InventoryPartUsage> netPartUsage(List<InventoryTransaction> transactions) {
        Map<UUID, SparePart> parts = new LinkedHashMap<>();
        Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
        for (InventoryTransaction transaction : transactions) {
            UUID partId = transaction.getSparePart().getId();
            parts.putIfAbsent(partId, transaction.getSparePart());
            BigDecimal signed = transaction.getTransactionType() == InventoryTransactionType.CONSUME
                    ? transaction.getQuantity()
                    : transaction.getQuantity().negate();
            quantities.merge(partId, signed, BigDecimal::add);
        }
        return quantities.entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0)
                .map(entry -> new InventoryPartUsage(parts.get(entry.getKey()), entry.getValue()))
                .toList();
    }
}
