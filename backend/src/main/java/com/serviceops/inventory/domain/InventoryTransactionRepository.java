package com.serviceops.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    Page<InventoryTransaction> findByTenantIdAndSparePartId(UUID tenantId, UUID sparePartId, Pageable pageable);
    boolean existsByTenantIdAndSparePartId(UUID tenantId, UUID sparePartId);

    @Query(value = """
            select tx from InventoryTransaction tx
            join fetch tx.sparePart p
            left join fetch tx.workOrder w
            where tx.tenantId = :tenantId
              and tx.transactionType in :types
              and tx.createdAt >= :fromTime
              and tx.createdAt <= :toTime
              and (:search = ''
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.code, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.summary, '')) like lower(concat('%', :search, '%'))
                   or lower(tx.createdBy) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.actorDisplayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.recipientDisplayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.actorRole, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.note, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(tx) from InventoryTransaction tx
            join tx.sparePart p
            left join tx.workOrder w
            where tx.tenantId = :tenantId
              and tx.transactionType in :types
              and tx.createdAt >= :fromTime
              and tx.createdAt <= :toTime
              and (:search = ''
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.code, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(w.summary, '')) like lower(concat('%', :search, '%'))
                   or lower(tx.createdBy) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.actorDisplayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.recipientDisplayName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.actorRole, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(tx.note, '')) like lower(concat('%', :search, '%')))
            """)
    Page<InventoryTransaction> search(@Param("tenantId") UUID tenantId,
                                      @Param("types") List<InventoryTransactionType> types,
                                      @Param("search") String search,
                                      @Param("fromTime") Instant fromTime,
                                      @Param("toTime") Instant toTime,
                                      Pageable pageable);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.CONSUME,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findPartUsageForWorkOrder(@Param("tenantId") UUID tenantId,
                                                          @Param("workOrderId") UUID workOrderId);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.sparePart.id = :sparePartId
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.CONSUME,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findPartUsageForWorkOrderAndSparePart(@Param("tenantId") UUID tenantId,
                                                                     @Param("workOrderId") UUID workOrderId,
                                                                     @Param("sparePartId") UUID sparePartId);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.ISSUE,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findWorkflowPartTransactionsForWorkOrder(@Param("tenantId") UUID tenantId,
                                                                         @Param("workOrderId") UUID workOrderId);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.workOrder
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id in :workOrderIds
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.ISSUE,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.workOrder.id, tx.createdAt asc
            """)
    List<InventoryTransaction> findWorkflowPartTransactionsForWorkOrders(
            @Param("tenantId") UUID tenantId,
            @Param("workOrderIds") Collection<UUID> workOrderIds
    );

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.CONSUME,
                  com.serviceops.inventory.domain.InventoryTransactionType.ISSUE,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findTimelinePartTransactionsForWorkOrder(@Param("tenantId") UUID tenantId,
                                                                         @Param("workOrderId") UUID workOrderId);

    @Query("""
            select tx from InventoryTransaction tx
            join fetch tx.sparePart
            where tx.tenantId = :tenantId
              and tx.workOrder.id = :workOrderId
              and tx.sparePart.id = :sparePartId
              and tx.transactionType in (
                  com.serviceops.inventory.domain.InventoryTransactionType.ISSUE,
                  com.serviceops.inventory.domain.InventoryTransactionType.RETURN
              )
            order by tx.createdAt asc
            """)
    List<InventoryTransaction> findWorkflowPartTransactionsForWorkOrderAndSparePart(
            @Param("tenantId") UUID tenantId,
            @Param("workOrderId") UUID workOrderId,
            @Param("sparePartId") UUID sparePartId
    );
}
