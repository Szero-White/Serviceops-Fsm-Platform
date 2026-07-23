package com.serviceops.servicerequest.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "service_request_channels",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_channel_code_tenant", columnNames = {"tenant_id", "code"})
)
public class ServiceChannel extends TenantScopedEntity {
    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 240)
    private String description;

    @Column(nullable = false, length = 30)
    private String color = "blue";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_defined", nullable = false)
    private boolean systemDefined;
}
