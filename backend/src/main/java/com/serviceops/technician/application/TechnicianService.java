package com.serviceops.technician.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.technician.web.TechnicianController.TechnicianRequest;
import com.serviceops.technician.web.TechnicianController.TechnicianResponse;
import com.serviceops.workorder.domain.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TechnicianService {
    private final TechnicianRepository repository;
    private final UserAccountRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TechnicianResponse> list(boolean activeOnly) {
        UUID tenantId = CurrentUser.tenantId();
        return (activeOnly ? repository.findActive(tenantId) : repository.findAllDetailed(tenantId))
                .stream()
                .map(TechnicianService::toResponse)
                .toList();
    }

    @Transactional
    public TechnicianResponse create(TechnicianRequest request) {
        UUID tenantId = CurrentUser.tenantId();
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw BusinessException.conflict("TECHNICIAN_USERNAME_EXISTS", "Tên đăng nhập đã tồn tại");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw BusinessException.badRequest("TECHNICIAN_PASSWORD_REQUIRED", "Mật khẩu là bắt buộc khi tạo kỹ thuật viên");
        }

        UserAccount user = new UserAccount();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(request.active() == null || request.active());
        userRepository.save(user);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setTenantId(tenantId);
        technician.setUser(user);
        applyProfile(technician, request.phone(), request.skills(), request.active());
        repository.save(technician);
        auditService.record("CREATE", "TECHNICIAN", technician.getId(), "Tạo kỹ thuật viên " + user.getUsername());
        return toResponse(technician);
    }

    @Transactional
    public TechnicianResponse update(UUID id, TechnicianRequest request) {
        TechnicianProfile technician = require(id);
        UserAccount user = technician.getUser();
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, user.getId())) {
            throw BusinessException.conflict("TECHNICIAN_USERNAME_EXISTS", "Tên đăng nhập đã tồn tại");
        }

        user.setUsername(username);
        user.setDisplayName(request.name().trim());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setActive(request.active() == null || request.active());
        applyProfile(technician, request.phone(), request.skills(), request.active());
        auditService.record("UPDATE", "TECHNICIAN", technician.getId(), "Cập nhật kỹ thuật viên " + user.getUsername());
        return toResponse(technician);
    }

    @Transactional
    public void delete(UUID id) {
        TechnicianProfile technician = require(id);
        UUID tenantId = CurrentUser.tenantId();
        long workOrderCount = workOrderRepository.countByTenantIdAndTechnicianId(tenantId, id);
        long appointmentCount = appointmentRepository.countByTenantIdAndTechnicianId(tenantId, id);
        if (workOrderCount > 0 || appointmentCount > 0) {
            throw BusinessException.conflict("TECHNICIAN_IN_USE", "Không thể xóa kỹ thuật viên đã có lịch hoặc work order");
        }

        UserAccount user = technician.getUser();
        repository.delete(technician);
        userRepository.delete(user);
        auditService.record("DELETE", "TECHNICIAN", id, "Xóa kỹ thuật viên " + user.getUsername());
    }

    private TechnicianProfile require(UUID id) {
        return repository.findDetailed(id, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("TECHNICIAN_NOT_FOUND", "Không tìm thấy kỹ thuật viên"));
    }

    private static void applyProfile(TechnicianProfile technician, String phone, String skills, Boolean active) {
        technician.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        technician.setSkills(skills == null || skills.isBlank() ? null : skills.trim());
        technician.setActive(active == null || active);
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private static TechnicianResponse toResponse(TechnicianProfile technician) {
        UserAccount user = technician.getUser();
        return new TechnicianResponse(
                technician.getId(),
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                technician.getPhone(),
                technician.getSkills(),
                technician.isActive(),
                user.isActive()
        );
    }
}
