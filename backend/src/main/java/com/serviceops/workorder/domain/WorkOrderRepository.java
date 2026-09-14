package com.serviceops.workorder.domain;

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

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    @Query("""
            select w from WorkOrder w
            join fetch w.customer c
            left join fetch w.asset a
            left join fetch w.technician t
            left join fetch t.user u
            where w.tenantId = :tenantId
              and w.status in :statuses
              and not (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED))
              and w.deletedAt is null
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> search(@Param("tenantId") UUID tenantId,
                           @Param("statuses") List<WorkOrderStatus> statuses,
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
              and w.status in :statuses
              and not (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED))
              and w.deletedAt is null
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(w) from WorkOrder w
            join w.customer c
            left join w.asset a
            join w.technician t
            join t.user u
            where w.tenantId = :tenantId
              and u.id = :userId
              and w.status in :statuses
              and not (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED))
              and w.deletedAt is null
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchAssigned(@Param("tenantId") UUID tenantId,
                                   @Param("userId") UUID userId,
                                   @Param("statuses") List<WorkOrderStatus> statuses,
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select w from WorkOrder w
            where w.id = :id and w.tenantId = :tenantId and w.deletedAt is null
            """)
    Optional<WorkOrder> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);


    @Query(value = """
            select count(*) as total,
                   count(*) filter (where w.status = 'CUSTOMER_ACCEPTED') as "pendingClosure",
                   count(*) filter (where w.status = 'CLOSED') as closed,
                   count(*) filter (where w.status = 'CANCELLED') as cancelled
            from work_orders w
            join customers c on c.id = w.customer_id
            left join assets a on a.id = w.asset_id
            left join technician_profiles t on t.id = w.technician_id
            left join user_accounts u on u.id = t.user_id
            where w.tenant_id = :tenantId
              and w.deleted_at is null
              and (:restrictToTechnician = false or u.id = :userId)
              and (w.status in ('CLOSED', 'CANCELLED')
                   or (w.status = 'CUSTOMER_ACCEPTED' and exists (
                       select 1 from payments p where p.work_order_id = w.id and p.status = 'SETTLED'
                   )))
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serial_number, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.display_name, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """, nativeQuery = true)
    WorkOrderHistorySummaryProjection summarizeHistory(@Param("tenantId") UUID tenantId,
                                                       @Param("userId") UUID userId,
                                                       @Param("restrictToTechnician") boolean restrictToTechnician,
                                                       @Param("search") String search);

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
              and w.status in :statuses
              and (w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
                   or (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED)))
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchHistory(@Param("tenantId") UUID tenantId,
                                  @Param("statuses") List<WorkOrderStatus> statuses,
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
              and w.status in :statuses
              and (w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
                   or (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED)))
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
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
              and w.status in :statuses
              and (w.status in (com.serviceops.workorder.domain.WorkOrderStatus.CLOSED, com.serviceops.workorder.domain.WorkOrderStatus.CANCELLED)
                   or (w.status = com.serviceops.workorder.domain.WorkOrderStatus.CUSTOMER_ACCEPTED and exists (select p.id from Payment p where p.workOrder = w and p.status = com.serviceops.payment.domain.PaymentStatus.SETTLED)))
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.description, '')) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(a.serialNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WorkOrder> searchAssignedHistory(@Param("tenantId") UUID tenantId,
                                          @Param("userId") UUID userId,
                                          @Param("statuses") List<WorkOrderStatus> statuses,
                                          @Param("search") String search,
                                          Pageable pageable);

    @Query("""
            select w from WorkOrder w
            join fetch w.customer
            where w.tenantId = :tenantId
              and w.deletedAt is null
              and w.status in (
                  com.serviceops.workorder.domain.WorkOrderStatus.OPEN,
                  com.serviceops.workorder.domain.WorkOrderStatus.REOPENED
              )
            order by
              case w.priority
                when com.serviceops.common.domain.Priority.URGENT then 0
                when com.serviceops.common.domain.Priority.HIGH then 1
                when com.serviceops.common.domain.Priority.NORMAL then 2
                else 3
              end,
              w.createdAt asc
            """)
    List<WorkOrder> findDispatchQueue(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("""
            select count(w) from WorkOrder w
            where w.tenantId = :tenantId
              and w.deletedAt is null
              and w.status in (
                  com.serviceops.workorder.domain.WorkOrderStatus.OPEN,
                  com.serviceops.workorder.domain.WorkOrderStatus.REOPENED
              )
            """)
    long countDispatchQueue(@Param("tenantId") UUID tenantId);

    @Query("""
            select case when count(w) > 0 then true else false end
            from WorkOrder w
            where w.tenantId = :tenantId
              and w.technician.id = :technicianId
              and w.deletedAt is null
              and w.status in (
                  com.serviceops.workorder.domain.WorkOrderStatus.SCHEDULED,
                  com.serviceops.workorder.domain.WorkOrderStatus.ASSIGNED,
                  com.serviceops.workorder.domain.WorkOrderStatus.ON_THE_WAY,
                  com.serviceops.workorder.domain.WorkOrderStatus.IN_PROGRESS,
                  com.serviceops.workorder.domain.WorkOrderStatus.WAITING_FOR_PARTS,
                  com.serviceops.workorder.domain.WorkOrderStatus.REOPENED
              )
            """)
    boolean existsActiveAssignment(@Param("tenantId") UUID tenantId, @Param("technicianId") UUID technicianId);

    long countByTenantIdAndTechnicianId(UUID tenantId, UUID technicianId);
    long countByTenantIdAndServiceRequestId(UUID tenantId, UUID serviceRequestId);
    long countByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    long countByTenantIdAndAssetId(UUID tenantId, UUID assetId);
}
