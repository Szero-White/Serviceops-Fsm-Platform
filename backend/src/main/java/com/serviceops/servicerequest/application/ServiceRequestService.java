package com.serviceops.servicerequest.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.servicerequest.web.ServiceRequestDtos.CreateServiceRequest;
import com.serviceops.servicerequest.web.ServiceRequestDtos.ServiceRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository repository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final ServiceChannelService serviceChannelService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ServiceRequestResponse> search(String search, ServiceRequestStatus status, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.from(repository.search(CurrentUser.tenantId(), status, search == null ? "" : search.trim(), pageable).map(ServiceRequestService::toResponse));
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        Customer customer = customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
        Asset asset = null;
        if (request.assetId() != null) {
            asset = assetRepository.findDetailed(request.assetId(), tenantId)
                    .orElseThrow(() -> BusinessException.notFound("ASSET_NOT_FOUND", "Không tìm thấy thiết bị"));
            if (!asset.getCustomer().getId().equals(customer.getId())) {
                throw BusinessException.badRequest("ASSET_CUSTOMER_MISMATCH", "Thiết bị không thuộc khách hàng đã chọn");
            }
        }
        ServiceRequest entity = new ServiceRequest();
        String channelCode = serviceChannelService.requireActive(tenantId, request.channel()).getCode();
        entity.setTenantId(tenantId);
        entity.setCustomer(customer);
        entity.setAsset(asset);
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description().trim());
        entity.setPriority(request.priority());
        entity.setChannel(channelCode);
        entity.setStatus(ServiceRequestStatus.OPEN);
        entity.setCreatedBy(CurrentUser.username());
        repository.save(entity);
        auditService.record("CREATE", "SERVICE_REQUEST", entity.getId(), "Tiếp nhận yêu cầu: " + entity.getTitle());
        return toResponse(entity);
    }

    @Transactional
    public ServiceRequestResponse cancel(UUID id) {
        ServiceRequest entity = require(id);
        try {
            entity.cancel();
        } catch (IllegalStateException ex) {
            throw BusinessException.conflict("SERVICE_REQUEST_INVALID_STATE", ex.getMessage());
        }
        auditService.record("CANCEL", "SERVICE_REQUEST", entity.getId(), "Hủy yêu cầu dịch vụ");
        return toResponse(entity);
    }

    public ServiceRequest require(UUID id) {
        return repository.findDetailed(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
    }

    public static ServiceRequestResponse toResponse(ServiceRequest r) {
        String assetLabel = r.getAsset() == null ? null : String.join(" ",
                r.getAsset().getBrand() == null ? "" : r.getAsset().getBrand(),
                r.getAsset().getModel() == null ? "" : r.getAsset().getModel(),
                "(" + r.getAsset().getSerialNumber() + ")").trim();
        return new ServiceRequestResponse(r.getId(), r.getCustomer().getId(), r.getCustomer().getName(),
                r.getAsset() == null ? null : r.getAsset().getId(), assetLabel, r.getTitle(), r.getDescription(),
                r.getPriority(), r.getChannel(), r.getStatus(), r.getCreatedBy(), r.getCreatedAt());
    }
}
