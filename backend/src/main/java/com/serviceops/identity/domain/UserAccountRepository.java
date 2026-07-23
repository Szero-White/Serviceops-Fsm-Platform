package com.serviceops.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);
}
