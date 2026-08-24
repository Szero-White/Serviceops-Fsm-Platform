package com.serviceops.servicerequest.application;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.servicerequest.web.ServiceRequestDtos.CreateServiceRequest;
import com.serviceops.servicerequest.web.ServiceRequestDtos.ServiceRequestResponse;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository repository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final ServiceChannelService serviceChannelService;
    private final WorkOrderRepository workOrderRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<ServiceRequestResponse> search(String search, ServiceRequestStatus status, int page, int size) {
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());
        String keyword = PageRequestSupport.normalizeSearch(search);
        return PageResponse.from(repository.search(CurrentUser.tenantId(), status, keyword, pageable).map(ServiceRequestService::toResponse));
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        ServiceRequest entity = new ServiceRequest();
        applyEditableFields(entity, request, tenantId);
        entity.setTenantId(tenantId);
        entity.setStatus(ServiceRequestStatus.OPEN);
        entity.setCreatedBy(CurrentUser.username());
        repository.save(entity);
        auditService.record("CREATE", "SERVICE_REQUEST", entity.getId(), "Tiếp nhận yêu cầu: " + entity.getTitle());
        notificationService.notifyRoles(
                tenantId,
                intakeRoles(),
                "Yêu cầu mới cần tiếp nhận",
                "Khách hàng: " + entity.getCustomer().getName() + ". Nội dung: " + entity.getTitle()
                        + ". Kiểm tra thông tin và tạo phiếu công việc khi sẵn sàng."
        );
        return toResponse(entity);
    }

    @Transactional
    public ServiceRequestResponse update(UUID id, CreateServiceRequest request) {
        ServiceRequest entity = require(id);
        if (entity.getStatus() != ServiceRequestStatus.OPEN) {
            throw BusinessException.conflict("SERVICE_REQUEST_LOCKED", "Chỉ có thể chỉnh sửa yêu cầu đang mở");
        }
        applyEditableFields(entity, request, CurrentUser.tenantId());
        auditService.record("UPDATE", "SERVICE_REQUEST", entity.getId(), "Cập nhật yêu cầu dịch vụ: " + entity.getTitle());
        notificationService.notifyRoles(
                CurrentUser.tenantId(),
                intakeRoles(),
                "Yêu cầu dịch vụ vừa được cập nhật",
                "Nội dung: " + entity.getTitle() + ". Mở Yêu cầu dịch vụ để xem thông tin mới."
        );
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
        notificationService.notifyRoles(
                CurrentUser.tenantId(),
                intakeRoles(),
                "Yêu cầu dịch vụ đã hủy",
                "Nội dung: " + entity.getTitle() + ". Yêu cầu này không còn tiếp tục xử lý."
        );
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        ServiceRequest entity = require(id);
        long workOrderCount = workOrderRepository.countByTenantIdAndServiceRequestId(CurrentUser.tenantId(), id);
        if (workOrderCount > 0 || entity.getStatus() == ServiceRequestStatus.CONVERTED) {
            throw BusinessException.conflict("SERVICE_REQUEST_IN_USE", "Không thể xóa yêu cầu đã tạo phiếu công việc");
        }
        if (attachmentRepository.existsByTenantIdAndReferenceTypeAndReferenceId(CurrentUser.tenantId(), "SERVICE_REQUEST", id)) {
            throw BusinessException.conflict(
                    "SERVICE_REQUEST_HAS_ATTACHMENTS",
                    "Không thể xóa yêu cầu dịch vụ khi còn file đính kèm; hãy xóa file đính kèm trước"
            );
        }
        repository.delete(entity);
        auditService.record("DELETE", "SERVICE_REQUEST", entity.getId(), "Xóa yêu cầu dịch vụ: " + entity.getTitle());
        notificationService.notifyRoles(
                CurrentUser.tenantId(),
                intakeRoles(),
                "Yêu cầu dịch vụ đã được xóa",
                "Nội dung: " + entity.getTitle() + "."
        );
    }

    public ServiceRequest require(UUID id) {
        return repository.findDetailed(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SERVICE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu dịch vụ"));
    }

    private void applyEditableFields(ServiceRequest entity, CreateServiceRequest request, UUID tenantId) {
        Customer customer = resolveCustomer(request.customerId(), tenantId);
        Asset asset = resolveAsset(request.assetId(), customer, tenantId);
        String channelCode = serviceChannelService.requireActive(tenantId, request.channel()).getCode();

        entity.setCustomer(customer);
        entity.setAsset(asset);
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description().trim());
        entity.setPriority(request.priority());
        entity.setChannel(channelCode);
    }

    private Customer resolveCustomer(UUID customerId, UUID tenantId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng"));
    }

    private Asset resolveAsset(UUID assetId, Customer customer, UUID tenantId) {
        if (assetId == null) {
            return null;
        }
        Asset asset = assetRepository.findDetailed(assetId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("ASSET_NOT_FOUND", "Không tìm thấy thiết bị"));
        if (!asset.getCustomer().getId().equals(customer.getId())) {
            throw BusinessException.badRequest("ASSET_CUSTOMER_MISMATCH", "Thiết bị không thuộc khách hàng đã chọn");
        }
        return asset;
    }

    private static List<UserRole> intakeRoles() {
        return List.of(UserRole.OWNER, UserRole.CUSTOMER_SERVICE);
    }

    public static ServiceRequestResponse toResponse(ServiceRequest request) {
        String assetLabel = request.getAsset() == null ? null : assetLabel(request.getAsset());
        return new ServiceRequestResponse(
                request.getId(),
                request.getCustomer().getId(),
                request.getCustomer().getName(),
                request.getAsset() == null ? null : request.getAsset().getId(),
                assetLabel,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getChannel(),
                request.getStatus(),
                request.getCreatedBy(),
                request.getCreatedAt()
        );
    }

    private static String assetLabel(Asset asset) {
        String equipmentName = ((asset.getBrand() == null ? "" : asset.getBrand() + " ")
                + (asset.getModel() == null ? "" : asset.getModel())).trim();
        if (equipmentName.isBlank()) {
            equipmentName = asset.getCategory();
        }
        String serial = asset.getSerialNumber() == null ? "Chưa xác định serial" : asset.getSerialNumber();
        return equipmentName + " (" + serial + ")";
    }
}
