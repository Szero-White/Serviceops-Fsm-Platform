# Thiết kế dữ liệu

## Nhóm bảng

### Identity và tenant
- `tenants`
- `user_accounts`
- `technician_profiles`

### Khách hàng và tài sản
- `customers`
- `assets`

### Dịch vụ
- `service_request_channels`
- `service_requests`
- `work_orders`
- `work_order_status_history`
- `appointments`

### Kho
- `spare_parts`
- `inventory_transactions`

### Hỗ trợ vận hành
- `attachments`
- `notifications`
- `audit_logs`

## Nguyên tắc

- UUID làm primary key cho entity nghiệp vụ.
- `tenant_id` trên dữ liệu tenant-scoped.
- `version` dùng cho optimistic locking ở các entity hỗ trợ concurrency.
- Timestamp lưu theo UTC.
- `inventory_transactions.transaction_type` dùng các giá trị `IMPORT`, `CONSUME`, `RETURN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`; `balance_after` giữ snapshot tồn sau mỗi movement.
- Stocktake/return mới tái sử dụng schema ledger hiện có, không cần migration mới.
- Flyway là nguồn schema; Hibernate dùng `ddl-auto=validate`, không auto-create production schema.
- FK/unique/index được đặt ở database khi quan hệ là relational trực tiếp.
- Query/service vẫn phải giữ tenant scope; FK không thay thế authorization.

### Ngoại lệ có chủ đích: attachment polymorphic reference

`attachments.reference_type + reference_id` có thể trỏ tới `WORK_ORDER`, `ASSET` hoặc `SERVICE_REQUEST`, nên `reference_id` không có một FK duy nhất tới parent table.

Vì vậy application layer phải bảo vệ integrity:

- upload/list/download resolve parent và kiểm tra ownership;
- Asset/Service Request không được hard-delete khi còn attachment;
- xóa attachment xóa metadata trong transaction và dọn physical file sau commit.

## Quan hệ chính

```text
tenant 1─n users/customers/assets/service_requests/work_orders/...
user_account 1─0..1 technician_profile
customer 1─n assets
customer 1─n service_requests
service_request 0..1─1 work_order
asset 1─n service_requests/work_orders
technician 1─n work_orders
technician 1─n appointments
work_order 1─0..1 appointment (MVP)
work_order 1─n status_history
spare_part 1─n inventory_transactions
work_order 0..1─n inventory_transactions
```

Public Work Order creation hiện đi qua Service Request conversion; database vẫn cho nullable source ở schema lịch sử/compatibility nhưng application flow và demo seed hiện giữ source nghiệp vụ rõ ràng.

## Flyway migrations hiện tại

Schema hiện không nằm chỉ trong V1. Phải đọc toàn bộ migration chain theo thứ tự:

1. `V1__initial_schema.sql` — baseline tables/constraints.
2. `V2__service_request_channels.sql` — configurable intake channels.
3. `V3__soft_delete_work_orders.sql` — archive/soft-delete fields for Work Order history.
4. `V4__schedule_board_query_index.sql` — scheduling/query index refinement.
5. `V5__asset_serial_optional.sql` — Asset serial trở thành optional cho intake chưa xác định serial.
6. `V6__inventory_transaction_actor_snapshot.sql` — snapshot actor cho inventory ledger.
7. `V7__notification_feed_cleanup.sql` — data migration loại routine CRUD/import/generic-status bell rows của release cũ; không xóa Audit/Timeline/Inventory Movements.

V1–V6 là migration lịch sử bất biến; thay đổi mới phải thêm migration mới thay vì sửa file cũ.

Source of truth: `backend/src/main/resources/db/migration/`.
