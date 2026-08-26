package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageResponse;
import com.serviceops.inventory.web.WorkOrderPartDtos.PartUsageUpdateRequest;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartUsageService {
    private final WorkOrderPartUsageRepository usageRepository;
    private final SparePartRepository sparePartRepository;
    private final WorkOrderPartWorkflowPolicy workflowPolicy;
    private final WorkOrderPartStockService stockService;
    private final AuditService auditService;

    @Transactional
    public PartUsageResponse updateUsage(UUID workOrderId, PartUsageUpdateRequest request) {
        WorkOrder workOrder = workflowPolicy.requireAssignedWorkOrderForUsage(workOrderId);
        UUID tenantId = CurrentUser.tenantId();
        SparePart part = sparePartRepository.findByIdAndTenantId(request.sparePartId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));

        var totals = stockService.totals(tenantId, workOrderId, part.getId());
        if (totals.issued().signum() <= 0) {
            throw BusinessException.conflict("PART_NOT_ISSUED", "Kho chưa cấp phụ tùng này cho phiếu công việc");
        }
        BigDecimal usableMaximum = totals.issued().subtract(totals.returned()).max(BigDecimal.ZERO);
        if (request.usedQuantity().compareTo(usableMaximum) > 0) {
            throw BusinessException.conflict(
                    "PART_USAGE_EXCEEDS_AVAILABLE",
                    "Số lượng thực tế sử dụng không được vượt quá "
                            + WorkOrderPartStockService.formatQuantity(usableMaximum) + " " + part.getUnit()
            );
        }

        WorkOrderPartUsage usage = usageRepository.findForUpdate(tenantId, workOrderId, part.getId())
                .orElseGet(() -> newUsage(workOrder, part, tenantId));
        BigDecimal previous = usage.getUsedQuantity();
        usage.setUsedQuantity(request.usedQuantity());
        usage.setUpdatedByUserId(CurrentUser.userId());
        usage.setUpdatedByUsername(CurrentUser.username());
        usage.setUpdatedByDisplayName(CurrentUser.displayName());
        usageRepository.save(usage);

        auditService.record(
                "CONFIRM_PART_USAGE",
                "WORK_ORDER",
                workOrder.getId(),
                "Xác nhận thực tế sử dụng " + part.getSku() + ": "
                        + WorkOrderPartStockService.formatQuantity(previous) + " -> "
                        + WorkOrderPartStockService.formatQuantity(request.usedQuantity()) + " " + part.getUnit()
        );
        return WorkOrderPartResponseMapper.toUsageResponse(
                workOrder,
                part,
                totals.issued(),
                request.usedQuantity(),
                totals.returned(),
                usage
        );
    }

    @Transactional(readOnly = true)
    public List<PartUsageResponse> usageForWorkOrder(UUID workOrderId) {
        WorkOrder workOrder = workflowPolicy.requireViewableWorkOrder(workOrderId);
        UUID tenantId = CurrentUser.tenantId();
        Map<UUID, WorkOrderPartStockService.PartStockSummary> summaries = new LinkedHashMap<>(
                stockService.summariesForWorkOrder(tenantId, workOrderId)
        );
        Map<UUID, WorkOrderPartUsage> usageByPart = new LinkedHashMap<>();

        for (WorkOrderPartUsage usage : usageRepository.findDetailedByWorkOrder(tenantId, workOrderId)) {
            usageByPart.put(usage.getSparePart().getId(), usage);
            summaries.putIfAbsent(
                    usage.getSparePart().getId(),
                    new WorkOrderPartStockService.PartStockSummary(
                            usage.getSparePart(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    )
            );
        }

        List<PartUsageResponse> responses = new ArrayList<>();
        for (Map.Entry<UUID, WorkOrderPartStockService.PartStockSummary> entry : summaries.entrySet()) {
            WorkOrderPartUsage usage = usageByPart.get(entry.getKey());
            var summary = entry.getValue();
            responses.add(WorkOrderPartResponseMapper.toUsageResponse(
                    workOrder,
                    summary.part(),
                    summary.issued(),
                    usage == null ? BigDecimal.ZERO : usage.getUsedQuantity(),
                    summary.returned(),
                    usage
            ));
        }
        return responses;
    }

    private static WorkOrderPartUsage newUsage(WorkOrder workOrder, SparePart part, UUID tenantId) {
        WorkOrderPartUsage usage = new WorkOrderPartUsage();
        usage.setTenantId(tenantId);
        usage.setWorkOrder(workOrder);
        usage.setSparePart(part);
        usage.setUsedQuantity(BigDecimal.ZERO);
        return usage;
    }
}
