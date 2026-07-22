package com.serviceops.asset.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.asset.web.AssetDtos.AssetRequest;
import com.serviceops.asset.web.AssetDtos.AssetResponse;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository repository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> search(String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.from(repository.search(CurrentUser.tenantId(), search == null ? "" : search.trim(), pageable).map(AssetService::toResponse));
    }

    @Transactional(readOnly = true)
    public AssetResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String serial = request.serialNumber().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTenantIdAndSerialNumberIgnoreCase(tenantId, serial)) {
            throw BusinessException.conflict("ASSET_SERIAL_EXISTS", "Số serial đã tồn tại");
        }
        Customer customer = customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomer(customer);
        apply(asset, request, serial);
        repository.save(asset);
        auditService.record("CREATE", "ASSET", asset.getId(), "Tạo thiết bị serial " + serial);
        return toResponse(asset);
    }

    @Transactional
    public AssetResponse update(UUID id, AssetRequest request) {
        Asset asset = require(id);
        String serial = request.serialNumber().trim().toUpperCase(Locale.ROOT);
        if (!asset.getSerialNumber().equalsIgnoreCase(serial) && repository.existsByTenantIdAndSerialNumberIgnoreCase(CurrentUser.tenantId(), serial)) {
            throw BusinessException.conflict("ASSET_SERIAL_EXISTS", "Số serial đã tồn tại");
        }
        Customer customer = customerRepository.findByIdAndTenantId(request.customerId(), CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
        asset.setCustomer(customer);
        apply(asset, request, serial);
        auditService.record("UPDATE", "ASSET", asset.getId(), "Cập nhật thiết bị serial " + serial);
        return toResponse(asset);
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

    public static AssetResponse toResponse(Asset a) {
        return new AssetResponse(a.getId(), a.getCustomer().getId(), a.getCustomer().getName(), a.getCategory(), a.getBrand(), a.getModel(), a.getSerialNumber(), a.getInstalledAt(), a.getWarrantyUntil(), a.isUnderWarranty(LocalDate.now()), a.getStatus(), a.getNotes(), a.getCreatedAt());
    }
}
