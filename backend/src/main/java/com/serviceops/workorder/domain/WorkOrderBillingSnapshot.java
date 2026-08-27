package com.serviceops.workorder.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "work_order_billing_snapshots")
public class WorkOrderBillingSnapshot extends TenantScopedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "parts_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal partsTotal;

    @Column(name = "labor_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal laborFee;

    @Column(name = "incidental_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal incidentalFee;

    @Column(name = "incidental_reason", length = 500)
    private String incidentalReason;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "accepted_by_user_id", nullable = false)
    private UUID acceptedByUserId;

    @Column(name = "accepted_by_username", nullable = false, length = 100)
    private String acceptedByUsername;

    @Column(name = "accepted_by_display_name", nullable = false, length = 150)
    private String acceptedByDisplayName;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
}
