package com.serviceops.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, UUID> {
    @Query("""
            select r from PaymentReceipt r
            join fetch r.workOrder w
            join fetch r.payment p
            join fetch r.billingSnapshot s
            where r.tenantId = :tenantId and w.id = :workOrderId
            """)
    Optional<PaymentReceipt> findByWorkOrder(@Param("tenantId") UUID tenantId,
                                             @Param("workOrderId") UUID workOrderId);
}
