package com.serviceops.technician.application;

import com.serviceops.security.CurrentUser;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.technician.web.TechnicianController.TechnicianResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnicianService {
    private final TechnicianRepository repository;

    @Transactional(readOnly = true)
    public List<TechnicianResponse> list() {
        return repository.findActive(CurrentUser.tenantId()).stream()
                .map(t -> new TechnicianResponse(t.getId(), t.getUser().getId(), t.getUser().getDisplayName(), t.getUser().getUsername(), t.getPhone(), t.getSkills(), t.isActive()))
                .toList();
    }
}
