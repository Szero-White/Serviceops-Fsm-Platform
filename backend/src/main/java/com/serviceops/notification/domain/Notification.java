package com.serviceops.notification.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.identity.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private UserAccount recipient;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_at")
    private Instant readAt;
}
