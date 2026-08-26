package com.serviceops.inventory.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderPartUsageRepository extends JpaRepository<WorkOrderPartUsage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from WorkOrderPartUsage u
            join fetch u.workOrder
            join fetch u.sparePart
            where u.tenantId = :tenantId
              and u.workOrder.id = :workOrderId
              and u.sparePart.id = :sparePartId
            """)
    Optional<WorkOrderPartUsage> findForUpdate(@Param("tenantId") UUID tenantId,
                                               @Param("workOrderId") UUID workOrderId,
                                               @Param("sparePartId") UUID sparePartId);

    @Query("""
            select u from WorkOrderPartUsage u
            join fetch u.sparePart
            where u.tenantId = :tenantId
              and u.workOrder.id = :workOrderId
            order by u.sparePart.name asc
            """)
    List<WorkOrderPartUsage> findDetailedByWorkOrder(@Param("tenantId") UUID tenantId,
                                                     @Param("workOrderId") UUID workOrderId);

    Optional<WorkOrderPartUsage> findByTenantIdAndWorkOrderIdAndSparePartId(
            UUID tenantId, UUID workOrderId, UUID sparePartId);
}
