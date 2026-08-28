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
import jakarta.persistence.ManyToOne;
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
@Table(name = "payments")
public class Payment extends TenantScopedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_snapshot_id", nullable = false)
    private WorkOrderBillingSnapshot billingSnapshot;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Column(name = "transfer_evidence_attachment_id")
    private UUID transferEvidenceAttachmentId;

    @Column(name = "transfer_reported_at")
    private Instant transferReportedAt;

    @Column(name = "cash_collected_at")
    private Instant cashCollectedAt;

    @Column(name = "collected_by_user_id")
    private UUID collectedByUserId;

    @Column(name = "collected_by_username", length = 100)
    private String collectedByUsername;

    @Column(name = "collected_by_display_name", length = 150)
    private String collectedByDisplayName;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "settled_by_user_id")
    private UUID settledByUserId;

    @Column(name = "settled_by_username", length = 100)
    private String settledByUsername;

    @Column(name = "settled_by_display_name", length = 150)
    private String settledByDisplayName;
}
