package com.serviceops.technician.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.identity.application.DemoAccountProtectionPolicy;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.technician.web.TechnicianController.TechnicianProfileRequest;
import com.serviceops.technician.web.TechnicianController.TechnicianResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TechnicianService {
    private final TechnicianRepository repository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final DemoAccountProtectionPolicy demoAccountProtectionPolicy;

    @Transactional(readOnly = true)
    public List<TechnicianResponse> list(boolean activeOnly) {
        UUID tenantId = CurrentUser.tenantId();

        return (activeOnly ? repository.findActive(tenantId) : repository.findAllDetailed(tenantId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TechnicianResponse updateProfile(UUID id, TechnicianProfileRequest request) {
        TechnicianProfile technician = require(id);
        UserAccount user = technician.getUser();

        demoAccountProtectionPolicy.guardMutation(user);

        technician.setPhone(blankToNull(request.phone()));
        technician.setSkills(blankToNull(request.skills()));
        if (Boolean.TRUE.equals(request.active())
                && (!user.isActive() || user.getRole() != UserRole.TECHNICIAN)) {
            throw com.serviceops.common.exception.BusinessException.conflict(
                    "TECHNICIAN_IDENTITY_INACTIVE",
                    "Không thể kích hoạt hồ sơ kỹ thuật viên khi tài khoản đang tạm ngưng hoặc không còn vai trò Kỹ thuật viên"
            );
        }
        if (request.active() != null) {
            technician.setActive(request.active());
        }

        repository.save(technician);
        auditService.record(
                "UPDATE",
                "TECHNICIAN_PROFILE",
                technician.getId(),
                "Cập nhật hồ sơ kỹ thuật viên " + user.getUsername()
        );
        notificationService.notifyRoles(
                CurrentUser.tenantId(),
                workforceRoles(),
                "Hồ sơ kỹ thuật viên được cập nhật",
                user.getDisplayName()
        );

        return toResponse(technician);
    }

    private TechnicianProfile require(UUID id) {
        return repository.findDetailed(id, CurrentUser.tenantId())
                .orElseThrow(() -> com.serviceops.common.exception.BusinessException.notFound(
                        "TECHNICIAN_NOT_FOUND",
                        "Không tìm thấy kỹ thuật viên"
                ));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<UserRole> workforceRoles() {
        return List.of(UserRole.OWNER, UserRole.DISPATCHER);
    }

    private TechnicianResponse toResponse(TechnicianProfile technician) {
        UserAccount user = technician.getUser();

        return new TechnicianResponse(
                technician.getId(),
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                technician.getPhone(),
                technician.getSkills(),
                technician.isActive(),
                user.isActive(),
                demoAccountProtectionPolicy.isProtected(user.getUsername())
        );
    }
}
