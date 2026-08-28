package com.serviceops.payment.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Query("""
            select p from Payment p
            join fetch p.workOrder w
            join fetch w.customer c
            left join fetch w.technician t
            left join fetch t.user u
            where p.tenantId = :tenantId and w.id = :workOrderId
            """)
    Optional<Payment> findDetailedByWorkOrder(@Param("tenantId") UUID tenantId,
                                              @Param("workOrderId") UUID workOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.workOrder w
            left join fetch w.technician t
            left join fetch t.user u
            where p.tenantId = :tenantId and p.id = :id
            """)
    Optional<Payment> findForUpdate(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.workOrder w
            left join fetch w.technician t
            left join fetch t.user u
            where p.tenantId = :tenantId and w.id = :workOrderId
            """)
    Optional<Payment> findForUpdateByWorkOrder(@Param("tenantId") UUID tenantId,
                                               @Param("workOrderId") UUID workOrderId);

    @Query(value = """
            select p from Payment p
            join fetch p.workOrder w
            join fetch w.customer c
            left join fetch w.technician t
            left join fetch t.user u
            where p.tenantId = :tenantId
              and (:status is null or p.status = :status)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(p) from Payment p
            join p.workOrder w
            join w.customer c
            left join w.technician t
            left join t.user u
            where p.tenantId = :tenantId
              and (:status is null or p.status = :status)
              and (:search = '' or lower(w.code) like lower(concat('%', :search, '%'))
                   or lower(w.summary) like lower(concat('%', :search, '%'))
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.displayName, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Payment> search(@Param("tenantId") UUID tenantId,
                         @Param("status") PaymentStatus status,
                         @Param("search") String search,
                         Pageable pageable);
}
