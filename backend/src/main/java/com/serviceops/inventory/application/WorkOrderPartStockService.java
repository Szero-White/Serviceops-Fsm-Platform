package com.serviceops.inventory.application;

import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartStockService {
    private final InventoryTransactionRepository transactionRepository;
    private final WorkOrderPartUsageRepository usageRepository;
    private final NotificationService notificationService;

    public void issue(SparePart part, WorkOrder workOrder, BigDecimal quantity, String note) {
        BigDecimal stockBeforeIssue = part.getStockQuantity();
        try {
            part.consume(quantity);
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Không đủ tồn kho để cấp " + formatQuantity(quantity) + " " + part.getUnit() + " - " + part.getSku()
            );
        }
        saveTransaction(part, workOrder, InventoryTransactionType.ISSUE, quantity, note);
        notifyLowStockIfCrossed(part, stockBeforeIssue, workOrder);
    }

    public void returnToStock(SparePart part, WorkOrder workOrder, BigDecimal quantity, String note) {
        part.addStock(quantity);
        saveTransaction(part, workOrder, InventoryTransactionType.RETURN, quantity, note);
    }

    public PartStockTotals totals(UUID tenantId, UUID workOrderId, UUID sparePartId) {
        List<InventoryTransaction> transactions = transactionRepository
                .findWorkflowPartTransactionsForWorkOrderAndSparePart(tenantId, workOrderId, sparePartId);
        BigDecimal issued = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        Instant firstIssueAt = null;

        for (InventoryTransaction transaction : transactions) {
            if (transaction.getTransactionType() == InventoryTransactionType.ISSUE) {
                issued = issued.add(transaction.getQuantity());
                if (firstIssueAt == null || transaction.getCreatedAt().isBefore(firstIssueAt)) {
                    firstIssueAt = transaction.getCreatedAt();
                }
            }
        }
        if (firstIssueAt != null) {
            for (InventoryTransaction transaction : transactions) {
                if (transaction.getTransactionType() == InventoryTransactionType.RETURN
                        && !transaction.getCreatedAt().isBefore(firstIssueAt)) {
                    returned = returned.add(transaction.getQuantity());
                }
            }
        }
        return new PartStockTotals(issued, returned);
    }

    public Map<UUID, PartStockSummary> summariesForWorkOrder(UUID tenantId, UUID workOrderId) {
        List<InventoryTransaction> transactions = transactionRepository
                .findWorkflowPartTransactionsForWorkOrder(tenantId, workOrderId);
        Map<UUID, PartStockSummaryBuilder> builders = new LinkedHashMap<>();

        for (InventoryTransaction transaction : transactions) {
            PartStockSummaryBuilder builder = builders.computeIfAbsent(
                    transaction.getSparePart().getId(),
                    ignored -> new PartStockSummaryBuilder(transaction.getSparePart())
            );
            if (transaction.getTransactionType() == InventoryTransactionType.ISSUE) {
                builder.issued = builder.issued.add(transaction.getQuantity());
                if (builder.firstIssueAt == null || transaction.getCreatedAt().isBefore(builder.firstIssueAt)) {
                    builder.firstIssueAt = transaction.getCreatedAt();
                }
            }
        }

        for (InventoryTransaction transaction : transactions) {
            PartStockSummaryBuilder builder = builders.get(transaction.getSparePart().getId());
            if (transaction.getTransactionType() == InventoryTransactionType.RETURN
                    && builder != null
                    && builder.firstIssueAt != null
                    && !transaction.getCreatedAt().isBefore(builder.firstIssueAt)) {
                builder.returned = builder.returned.add(transaction.getQuantity());
            }
        }

        Map<UUID, PartStockSummary> summaries = new LinkedHashMap<>();
        builders.forEach((partId, builder) -> summaries.put(
                partId,
                new PartStockSummary(builder.part, builder.issued, builder.returned)
        ));
        return summaries;
    }

    public BigDecimal returnableQuantity(UUID tenantId, UUID workOrderId, UUID sparePartId) {
        PartStockTotals totals = totals(tenantId, workOrderId, sparePartId);
        if (totals.issued().signum() > 0) {
            BigDecimal used = usageRepository.findByTenantIdAndWorkOrderIdAndSparePartId(tenantId, workOrderId, sparePartId)
                    .map(WorkOrderPartUsage::getUsedQuantity)
                    .orElse(BigDecimal.ZERO);
            return totals.issued().subtract(used).subtract(totals.returned()).max(BigDecimal.ZERO);
        }

        BigDecimal legacyReturnable = BigDecimal.ZERO;
        for (InventoryTransaction transaction : transactionRepository.findPartUsageForWorkOrderAndSparePart(
                tenantId, workOrderId, sparePartId)) {
            legacyReturnable = transaction.getTransactionType() == InventoryTransactionType.CONSUME
                    ? legacyReturnable.add(transaction.getQuantity())
                    : legacyReturnable.subtract(transaction.getQuantity());
        }
        return legacyReturnable.max(BigDecimal.ZERO);
    }

    static String formatQuantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void saveTransaction(
            SparePart part,
            WorkOrder workOrder,
            InventoryTransactionType type,
            BigDecimal quantity,
            String note
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTenantId(part.getTenantId());
        transaction.setSparePart(part);
        transaction.setWorkOrder(workOrder);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setBalanceAfter(part.getStockQuantity());
        transaction.setNote(note == null || note.isBlank() ? null : note.trim());
        transaction.setCreatedBy(CurrentUser.username());
        transaction.setActorDisplayName(CurrentUser.displayName());
        transaction.setActorRole(CurrentUser.primaryRole());
        transactionRepository.save(transaction);
    }

    private void notifyLowStockIfCrossed(SparePart part, BigDecimal previousStock, WorkOrder workOrder) {
        boolean wasLowStock = previousStock.compareTo(part.getReorderLevel()) <= 0;
        boolean isLowStock = part.isActive() && part.getStockQuantity().compareTo(part.getReorderLevel()) <= 0;
        if (!wasLowStock && isLowStock) {
            var copy = NotificationCopy.lowStockAfterIssue(
                    part.getSku(),
                    part.getName(),
                    part.getStockQuantity(),
                    part.getUnit(),
                    part.getReorderLevel(),
                    workOrder.getCode(),
                    CurrentUser.displayName()
            );
            notificationService.notifyRolesIncludingCurrentUser(
                    part.getTenantId(),
                    List.of(UserRole.WAREHOUSE_STAFF),
                    copy.title(),
                    copy.message()
            );
        }
    }

    public record PartStockTotals(BigDecimal issued, BigDecimal returned) {
    }

    public record PartStockSummary(SparePart part, BigDecimal issued, BigDecimal returned) {
    }

    private static final class PartStockSummaryBuilder {
        private final SparePart part;
        private BigDecimal issued = BigDecimal.ZERO;
        private BigDecimal returned = BigDecimal.ZERO;
        private Instant firstIssueAt;

        private PartStockSummaryBuilder(SparePart part) {
            this.part = part;
        }
    }
}
