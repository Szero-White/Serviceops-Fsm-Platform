package com.serviceops.customer.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.customer.web.CustomerDtos.CustomerRequest;
import com.serviceops.customer.web.CustomerDtos.CustomerResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.from(repository.search(CurrentUser.tenantId(), search == null ? "" : search.trim(), pageable).map(CustomerService::toResponse));
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

    public static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getCode(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getNotes(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
