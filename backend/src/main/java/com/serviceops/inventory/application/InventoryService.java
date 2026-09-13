package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.application.InventoryCsvService.SparePartCsvRow;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.InventoryDtos.InventoryTransactionResponse;
import com.serviceops.inventory.web.InventoryDtos.ReorderLevelRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartImportRowResult;
import com.serviceops.inventory.web.InventoryDtos.SparePartRequest;
import com.serviceops.inventory.web.InventoryDtos.SparePartResponse;
import com.serviceops.inventory.web.InventoryDtos.StockAdjustmentRequest;
import com.serviceops.inventory.web.InventoryDtos.StocktakeRequest;
import com.serviceops.inventory.web.InventoryDtos.StocktakeResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final Map<String, String> SPARE_PART_SORT_FIELDS = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("sku", "sku"),
            Map.entry("stockQuantity", "stockQuantity"),
            Map.entry("reorderLevel", "reorderLevel"),
            Map.entry("unitPrice", "unitPrice"),
            Map.entry("active", "active"),
            Map.entry("updatedAt", "updatedAt"),
            Map.entry("createdAt", "createdAt")
    );
    private static final Map<String, String> TRANSACTION_SORT_FIELDS = Map.ofEntries(
            Map.entry("createdAt", "createdAt"),
            Map.entry("type", "transactionType"),
            Map.entry("sparePartName", "sparePart.name"),
            Map.entry("quantity", "quantity"),
            Map.entry("balanceAfter", "balanceAfter"),
            Map.entry("workOrderCode", "workOrder.code"),
            Map.entry("recipientDisplayName", "recipientDisplayName"),
            Map.entry("actorDisplayName", "actorDisplayName"),
            Map.entry("note", "note")
    );
    private static final Instant INVENTORY_HISTORY_MIN_TIME = Instant.EPOCH;
    private static final Instant INVENTORY_HISTORY_MAX_TIME = Instant.parse("9999-12-31T23:59:59Z");
    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryCsvService csvService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String search, Boolean active, int page, int size) {
        return search(search, active, page, size, "createdAt", "desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String search, Boolean active, int page, int size, String sortBy, String sortDir) {
        var sort = PageRequestSupport.safeSort(sortBy, sortDir, SPARE_PART_SORT_FIELDS, "createdAt", Sort.Direction.DESC);
        var pageable = PageRequestSupport.of(page, size, sort);
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
    public PageResponse<InventoryTransactionResponse> searchTransactions(String search, List<InventoryTransactionType> type, Instant fromTime, Instant toTime, int page, int size) {
        return searchTransactions(search, type, fromTime, toTime, page, size, "createdAt", "desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> searchTransactions(String search,
                                                                          List<InventoryTransactionType> type,
                                                                          Instant fromTime,
                                                                          Instant toTime,
                                                                          int page,
                                                                          int size,
                                                                          String sortBy,
                                                                          String sortDir) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw BusinessException.badRequest("INVALID_TIME_RANGE", "Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        var sort = PageRequestSupport.safeSort(sortBy, sortDir, TRANSACTION_SORT_FIELDS, "createdAt", Sort.Direction.DESC);
        var pageable = PageRequestSupport.of(page, size, sort);
        List<InventoryTransactionType> types = type == null || type.isEmpty()
                ? List.of(InventoryTransactionType.values())
                : type;
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
            part.decreaseStock(adjustmentQuantity);
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
        List<SparePartImportSupport.Candidate> candidates = new ArrayList<>();
        List<SparePartImportRowResult> results = new ArrayList<>();

        for (SparePartCsvRow row : rows) {
            SparePartImportSupport.Candidate candidate = SparePartImportSupport.validate(row, seenSkus, tenantId, sparePartRepository);
            candidates.add(candidate);
            results.add(new SparePartImportRowResult(row.rowNumber(), row.sku(), row.name(), candidate.valid(), candidate.message()));
        }

        int validRows = (int) results.stream().filter(SparePartImportRowResult::valid).count();
        int errorRows = results.size() - validRows;
        if (!commit || errorRows > 0) {
            return new SparePartImportResult(rows.size(), validRows, errorRows, 0, false, results);
        }

        for (SparePartImportSupport.Candidate candidate : candidates) {
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

    private SparePart requireLocked(UUID id) {
        return sparePartRepository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
    }

    private void createImportedPart(UUID tenantId, SparePartImportSupport.Candidate candidate) {
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
            saveTransaction(part, null, InventoryTransactionType.IMPORT, candidate.initialStock(), "Nhập tồn ban đầu từ CSV");
        }
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
                tx.getRecipientUserId(), tx.getRecipientDisplayName(),
                tx.getActorDisplayName() == null || tx.getActorDisplayName().isBlank() ? tx.getCreatedBy() : tx.getActorDisplayName(),
                tx.getActorRole(),
                tx.getCreatedAt());
    }

}
