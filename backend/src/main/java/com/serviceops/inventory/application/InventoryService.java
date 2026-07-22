package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.InventoryDtos.ConsumePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name").ascending());
        return PageResponse.from(sparePartRepository.search(CurrentUser.tenantId(), search == null ? "" : search.trim(), pageable).map(InventoryService::toResponse));
    }

    @Transactional
    public SparePartResponse create(SparePartRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (sparePartRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, sku)) {
            throw BusinessException.conflict("SPARE_PART_SKU_EXISTS", "Mã phụ tùng đã tồn tại");
        }
        SparePart part = new SparePart();
        part.setTenantId(tenantId);
        part.setSku(sku);
        part.setName(request.name().trim());
        part.setUnit(request.unit().trim());
        part.setStockQuantity(BigDecimal.ZERO);
        part.setReorderLevel(request.reorderLevel());
        part.setUnitPrice(request.unitPrice());
        part.setActive(request.active() == null || request.active());
        sparePartRepository.save(part);
        if (request.initialStock().signum() > 0) {
            part.addStock(request.initialStock());
            saveTransaction(part, null, InventoryTransactionType.IMPORT, request.initialStock(), "Tồn đầu kỳ");
        }
        auditService.record("CREATE", "SPARE_PART", part.getId(), "Tạo phụ tùng " + sku);
        return toResponse(part);
    }

    @Transactional
    public SparePartResponse importStock(UUID id, StockAdjustmentRequest request) {
        SparePart part = requireLocked(id);
        part.addStock(request.quantity());
        saveTransaction(part, null, InventoryTransactionType.IMPORT, request.quantity(), request.note());
        auditService.record("IMPORT_STOCK", "SPARE_PART", part.getId(), "Nhập " + request.quantity() + " " + part.getUnit());
        return toResponse(part);
    }

    @Transactional
    public SparePartResponse consume(UUID workOrderId, ConsumePartRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workOrderRepository.findDetailed(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy work order"));
        if (CurrentUser.hasRole("TECHNICIAN")
                && (workOrder.getTechnician() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId()))) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được dùng phụ tùng cho công việc được phân công cho mình");
        }
        if (workOrder.getStatus() == WorkOrderStatus.CLOSED || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw BusinessException.conflict("WORK_ORDER_NOT_EDITABLE", "Không thể dùng phụ tùng cho work order đã đóng hoặc hủy");
        }
        SparePart part = requireLocked(request.sparePartId());
        try {
            part.consume(request.quantity());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("INVALID_QUANTITY", ex.getMessage());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INSUFFICIENT_STOCK", "Không đủ tồn kho cho phụ tùng " + part.getSku());
        }
        saveTransaction(part, workOrder, InventoryTransactionType.CONSUME, request.quantity(), request.note());
        auditService.record("CONSUME_PART", "WORK_ORDER", workOrder.getId(), "Dùng " + request.quantity() + " " + part.getUnit() + " - " + part.getSku());
        return toResponse(part);
    }

    private SparePart requireLocked(UUID id) {
        return sparePartRepository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
    }

    private void saveTransaction(SparePart part, WorkOrder workOrder, InventoryTransactionType type, BigDecimal quantity, String note) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setTenantId(part.getTenantId());
        tx.setSparePart(part);
        tx.setWorkOrder(workOrder);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setBalanceAfter(part.getStockQuantity());
        tx.setNote(note == null || note.isBlank() ? null : note.trim());
        tx.setCreatedBy(CurrentUser.username());
        transactionRepository.save(tx);
    }

    public static SparePartResponse toResponse(SparePart p) {
        return new SparePartResponse(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getStockQuantity(), p.getReorderLevel(), p.getUnitPrice(), p.getStockQuantity().compareTo(p.getReorderLevel()) <= 0, p.isActive(), p.getUpdatedAt());
    }
}
