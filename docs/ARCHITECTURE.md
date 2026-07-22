# Kiến trúc ServiceOps

## 1. Quyết định kiến trúc

ServiceOps dùng **modular monolith**. Mục tiêu là giữ một artifact backend dễ chạy, dễ debug và triển khai cho khách hàng nhỏ, nhưng ranh giới module đủ rõ để tách service khi quy mô thực sự yêu cầu.

```text
React SPA
   │ REST/JSON + JWT
   ▼
Spring Boot Modular Monolith
   ├── identity/security
   ├── customer
   ├── asset
   ├── service-request
   ├── work-order
   ├── technician/scheduling
   ├── inventory
   ├── attachment
   ├── notification
   ├── audit
   └── dashboard
   │
   ├── PostgreSQL (system of record)
   └── Local file storage (MVP; adapter thay MinIO/S3 về sau)
```

## 2. Quy tắc package

Mỗi module ưu tiên bốn vùng:

- `domain`: entity, enum, repository port và quy tắc nghiệp vụ.
- `application`: use case, transaction boundary, orchestration.
- `web`: controller, DTO, validation và HTTP mapping.
- `infrastructure`: adapter ngoài hệ thống khi module cần.

`common` chỉ chứa thành phần thật sự dùng chung; không biến thành thư mục “rác”. Module không truy cập trực tiếp controller/DTO của module khác.

## 3. Transaction boundary

- Controller không chứa business rule.
- Application service mở transaction cho use case thay đổi dữ liệu.
- PostgreSQL là nguồn sự thật duy nhất.
- Phân công kỹ thuật viên khóa hồ sơ kỹ thuật viên và kiểm tra overlap trong cùng transaction.
- Trừ kho khóa phụ tùng, kiểm tra số lượng, cập nhật tồn và ghi inventory transaction trong cùng transaction.

## 4. Multi-tenancy

MVP dùng shared database/shared schema:

- Mọi entity nghiệp vụ kế thừa `TenantScopedEntity`.
- `tenantId` lấy từ JWT, không nhận tùy ý từ request body.
- Repository luôn lọc theo `tenantId`.
- Unique constraint quan trọng kết hợp `tenant_id`.

Roadmap production có thể bổ sung PostgreSQL Row-Level Security và kiểm thử xuyên tenant toàn diện.

## 5. Khả năng thay thế hạ tầng

`FileStorageService` là interface. Bản local dùng `LocalFileStorageService`; bản triển khai có thể thêm `S3FileStorageService` hoặc `MinioFileStorageService` mà không đổi controller/business flow.

Redis và RabbitMQ chưa được bật trong MVP. Chỉ thêm sau khi flow đồng bộ chạy ổn và có use case cụ thể.
