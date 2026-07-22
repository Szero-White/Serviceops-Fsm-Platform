package com.serviceops.asset.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    @Query("""
            select a from Asset a join fetch a.customer c
            where a.tenantId = :tenantId
              and (:search = '' or lower(a.serialNumber) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.brand, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.model, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%')))
            """)
    Page<Asset> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("select a from Asset a join fetch a.customer where a.id = :id and a.tenantId = :tenantId")
    Optional<Asset> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    boolean existsByTenantIdAndSerialNumberIgnoreCase(UUID tenantId, String serialNumber);
    long countByTenantId(UUID tenantId);
}
