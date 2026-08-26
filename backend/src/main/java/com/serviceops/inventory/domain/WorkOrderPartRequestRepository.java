package com.serviceops.inventory.domain;

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

public interface WorkOrderPartRequestRepository extends JpaRepository<WorkOrderPartRequest, UUID> {
    @Query("""
            select r from WorkOrderPartRequest r
            join fetch r.workOrder w
            join fetch r.sparePart p
            left join fetch w.technician t
            left join fetch t.user
            where r.id = :id and r.tenantId = :tenantId
            """)
    Optional<WorkOrderPartRequest> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from WorkOrderPartRequest r
            join fetch r.workOrder w
            join fetch r.sparePart p
            left join fetch w.technician t
            left join fetch t.user
            where r.id = :id and r.tenantId = :tenantId
            """)
    Optional<WorkOrderPartRequest> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
            select r from WorkOrderPartRequest r
            join fetch r.workOrder w
            join fetch r.sparePart p
            left join fetch w.technician t
            left join fetch t.user
            where r.tenantId = :tenantId
              and r.workOrder.id = :workOrderId
            order by r.createdAt asc
            """)
    List<WorkOrderPartRequest> findDetailedByWorkOrder(@Param("tenantId") UUID tenantId,
                                                       @Param("workOrderId") UUID workOrderId);

    @Query("""
            select r from WorkOrderPartRequest r
            join fetch r.workOrder w
            join fetch r.sparePart p
            left join fetch w.technician t
            left join fetch t.user
            where r.tenantId = :tenantId
              and r.workOrder.id = :workOrderId
              and r.status = :status
            order by r.createdAt asc
            """)
    List<WorkOrderPartRequest> findDetailedByWorkOrderAndStatus(@Param("tenantId") UUID tenantId,
                                                                @Param("workOrderId") UUID workOrderId,
                                                                @Param("status") WorkOrderPartRequestStatus status);

    boolean existsByTenantIdAndWorkOrderIdAndSparePartIdAndStatus(
            UUID tenantId,
            UUID workOrderId,
            UUID sparePartId,
            WorkOrderPartRequestStatus status
    );

    @Query(value = """
            select r from WorkOrderPartRequest r
            join fetch r.workOrder w
            join fetch r.sparePart p
            left join fetch w.technician t
            left join fetch t.user
            where r.tenantId = :tenantId
              and (:status is null or r.status = :status)
              and (:search = ''
                   or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(r.requestedByDisplayName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(r.receivedByDisplayName, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(r) from WorkOrderPartRequest r
            join r.workOrder w
            join r.sparePart p
            where r.tenantId = :tenantId
              and (:status is null or r.status = :status)
              and (:search = ''
                   or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(r.requestedByDisplayName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(r.receivedByDisplayName, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrderPartRequest> search(@Param("tenantId") UUID tenantId,
                                      @Param("status") WorkOrderPartRequestStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);
}
