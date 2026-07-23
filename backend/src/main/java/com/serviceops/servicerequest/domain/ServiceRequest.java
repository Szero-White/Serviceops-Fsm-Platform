package com.serviceops.servicerequest.domain;

import com.serviceops.asset.domain.Asset;
import com.serviceops.common.domain.Priority;
import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.customer.domain.Customer;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_requests")
public class ServiceRequest extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(nullable = false, length = 30)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestStatus status = ServiceRequestStatus.OPEN;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    public void markConverted() {
        if (status != ServiceRequestStatus.OPEN) {
            throw new IllegalStateException("Only open service requests can be converted");
        }
        status = ServiceRequestStatus.CONVERTED;
    }

    public void cancel() {
        if (status == ServiceRequestStatus.CONVERTED) {
            throw new IllegalStateException("Converted service request cannot be cancelled");
        }
        status = ServiceRequestStatus.CANCELLED;
    }
}
