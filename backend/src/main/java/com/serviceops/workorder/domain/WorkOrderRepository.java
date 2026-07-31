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
              and w.status not in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
              and w.deletedAt is null
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
              and w.status not in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
              and w.deletedAt is null
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
              and w.status not in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
              and w.deletedAt is null
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
            where w.id = :id and w.tenantId = :tenantId and w.deletedAt is null
            """)
    Optional<WorkOrder> findDetailed(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
            select w from WorkOrder w
            join fetch w.customer
            left join fetch w.asset
            join fetch w.technician t
            join fetch t.user u
            left join fetch w.serviceRequest
            where w.id = :id and w.tenantId = :tenantId and u.id = :userId and w.deletedAt is null
            """)
    Optional<WorkOrder> findDetailedAssigned(@Param("id") UUID id,
                                             @Param("tenantId") UUID tenantId,
                                             @Param("userId") UUID userId);

    @Query(value = "select nextval('work_order_number_seq')", nativeQuery = true)
    long nextNumber();

    @Query("select count(w) from WorkOrder w where w.tenantId = :tenantId and w.status = :status and w.deletedAt is null")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") WorkOrderStatus status);

    @Query("""
            select count(w) from WorkOrder w
            join w.technician t
            join t.user u
            where w.tenantId = :tenantId
              and u.id = :userId
              and w.status = :status
              and w.deletedAt is null
            """)
    long countByTenantIdAndTechnicianUserIdAndStatus(@Param("tenantId") UUID tenantId,
                                                     @Param("userId") UUID userId,
                                                     @Param("status") WorkOrderStatus status);

    @Query("""
            select w from WorkOrder w
            join fetch w.customer c
            left join fetch w.asset a
            left join fetch w.technician t
            left join fetch t.user u
            where w.tenantId = :tenantId
              and w.deletedAt is null
              and (:status is null or w.status = :status)
              and w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchHistory(@Param("tenantId") UUID tenantId,
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
              and w.deletedAt is null
              and (:status is null or w.status = :status)
              and w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
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
              and w.deletedAt is null
              and (:status is null or w.status = :status)
              and w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchAssignedHistory(@Param("tenantId") UUID tenantId,
                                          @Param("userId") UUID userId,
                                          @Param("status") WorkOrderStatus status,
                                          @Param("search") String search,
                                          Pageable pageable);

    long countByTenantIdAndTechnicianId(UUID tenantId, UUID technicianId);
    long countByTenantIdAndServiceRequestId(UUID tenantId, UUID serviceRequestId);
    long countByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    long countByTenantIdAndAssetId(UUID tenantId, UUID assetId);
}
