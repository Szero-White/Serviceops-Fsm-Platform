package com.serviceops.workorder.application;

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
    void mergesStatusConsumeAndReturnIntoOneChronologicalTimeline() {
        SparePart part = part("FILTER-AC-01", "Lưới lọc máy lạnh tiêu chuẩn", "cái");
        WorkOrderStatusHistory started = statusHistory(
                WorkOrderStatus.IN_PROGRESS,
                "technician",
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
                "2026-08-24T08:56:00Z"
        );

        var activities = WorkOrderActivityMapper.merge(
                List.of(started, completed),
                List.of(consumed, returned)
        );

        assertThat(activities).extracting(activity -> activity.type()).containsExactly(
                WorkOrderActivityType.STATUS_CHANGE,
                WorkOrderActivityType.PART_CONSUMED,
                WorkOrderActivityType.PART_RETURNED,
                WorkOrderActivityType.STATUS_CHANGE
        );

        var consumeActivity = activities.get(1);
        assertThat(consumeActivity.sparePartSku()).isEqualTo("FILTER-AC-01");
        assertThat(consumeActivity.sparePartName()).isEqualTo("Lưới lọc máy lạnh tiêu chuẩn");
        assertThat(consumeActivity.quantity()).isEqualByComparingTo("4.000");
        assertThat(consumeActivity.unit()).isEqualTo("cái");
        assertThat(consumeActivity.actor()).isEqualTo("technician");
        assertThat(consumeActivity.actorDisplayName()).isEqualTo("Phạm Quốc Kỹ thuật");
        assertThat(consumeActivity.actorRole()).isEqualTo("TECHNICIAN");
        assertThat(consumeActivity.note()).isEqualTo("Lắp thay cho khách");

        var returnActivity = activities.get(2);
        assertThat(returnActivity.quantity()).isEqualByComparingTo("1.000");
        assertThat(returnActivity.actorRole()).isEqualTo("WAREHOUSE_STAFF");
        assertThat(returnActivity.note()).isEqualTo("Trả phần chưa dùng");
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

    private static WorkOrderStatusHistory statusHistory(WorkOrderStatus status, String actor, String createdAt) {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setId(UUID.randomUUID());
        history.setToStatus(status);
        history.setChangedBy(actor);
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
