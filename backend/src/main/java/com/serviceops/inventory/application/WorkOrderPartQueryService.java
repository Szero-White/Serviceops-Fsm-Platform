package com.serviceops.inventory.application;

import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartQueryService {
    private final WorkOrderPartRequestRepository requestRepository;
    private final WorkOrderPartWorkflowPolicy workflowPolicy;

    @Transactional(readOnly = true)
    public List<PartRequestResponse> requestsForWorkOrder(UUID workOrderId) {
        WorkOrder workOrder = workflowPolicy.requireViewableWorkOrder(workOrderId);
        return requestRepository.findDetailedByWorkOrder(CurrentUser.tenantId(), workOrder.getId()).stream()
                .map(WorkOrderPartResponseMapper::toRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PartRequestResponse> searchRequests(
            WorkOrderPartRequestStatus status,
            String search,
            int page,
            int size
    ) {
        workflowPolicy.requireAnyRole(
                Set.of("OWNER", "WAREHOUSE_STAFF"),
                "Bạn không có quyền xem hàng đợi cấp phụ tùng"
        );
        var pageable = PageRequestSupport.of(page, size, Sort.by("createdAt").ascending());
        return PageResponse.from(requestRepository.search(
                CurrentUser.tenantId(),
                status,
                PageRequestSupport.normalizeSearch(search),
                pageable
        ).map(WorkOrderPartResponseMapper::toRequestResponse));
    }
}
