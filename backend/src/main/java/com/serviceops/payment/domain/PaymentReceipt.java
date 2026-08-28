package com.serviceops.payment.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_receipts")
public class PaymentReceipt extends TenantScopedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_snapshot_id", nullable = false)
    private WorkOrderBillingSnapshot billingSnapshot;

    @Column(name = "receipt_code", nullable = false, length = 60)
    private String receiptCode;

    @Column(name = "work_order_code_snapshot", nullable = false, length = 40)
    private String workOrderCodeSnapshot;

    @Column(name = "customer_name_snapshot", nullable = false, length = 200)
    private String customerNameSnapshot;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @Column(name = "settled_by_display_name", nullable = false, length = 150)
    private String settledByDisplayName;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "issued_by_username", nullable = false, length = 100)
    private String issuedByUsername;

    @Column(name = "issued_by_display_name", nullable = false, length = 150)
    private String issuedByDisplayName;
}
