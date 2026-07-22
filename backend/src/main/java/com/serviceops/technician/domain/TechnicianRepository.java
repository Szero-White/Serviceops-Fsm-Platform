package com.serviceops.technician.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianRepository extends JpaRepository<TechnicianProfile, UUID> {
    @Query("select t from TechnicianProfile t join fetch t.user where t.tenantId = :tenantId and t.active = true order by t.user.displayName")
    List<TechnicianProfile> findActive(@Param("tenantId") UUID tenantId);

    @Query("select t from TechnicianProfile t join fetch t.user where t.id = :id and t.tenantId = :tenantId")
    Optional<TechnicianProfile> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TechnicianProfile t where t.id = :id and t.tenantId = :tenantId")
    Optional<TechnicianProfile> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    long countByTenantIdAndActiveTrue(UUID tenantId);
}
