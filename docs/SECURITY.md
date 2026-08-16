# Bảo mật

## Đã triển khai

- BCrypt cost 12 cho mật khẩu.
- JWT HMAC-SHA256, có issuer và thời hạn; profile production không có secret fallback và yêu cầu secret Base64 tối thiểu 256 bit sau khi giải mã.
- Production/demo mặc định access token 30 phút; local development giữ hành vi cũ.
- Stateless API; không lưu session server.
- Method-level authorization theo role và tenant context lấy từ claim đã ký.
- Bean Validation cho request; tài khoản mới qua API yêu cầu mật khẩu tối thiểu 8 ký tự.
- Login failure throttling theo cặp IP + username, tổng theo account và tổng theo IP cho deployment single-node.
- Correlation/request ID (`X-Request-ID`) được sanitize, đưa vào MDC và trả lại client; exception 500 được log server-side nhưng không trả stack trace.
- Public `DEMO_MODE` giữ nguyên endpoint nhưng chặn DELETE và các administrative writes có tính phá dữ liệu.
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
| Double booking | Transaction + pessimistic technician lock + overlap query + concurrency test |
| Tồn kho âm | Part lock + work-order lock + validate + ledger trong một transaction + concurrency test |
| Hai OWNER vô hiệu hóa nhau | Pessimistic tenant-row lock trước invariant “ít nhất một OWNER active” |
| Upload giả MIME/traversal | Size limit + MIME allowlist + magic bytes + normalized path-boundary check |
| Upload lấp đầy public demo | Quota theo tenant; demo mặc định 100 MiB |
| Brute-force/credential stuffing cơ bản | Layered in-memory login throttling theo pair/account/IP |
| Client gửi request ID độc hại | Header được whitelist ký tự/độ dài, nếu không hợp lệ sẽ tạo UUID mới |
| DB rollback sau khi ghi file | Rollback cleanup; delete physical file sau DB commit |
| Public demo bị phá dữ liệu | Demo safety gate chặn DELETE và administrative writes được bảo vệ |
