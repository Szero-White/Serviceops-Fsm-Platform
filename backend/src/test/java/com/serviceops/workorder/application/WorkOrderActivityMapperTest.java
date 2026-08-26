package com.serviceops.workorder.application;

import com.serviceops.audit.domain.AuditLog;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderActivityMapperTest {

    @Test
    void keepsOperationalTimelineFocusedOnStatusAndTechnicianConsumption() {
        SparePart part = part("FILTER-AC-01", "Lưới lọc máy lạnh tiêu chuẩn", "cái");
        WorkOrderStatusHistory started = statusHistory(
                WorkOrderStatus.IN_PROGRESS,
                "technician",
                "Phạm Quốc Kỹ thuật",
                "TECHNICIAN",
                "2026-08-24T03:23:00Z"
        );
        InventoryTransaction consumed = partTransaction(
                part,
                InventoryTransactionType.CONSUME,
                "4.000",
                "technician",
                "Phạm Quốc Kỹ thuật",
                "TECHNICIAN",
                "Lắp thay cho khách",
                "2026-08-24T03:40:00Z"
        );
        InventoryTransaction returned = partTransaction(
                part,
                InventoryTransactionType.RETURN,
                "1.000",
                "warehouse",
                "Nguyễn Văn Kho",
                "WAREHOUSE_STAFF",
                "Trả phần chưa dùng",
                "2026-08-24T04:00:00Z"
        );
        WorkOrderStatusHistory completed = statusHistory(
                WorkOrderStatus.COMPLETED,
                "technician",
                "Phạm Quốc Kỹ thuật",
                "TECHNICIAN",
                "2026-08-24T08:56:00Z"
        );
        completed.setDiagnosisSnapshot("Tụ khởi động suy giảm");
        completed.setResolutionSnapshot("Thay tụ và chạy thử ổn định");
        completed.setNote("Đã bàn giao vận hành");

        var activities = WorkOrderActivityMapper.merge(
                List.of(started, completed),
                List.of(consumed, returned)
        );

        assertThat(activities).extracting(activity -> activity.type()).containsExactly(
                WorkOrderActivityType.STATUS_CHANGE,
                WorkOrderActivityType.PART_CONSUMED,
                WorkOrderActivityType.STATUS_CHANGE
        );

        var statusActivity = activities.getFirst();
        assertThat(statusActivity.actor()).isEqualTo("technician");
        assertThat(statusActivity.actorDisplayName()).isEqualTo("Phạm Quốc Kỹ thuật");
        assertThat(statusActivity.actorRole()).isEqualTo("TECHNICIAN");

        var consumeActivity = activities.get(1);
        assertThat(consumeActivity.sparePartSku()).isEqualTo("FILTER-AC-01");
        assertThat(consumeActivity.sparePartName()).isEqualTo("Lưới lọc máy lạnh tiêu chuẩn");
        assertThat(consumeActivity.quantity()).isEqualByComparingTo("4.000");
        assertThat(consumeActivity.unit()).isEqualTo("cái");
        assertThat(consumeActivity.actor()).isEqualTo("technician");
        assertThat(consumeActivity.actorDisplayName()).isEqualTo("Phạm Quốc Kỹ thuật");
        assertThat(consumeActivity.actorRole()).isEqualTo("TECHNICIAN");
        assertThat(consumeActivity.note()).isEqualTo("Lắp thay cho khách");

        var completionActivity = activities.getLast();
        assertThat(completionActivity.status()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(completionActivity.diagnosis()).isEqualTo("Tụ khởi động suy giảm");
        assertThat(completionActivity.resolution()).isEqualTo("Thay tụ và chạy thử ổn định");
        assertThat(completionActivity.note()).isEqualTo("Đã bàn giao vận hành");
    }



    @Test
    void preservesDifferentCompletionSnapshotsAcrossReopenedRepairCycles() {
        WorkOrderStatusHistory firstCompletion = statusHistory(
                WorkOrderStatus.COMPLETED,
                "technician",
                "Phạm Quốc Kỹ thuật",
                "TECHNICIAN",
                "2026-08-24T08:56:00Z"
        );
        firstCompletion.setDiagnosisSnapshot("Hỏng van cấp nước");
        firstCompletion.setResolutionSnapshot("Thay van cấp nước");
        firstCompletion.setNote("Đã chạy thử một chu kỳ");

        WorkOrderStatusHistory reopened = statusHistory(
                WorkOrderStatus.REOPENED,
                "customer-service",
                "Trần Mai CSKH",
                "CUSTOMER_SERVICE",
                "2026-08-24T09:20:00Z"
        );
        reopened.setNote("Khách phản ánh lỗi còn tái diễn");

        WorkOrderStatusHistory secondCompletion = statusHistory(
                WorkOrderStatus.COMPLETED,
                "technician",
                "Phạm Quốc Kỹ thuật",
                "TECHNICIAN",
                "2026-08-24T10:15:00Z"
        );
        secondCompletion.setDiagnosisSnapshot("Relay cấp nguồn trên bo điều khiển chập chờn");
        secondCompletion.setResolutionSnapshot("Thay relay và kiểm tra lại tín hiệu cấp nước");
        secondCompletion.setNote("Đã test lại hai chu kỳ ổn định");

        var activities = WorkOrderActivityMapper.merge(
                List.of(firstCompletion, reopened, secondCompletion),
                List.of()
        );

        assertThat(activities).hasSize(3);
        assertThat(activities.get(0).diagnosis()).isEqualTo("Hỏng van cấp nước");
        assertThat(activities.get(0).resolution()).isEqualTo("Thay van cấp nước");
        assertThat(activities.get(0).note()).isEqualTo("Đã chạy thử một chu kỳ");
        assertThat(activities.get(2).diagnosis()).isEqualTo("Relay cấp nguồn trên bo điều khiển chập chờn");
        assertThat(activities.get(2).resolution()).isEqualTo("Thay relay và kiểm tra lại tín hiệu cấp nước");
        assertThat(activities.get(2).note()).isEqualTo("Đã test lại hai chu kỳ ổn định");
    }

    @Test
    void ignoresNonTechnicianConsumeAndWarehouseReturnInOperationalTimeline() {
        SparePart part = part("SENSOR-TEMP-10K", "Cảm biến nhiệt độ 10K", "cái");
        InventoryTransaction legacyWarehouseConsume = partTransaction(
                part,
                InventoryTransactionType.CONSUME,
                "1.000",
                "warehouse",
                "Đặng Nam Kho",
                "WAREHOUSE_STAFF",
                "Legacy UAT consume",
                "2026-08-19T09:45:00Z"
        );
        InventoryTransaction warehouseReturn = partTransaction(
                part,
                InventoryTransactionType.RETURN,
                "1.000",
                "warehouse",
                "Đặng Nam Kho",
                "WAREHOUSE_STAFF",
                "Trả phần chưa dùng",
                "2026-08-19T10:00:00Z"
        );

        assertThat(WorkOrderActivityMapper.merge(
                List.of(),
                List.of(legacyWarehouseConsume, warehouseReturn)
        )).isEmpty();
    }

    @Test
    void includesRedispatchAuditAsDedicatedTimelineActivity() {
        AuditLog redispatch = new AuditLog();
        redispatch.setId(UUID.randomUUID());
        redispatch.setActorUsername("dispatcher");
        redispatch.setActorDisplayName("Lê Thu Điều phối");
        redispatch.setActorRole("DISPATCHER");
        redispatch.setAction("RESCHEDULE");
        redispatch.setEntityType("WORK_ORDER");
        redispatch.setEntityId(UUID.randomUUID());
        redispatch.setDetails("Đã điều phối lại kỹ thuật viên từ A sang B. Lý do: đáp ứng khách hàng nhanh hơn");
        redispatch.setCreatedAt(Instant.parse("2026-08-24T03:30:00Z"));

        var activities = WorkOrderActivityMapper.merge(List.of(), List.of(), List.of(redispatch));

        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().type()).isEqualTo(WorkOrderActivityType.DISPATCH_UPDATED);
        assertThat(activities.getFirst().actor()).isEqualTo("dispatcher");
        assertThat(activities.getFirst().actorDisplayName()).isEqualTo("Lê Thu Điều phối");
        assertThat(activities.getFirst().actorRole()).isEqualTo("DISPATCHER");
        assertThat(activities.getFirst().note()).contains("từ A sang B");
    }

    @Test
    void ignoresNonWorkOrderPartMovementTypesDefensively() {
        SparePart part = part("FILTER-AC-01", "Lưới lọc máy lạnh tiêu chuẩn", "cái");
        InventoryTransaction importTransaction = partTransaction(
                part,
                InventoryTransactionType.IMPORT,
                "10.000",
                "warehouse",
                "Nguyễn Văn Kho",
                "WAREHOUSE_STAFF",
                "Nhập kho",
                "2026-08-24T03:00:00Z"
        );

        assertThat(WorkOrderActivityMapper.merge(List.of(), List.of(importTransaction))).isEmpty();
    }

    private static WorkOrderStatusHistory statusHistory(
            WorkOrderStatus status,
            String actor,
            String actorDisplayName,
            String actorRole,
            String createdAt
    ) {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setId(UUID.randomUUID());
        history.setToStatus(status);
        history.setChangedBy(actor);
        history.setActorDisplayName(actorDisplayName);
        history.setActorRole(actorRole);
        history.setCreatedAt(Instant.parse(createdAt));
        return history;
    }

    private static SparePart part(String sku, String name, String unit) {
        SparePart part = new SparePart();
        part.setId(UUID.randomUUID());
        part.setSku(sku);
        part.setName(name);
        part.setUnit(unit);
        return part;
    }

    private static InventoryTransaction partTransaction(
            SparePart part,
            InventoryTransactionType type,
            String quantity,
            String actor,
            String actorDisplayName,
            String actorRole,
            String note,
            String createdAt
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setSparePart(part);
        transaction.setTransactionType(type);
        transaction.setQuantity(new BigDecimal(quantity));
        transaction.setCreatedBy(actor);
        transaction.setActorDisplayName(actorDisplayName);
        transaction.setActorRole(actorRole);
        transaction.setNote(note);
        transaction.setCreatedAt(Instant.parse(createdAt));
        return transaction;
    }
}
