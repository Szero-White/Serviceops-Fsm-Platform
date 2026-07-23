package com.serviceops.servicerequest.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceChannel;
import com.serviceops.servicerequest.domain.ServiceChannelRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelRequest;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelResponse;
import com.serviceops.servicerequest.web.ServiceChannelDtos.ServiceChannelUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceChannelService {
    private final ServiceChannelRepository repository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ServiceChannelResponse> list(boolean activeOnly) {
        UUID tenantId = CurrentUser.tenantId();
        List<ServiceChannel> channels = activeOnly
                ? repository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(tenantId)
                : repository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId);
        return channels.stream().map(ServiceChannelService::toResponse).toList();
    }

    @Transactional
    public ServiceChannelResponse create(ServiceChannelRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String code = normalizeCode(request.code());
        if (repository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw BusinessException.conflict("SERVICE_CHANNEL_CODE_EXISTS", "Kênh tiếp nhận đã tồn tại");
        }

        ServiceChannel channel = new ServiceChannel();
        channel.setTenantId(tenantId);
        channel.setCode(code);
        apply(channel, request.name(), request.description(), request.color(), request.sortOrder(), request.active());
        repository.save(channel);
        auditService.record("CREATE", "SERVICE_CHANNEL", channel.getId(), "Tạo kênh tiếp nhận " + channel.getCode());
        return toResponse(channel);
    }

    @Transactional
    public ServiceChannelResponse update(UUID id, ServiceChannelUpdateRequest request) {
        ServiceChannel channel = require(id);
        apply(channel, request.name(), request.description(), request.color(), request.sortOrder(), request.active());
        auditService.record("UPDATE", "SERVICE_CHANNEL", channel.getId(), "Cập nhật kênh tiếp nhận " + channel.getCode());
        return toResponse(channel);
    }

    @Transactional
    public void delete(UUID id) {
        ServiceChannel channel = require(id);
        long usageCount = serviceRequestRepository.countByTenantIdAndChannel(CurrentUser.tenantId(), channel.getCode());
        if (usageCount > 0) {
            throw BusinessException.conflict("SERVICE_CHANNEL_IN_USE", "Không thể xóa kênh đã được dùng trong yêu cầu dịch vụ");
        }
        repository.delete(channel);
        auditService.record("DELETE", "SERVICE_CHANNEL", channel.getId(), "Xóa kênh tiếp nhận " + channel.getCode());
    }

    public ServiceChannel requireActive(UUID tenantId, String code) {
        return repository.findByTenantIdAndCodeIgnoreCase(tenantId, normalizeCode(code))
                .filter(ServiceChannel::isActive)
                .orElseThrow(() -> BusinessException.badRequest("SERVICE_CHANNEL_INVALID", "Kênh tiếp nhận không hợp lệ hoặc đã ngừng dùng"));
    }

    private ServiceChannel require(UUID id) {
        return repository.findByIdAndTenantId(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SERVICE_CHANNEL_NOT_FOUND", "Không tìm thấy kênh tiếp nhận"));
    }

    private static void apply(ServiceChannel channel, String name, String description, String color, Integer sortOrder, Boolean active) {
        channel.setName(name.trim());
        channel.setDescription(description == null || description.isBlank() ? null : description.trim());
        channel.setColor(color == null || color.isBlank() ? "blue" : color.trim());
        channel.setSortOrder(sortOrder == null ? 100 : sortOrder);
        channel.setActive(active == null || active);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static ServiceChannelResponse toResponse(ServiceChannel channel) {
        return new ServiceChannelResponse(
                channel.getId(),
                channel.getCode(),
                channel.getName(),
                channel.getDescription(),
                channel.getColor(),
                channel.getSortOrder(),
                channel.isActive(),
                channel.isSystemDefined(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
