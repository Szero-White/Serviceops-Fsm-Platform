package com.serviceops.workorder.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderBillingSnapshotRepository extends JpaRepository<WorkOrderBillingSnapshot, UUID> {
    @Query("""
            select s from WorkOrderBillingSnapshot s
            join fetch s.workOrder w
            where s.tenantId = :tenantId and w.id = :workOrderId
            """)
    Optional<WorkOrderBillingSnapshot> findByWorkOrder(@Param("tenantId") UUID tenantId,
                                                       @Param("workOrderId") UUID workOrderId);
}
