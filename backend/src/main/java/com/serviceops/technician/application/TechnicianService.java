package com.serviceops.technician.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.application.DemoAccountProtectionPolicy;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.technician.web.TechnicianController.TechnicianProfileRequest;
import com.serviceops.technician.web.TechnicianController.TechnicianResponse;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TechnicianService {
    private final TechnicianRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AuditService auditService;
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
        TechnicianProfile technician = requireForUpdate(id);
        UserAccount user = technician.getUser();

        demoAccountProtectionPolicy.guardMutation(user);

        technician.setPhone(blankToNull(request.phone()));
        technician.setSkills(blankToNull(request.skills()));
        if (request.active() != null) {
            if (user.getRole() != UserRole.TECHNICIAN) {
                throw BusinessException.conflict(
                        "TECHNICIAN_ROLE_REQUIRED",
                        "Chỉ tài khoản có vai trò Kỹ thuật viên mới được quản lý tại Đội ngũ kỹ thuật"
                );
            }
            if (!request.active()
                    && (technician.isActive() || user.isActive())
                    && workOrderRepository.existsActiveAssignment(CurrentUser.tenantId(), technician.getId())) {
                throw BusinessException.conflict(
                        "TECHNICIAN_ACTIVE_ASSIGNMENTS",
                        "Không thể tạm ngưng kỹ thuật viên khi còn phiếu công việc đang hoạt động; hãy điều phối lại hoặc hủy công việc trước"
                );
            }

            // One business state, two management entry points: changing the technician
            // status must change login status in the same transaction, and vice versa.
            technician.setActive(request.active());
            user.setActive(request.active());
            userAccountRepository.save(user);
        }

        repository.save(technician);
        auditService.record(
                "UPDATE",
                "TECHNICIAN_PROFILE",
                technician.getId(),
                "Cập nhật hồ sơ kỹ thuật viên " + user.getUsername()
                        + (request.active() == null
                        ? ""
                        : " · trạng thái đồng bộ " + (request.active() ? "Hoạt động" : "Tạm ngưng"))
        );
        if (request.active() != null) {
            auditService.record(
                    "UPDATE",
                    "USER_ACCOUNT",
                    user.getId(),
                    "Đồng bộ trạng thái từ hồ sơ kỹ thuật viên " + user.getUsername()
                            + " -> " + (request.active() ? "Hoạt động" : "Tạm ngưng")
            );
        }

        return toResponse(technician);
    }

    private TechnicianProfile requireForUpdate(UUID id) {
        return repository.findForUpdate(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound(
                        "TECHNICIAN_NOT_FOUND",
                        "Không tìm thấy kỹ thuật viên"
                ));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
                demoAccountProtectionPolicy.isProtected(user.getUsername()),
                technician.getCreatedAt(),
                technician.getUpdatedAt()
        );
    }
}
