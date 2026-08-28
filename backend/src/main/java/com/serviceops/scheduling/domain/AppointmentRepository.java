package com.serviceops.scheduling.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
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

    @Query("""
            select a from Appointment a
            join fetch a.workOrder w
            join fetch w.customer
            join fetch a.technician t
            join fetch t.user
            where a.tenantId = :tenantId
              and a.status = :status
              and a.startTime < :rangeEnd
              and a.endTime > :rangeStart
              and w.deletedAt is null
              and w.status in (
                  com.serviceops.workorder.domain.WorkOrderStatus.SCHEDULED,
                  com.serviceops.workorder.domain.WorkOrderStatus.ASSIGNED,
                  com.serviceops.workorder.domain.WorkOrderStatus.ON_THE_WAY,
                  com.serviceops.workorder.domain.WorkOrderStatus.IN_PROGRESS,
                  com.serviceops.workorder.domain.WorkOrderStatus.WAITING_FOR_PARTS
              )
            order by a.startTime asc
            """)
    List<Appointment> findBoardRange(@Param("tenantId") UUID tenantId,
                                     @Param("rangeStart") Instant rangeStart,
                                     @Param("rangeEnd") Instant rangeEnd,
                                     @Param("status") AppointmentStatus status);

    @Query("""
            select a from Appointment a
            join fetch a.workOrder w
            join fetch w.customer
            left join fetch w.asset
            join fetch a.technician t
            join fetch t.user
            where a.tenantId = :tenantId
              and t.id = :technicianId
              and a.status = :status
              and a.startTime < :rangeEnd
              and a.endTime > :rangeStart
              and w.deletedAt is null
              and w.status <> com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED
            order by a.startTime asc
            """)
    List<Appointment> findTechnicianRange(@Param("tenantId") UUID tenantId,
                                          @Param("technicianId") UUID technicianId,
                                          @Param("rangeStart") Instant rangeStart,
                                          @Param("rangeEnd") Instant rangeEnd,
                                          @Param("status") AppointmentStatus status);

    @Query("""
            select a from Appointment a
            join fetch a.workOrder w
            join fetch w.customer
            join fetch a.technician t
            join fetch t.user
            where a.status = :status
              and a.endTime < :now
              and w.deletedAt is null
              and w.status in (
                  com.serviceops.workorder.domain.WorkOrderStatus.SCHEDULED,
                  com.serviceops.workorder.domain.WorkOrderStatus.ASSIGNED
              )
            order by a.endTime asc
            """)
    List<Appointment> findOverdueNotificationCandidates(@Param("now") Instant now,
                                                        @Param("status") AppointmentStatus status);

    Optional<Appointment> findByTenantIdAndWorkOrderId(UUID tenantId, UUID workOrderId);
    long countByTenantIdAndTechnicianId(UUID tenantId, UUID technicianId);
}
