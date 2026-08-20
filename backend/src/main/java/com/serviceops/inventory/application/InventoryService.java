package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.application.InventoryCsvService.SparePartCsvRow;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.InventoryDtos.ConsumePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportRowResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final InventoryCsvService csvService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String search, int page, int size) {
        var pageable = PageRequestSupport.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(sparePartRepository.search(CurrentUser.tenantId(), PageRequestSupport.normalizeSearch(search), pageable).map(InventoryService::toResponse));
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
        notificationService.notifyRoles(tenantId, warehouseRoles(), "Phụ tùng mới: " + part.getSku(), part.getName());
        return toResponse(part);
    }

    @Transactional
    public SparePartResponse importStock(UUID id, StockAdjustmentRequest request) {
        SparePart part = requireLocked(id);
        part.addStock(request.quantity());
        saveTransaction(part, null, InventoryTransactionType.IMPORT, request.quantity(), request.note());
        auditService.record("IMPORT_STOCK", "SPARE_PART", part.getId(), "Nhập " + request.quantity() + " " + part.getUnit());
        notificationService.notifyRoles(part.getTenantId(), warehouseRoles(), "Đã nhập kho " + part.getSku(), request.quantity() + " " + part.getUnit() + " - " + part.getName());
        return toResponse(part);
    }

    @Transactional(readOnly = true)
    public byte[] exportSpareParts(String search) {
        var pageable = PageRequest.of(0, 5_000, Sort.by("sku").ascending());
        List<SparePartResponse> parts = sparePartRepository.search(CurrentUser.tenantId(), PageRequestSupport.normalizeSearch(search), pageable)
                .stream()
                .map(InventoryService::toResponse)
                .toList();
        return csvService.exportSpareParts(parts);
    }

    public byte[] sparePartImportTemplate() {
        return csvService.sparePartTemplate();
    }

    @Transactional
    public SparePartImportResult importSpareParts(MultipartFile file, boolean commit) {
        UUID tenantId = CurrentUser.tenantId();
        List<SparePartCsvRow> rows = csvService.parseSpareParts(file);
        Set<String> seenSkus = new HashSet<>();
        List<SparePartImportCandidate> candidates = new ArrayList<>();
        List<SparePartImportRowResult> results = new ArrayList<>();

        for (SparePartCsvRow row : rows) {
            SparePartImportCandidate candidate = validateImportRow(row, seenSkus, tenantId);
            candidates.add(candidate);
            results.add(new SparePartImportRowResult(row.rowNumber(), row.sku(), row.name(), candidate.valid(), candidate.message()));
        }

        int validRows = (int) results.stream().filter(SparePartImportRowResult::valid).count();
        int errorRows = results.size() - validRows;
        if (!commit || errorRows > 0) {
            return new SparePartImportResult(rows.size(), validRows, errorRows, 0, false, results);
        }

        for (SparePartImportCandidate candidate : candidates) {
            createImportedPart(tenantId, candidate);
        }

        auditService.record("IMPORT_SPARE_PARTS", "SPARE_PART", null, "Import " + validRows + " phu tung tu CSV");
        notificationService.notifyRoles(tenantId, warehouseRoles(), "Da import danh muc phu tung", validRows + " SKU moi duoc them vao kho");
        return new SparePartImportResult(rows.size(), validRows, 0, validRows, true, results);
    }

    @Transactional
    public SparePartResponse consume(UUID workOrderId, ConsumePartRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (CurrentUser.hasRole("TECHNICIAN")
                && (workOrder.getTechnician() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId()))) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được dùng phụ tùng cho công việc được phân công cho mình");
        }
        if (workOrder.getStatus() == WorkOrderStatus.CLOSED || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw BusinessException.conflict("WORK_ORDER_NOT_EDITABLE", "Không thể dùng phụ tùng cho phiếu công việc đã đóng hoặc hủy");
        }
        SparePart part = requireLocked(request.sparePartId());
        if (!part.isActive()) {
            throw BusinessException.conflict("SPARE_PART_INACTIVE", "Phụ tùng đã ngừng hoạt động và không thể xuất dùng");
        }
        try {
            part.consume(request.quantity());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("INVALID_QUANTITY", ex.getMessage());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INSUFFICIENT_STOCK", "Không đủ tồn kho cho phụ tùng " + part.getSku());
        }
        saveTransaction(part, workOrder, InventoryTransactionType.CONSUME, request.quantity(), request.note());
        auditService.record("CONSUME_PART", "WORK_ORDER", workOrder.getId(), "Dùng " + request.quantity() + " " + part.getUnit() + " - " + part.getSku());
        if (part.getStockQuantity().compareTo(part.getReorderLevel()) <= 0) {
            notificationService.notifyRolesIncludingCurrentUser(tenantId, warehouseRoles(), "Phụ tùng sắp hết: " + part.getSku(), part.getName() + " còn " + part.getStockQuantity() + " " + part.getUnit());
        }
        return toResponse(part);
    }

    private SparePart requireLocked(UUID id) {
        return sparePartRepository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
    }

    private SparePartImportCandidate validateImportRow(SparePartCsvRow row, Set<String> seenSkus, UUID tenantId) {
        String sku = row.sku().trim().toUpperCase(Locale.ROOT);
        if (sku.isBlank()) {
            return SparePartImportCandidate.invalid(row, "SKU khong duoc de trong");
        }
        if (sku.length() > 60) {
            return SparePartImportCandidate.invalid(row, "SKU khong duoc vuot qua 60 ky tu");
        }
        if (!seenSkus.add(sku)) {
            return SparePartImportCandidate.invalid(row, "SKU bi trung trong file import");
        }
        if (sparePartRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, sku)) {
            return SparePartImportCandidate.invalid(row, "SKU da ton tai trong he thong");
        }
        if (row.name().isBlank() || row.name().length() > 180) {
            return SparePartImportCandidate.invalid(row, "Ten phu tung bat buoc va toi da 180 ky tu");
        }
        if (row.unit().isBlank() || row.unit().length() > 30) {
            return SparePartImportCandidate.invalid(row, "Don vi bat buoc va toi da 30 ky tu");
        }

        try {
            BigDecimal initialStock = parseNonNegative(row.initialStock(), "Ton ban dau");
            BigDecimal reorderLevel = parseNonNegative(row.reorderLevel(), "Muc dat hang lai");
            BigDecimal unitPrice = parseNonNegative(row.unitPrice(), "Don gia");
            boolean active = parseBoolean(row.active());
            return new SparePartImportCandidate(row, sku, row.name().trim(), row.unit().trim(), initialStock, reorderLevel, unitPrice, active, true, "Hop le");
        } catch (IllegalArgumentException ex) {
            return SparePartImportCandidate.invalid(row, ex.getMessage());
        }
    }

    private void createImportedPart(UUID tenantId, SparePartImportCandidate candidate) {
        SparePart part = new SparePart();
        part.setTenantId(tenantId);
        part.setSku(candidate.sku());
        part.setName(candidate.name());
        part.setUnit(candidate.unit());
        part.setStockQuantity(BigDecimal.ZERO);
        part.setReorderLevel(candidate.reorderLevel());
        part.setUnitPrice(candidate.unitPrice());
        part.setActive(candidate.active());
        sparePartRepository.save(part);
        if (candidate.initialStock().signum() > 0) {
            part.addStock(candidate.initialStock());
            saveTransaction(part, null, InventoryTransactionType.IMPORT, candidate.initialStock(), "Import ton ban dau tu CSV");
        }
    }

    private static BigDecimal parseNonNegative(String value, String label) {
        try {
            BigDecimal parsed = new BigDecimal(value == null || value.isBlank() ? "0" : value.trim());
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException(label + " khong duoc am");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " khong dung dinh dang so");
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Cot active chi nhan true hoac false");
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

    private static List<UserRole> warehouseRoles() {
        return List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF);
    }

    public static SparePartResponse toResponse(SparePart p) {
        return new SparePartResponse(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getStockQuantity(), p.getReorderLevel(), p.getUnitPrice(), p.getStockQuantity().compareTo(p.getReorderLevel()) <= 0, p.isActive(), p.getUpdatedAt());
    }

    private record SparePartImportCandidate(
            SparePartCsvRow row,
            String sku,
            String name,
            String unit,
            BigDecimal initialStock,
            BigDecimal reorderLevel,
            BigDecimal unitPrice,
            boolean active,
            boolean valid,
            String message
    ) {
        static SparePartImportCandidate invalid(SparePartCsvRow row, String message) {
            return new SparePartImportCandidate(row, row.sku(), row.name(), row.unit(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false, message);
        }
    }
}
