package com.serviceops.attachment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    Optional<Attachment> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Attachment> findByTenantIdAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(UUID tenantId, String referenceType, UUID referenceId);
}
