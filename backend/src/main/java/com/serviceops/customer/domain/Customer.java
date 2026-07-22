package com.serviceops.customer.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends TenantScopedEntity {
    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String address;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
