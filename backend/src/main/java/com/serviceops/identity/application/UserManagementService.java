package com.serviceops.identity.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.identity.web.UserManagementController.UserAccountRequest;
import com.serviceops.identity.web.UserManagementController.UserAccountResponse;
import com.serviceops.notification.domain.NotificationRepository;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.tenant.domain.TenantRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    private final UserAccountRepository repository;
    private final TechnicianRepository technicianRepository;
    private final TenantRepository tenantRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final DemoAccountProtectionPolicy demoAccountProtectionPolicy;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> list() {
        UUID tenantId = CurrentUser.tenantId();
        return repository.findByTenantIdOrderByDisplayNameAsc(tenantId).stream()
                .map(user -> toResponse(user, technicianRepository.findByTenantIdAndUserId(tenantId, user.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public UserAccountResponse create(UserAccountRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String username = normalizeUsername(request.username());
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw BusinessException.conflict("USER_USERNAME_EXISTS", "Tên đăng nhập đã tồn tại");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw BusinessException.badRequest("USER_PASSWORD_REQUIRED", "Mật khẩu là bắt buộc khi tạo người dùng");
        }

        UserAccount user = new UserAccount();
        user.setTenantId(tenantId);
        applyUser(user, request, username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        repository.save(user);

        TechnicianProfile technician = syncTechnicianProfile(user, request);
        auditService.record("CREATE", "USER_ACCOUNT", user.getId(), "Tạo người dùng " + user.getUsername() + " với vai trò " + user.getRole());
        return toResponse(user, technician);
    }

    @Transactional
    public UserAccountResponse update(UUID id, UserAccountRequest request) {
        UserAccount user = require(id);
        demoAccountProtectionPolicy.guardMutation(user);
        guardRoleChange(user, request.role());
        guardUsernameChange(user, request.username());
        guardSelfUpdate(user, request);
        guardLastOwner(user, request.role(), request.active());
        guardTechnicianDeactivation(user, request.active());

        String username = normalizeUsername(request.username());
        applyUser(user, request, username);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        TechnicianProfile technician = syncTechnicianProfile(user, request);
        auditService.record("UPDATE", "USER_ACCOUNT", user.getId(), "Cập nhật người dùng " + user.getUsername());
        return toResponse(user, technician);
    }

    @Transactional
    public void delete(UUID id) {
        UserAccount user = require(id);
        demoAccountProtectionPolicy.guardMutation(user);
        if (user.getId().equals(CurrentUser.userId())) {
            throw BusinessException.badRequest("USER_SELF_DELETE_BLOCKED", "Không thể xóa chính tài khoản đang đăng nhập");
        }
        guardLastOwner(user, null, false);

        UUID tenantId = CurrentUser.tenantId();
        TechnicianProfile technician = technicianRepository.findByTenantIdAndUserIdForUpdate(tenantId, user.getId()).orElse(null);
        if (technician != null) {
            long workOrderCount = workOrderRepository.countByTenantIdAndTechnicianId(tenantId, technician.getId());
            long appointmentCount = appointmentRepository.countByTenantIdAndTechnicianId(tenantId, technician.getId());
            if (workOrderCount > 0 || appointmentCount > 0) {
                throw BusinessException.conflict("USER_TECHNICIAN_IN_USE", "Không thể xóa người dùng kỹ thuật viên đã có lịch hoặc phiếu công việc");
            }
            technicianRepository.delete(technician);
        }

        long notificationCount = notificationRepository.countByTenantIdAndRecipientId(tenantId, user.getId());
        if (notificationCount > 0) {
            throw BusinessException.conflict("USER_NOTIFICATION_IN_USE", "Không thể xóa người dùng đã có thông báo hệ thống, hãy tạm ngưng tài khoản thay vì xóa");
        }

        repository.delete(user);
        auditService.record("DELETE", "USER_ACCOUNT", id, "Xóa người dùng " + user.getUsername());
    }

    private UserAccount require(UUID id) {
        return repository.findByIdAndTenantId(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private void applyUser(UserAccount user, UserAccountRequest request, String username) {
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role());
        user.setActive(request.active() == null || request.active());
    }

    private TechnicianProfile syncTechnicianProfile(UserAccount user, UserAccountRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        TechnicianProfile technician = technicianRepository.findByTenantIdAndUserId(tenantId, user.getId()).orElse(null);
        if (user.getRole() != UserRole.TECHNICIAN) {
            if (technician != null) {
                technician.setActive(false);
            }
            return technician;
        }

        boolean newProfile = technician == null;
        if (newProfile) {
            technician = new TechnicianProfile();
            technician.setTenantId(tenantId);
            technician.setUser(user);
        }

        if (newProfile || request.phone() != null) {
            technician.setPhone(blankToNull(request.phone()));
        }
        if (newProfile || request.skills() != null) {
            technician.setSkills(blankToNull(request.skills()));
        }
        if (newProfile) {
            technician.setActive(user.isActive());
        } else if (!user.isActive()) {
            technician.setActive(false);
        }

        return technicianRepository.save(technician);
    }

    private void guardTechnicianDeactivation(UserAccount user, Boolean nextActive) {
        if (user.getRole() != UserRole.TECHNICIAN || !user.isActive() || nextActive == null || nextActive) {
            return;
        }
        UUID tenantId = CurrentUser.tenantId();
        technicianRepository.findByTenantIdAndUserIdForUpdate(tenantId, user.getId()).ifPresent(technician -> {
            if (workOrderRepository.existsActiveAssignment(tenantId, technician.getId())) {
                throw BusinessException.conflict(
                        "TECHNICIAN_ACTIVE_ASSIGNMENTS",
                        "Không thể tạm ngưng kỹ thuật viên khi còn phiếu công việc đang hoạt động; hãy điều phối lại hoặc hủy công việc trước"
                );
            }
        });
    }

    private void guardUsernameChange(UserAccount user, String requestedUsername) {
        String normalized = normalizeUsername(requestedUsername);
        if (!user.getUsername().equalsIgnoreCase(normalized)) {
            throw BusinessException.conflict(
                    "USER_USERNAME_CHANGE_BLOCKED",
                    "Tên đăng nhập được cố định sau khi tạo tài khoản để bảo toàn lịch sử và quyền sở hữu dữ liệu"
            );
        }
    }

    private void guardRoleChange(UserAccount user, UserRole nextRole) {
        if (nextRole == user.getRole()) {
            return;
        }
        throw BusinessException.conflict(
                "USER_ROLE_CHANGE_BLOCKED",
                "Vai trò được cố định sau khi tạo tài khoản để bảo toàn dữ liệu nghiệp vụ liên kết"
        );
    }

    private void guardSelfUpdate(UserAccount user, UserAccountRequest request) {
        if (!user.getId().equals(CurrentUser.userId())) {
            return;
        }
        if (request.active() != null && !request.active()) {
            throw BusinessException.badRequest("USER_SELF_DISABLE_BLOCKED", "Không thể tạm ngưng chính tài khoản đang đăng nhập");
        }
        if (request.role() != user.getRole()) {
            throw BusinessException.badRequest("USER_SELF_ROLE_CHANGE_BLOCKED", "Không thể tự đổi vai trò của chính mình");
        }
    }

    private void guardLastOwner(UserAccount user, UserRole nextRole, Boolean nextActive) {
        if (user.getRole() != UserRole.OWNER) {
            return;
        }
        boolean remainsActiveOwner = (nextRole == null || nextRole == UserRole.OWNER) && (nextActive == null || nextActive);
        if (remainsActiveOwner) {
            return;
        }
        UUID tenantId = CurrentUser.tenantId();
        tenantRepository.findForUpdate(tenantId)
                .orElseThrow(() -> BusinessException.notFound("TENANT_NOT_FOUND", "Không tìm thấy doanh nghiệp hiện tại"));
        long activeOwnerCount = repository.countByTenantIdAndRoleAndActiveTrue(tenantId, UserRole.OWNER);
        if (activeOwnerCount <= 1 && user.isActive()) {
            throw BusinessException.conflict("USER_LAST_OWNER_BLOCKED", "Doanh nghiệp phải còn ít nhất một chủ sở hữu đang hoạt động");
        }
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserAccountResponse toResponse(UserAccount user, TechnicianProfile technician) {
        return new UserAccountResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                technician == null ? null : technician.getId(),
                technician == null ? null : technician.getPhone(),
                technician == null ? null : technician.getSkills(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                demoAccountProtectionPolicy.isProtected(user.getUsername())
        );
    }
}
