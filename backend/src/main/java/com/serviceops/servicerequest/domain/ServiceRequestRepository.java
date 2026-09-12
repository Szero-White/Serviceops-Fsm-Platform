package com.serviceops.servicerequest.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    @Query("""
            select r from ServiceRequest r
            join fetch r.customer c
            left join fetch r.asset a
            where r.tenantId = :tenantId
              and r.status in :statuses
              and (:search = '' or lower(r.title) like lower(concat('%', :search, '%'))
                   or lower(r.description) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<ServiceRequest> search(@Param("tenantId") UUID tenantId,
                                @Param("statuses") List<ServiceRequestStatus> statuses,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("select r from ServiceRequest r join fetch r.customer left join fetch r.asset where r.id = :id and r.tenantId = :tenantId")
    Optional<ServiceRequest> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ServiceRequest r join fetch r.customer left join fetch r.asset where r.id = :id and r.tenantId = :tenantId")
    Optional<ServiceRequest> findDetailedForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, ServiceRequestStatus status);
    long countByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    long countByTenantIdAndAssetId(UUID tenantId, UUID assetId);
    long countByTenantIdAndChannel(UUID tenantId, String channel);
}
