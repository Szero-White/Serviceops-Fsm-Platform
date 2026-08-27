package com.serviceops.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyPaymentProfileRepository extends JpaRepository<CompanyPaymentProfile, UUID> {
    Optional<CompanyPaymentProfile> findByTenantId(UUID tenantId);
}
