package com.serviceops.technician.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.identity.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "technician_profiles")
public class TechnicianProfile extends TenantScopedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String skills;

    @Column(nullable = false)
    private boolean active = true;
}
