package com.serviceops.audit.application;

import com.serviceops.audit.domain.AuditLog;
import com.serviceops.audit.domain.AuditLogRepository;
import com.serviceops.audit.web.AuditController.AuditResponse;
import com.serviceops.common.web.PageResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, UUID entityId, String details) {
        recordAs(CurrentUser.tenantId(), CurrentUser.username(), action, entityType, entityId, details);
    }

    public void recordAs(UUID tenantId, String actor, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setActorUsername(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.from(repository.findByTenantId(CurrentUser.tenantId(), pageable)
                .map(a -> new AuditResponse(a.getId(), a.getActorUsername(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())));
    }
}
