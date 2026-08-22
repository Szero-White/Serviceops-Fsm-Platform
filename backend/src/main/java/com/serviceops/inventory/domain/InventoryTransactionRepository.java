package com.serviceops.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    Page<InventoryTransaction> findByTenantIdAndSparePartId(UUID tenantId, UUID sparePartId, Pageable pageable);
    boolean existsByTenantIdAndSparePartId(UUID tenantId, UUID sparePartId);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.transactionType = com.serviceops.inventory.domain.InventoryTransactionType.CONSUME
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findConsumedPartsForWorkOrder(@Param("tenantId") UUID tenantId,
                                                             @Param("workOrderId") UUID workOrderId);
}
