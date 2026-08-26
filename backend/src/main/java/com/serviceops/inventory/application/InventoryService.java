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
import com.serviceops.inventory.web.InventoryDtos.InventoryTransactionResponse;
import com.serviceops.inventory.web.InventoryDtos.ReturnablePartResponse;
import com.serviceops.inventory.web.InventoryDtos.ReturnPartRequest;
import com.serviceops.inventory.web.InventoryDtos.ReorderLevelRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportRowResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
import com.serviceops.inventory.web.InventoryDtos.StocktakeRequest;
import com.serviceops.inventory.web.InventoryDtos.StocktakeResponse;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final Instant INVENTORY_HISTORY_MIN_TIME = Instant.EPOCH;
    private static final Instant INVENTORY_HISTORY_MAX_TIME = Instant.parse("9999-12-31T23:59:59Z");
    private static final Set<WorkOrderStatus> PART_CONSUMPTION_ALLOWED_STATUSES = Set.of(
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.REOPENED
    );

    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final InventoryCsvService csvService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String search, Boolean active, int page, int size) {
        var pageable = PageRequestSupport.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(sparePartRepository.search(
                CurrentUser.tenantId(),
                active,
                PageRequestSupport.normalizeSearch(search),
                pageable
        ).map(InventoryService::toResponse));
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
    public SparePartResponse updateReorderLevel(UUID id, ReorderLevelRequest request) {
        SparePart part = requireLocked(id);
        BigDecimal previousLevel = part.getReorderLevel();
        BigDecimal newLevel = request.reorderLevel();

        if (previousLevel.compareTo(newLevel) == 0) {
            return toResponse(part);
        }

        part.setReorderLevel(newLevel);
        auditService.record(
                "UPDATE_REORDER_LEVEL",
                "SPARE_PART",
                part.getId(),
                "Cập nhật ngưỡng tồn tối thiểu " + part.getSku() + ": "
                        + previousLevel.stripTrailingZeros().toPlainString() + " -> "
                        + newLevel.stripTrailingZeros().toPlainString() + " " + part.getUnit()
        );

        eventPublisher.publishEvent(new InventoryReorderLevelChangedEvent(
                part.getTenantId(),
                part.getId(),
                part.getSku(),
                part.getName(),
                part.getUnit(),
                part.getStockQuantity(),
                previousLevel,
                newLevel,
                part.isActive(),
                CurrentUser.userId(),
                CurrentUser.displayName()
        ));

        return toResponse(part);
    }

    @Transactional
    public SparePartResponse importStock(UUID id, StockAdjustmentRequest request) {
        SparePart part = requireLocked(id);
        if (!part.isActive()) {
            throw BusinessException.conflict("SPARE_PART_INACTIVE", "Phụ tùng đã ngừng sử dụng và không thể nhập kho");
        }
        part.addStock(request.quantity());
        saveTransaction(part, null, InventoryTransactionType.IMPORT, request.quantity(), request.note());
        auditService.record("IMPORT_STOCK", "SPARE_PART", part.getId(), "Nhập " + request.quantity() + " " + part.getUnit());
        return toResponse(part);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> searchTransactions(String search,
                                                                          InventoryTransactionType type,
                                                                          Instant fromTime,
                                                                          Instant toTime,
                                                                          int page,
                                                                          int size) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw BusinessException.badRequest("INVALID_TIME_RANGE", "Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        List<InventoryTransactionType> types = type == null
                ? List.of(InventoryTransactionType.values())
                : List.of(type);
        Instant effectiveFromTime = fromTime == null ? INVENTORY_HISTORY_MIN_TIME : fromTime;
        Instant effectiveToTime = toTime == null ? INVENTORY_HISTORY_MAX_TIME : toTime;

        return PageResponse.from(transactionRepository.search(
                CurrentUser.tenantId(),
                types,
                PageRequestSupport.normalizeSearch(search),
                effectiveFromTime,
                effectiveToTime,
                pageable
        ).map(InventoryService::toTransactionResponse));
    }

    @Transactional
    public StocktakeResponse stocktake(UUID id, StocktakeRequest request) {
        SparePart part = requireLocked(id);
        BigDecimal systemQuantity = part.getStockQuantity();
        BigDecimal actualQuantity = request.actualQuantity();
        BigDecimal difference = actualQuantity.subtract(systemQuantity);
        InventoryTransactionType adjustmentType = null;

        if (difference.signum() > 0) {
            part.addStock(difference);
            adjustmentType = InventoryTransactionType.ADJUSTMENT_IN;
            saveTransaction(part, null, adjustmentType, difference, "Kiểm kê: " + request.reason().trim());
        } else if (difference.signum() < 0) {
            BigDecimal adjustmentQuantity = difference.abs();
            part.consume(adjustmentQuantity);
            adjustmentType = InventoryTransactionType.ADJUSTMENT_OUT;
            saveTransaction(part, null, adjustmentType, adjustmentQuantity, "Kiểm kê: " + request.reason().trim());
        }

        String reason = request.reason().trim();
        auditService.record("STOCKTAKE", "SPARE_PART", part.getId(),
                "Kiểm kê " + part.getSku() + ": " + systemQuantity + " -> " + actualQuantity
                        + "; lý do: " + reason);

        if (difference.signum() != 0) {
            eventPublisher.publishEvent(new InventoryStockAdjustedEvent(
                    part.getTenantId(),
                    part.getId(),
                    part.getSku(),
                    part.getName(),
                    part.getUnit(),
                    systemQuantity,
                    actualQuantity,
                    part.getReorderLevel(),
                    CurrentUser.userId(),
                    CurrentUser.displayName(),
                    reason
            ));
        }
        return new StocktakeResponse(toResponse(part), systemQuantity, actualQuantity, difference, adjustmentType);
    }

    @Transactional(readOnly = true)
    public ReturnablePartResponse getReturnablePart(UUID workOrderId, UUID sparePartId) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workOrderRepository.findDetailed(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (workOrder.getStatus() == WorkOrderStatus.CLOSED || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw BusinessException.conflict("WORK_ORDER_NOT_EDITABLE", "Không thể hoàn trả phụ tùng cho phiếu công việc đã đóng hoặc hủy");
        }
        SparePart part = sparePartRepository.findByIdAndTenantId(sparePartId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
        return toReturnableResponse(workOrder, part, netConsumedQuantity(tenantId, workOrderId, sparePartId));
    }

    @Transactional
    public ReturnablePartResponse returnPart(UUID workOrderId, UUID sparePartId, ReturnPartRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (workOrder.getStatus() == WorkOrderStatus.CLOSED || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw BusinessException.conflict("WORK_ORDER_NOT_EDITABLE", "Không thể hoàn trả phụ tùng cho phiếu công việc đã đóng hoặc hủy");
        }

        SparePart part = requireLocked(sparePartId);
        BigDecimal returnableBefore = netConsumedQuantity(tenantId, workOrderId, sparePartId);
        if (returnableBefore.signum() <= 0) {
            throw BusinessException.conflict("NO_PARTS_TO_RETURN", "Phiếu công việc không còn phụ tùng này để hoàn trả");
        }
        if (request.quantity().compareTo(returnableBefore) > 0) {
            throw BusinessException.conflict("RETURN_EXCEEDS_CONSUMED",
                    "Số lượng hoàn trả không được vượt quá " + returnableBefore.stripTrailingZeros().toPlainString() + " " + part.getUnit());
        }

        part.addStock(request.quantity());
        saveTransaction(part, workOrder, InventoryTransactionType.RETURN, request.quantity(), request.note());
        auditService.record("RETURN_PART", "WORK_ORDER", workOrder.getId(),
                "Hoàn trả " + request.quantity() + " " + part.getUnit() + " - " + part.getSku()
                        + "; lý do: " + request.note().trim());
        return toReturnableResponse(workOrder, part, returnableBefore.subtract(request.quantity()));
    }

    @Transactional(readOnly = true)
    public byte[] exportSpareParts(String search) {
        var pageable = PageRequest.of(0, 5_000, Sort.by("sku").ascending());
        List<SparePartResponse> parts = sparePartRepository.search(CurrentUser.tenantId(), null, PageRequestSupport.normalizeSearch(search), pageable)
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

        auditService.record("IMPORT_SPARE_PARTS", "SPARE_PART", null, "Import " + validRows + " phụ tùng từ CSV");
        return new SparePartImportResult(rows.size(), validRows, 0, validRows, true, results);
    }

    @Transactional
    public SparePartResponse setActive(UUID id, boolean active) {
        SparePart part = requireLocked(id);
        if (part.isActive() == active) {
            return toResponse(part);
        }

        part.setActive(active);
        auditService.record(
                active ? "REACTIVATE" : "DISCONTINUE",
                "SPARE_PART",
                part.getId(),
                (active ? "Kích hoạt lại phụ tùng " : "Ngừng sử dụng phụ tùng ") + part.getSku()
        );
        return toResponse(part);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = CurrentUser.tenantId();
        SparePart part = requireLocked(id);

        if (part.getStockQuantity().signum() != 0) {
            throw BusinessException.conflict(
                    "SPARE_PART_STOCK_NOT_ZERO",
                    "Chỉ có thể xóa phụ tùng khi tồn kho bằng 0"
            );
        }
        if (transactionRepository.existsByTenantIdAndSparePartId(tenantId, part.getId())) {
            throw BusinessException.conflict(
                    "SPARE_PART_HAS_HISTORY",
                    "Phụ tùng đã có lịch sử kho và không thể xóa; hãy chuyển sang Ngừng sử dụng"
            );
        }

        sparePartRepository.delete(part);
        auditService.record("DELETE", "SPARE_PART", id, "Xóa phụ tùng chưa phát sinh nghiệp vụ " + part.getSku());
    }

    @Transactional
    public SparePartResponse consume(UUID workOrderId, ConsumePartRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden(
                    "WORK_ORDER_PART_CONSUMPTION_FORBIDDEN",
                    "Chỉ kỹ thuật viên được phân công mới được ghi nhận phụ tùng đã dùng cho công việc"
            );
        }
        if (workOrder.getTechnician() == null
                || !workOrder.getTechnician().getUser().getId().equals(CurrentUser.userId())) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được dùng phụ tùng cho công việc được phân công cho mình");
        }
        if (!PART_CONSUMPTION_ALLOWED_STATUSES.contains(workOrder.getStatus())) {
            throw BusinessException.conflict(
                    "WORK_ORDER_PART_CONSUMPTION_NOT_ALLOWED",
                    "Chỉ có thể dùng phụ tùng khi công việc đang được thực hiện; phiếu đã hoàn thành, khách đã xác nhận, đã đóng hoặc đã hủy không được ghi nhận thêm phụ tùng"
            );
        }
        SparePart part = requireLocked(request.sparePartId());
        if (!part.isActive()) {
            throw BusinessException.conflict("SPARE_PART_INACTIVE", "Phụ tùng đã ngừng hoạt động và không thể xuất dùng");
        }
        BigDecimal stockBeforeConsumption = part.getStockQuantity();
        try {
            part.consume(request.quantity());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("INVALID_QUANTITY", ex.getMessage());
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("INSUFFICIENT_STOCK", "Không đủ tồn kho cho phụ tùng " + part.getSku());
        }
        saveTransaction(part, workOrder, InventoryTransactionType.CONSUME, request.quantity(), request.note());
        auditService.record("CONSUME_PART", "WORK_ORDER", workOrder.getId(), "Dùng " + request.quantity() + " " + part.getUnit() + " - " + part.getSku());
        notifyLowStockIfCrossed(part, stockBeforeConsumption, workOrder);
        return toResponse(part);
    }

    private BigDecimal netConsumedQuantity(UUID tenantId, UUID workOrderId, UUID sparePartId) {
        BigDecimal net = BigDecimal.ZERO;
        for (InventoryTransaction transaction : transactionRepository.findPartUsageForWorkOrderAndSparePart(tenantId, workOrderId, sparePartId)) {
            net = transaction.getTransactionType() == InventoryTransactionType.CONSUME
                    ? net.add(transaction.getQuantity())
                    : net.subtract(transaction.getQuantity());
        }
        return net.max(BigDecimal.ZERO);
    }

    private void notifyLowStockIfCrossed(SparePart part, BigDecimal previousStock, WorkOrder workOrder) {
        boolean wasLowStock = previousStock.compareTo(part.getReorderLevel()) <= 0;
        boolean isLowStock = part.isActive() && part.getStockQuantity().compareTo(part.getReorderLevel()) <= 0;
        if (!wasLowStock && isLowStock) {
            var copy = NotificationCopy.lowStock(
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
                    List.of(UserRole.OWNER, UserRole.WAREHOUSE_STAFF),
                    copy.title(),
                    copy.message()
            );
        }
    }

    private SparePart requireLocked(UUID id) {
        return sparePartRepository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
    }

    private SparePartImportCandidate validateImportRow(SparePartCsvRow row, Set<String> seenSkus, UUID tenantId) {
        String sku = row.sku().trim().toUpperCase(Locale.ROOT);
        if (sku.isBlank()) {
            return SparePartImportCandidate.invalid(row, "SKU không được để trống");
        }
        if (sku.length() > 60) {
            return SparePartImportCandidate.invalid(row, "SKU không được vượt quá 60 ký tự");
        }
        if (!seenSkus.add(sku)) {
            return SparePartImportCandidate.invalid(row, "SKU bi trung trong file import");
        }
        if (sparePartRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, sku)) {
            return SparePartImportCandidate.invalid(row, "SKU da ton tai trong he thong");
        }
        if (row.name().isBlank() || row.name().length() > 180) {
            return SparePartImportCandidate.invalid(row, "Tên phụ tùng bắt buộc và tối đa 180 ký tự");
        }
        if (row.unit().isBlank() || row.unit().length() > 30) {
            return SparePartImportCandidate.invalid(row, "Đơn vị bắt buộc và tối đa 30 ký tự");
        }

        try {
            BigDecimal initialStock = parseNonNegative(row.initialStock(), "Ton ban dau");
            BigDecimal reorderLevel = parseNonNegative(row.reorderLevel(), "Ngưỡng tồn tối thiểu");
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
                throw new IllegalArgumentException(label + " không được âm");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " không đúng định dạng số");
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
        tx.setActorDisplayName(CurrentUser.displayName());
        tx.setActorRole(CurrentUser.primaryRole());
        transactionRepository.save(tx);
    }

    public static SparePartResponse toResponse(SparePart p) {
        return new SparePartResponse(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getStockQuantity(), p.getReorderLevel(), p.getUnitPrice(), p.getStockQuantity().compareTo(p.getReorderLevel()) <= 0, p.isActive(), p.getUpdatedAt());
    }

    private static InventoryTransactionResponse toTransactionResponse(InventoryTransaction tx) {
        WorkOrder workOrder = tx.getWorkOrder();
        SparePart part = tx.getSparePart();
        return new InventoryTransactionResponse(
                tx.getId(), tx.getTransactionType(), part.getId(), part.getSku(), part.getName(), part.getUnit(),
                tx.getQuantity(), tx.getBalanceAfter(),
                workOrder == null ? null : workOrder.getId(),
                workOrder == null ? null : workOrder.getCode(),
                workOrder == null ? null : workOrder.getSummary(),
                tx.getNote(), tx.getCreatedBy(),
                tx.getActorDisplayName() == null || tx.getActorDisplayName().isBlank() ? tx.getCreatedBy() : tx.getActorDisplayName(),
                tx.getActorRole(),
                tx.getCreatedAt());
    }

    private static ReturnablePartResponse toReturnableResponse(WorkOrder workOrder, SparePart part, BigDecimal quantity) {
        return new ReturnablePartResponse(workOrder.getId(), workOrder.getCode(), part.getId(), part.getSku(),
                part.getName(), part.getUnit(), quantity);
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
