package com.serviceops.workorder.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    @Query("""
            select w from WorkOrder w
            join fetch w.customer c
            left join fetch w.asset a
            left join fetch w.technician t
            left join fetch t.user u
            where w.tenantId = :tenantId
              and (:status is null or w.status = :status)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> search(@Param("tenantId") UUID tenantId,
                           @Param("status") WorkOrderStatus status,
                           @Param("search") String search,
                           Pageable pageable);


    @Query(value = """
            select w from WorkOrder w
            join fetch w.customer c
            left join fetch w.asset a
            join fetch w.technician t
            join fetch t.user u
            where w.tenantId = :tenantId
              and u.id = :userId
              and (:status is null or w.status = :status)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(w) from WorkOrder w
            join w.customer c
            left join w.asset a
            join w.technician t
            join t.user u
            where w.tenantId = :tenantId
              and u.id = :userId
              and (:status is null or w.status = :status)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchAssigned(@Param("tenantId") UUID tenantId,
                                   @Param("userId") UUID userId,
                                   @Param("status") WorkOrderStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);

    @Query("""
            select w from WorkOrder w
            join fetch w.customer
            left join fetch w.asset
            left join fetch w.technician t
            left join fetch t.user
            left join fetch w.serviceRequest
            where w.id = :id and w.tenantId = :tenantId
            """)
    Optional<WorkOrder> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
            select w from WorkOrder w
            join fetch w.customer
            left join fetch w.asset
            join fetch w.technician t
            join fetch t.user u
            left join fetch w.serviceRequest
            where w.id = :id and w.tenantId = :tenantId and u.id = :userId
            """)
    Optional<WorkOrder> findDetailedAssigned(@Param("id") UUID id,
                                             @Param("tenantId") UUID tenantId,
                                             @Param("userId") UUID userId);

    @Query(value = "select nextval('work_order_number_seq')", nativeQuery = true)
    long nextNumber();

    long countByTenantIdAndStatus(UUID tenantId, WorkOrderStatus status);
    long countByTenantIdAndTechnicianUserIdAndStatus(UUID tenantId, UUID userId, WorkOrderStatus status);
        long countByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
        long countByTenantIdAndAssetId(UUID tenantId, UUID assetId);
}
