package com.serviceops.payment.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_payment_profiles")
public class CompanyPaymentProfile extends TenantScopedEntity {
    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "account_holder", nullable = false, length = 180)
    private String accountHolder;

    @Column(name = "account_number", nullable = false, length = 80)
    private String accountNumber;

    @Column(name = "qr_attachment_id")
    private UUID qrAttachmentId;

    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;

    @Column(name = "updated_by_username", nullable = false, length = 100)
    private String updatedByUsername;

    @Column(name = "updated_by_display_name", nullable = false, length = 150)
    private String updatedByDisplayName;
}
