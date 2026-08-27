package com.serviceops.inventory.application;

import com.serviceops.common.web.PageRequestSupport;
import com.serviceops.common.web.PageResponse;
import com.serviceops.inventory.domain.WorkOrderPartRequest;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestStatus;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartRequestResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.OutstandingPartResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartQueryService {
    private final WorkOrderPartRequestRepository requestRepository;
    private final WorkOrderPartUsageRepository usageRepository;
    private final WorkOrderPartStockService stockService;
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
    @Transactional(readOnly = true)
    public List<OutstandingPartResponse> outstandingParts(String search) {
        workflowPolicy.requireAnyRole(
                Set.of("OWNER", "WAREHOUSE_STAFF"),
                "Bạn không có quyền xem vật tư kỹ thuật viên đang giữ"
        );
        UUID tenantId = CurrentUser.tenantId();
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Map<OutstandingKey, WorkOrderPartRequest> unique = new LinkedHashMap<>();
        for (WorkOrderPartRequest request : requestRepository.findIssuedForOutstanding(tenantId)) {
            OutstandingKey key = new OutstandingKey(request.getWorkOrder().getId(), request.getSparePart().getId());
            unique.putIfAbsent(key, request);
        }

        return unique.values().stream()
                .map(request -> {
                    var workOrder = request.getWorkOrder();
                    var part = request.getSparePart();
                    var totals = stockService.totals(tenantId, workOrder.getId(), part.getId());
                    BigDecimal used = usageRepository
                            .findByTenantIdAndWorkOrderIdAndSparePartId(tenantId, workOrder.getId(), part.getId())
                            .map(WorkOrderPartUsage::getUsedQuantity)
                            .orElse(BigDecimal.ZERO);
                    BigDecimal outstanding = totals.issued().subtract(used).subtract(totals.returned()).max(BigDecimal.ZERO);
                    String technicianName = workOrder.getTechnician() == null || workOrder.getTechnician().getUser() == null
                            ? null
                            : workOrder.getTechnician().getUser().getDisplayName();
                    UUID technicianUserId = workOrder.getTechnician() == null || workOrder.getTechnician().getUser() == null
                            ? null
                            : workOrder.getTechnician().getUser().getId();
                    return new OutstandingPartResponse(
                            workOrder.getId(),
                            workOrder.getCode(),
                            workOrder.getSummary(),
                            technicianUserId,
                            technicianName,
                            part.getId(),
                            part.getSku(),
                            part.getName(),
                            part.getUnit(),
                            totals.issued(),
                            used,
                            totals.returned(),
                            outstanding,
                            request.getIssuedAt()
                    );
                })
                .filter(item -> item.outstandingQuantity().signum() > 0)
                .filter(item -> normalizedSearch.isEmpty() || (
                        lower(item.workOrderCode()).contains(normalizedSearch)
                                || lower(item.workOrderSummary()).contains(normalizedSearch)
                                || lower(item.technicianName()).contains(normalizedSearch)
                                || lower(item.sparePartSku()).contains(normalizedSearch)
                                || lower(item.sparePartName()).contains(normalizedSearch)
                ))
                .toList();
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record OutstandingKey(UUID workOrderId, UUID sparePartId) {
    }
}
