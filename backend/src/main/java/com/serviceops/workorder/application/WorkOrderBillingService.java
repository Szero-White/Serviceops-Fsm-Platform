package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.WorkOrderPartUsage;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingItem;
import com.serviceops.workorder.domain.WorkOrderBillingItemRepository;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshotRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.web.WorkOrderBillingDtos.BillingDraftRequest;
import com.serviceops.workorder.web.WorkOrderBillingDtos.BillingItemResponse;
import com.serviceops.workorder.web.WorkOrderBillingDtos.BillingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderBillingService {
    private static final EnumSet<WorkOrderStatus> DRAFT_EDITABLE_STATUSES = EnumSet.of(
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.REOPENED
    );

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderPartUsageRepository usageRepository;
    private final WorkOrderBillingSnapshotRepository snapshotRepository;
    private final WorkOrderBillingItemRepository itemRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public BillingResponse getBilling(UUID workOrderId) {
        WorkOrder workOrder = requireViewableWorkOrder(workOrderId);
        return snapshotRepository.findByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .map(snapshot -> toFrozenResponse(workOrder, snapshot))
                .orElseGet(() -> toDraftResponse(workOrder));
    }

    @Transactional
    public BillingResponse updateDraft(UUID workOrderId, BillingDraftRequest request) {
        requireTechnicianRole();
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        ensureAssignedTechnician(workOrder);
        if (!DRAFT_EDITABLE_STATUSES.contains(workOrder.getStatus())) {
            throw BusinessException.conflict(
                    "BILLING_DRAFT_LOCKED",
                    "Chi phí dịch vụ chỉ được cập nhật trước khi khách xác nhận"
            );
        }
        if (snapshotRepository.findByWorkOrder(CurrentUser.tenantId(), workOrderId).isPresent()) {
            throw BusinessException.conflict("BILLING_ALREADY_FROZEN", "Chi phí đã được khóa khi khách xác nhận");
        }

        BigDecimal laborFee = money(request.laborFee());
        BigDecimal incidentalFee = money(request.incidentalFee());
        String incidentalReason = blankToNull(request.incidentalReason());
        if (incidentalFee.signum() > 0 && incidentalReason == null) {
            throw BusinessException.badRequest(
                    "INCIDENTAL_REASON_REQUIRED",
                    "Phải nhập lý do khi có phí phát sinh"
            );
        }

        workOrder.setLaborFee(laborFee);
        workOrder.setIncidentalFee(incidentalFee);
        workOrder.setIncidentalReason(incidentalFee.signum() == 0 ? null : incidentalReason);
        auditService.record(
                "UPDATE_BILLING_DRAFT",
                "WORK_ORDER",
                workOrder.getId(),
                "Cập nhật phí dịch vụ cho " + workOrder.getCode()
        );
        return toDraftResponse(workOrder);
    }

    public void assertReviewedBillingUnchanged(WorkOrder workOrder, BigDecimal reviewedTotalAmount, String reviewToken) {
        if (reviewedTotalAmount == null) {
            throw BusinessException.badRequest(
                    "REVIEWED_TOTAL_REQUIRED",
                    "Thiếu tổng chi phí khách hàng đã kiểm tra"
            );
        }
        if (reviewToken == null || reviewToken.isBlank()) {
            throw BusinessException.badRequest(
                    "BILLING_REVIEW_TOKEN_REQUIRED",
                    "Thiếu mã xác nhận nội dung chi phí đã được kiểm tra"
            );
        }

        BillingResponse current = toDraftResponse(workOrder);
        boolean sameTotal = money(reviewedTotalAmount).compareTo(money(current.totalAmount())) == 0;
        boolean sameContent = reviewToken.trim().equals(current.reviewToken());
        if (!sameTotal || !sameContent) {
            throw BusinessException.conflict(
                    "BILLING_CHANGED_REVIEW_REQUIRED",
                    "Thông tin phụ tùng hoặc chi phí đã thay đổi. Vui lòng kiểm tra lại trước khi khách xác nhận"
            );
        }
    }

    @Transactional
    public WorkOrderBillingSnapshot freezeForCustomerAcceptance(WorkOrder workOrder) {
        UUID tenantId = CurrentUser.tenantId();
        var existing = snapshotRepository.findByWorkOrder(tenantId, workOrder.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw BusinessException.conflict(
                    "WORK_ORDER_NOT_READY_FOR_ACCEPTANCE",
                    "Chỉ phiếu đã hoàn thành mới được ghi nhận khách xác nhận"
            );
        }

        List<WorkOrderPartUsage> usages = usageRepository.findDetailedByWorkOrder(tenantId, workOrder.getId());
        List<WorkOrderBillingItem> items = new ArrayList<>();
        BigDecimal partsTotal = BigDecimal.ZERO;
        for (WorkOrderPartUsage usage : usages) {
            if (usage.getUsedQuantity() == null || usage.getUsedQuantity().signum() <= 0) {
                continue;
            }
            BigDecimal unitPrice = money(usage.getSparePart().getUnitPrice());
            BigDecimal lineTotal = money(unitPrice.multiply(usage.getUsedQuantity()));
            partsTotal = partsTotal.add(lineTotal);

            WorkOrderBillingItem item = new WorkOrderBillingItem();
            item.setTenantId(tenantId);
            item.setSparePart(usage.getSparePart());
            item.setSparePartSku(usage.getSparePart().getSku());
            item.setSparePartName(usage.getSparePart().getName());
            item.setUnit(usage.getSparePart().getUnit());
            item.setQuantity(usage.getUsedQuantity());
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);
            items.add(item);
        }

        BigDecimal laborFee = money(workOrder.getLaborFee());
        BigDecimal incidentalFee = money(workOrder.getIncidentalFee());
        String incidentalReason = blankToNull(workOrder.getIncidentalReason());
        if (incidentalFee.signum() > 0 && incidentalReason == null) {
            throw BusinessException.badRequest(
                    "INCIDENTAL_REASON_REQUIRED",
                    "Phải nhập lý do phí phát sinh trước khi khách xác nhận"
            );
        }

        WorkOrderBillingSnapshot snapshot = new WorkOrderBillingSnapshot();
        snapshot.setTenantId(tenantId);
        snapshot.setWorkOrder(workOrder);
        snapshot.setPartsTotal(money(partsTotal));
        snapshot.setLaborFee(laborFee);
        snapshot.setIncidentalFee(incidentalFee);
        snapshot.setIncidentalReason(incidentalFee.signum() == 0 ? null : incidentalReason);
        snapshot.setTotalAmount(money(partsTotal.add(laborFee).add(incidentalFee)));
        snapshot.setAcceptedByUserId(CurrentUser.userId());
        snapshot.setAcceptedByUsername(CurrentUser.username());
        snapshot.setAcceptedByDisplayName(CurrentUser.displayName());
        snapshot.setAcceptedAt(Instant.now());
        snapshotRepository.save(snapshot);

        for (WorkOrderBillingItem item : items) {
            item.setBillingSnapshot(snapshot);
        }
        itemRepository.saveAll(items);
        return snapshot;
    }

    private BillingResponse toDraftResponse(WorkOrder workOrder) {
        List<BillingItemResponse> items = draftItems(workOrder.getId());
        BigDecimal partsTotal = items.stream()
                .map(BillingItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal laborFee = money(workOrder.getLaborFee());
        BigDecimal incidentalFee = money(workOrder.getIncidentalFee());
        BigDecimal normalizedPartsTotal = money(partsTotal);
        BigDecimal totalAmount = money(partsTotal.add(laborFee).add(incidentalFee));
        String incidentalReason = blankToNull(workOrder.getIncidentalReason());
        return new BillingResponse(
                workOrder.getId(),
                workOrder.getCode(),
                false,
                items,
                normalizedPartsTotal,
                laborFee,
                incidentalFee,
                incidentalReason,
                totalAmount,
                createReviewToken(items, normalizedPartsTotal, laborFee, incidentalFee, incidentalReason, totalAmount),
                null,
                null
        );
    }

    private BillingResponse toFrozenResponse(WorkOrder workOrder, WorkOrderBillingSnapshot snapshot) {
        List<BillingItemResponse> items = itemRepository
                .findByTenantIdAndBillingSnapshotIdOrderBySparePartNameAsc(CurrentUser.tenantId(), snapshot.getId())
                .stream()
                .map(item -> new BillingItemResponse(
                        item.getSparePart().getId(),
                        item.getSparePartSku(),
                        item.getSparePartName(),
                        item.getUnit(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();
        BigDecimal partsTotal = money(snapshot.getPartsTotal());
        BigDecimal laborFee = money(snapshot.getLaborFee());
        BigDecimal incidentalFee = money(snapshot.getIncidentalFee());
        BigDecimal totalAmount = money(snapshot.getTotalAmount());
        String incidentalReason = blankToNull(snapshot.getIncidentalReason());
        return new BillingResponse(
                workOrder.getId(),
                workOrder.getCode(),
                true,
                items,
                partsTotal,
                laborFee,
                incidentalFee,
                incidentalReason,
                totalAmount,
                createReviewToken(items, partsTotal, laborFee, incidentalFee, incidentalReason, totalAmount),
                snapshot.getAcceptedByDisplayName(),
                snapshot.getAcceptedAt()
        );
    }

    private List<BillingItemResponse> draftItems(UUID workOrderId) {
        return usageRepository.findDetailedByWorkOrder(CurrentUser.tenantId(), workOrderId).stream()
                .filter(usage -> usage.getUsedQuantity() != null && usage.getUsedQuantity().signum() > 0)
                .map(usage -> {
                    BigDecimal unitPrice = money(usage.getSparePart().getUnitPrice());
                    return new BillingItemResponse(
                            usage.getSparePart().getId(),
                            usage.getSparePart().getSku(),
                            usage.getSparePart().getName(),
                            usage.getSparePart().getUnit(),
                            usage.getUsedQuantity(),
                            unitPrice,
                            money(unitPrice.multiply(usage.getUsedQuantity()))
                    );
                })
                .toList();
    }


    private static String createReviewToken(
            List<BillingItemResponse> items,
            BigDecimal partsTotal,
            BigDecimal laborFee,
            BigDecimal incidentalFee,
            String incidentalReason,
            BigDecimal totalAmount
    ) {
        StringBuilder canonical = new StringBuilder();
        items.stream()
                .sorted(Comparator.comparing(item -> item.sparePartId().toString()))
                .forEach(item -> {
                    appendTokenPart(canonical, item.sparePartId().toString());
                    appendTokenPart(canonical, decimal(item.quantity()));
                    appendTokenPart(canonical, decimal(money(item.unitPrice())));
                    appendTokenPart(canonical, decimal(money(item.lineTotal())));
                });
        appendTokenPart(canonical, decimal(money(partsTotal)));
        appendTokenPart(canonical, decimal(money(laborFee)));
        appendTokenPart(canonical, decimal(money(incidentalFee)));
        appendTokenPart(canonical, incidentalReason == null ? "" : incidentalReason);
        appendTokenPart(canonical, decimal(money(totalAmount)));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void appendTokenPart(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value).append('|');
    }

    private static String decimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString();
    }

    private WorkOrder requireViewableWorkOrder(UUID workOrderId) {
        UUID tenantId = CurrentUser.tenantId();
        if (CurrentUser.hasRole("TECHNICIAN")) {
            return workOrderRepository.findDetailedAssigned(workOrderId, tenantId, CurrentUser.userId())
                    .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        }
        if (!CurrentUser.hasRole("OWNER") && !CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            throw BusinessException.forbidden("BILLING_ACCESS_DENIED", "Bạn không có quyền xem thông tin thanh toán");
        }
        return workOrderRepository.findDetailed(workOrderId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
    }

    private static void requireTechnicianRole() {
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden("BILLING_UPDATE_DENIED", "Chỉ kỹ thuật viên được cập nhật chi phí trước khi khách xác nhận");
        }
    }

    private static void ensureAssignedTechnician(WorkOrder workOrder) {
        if (workOrder.getTechnician() == null
                || workOrder.getTechnician().getUser() == null
                || !CurrentUser.userId().equals(workOrder.getTechnician().getUser().getId())) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được thao tác công việc được phân công cho mình");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
