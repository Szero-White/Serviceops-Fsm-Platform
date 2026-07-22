package com.serviceops.inventory.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SparePartRepository extends JpaRepository<SparePart, UUID> {
    @Query("""
            select p from SparePart p
            where p.tenantId = :tenantId
              and (:search = '' or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(p.name) like lower(concat('%', :search, '%')))
            """)
    Page<SparePart> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    Optional<SparePart> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndSkuIgnoreCase(UUID tenantId, String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SparePart p where p.id = :id and p.tenantId = :tenantId")
    Optional<SparePart> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select count(p) from SparePart p where p.tenantId = :tenantId and p.active = true and p.stockQuantity <= p.reorderLevel")
    long countLowStock(@Param("tenantId") UUID tenantId);
}
