# Bảo mật

## Đã triển khai

- BCrypt cost 12 cho mật khẩu.
- JWT HMAC-SHA256, có issuer và thời hạn; profile production không có secret fallback và yêu cầu secret Base64 tối thiểu 256 bit sau khi giải mã.
- Production/demo mặc định access token 30 phút; local development giữ hành vi cũ.
- Stateless API; không lưu session server. Mỗi JWT authenticated được đối chiếu lại với trạng thái `UserAccount`; token cũ bị từ chối nếu tài khoản đã bị tạm ngưng/xóa hoặc identity/role trong token không còn khớp tài khoản hiện tại.
- Method-level authorization theo role và tenant context lấy từ claim đã ký. Frontend dùng cùng ma trận route-role để tránh đưa người dùng vào màn hình không thuộc trách nhiệm, nhưng backend vẫn là lớp authorization quyết định.
- Bean Validation cho request; tài khoản mới qua API yêu cầu mật khẩu tối thiểu 8 ký tự.
- Login failure throttling theo cặp IP + username, tổng theo account và tổng theo IP cho deployment single-node.
- Correlation/request ID (`X-Request-ID`) được sanitize, đưa vào MDC và trả lại client; exception 500 được log server-side nhưng không trả stack trace.
- Public `DEMO_MODE` giữ nguyên CRUD theo RBAC cho dữ liệu do recruiter tạo; service-level policy chỉ bảo vệ seeded demo identities và các service channel `systemDefined`, còn custom channel vẫn CRUD bình thường.
- Demo password được externalize; public demo từ chối khởi động nếu dùng `123456` hoặc placeholder đi kèm source.
- Upload giới hạn 10 MB/request, MIME allowlist JPG/PNG/WEBP/PDF, kiểm tra magic bytes, path normalization/traversal protection và quota theo tenant có thể cấu hình.
- Attachment upload được dọn nếu DB rollback; file vật lý chỉ bị xóa sau DB commit.
- CORS theo allowlist; production yêu cầu cấu hình origin thật.
- Nginx production thêm HSTS, CSP, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` và frame protection.
- `.env`, dữ liệu runtime và backup thật bị loại khỏi Git.
- Audit log cho hành động quan trọng.
- Pessimistic locking cho lịch, tồn kho và các invariant concurrency quan trọng; optimistic-lock conflict được map thành HTTP 409.
- Cross-tenant/RBAC/concurrency paths quan trọng có Testcontainers integration tests trong source.

## Cần hoàn thiện thêm nếu phát triển thành sản phẩm thương mại / scale lớn

- Refresh-token rotation/revocation hoặc chuyển sang mô hình session/BFF phù hợp threat model.
- Frontend hiện lưu access token phía client; nếu threat model yêu cầu chặt hơn, cân nhắc HttpOnly secure cookie/BFF kèm CSRF protection thay vì thay đổi vội trong bản demo này.
- Redis/distributed rate limiting khi chạy nhiều backend instance; limiter hiện tại cố ý là in-memory cho single-node demo.
- Antivirus/malware scanning trước khi phân phối file upload ra ngoài.
- Object storage S3-compatible + signed URL trước khi horizontal scale.
- Secret manager thay `.env` trên server thực tế.
- Structured JSON logging, central log aggregation, dashboards và distributed tracing.
- Dependency/container scanning trong CI nếu runner/tooling cho phép.
- PostgreSQL Row-Level Security như defense-in-depth sau khi có migration/test plan riêng.
- Restore drill định kỳ và mã hóa volume/object storage theo yêu cầu môi trường vận hành.

## Threat model ngắn

| Rủi ro | Biện pháp hiện tại |
|---|---|
| Đọc chéo doanh nghiệp | Tenant claim + repository tenant scope + cross-tenant integration test |
| Kỹ thuật viên sửa phiếu người khác | Role/assignment authorization tại service/controller |
| Kỹ thuật viên xem lịch người khác | `/my-schedule` suy ra `TechnicianProfile` từ signed-in `userId`; client không gửi `technicianId` |
| Double booking | Transaction + pessimistic technician lock + overlap query + concurrency test |
| Technician thao tác Work Order ngoài phạm vi | Backend giới hạn Technician vào Work Order được assign; field progress và bước `CUSTOMER_ACCEPTED`/`CLOSED`/`REOPENED` chỉ áp trên chính job đó. `CANCELLED` vẫn không thuộc Technician |
| Dispatcher thực hiện field/management transition | Service-level target-status policy chỉ cho Dispatcher operational cancellation; field progress/acceptance/close/reopen trả 403 |
| Owner admin override Work Order | OWNER được ghi nhận `CUSTOMER_ACCEPTED`, `CLOSED`, `REOPENED`, `CANCELLED` qua service policy; không được dùng generic transition để giả lập field progress và không consume phụ tùng thay Technician |
| Customer acceptance không có tài khoản CUSTOMER | Assigned Technician hoặc OWNER ghi nhận **Khách xác nhận** sau khi khách đồng ý ngoài hệ thống; Customer Service chỉ tiếp nhận follow-up để `REOPENED`/`CANCELLED`. Actor thật vẫn được lưu history/audit |
| Warehouse đọc Work Order/dashboard | Controller authorization loại Warehouse khỏi Work Order và operational dashboard; frontend default workspace là `/inventory` |
| Đổi account nhưng UI giữ cache role cũ | AuthProvider cancel/clear TanStack Query cache khi login/logout để dữ liệu identity trước không được tái sử dụng |
| Tài khoản bị tạm ngưng nhưng JWT cũ còn hạn | JWT validator kiểm tra lại UserAccount hiện tại; inactive/deleted/stale identity bị từ chối |
| Đổi username làm lệch audit/attachment ownership | Username được cố định sau khi tạo; chỉ display name/password/active profile được cập nhật |
| Tạm ngưng technician đang còn job | User/profile lifecycle guard dùng cùng pessimistic technician lock với scheduling, kiểm tra operational Work Order assignment và trả 409 trước khi deactivate |
| Hard-delete parent làm orphan attachment | Asset/Service Request delete kiểm tra polymorphic attachment reference trước khi xóa |
| Tồn kho âm | Part lock + work-order lock + validate + ledger trong một transaction + concurrency test |
| Hai OWNER vô hiệu hóa nhau | Pessimistic tenant-row lock trước invariant “ít nhất một OWNER active” |
| Upload giả MIME/traversal | Size limit + MIME allowlist + magic bytes + normalized path-boundary check |
| Upload lấp đầy public demo | Quota theo tenant; demo mặc định 100 MiB |
| Brute-force/credential stuffing cơ bản | Layered in-memory login throttling theo pair/account/IP |
| Client gửi request ID độc hại | Header được whitelist ký tự/độ dài, nếu không hợp lệ sẽ tạo UUID mới |
| DB rollback sau khi ghi file | Rollback cleanup; delete physical file sau DB commit |
| Public demo bị phá dữ liệu | Service policy bảo vệ seeded demo identities và system-defined service channels; dữ liệu do recruiter tạo vẫn CRUD theo RBAC để demo đầy đủ chức năng |
