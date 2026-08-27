package com.serviceops.asset.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.asset.application.AssetCsvService.AssetCsvRow;
import com.serviceops.asset.web.AssetDtos.AssetImportResult;
import com.serviceops.asset.web.AssetDtos.AssetImportRowResult;
import com.serviceops.asset.web.AssetDtos.AssetRequest;
import com.serviceops.asset.web.AssetDtos.AssetResponse;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository repository;
    private final CustomerRepository customerRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AttachmentRepository attachmentRepository;
    private final AssetCsvService csvService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> search(String search, UUID customerId, int page, int size) {
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(repository.search(
                CurrentUser.tenantId(),
                customerId,
                PageRequestSupport.normalizeSearch(search),
                pageable
        ).map(AssetService::toResponse));
    }

    @Transactional(readOnly = true)
    public AssetResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String serial = normalizeSerial(request.serialNumber());
        if (serial != null && repository.existsByTenantIdAndSerialNumberIgnoreCase(tenantId, serial)) {
            throw BusinessException.conflict("ASSET_SERIAL_EXISTS", "Số serial đã tồn tại");
        }
        Customer customer = requireCustomer(request.customerId(), tenantId);
        requireActiveCustomerForNewAsset(customer);
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomer(customer);
        apply(asset, request, serial);
        repository.save(asset);
        String label = assetDisplayLabel(asset);
        auditService.record("CREATE", "ASSET", asset.getId(), "Tạo thiết bị " + label);
        return toResponse(asset);
    }

    @Transactional
    public AssetResponse update(UUID id, AssetRequest request) {
        Asset asset = require(id);
        String serial = normalizeSerial(request.serialNumber());
        String currentSerial = asset.getSerialNumber();
        boolean serialChanged = currentSerial == null ? serial != null : serial == null || !currentSerial.equalsIgnoreCase(serial);
        if (serialChanged && serial != null
                && repository.existsByTenantIdAndSerialNumberIgnoreCase(CurrentUser.tenantId(), serial)) {
            throw BusinessException.conflict("ASSET_SERIAL_EXISTS", "Số serial đã tồn tại");
        }
        UUID tenantId = CurrentUser.tenantId();
        Customer customer = requireCustomer(request.customerId(), tenantId);
        boolean customerChanged = !asset.getCustomer().getId().equals(customer.getId());
        if (customerChanged && !customer.isActive()) {
            throw BusinessException.conflict(
                    "CUSTOMER_INACTIVE",
                    "Khách hàng đã ngừng hoạt động, không thể đăng ký thiết bị mới"
            );
        }
        if (customerChanged) {
            long serviceRequestCount = serviceRequestRepository.countByTenantIdAndAssetId(tenantId, id);
            long workOrderCount = workOrderRepository.countByTenantIdAndAssetId(tenantId, id);
            if (serviceRequestCount > 0 || workOrderCount > 0) {
                throw BusinessException.conflict(
                        "ASSET_CUSTOMER_CHANGE_BLOCKED",
                        "Không thể đổi khách hàng của thiết bị đã có lịch sử yêu cầu hoặc phiếu công việc"
                );
            }
        }
        asset.setCustomer(customer);
        apply(asset, request, serial);
        String label = assetDisplayLabel(asset);
        auditService.record("UPDATE", "ASSET", asset.getId(), "Cập nhật thiết bị " + label);
        return toResponse(asset);
    }

    @Transactional
    public void delete(UUID id) {
        Asset asset = require(id);
        UUID tenantId = CurrentUser.tenantId();
        long serviceRequestCount = serviceRequestRepository.countByTenantIdAndAssetId(tenantId, id);
        long workOrderCount = workOrderRepository.countByTenantIdAndAssetId(tenantId, id);
        if (serviceRequestCount > 0 || workOrderCount > 0) {
            throw BusinessException.conflict("ASSET_IN_USE", "Không thể xóa thiết bị đang được sử dụng");
        }
        if (attachmentRepository.existsByTenantIdAndReferenceTypeAndReferenceId(tenantId, "ASSET", id)) {
            throw BusinessException.conflict(
                    "ASSET_HAS_ATTACHMENTS",
                    "Không thể xóa thiết bị khi còn file đính kèm; hãy xóa file đính kèm trước"
            );
        }
        String label = assetDisplayLabel(asset);
        repository.delete(asset);
        auditService.record("DELETE", "ASSET", asset.getId(), "Xóa thiết bị " + label);
    }

    @Transactional(readOnly = true)
    public byte[] exportAssets(String search) {
        var pageable = PageRequest.of(0, 5_000, Sort.by("serialNumber").ascending());
        List<AssetResponse> assets = repository.search(CurrentUser.tenantId(), null, PageRequestSupport.normalizeSearch(search), pageable)
                .stream()
                .map(AssetService::toResponse)
                .toList();
        return csvService.exportAssets(assets);
    }

    public byte[] assetImportTemplate() {
        return csvService.assetTemplate();
    }

    @Transactional
    public AssetImportResult importAssets(MultipartFile file, boolean commit) {
        UUID tenantId = CurrentUser.tenantId();
        List<AssetCsvRow> rows = csvService.parseAssets(file);
        Set<String> seenSerials = new HashSet<>();
        List<AssetImportCandidate> candidates = new ArrayList<>();
        List<AssetImportRowResult> results = new ArrayList<>();

        for (AssetCsvRow row : rows) {
            AssetImportCandidate candidate = validateImportRow(row, seenSerials, tenantId);
            candidates.add(candidate);
            results.add(new AssetImportRowResult(row.rowNumber(), row.serialNumber(), row.customerCode(), candidate.valid(), candidate.message()));
        }

        int validRows = (int) results.stream().filter(AssetImportRowResult::valid).count();
        int errorRows = results.size() - validRows;
        if (!commit || errorRows > 0) {
            return new AssetImportResult(rows.size(), validRows, errorRows, 0, false, results);
        }

        for (AssetImportCandidate candidate : candidates) {
            createImportedAsset(tenantId, candidate);
        }

        auditService.record("IMPORT_ASSETS", "ASSET", null, "Import " + validRows + " thiết bị từ CSV");
        return new AssetImportResult(rows.size(), validRows, 0, validRows, true, results);
    }

    private Customer requireCustomer(UUID customerId, UUID tenantId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
    }

    private static void requireActiveCustomerForNewAsset(Customer customer) {
        if (!customer.isActive()) {
            throw BusinessException.conflict(
                    "CUSTOMER_INACTIVE",
                    "Khách hàng đã ngừng hoạt động, không thể đăng ký thiết bị mới"
            );
        }
    }

    private Asset require(UUID id) {
        return repository.findDetailed(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("ASSET_NOT_FOUND", "Không tìm thấy thiết bị"));
    }

    private static void apply(Asset asset, AssetRequest request, String serial) {
        asset.setCategory(request.category().trim());
        asset.setBrand(blankToNull(request.brand()));
        asset.setModel(blankToNull(request.model()));
        asset.setSerialNumber(serial);
        asset.setInstalledAt(request.installedAt());
        asset.setWarrantyUntil(request.warrantyUntil());
        asset.setStatus(request.status() == null ? AssetStatus.ACTIVE : request.status());
        asset.setNotes(blankToNull(request.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeSerial(String value) {
        String serial = blankToNull(value);
        return serial == null ? null : serial.toUpperCase(Locale.ROOT);
    }

    private static String assetDisplayLabel(Asset asset) {
        String equipmentName = ((asset.getBrand() == null ? "" : asset.getBrand() + " ")
                + (asset.getModel() == null ? "" : asset.getModel())).trim();
        if (equipmentName.isBlank()) {
            equipmentName = asset.getCategory();
        }
        String serial = asset.getSerialNumber() == null ? "Chưa xác định serial" : asset.getSerialNumber();
        return equipmentName + " · " + serial;
    }

    private AssetImportCandidate validateImportRow(AssetCsvRow row, Set<String> seenSerials, UUID tenantId) {
        String customerCode = row.customerCode().trim().toUpperCase(Locale.ROOT);
        if (customerCode.isBlank()) {
            return AssetImportCandidate.invalid(row, "Mã khách hàng không được để trống");
        }
        Customer customer = customerRepository.findByTenantIdAndCodeIgnoreCase(tenantId, customerCode).orElse(null);
        if (customer == null) {
            return AssetImportCandidate.invalid(row, "Không tìm thấy khách hàng theo mã " + customerCode);
        }
        if (!customer.isActive()) {
            return AssetImportCandidate.invalid(row, "Khách hàng " + customerCode + " đã ngừng hoạt động");
        }

        String serial = row.serialNumber().trim().toUpperCase(Locale.ROOT);
        if (serial.isBlank()) {
            return AssetImportCandidate.invalid(row, "Serial không được để trống");
        }
        if (serial.length() > 120) {
            return AssetImportCandidate.invalid(row, "Serial tối đa 120 ký tự");
        }
        if (!seenSerials.add(serial)) {
            return AssetImportCandidate.invalid(row, "Serial bị trùng trong file import");
        }
        if (repository.existsByTenantIdAndSerialNumberIgnoreCase(tenantId, serial)) {
            return AssetImportCandidate.invalid(row, "Serial đã tồn tại trong hệ thống");
        }
        if (row.category().isBlank() || row.category().length() > 80) {
            return AssetImportCandidate.invalid(row, "Loại thiết bị bắt buộc và tối đa 80 ký tự");
        }
        if (row.brand().length() > 100 || row.model().length() > 100) {
            return AssetImportCandidate.invalid(row, "Hãng/model tối đa 100 ký tự");
        }
        if (row.notes().length() > 2000) {
            return AssetImportCandidate.invalid(row, "Ghi chú tối đa 2000 ký tự");
        }

        try {
            LocalDate installedAt = parseDate(row.installedAt());
            LocalDate warrantyUntil = parseDate(row.warrantyUntil());
            AssetStatus status = row.status().isBlank() ? AssetStatus.ACTIVE : AssetStatus.valueOf(row.status().trim().toUpperCase(Locale.ROOT));
            return new AssetImportCandidate(row, customer, row.category().trim(), serial, installedAt, warrantyUntil, status, true, "Hop le");
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            return AssetImportCandidate.invalid(row, "Ngày hoặc trạng thái không hợp lệ");
        }
    }

    private void createImportedAsset(UUID tenantId, AssetImportCandidate candidate) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomer(candidate.customer());
        asset.setCategory(candidate.category());
        asset.setBrand(blankToNull(candidate.row().brand()));
        asset.setModel(blankToNull(candidate.row().model()));
        asset.setSerialNumber(candidate.serialNumber());
        asset.setInstalledAt(candidate.installedAt());
        asset.setWarrantyUntil(candidate.warrantyUntil());
        asset.setStatus(candidate.status());
        asset.setNotes(blankToNull(candidate.row().notes()));
        repository.save(asset);
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }


    public static AssetResponse toResponse(Asset a) {
        return new AssetResponse(a.getId(), a.getCustomer().getId(), a.getCustomer().getName(), a.getCategory(), a.getBrand(), a.getModel(), a.getSerialNumber(), a.getInstalledAt(), a.getWarrantyUntil(), a.isUnderWarranty(LocalDate.now()), a.getStatus(), a.getNotes(), a.getCreatedAt());
    }

    private record AssetImportCandidate(
            AssetCsvRow row,
            Customer customer,
            String category,
            String serialNumber,
            LocalDate installedAt,
            LocalDate warrantyUntil,
            AssetStatus status,
            boolean valid,
            String message
    ) {
        static AssetImportCandidate invalid(AssetCsvRow row, String message) {
            return new AssetImportCandidate(row, null, row.category(), row.serialNumber(), null, null, AssetStatus.ACTIVE, false, message);
        }
    }
}
