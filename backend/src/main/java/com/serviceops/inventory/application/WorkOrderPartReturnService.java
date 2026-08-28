package com.serviceops.inventory.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnPartRequest;
import com.serviceops.inventory.web.WorkOrderPartDtos.ReturnablePartResponse;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderPartReturnService {
    private final SparePartRepository sparePartRepository;
    private final WorkOrderPartWorkflowPolicy workflowPolicy;
    private final WorkOrderPartStockService stockService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ReturnablePartResponse getReturnablePart(UUID workOrderId, UUID sparePartId) {
        workflowPolicy.requireAnyRole(
                Set.of("OWNER", "WAREHOUSE_STAFF"),
                "Bạn không có quyền xem phụ tùng có thể hoàn trả"
        );
        WorkOrder workOrder = workflowPolicy.requireViewableWorkOrder(workOrderId);
        SparePart part = sparePartRepository.findByIdAndTenantId(sparePartId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
        BigDecimal returnable = stockService.returnableQuantity(CurrentUser.tenantId(), workOrderId, sparePartId);
        return WorkOrderPartResponseMapper.toReturnableResponse(workOrder, part, returnable);
    }

    @Transactional
    public ReturnablePartResponse returnPart(UUID workOrderId, UUID sparePartId, ReturnPartRequest request) {
        workflowPolicy.requireRole(
                "WAREHOUSE_STAFF",
                "Chỉ nhân viên kho được xác nhận hoàn trả phụ tùng"
        );
        UUID tenantId = CurrentUser.tenantId();
        WorkOrder workOrder = workflowPolicy.requireWorkOrderForUpdate(workOrderId);
        SparePart part = sparePartRepository.findForUpdate(sparePartId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("SPARE_PART_NOT_FOUND", "Không tìm thấy phụ tùng"));
        BigDecimal returnableBefore = stockService.returnableQuantity(tenantId, workOrderId, sparePartId);
        validateReturnQuantity(request.quantity(), returnableBefore, part);

        stockService.returnToStock(part, workOrder, request.quantity(), request.note());
        auditService.record(
                "RETURN_PART",
                "WORK_ORDER",
                workOrder.getId(),
                "Hoàn trả " + WorkOrderPartStockService.formatQuantity(request.quantity()) + " " + part.getUnit()
                        + " - " + part.getSku() + "; lý do: " + request.note().trim()
        );
        return WorkOrderPartResponseMapper.toReturnableResponse(
                workOrder,
                part,
                returnableBefore.subtract(request.quantity()).max(BigDecimal.ZERO)
        );
    }

    private static void validateReturnQuantity(BigDecimal requested, BigDecimal returnable, SparePart part) {
        if (returnable.signum() <= 0) {
            throw BusinessException.conflict(
                    "NO_PARTS_TO_RETURN",
                    "Phiếu công việc không còn phụ tùng này để hoàn trả"
            );
        }
        if (requested.compareTo(returnable) > 0) {
            throw BusinessException.conflict(
                    "RETURN_EXCEEDS_OUTSTANDING",
                    "Số lượng hoàn trả không được vượt quá "
                            + WorkOrderPartStockService.formatQuantity(returnable) + " " + part.getUnit()
            );
        }
    }
}
