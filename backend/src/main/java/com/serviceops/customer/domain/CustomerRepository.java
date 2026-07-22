package com.serviceops.customer.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    @Query("""
            select c from Customer c
            where c.tenantId = :tenantId
              and (:search = '' or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(c.code) like lower(concat('%', :search, '%'))
                   or lower(coalesce(c.phone, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Customer> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
    long countByTenantIdAndActiveTrue(UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Customer> findLockedByIdAndTenantId(UUID id, UUID tenantId);
}
