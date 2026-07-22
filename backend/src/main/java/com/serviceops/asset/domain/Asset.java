package com.serviceops.asset.domain;

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

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assets")
public class Asset extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    @Column(name = "serial_number", nullable = false, length = 120)
    private String serialNumber;

    @Column(name = "installed_at")
    private LocalDate installedAt;

    @Column(name = "warranty_until")
    private LocalDate warrantyUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(columnDefinition = "text")
    private String notes;

    public boolean isUnderWarranty(LocalDate date) {
        return warrantyUntil != null && !warrantyUntil.isBefore(date);
    }
}
