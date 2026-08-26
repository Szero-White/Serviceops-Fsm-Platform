package com.serviceops.customer.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.customer.application.CustomerCsvService.CustomerCsvRow;
import com.serviceops.customer.web.CustomerDtos.CustomerImportResult;
import com.serviceops.customer.web.CustomerDtos.CustomerImportRowResult;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.customer.web.CustomerDtos.CustomerRequest;
import com.serviceops.customer.web.CustomerDtos.CustomerResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CustomerCsvService csvService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String search, Boolean active, int page, int size) {
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(repository.search(CurrentUser.tenantId(), active, PageRequestSupport.normalizeSearch(search), pageable).map(CustomerService::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw BusinessException.conflict("CUSTOMER_CODE_EXISTS", "Mã khách hàng đã tồn tại");
        }
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        apply(customer, request, code);
        repository.save(customer);
        auditService.record("CREATE", "CUSTOMER", customer.getId(), "Tạo khách hàng " + customer.getCode());
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = require(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!customer.getCode().equalsIgnoreCase(code) && repository.existsByTenantIdAndCodeIgnoreCase(CurrentUser.tenantId(), code)) {
            throw BusinessException.conflict("CUSTOMER_CODE_EXISTS", "Mã khách hàng đã tồn tại");
        }
        apply(customer, request, code);
        auditService.record("UPDATE", "CUSTOMER", customer.getId(), "Cập nhật khách hàng " + customer.getCode());
        return toResponse(customer);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = require(id);
        UUID tenantId = CurrentUser.tenantId();
        long assetCount = assetRepository.countByTenantIdAndCustomerId(tenantId, id);
        long requestCount = serviceRequestRepository.countByTenantIdAndCustomerId(tenantId, id);
        long workOrderCount = workOrderRepository.countByTenantIdAndCustomerId(tenantId, id);
        if (assetCount > 0 || requestCount > 0 || workOrderCount > 0) {
            throw BusinessException.conflict("CUSTOMER_IN_USE", "Không thể xóa khách hàng đang được sử dụng");
        }
        repository.delete(customer);
        auditService.record("DELETE", "CUSTOMER", customer.getId(), "Xóa khách hàng " + customer.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomers(String search) {
        var pageable = PageRequest.of(0, 5_000, Sort.by("code").ascending());
        List<CustomerResponse> customers = repository.search(CurrentUser.tenantId(), null, PageRequestSupport.normalizeSearch(search), pageable)
                .stream()
                .map(CustomerService::toResponse)
                .toList();
        return csvService.exportCustomers(customers);
    }

    public byte[] customerImportTemplate() {
        return csvService.customerTemplate();
    }

    @Transactional
    public CustomerImportResult importCustomers(MultipartFile file, boolean commit) {
        UUID tenantId = CurrentUser.tenantId();
        List<CustomerCsvRow> rows = csvService.parseCustomers(file);
        Set<String> seenCodes = new HashSet<>();
        List<CustomerImportCandidate> candidates = new ArrayList<>();
        List<CustomerImportRowResult> results = new ArrayList<>();

        for (CustomerCsvRow row : rows) {
            CustomerImportCandidate candidate = validateImportRow(row, seenCodes, tenantId);
            candidates.add(candidate);
            results.add(new CustomerImportRowResult(row.rowNumber(), row.code(), row.name(), candidate.valid(), candidate.message()));
        }

        int validRows = (int) results.stream().filter(CustomerImportRowResult::valid).count();
        int errorRows = results.size() - validRows;
        if (!commit || errorRows > 0) {
            return new CustomerImportResult(rows.size(), validRows, errorRows, 0, false, results);
        }

        for (CustomerImportCandidate candidate : candidates) {
            createImportedCustomer(tenantId, candidate);
        }

        auditService.record("IMPORT_CUSTOMERS", "CUSTOMER", null, "Import " + validRows + " khách hàng từ CSV");
        return new CustomerImportResult(rows.size(), validRows, 0, validRows, true, results);
    }

    private Customer require(UUID id) {
        return repository.findByIdAndTenantId(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
    }

    private static void apply(Customer customer, CustomerRequest request, String code) {
        customer.setCode(code);
        customer.setName(request.name().trim());
        customer.setPhone(blankToNull(request.phone()));
        customer.setEmail(blankToNull(request.email()));
        customer.setAddress(blankToNull(request.address()));
        customer.setNotes(blankToNull(request.notes()));
        customer.setActive(request.active() == null || request.active());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CustomerImportCandidate validateImportRow(CustomerCsvRow row, Set<String> seenCodes, UUID tenantId) {
        String code = row.code().trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            return CustomerImportCandidate.invalid(row, "Mã khách hàng không được để trống");
        }
        if (code.length() > 40) {
            return CustomerImportCandidate.invalid(row, "Mã khách hàng tối đa 40 ký tự");
        }
        if (!seenCodes.add(code)) {
            return CustomerImportCandidate.invalid(row, "Mã khách hàng bị trùng trong file import");
        }
        if (repository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            return CustomerImportCandidate.invalid(row, "Mã khách hàng đã tồn tại trong hệ thống");
        }
        if (row.name().isBlank() || row.name().length() > 180) {
            return CustomerImportCandidate.invalid(row, "Tên khách hàng bắt buộc và tối đa 180 ký tự");
        }
        if (row.phone().length() > 30) {
            return CustomerImportCandidate.invalid(row, "Số điện thoại tối đa 30 ký tự");
        }
        if (row.email().length() > 150 || (!row.email().isBlank() && !row.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            return CustomerImportCandidate.invalid(row, "Email không hợp lệ");
        }
        if (row.address().length() > 300) {
            return CustomerImportCandidate.invalid(row, "Địa chỉ tối đa 300 ký tự");
        }
        if (row.notes().length() > 2000) {
            return CustomerImportCandidate.invalid(row, "Ghi chú tối đa 2000 ký tự");
        }

        try {
            boolean active = parseBoolean(row.active());
            return new CustomerImportCandidate(row, code, row.name().trim(), active, true, "Hop le");
        } catch (IllegalArgumentException ex) {
            return CustomerImportCandidate.invalid(row, ex.getMessage());
        }
    }

    private void createImportedCustomer(UUID tenantId, CustomerImportCandidate candidate) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setCode(candidate.code());
        customer.setName(candidate.name());
        customer.setPhone(blankToNull(candidate.row().phone()));
        customer.setEmail(blankToNull(candidate.row().email()));
        customer.setAddress(blankToNull(candidate.row().address()));
        customer.setNotes(blankToNull(candidate.row().notes()));
        customer.setActive(candidate.active());
        repository.save(customer);
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


    public static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getCode(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getNotes(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private record CustomerImportCandidate(
            CustomerCsvRow row,
            String code,
            String name,
            boolean active,
            boolean valid,
            String message
    ) {
        static CustomerImportCandidate invalid(CustomerCsvRow row, String message) {
            return new CustomerImportCandidate(row, row.code(), row.name(), false, false, message);
        }
    }
}
