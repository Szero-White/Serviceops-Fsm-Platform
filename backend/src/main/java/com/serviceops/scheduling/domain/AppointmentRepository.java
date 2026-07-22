package com.serviceops.scheduling.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.tenantId = :tenantId
              and a.technician.id = :technicianId
              and a.status = :status
              and a.startTime < :endTime
              and a.endTime > :startTime
              and (:excludeWorkOrderId is null or a.workOrder.id <> :excludeWorkOrderId)
            """)
    boolean existsOverlap(@Param("tenantId") UUID tenantId,
                          @Param("technicianId") UUID technicianId,
                          @Param("startTime") Instant startTime,
                          @Param("endTime") Instant endTime,
                          @Param("status") AppointmentStatus status,
                          @Param("excludeWorkOrderId") UUID excludeWorkOrderId);

    Optional<Appointment> findByTenantIdAndWorkOrderId(UUID tenantId, UUID workOrderId);
}
