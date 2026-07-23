package com.serviceops.servicerequest.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceChannelRepository extends JpaRepository<ServiceChannel, UUID> {
    List<ServiceChannel> findByTenantIdOrderBySortOrderAscNameAsc(UUID tenantId);

    List<ServiceChannel> findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID tenantId);

    Optional<ServiceChannel> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ServiceChannel> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
