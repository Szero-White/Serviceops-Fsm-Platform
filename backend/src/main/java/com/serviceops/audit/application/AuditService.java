package com.serviceops.audit.application;

import jakarta.persistence.criteria.Predicate;
import com.serviceops.audit.domain.AuditLog;
import com.serviceops.audit.domain.AuditLogRepository;
import com.serviceops.audit.web.AuditController.AuditResponse;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, UUID entityId, String details) {
        recordAs(
                CurrentUser.tenantId(),
                CurrentUser.username(),
                CurrentUser.displayName(),
                CurrentUser.primaryRole(),
                action,
                entityType,
                entityId,
                details
        );
    }

    public void recordAs(UUID tenantId, String actor, String action, String entityType, UUID entityId, String details) {
        recordAs(tenantId, actor, null, null, action, entityType, entityId, details);
    }

    public void recordAs(
            UUID tenantId,
            String actor,
            String actorDisplayName,
            String actorRole,
            String action,
            String entityType,
            UUID entityId,
            String details
    ) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setActorUsername(actor);
        log.setActorDisplayName(actorDisplayName);
        log.setActorRole(actorRole);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findEntityEvents(UUID entityId, String entityType, List<String> actions) {
        if (entityId == null || actions == null || actions.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndEntityTypeAndEntityIdAndActionInOrderByCreatedAtAsc(
                CurrentUser.tenantId(),
                entityType,
                entityId,
                actions
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> list(
            int page,
            int size,
            String query,
            String actor,
            String action,
            String entityType,
            Instant from,
            Instant to
    ) {
        validateRange(from, to);

        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").descending());

        String normalizedQuery = trimToNull(query);
        String normalizedActor = trimToNull(actor);
        String normalizedAction = upperToNull(action);
        String normalizedEntityType = upperToNull(entityType);
        UUID entityId = tryParseUuid(normalizedQuery);

        Specification<AuditLog> specification = buildSpecification(
                CurrentUser.tenantId(),
                normalizedQuery,
                entityId,
                normalizedActor,
                normalizedAction,
                normalizedEntityType,
                from,
                to
        );

        return PageResponse.from(repository.findAll(specification, pageable)
                .map(AuditService::toResponse));
    }

    private static Specification<AuditLog> buildSpecification(
            UUID tenantId,
            String query,
            UUID entityId,
            String actor,
            String action,
            String entityType,
            Instant from,
            Instant to
    ) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));

            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (actor != null) {
                String pattern = likePattern(actor);
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("actorUsername")), pattern),
                        builder.like(builder.lower(root.get("actorDisplayName")), pattern),
                        builder.like(builder.lower(root.get("actorRole")), pattern)
                ));
            }
            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }
            if (entityType != null) {
                predicates.add(builder.equal(root.get("entityType"), entityType));
            }
            if (query != null) {
                String pattern = likePattern(query);
                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(builder.like(builder.lower(root.get("actorUsername")), pattern));
                searchPredicates.add(builder.like(builder.lower(root.get("actorDisplayName")), pattern));
                searchPredicates.add(builder.like(builder.lower(root.get("actorRole")), pattern));
                searchPredicates.add(builder.like(builder.lower(root.get("action")), pattern));
                searchPredicates.add(builder.like(builder.lower(root.get("entityType")), pattern));
                searchPredicates.add(builder.like(builder.lower(root.get("details")), pattern));
                if (entityId != null) {
                    searchPredicates.add(builder.equal(root.get("entityId"), entityId));
                }
                predicates.add(builder.or(searchPredicates.toArray(Predicate[]::new)));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessException.badRequest("INVALID_AUDIT_RANGE", "Khoảng thời gian audit không hợp lệ");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static UUID tryParseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static AuditResponse toResponse(AuditLog audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getActorUsername(),
                audit.getAction(),
                audit.getEntityType(),
                audit.getEntityId(),
                audit.getDetails(),
                audit.getCreatedAt()
        );
    }
}
