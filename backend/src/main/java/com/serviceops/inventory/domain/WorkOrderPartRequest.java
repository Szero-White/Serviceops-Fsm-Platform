package com.serviceops.inventory.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.workorder.domain.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "work_order_part_requests")
public class WorkOrderPartRequest extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(name = "requested_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal requestedQuantity;

    @Column(name = "request_note", nullable = false, length = 300)
    private String requestNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkOrderPartRequestStatus status;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "requested_by_username", nullable = false, length = 100)
    private String requestedByUsername;

    @Column(name = "requested_by_display_name", nullable = false, length = 150)
    private String requestedByDisplayName;

    @Column(name = "issued_quantity", precision = 18, scale = 3)
    private BigDecimal issuedQuantity;

    @Column(name = "issued_by_user_id")
    private UUID issuedByUserId;

    @Column(name = "issued_by_username", length = 100)
    private String issuedByUsername;

    @Column(name = "issued_by_display_name", length = 150)
    private String issuedByDisplayName;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "received_by_user_id")
    private UUID receivedByUserId;

    @Column(name = "received_by_display_name", length = 150)
    private String receivedByDisplayName;

    @Column(name = "resolution_reason", length = 500)
    private String resolutionReason;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "resolved_by_username", length = 100)
    private String resolvedByUsername;

    @Column(name = "resolved_by_display_name", length = 150)
    private String resolvedByDisplayName;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
