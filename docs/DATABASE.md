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
- `inventory_transactions.transaction_type` dùng các giá trị `IMPORT`, `ISSUE`, `CONSUME`, `RETURN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`; `ISSUE` là stock-out của workflow mới, còn `CONSUME` được giữ cho lịch sử legacy. `balance_after` giữ snapshot tồn sau mỗi movement.
- `work_order_part_requests` lưu lifecycle request (`REQUESTED/ISSUED/CANCELLED/UNAVAILABLE/EXPIRED`); partial unique index chỉ cho tối đa một `REQUESTED` active trên cùng Work Order + part.
- `work_order_part_usage` lưu actual `USED` aggregate theo Work Order + part; outstanding được suy ra từ `ISSUE - USED - RETURN`. Schema này được thêm bằng Flyway V11, không sửa V1–V10.
- Flyway là nguồn schema; Hibernate dùng `ddl-auto=validate`, không auto-create production schema.
- FK/unique/index được đặt ở database khi quan hệ là relational trực tiếp.
- Query/service vẫn phải giữ tenant scope; FK không thay thế authorization.

### Ngoại lệ có chủ đích: attachment polymorphic reference

`attachments.reference_type + reference_id` có thể trỏ tới `WORK_ORDER`, `ASSET` hoặc `SERVICE_REQUEST`, nên `reference_id` không có một FK duy nhất tới parent table.

Vì vậy application layer phải bảo vệ integrity:

- upload/list/download resolve parent và kiểm tra ownership;
- `attachments.purpose` phân biệt `GENERAL`, `WORK_EVIDENCE`, `PAYMENT_EVIDENCE`; `locked_at` khóa evidence thanh toán sau khi liên kết Payment;
- Work Order finalized (`CUSTOMER_ACCEPTED`/`CLOSED`/`CANCELLED`) làm `WORK_EVIDENCE` read-only ở application layer;
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
8. `V8__overdue_notification_dedup.sql` — thêm `notifications.event_key`, unique dedupe theo recipient/event và index phục vụ quét appointment quá hạn.
9. `V9__work_order_actor_identity_snapshot.sql` — thêm snapshot `actor_display_name`/`actor_role` cho Work Order status history và audit, đồng thời backfill từ `user_accounts` để Timeline cũ hiển thị đúng người thao tác khi còn đối chiếu được.
10. `V10__work_order_completion_snapshot.sql` — lưu `diagnosis_snapshot`/`resolution_snapshot` trên từng lần Work Order chuyển `COMPLETED`; `work_orders.diagnosis`/`resolution` vẫn là bản mới nhất, còn Timeline giữ kết quả riêng của từng repair cycle. Migration chỉ backfill lần hoàn thành gần nhất từ dữ liệu hiện có để không giả lập lịch sử cũ không thể khôi phục chính xác.
11. `V11__work_order_part_request_and_usage.sql` — part request workflow + actual-used aggregate phục vụ `REQUEST/ISSUE/USED/RETURN`.
12. `V12__billing_payment_reconciliation.sql` — billing snapshot, payment state và cấu hình tài khoản công ty.
13. `V13__payment_receipt.sql` — official payment receipt sau `SETTLED`.
14. `V14__attachment_lifecycle.sql` — phân loại attachment purpose và khóa payment evidence đã liên kết.

V1–V13 là migration lịch sử bất biến ở checkpoint hiện tại; thay đổi schema/data tiếp theo phải thêm migration mới thay vì sửa file cũ.

Source of truth: `backend/src/main/resources/db/migration/`.
