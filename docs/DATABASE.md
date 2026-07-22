# Thiết kế dữ liệu

## Nhóm bảng

### Identity và tenant
- `tenants`
- `user_accounts`

### Khách hàng và tài sản
- `customers`
- `assets`

### Dịch vụ
- `service_requests`
- `work_orders`
- `work_order_status_history`
- `appointments`
- `technician_profiles`

### Kho
- `spare_parts`
- `inventory_transactions`

### Hỗ trợ vận hành
- `attachments`
- `notifications`
- `audit_logs`

## Nguyên tắc

- UUID làm primary key.
- `tenant_id` trên bảng nghiệp vụ.
- `version` cho optimistic locking.
- `created_at`, `updated_at` theo UTC.
- Flyway là nguồn duy nhất thay đổi schema; không dùng Hibernate auto-create.
- Foreign key và unique constraint ở database, không chỉ validation Java.
- Index phục vụ tenant scope, trạng thái, mã phiếu, serial, lịch và timestamp.

## Quan hệ chính

```text
tenant 1─n users/customers/assets/work_orders/...
customer 1─n assets
customer 1─n service_requests
service_request 0..1─1 work_order
asset 1─n work_orders
technician 1─n appointments
work_order 1─1 appointment (MVP)
work_order 1─n status_history
spare_part 1─n inventory_transactions
work_order 0..1─n inventory_transactions
```

Schema chi tiết nằm tại `backend/src/main/resources/db/migration/V1__initial_schema.sql`.
