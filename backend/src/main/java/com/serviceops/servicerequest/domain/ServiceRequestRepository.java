package com.serviceops.servicerequest.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    @Query("""
            select r from ServiceRequest r
            join fetch r.customer c
            left join fetch r.asset a
            where r.tenantId = :tenantId
              and (:status is null or r.status = :status)
              and (:search = '' or lower(r.title) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<ServiceRequest> search(@Param("tenantId") UUID tenantId,
                                @Param("status") ServiceRequestStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("select r from ServiceRequest r join fetch r.customer left join fetch r.asset where r.id = :id and r.tenantId = :tenantId")
    Optional<ServiceRequest> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, ServiceRequestStatus status);
}
