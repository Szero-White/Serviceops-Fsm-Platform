package com.serviceops.dashboard.application;

import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.dashboard.web.DashboardController.DashboardResponse;
import com.serviceops.dashboard.web.DashboardController.RecentWorkOrder;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final SparePartRepository sparePartRepository;

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        UUID tenantId = CurrentUser.tenantId();
        boolean technician = CurrentUser.hasRole("TECHNICIAN");
        var recentPage = technician
                ? workOrderRepository.searchAssigned(tenantId, CurrentUser.userId(), null, "", PageRequest.of(0, 6, Sort.by("createdAt").descending()))
                : workOrderRepository.search(tenantId, null, "", PageRequest.of(0, 6, Sort.by("createdAt").descending()));
        var recent = recentPage.map(w -> new RecentWorkOrder(
                        w.getId(),
                        w.getCode(),
                        w.getSummary(),
                        w.getCustomer().getName(),
                        w.getTechnician() == null ? null : w.getTechnician().getUser().getDisplayName(),
                        w.getStatus(),
                        w.getPriority(),
                        w.getScheduledStart())).getContent();

        if (technician) {
            UUID userId = CurrentUser.userId();
            return new DashboardResponse(
                    0, 0, 0, 0,
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.OPEN),
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.ASSIGNED),
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.IN_PROGRESS),
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.WAITING_FOR_PARTS),
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.COMPLETED),
                    workOrderRepository.countByTenantIdAndTechnicianUserIdAndStatus(tenantId, userId, WorkOrderStatus.CLOSED),
                    0,
                    recent
            );
        }

        return new DashboardResponse(
                customerRepository.countByTenantIdAndActiveTrue(tenantId),
                assetRepository.countByTenantId(tenantId),
                serviceRequestRepository.countByTenantIdAndStatus(tenantId, ServiceRequestStatus.OPEN),
                technicianRepository.countByTenantIdAndActiveTrue(tenantId),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.OPEN),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.ASSIGNED),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.IN_PROGRESS),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.WAITING_FOR_PARTS),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.COMPLETED),
                workOrderRepository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.CLOSED),
                sparePartRepository.countLowStock(tenantId),
                recent
        );
    }
}
