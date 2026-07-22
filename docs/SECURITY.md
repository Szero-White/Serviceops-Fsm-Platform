# Bảo mật

## Đã triển khai trong MVP

- BCrypt cost 12 cho mật khẩu.
- JWT HMAC-SHA256, có issuer và thời hạn.
- Stateless API; không lưu session server.
- Method-level authorization theo role.
- Tenant context lấy từ claim đã ký.
- Bean Validation cho request.
- MIME whitelist và giới hạn 10 MB khi upload.
- CORS whitelist cho localhost.
- Problem Details không trả stack trace cho client.
- `.env` và dữ liệu runtime bị loại khỏi Git.
- Audit log cho hành động quan trọng.
- Pessimistic locking cho lịch và tồn kho.

## Việc bắt buộc trước production

- Đổi JWT secret, database password và toàn bộ tài khoản seed.
- Chạy HTTPS qua reverse proxy.
- Access token ngắn hơn; bổ sung refresh-token rotation/revocation.
- Rate limiting cho login/upload.
- Antivirus scan cho file và signed URL từ object storage.
- CSP, HSTS, secure headers và chính sách CORS theo domain thật.
- Secret manager thay `.env` trên server.
- Backup, restore drill và mã hóa ổ đĩa/object storage.
- Structured logging có correlation ID, không log token/mật khẩu.
- Dependency/container scanning trong CI.
- Kiểm thử IDOR, cross-tenant, privilege escalation và file upload.

## Threat model ngắn

| Rủi ro | Biện pháp |
|---|---|
| Đọc chéo doanh nghiệp | Tenant claim + repository tenant scope |
| Kỹ thuật viên sửa phiếu người khác | Kiểm tra assigned technician tại service |
| Double booking | Transaction + pessimistic lock + overlap query |
| Tồn kho âm | Lock part + validate + ledger trong một transaction |
| Upload file nguy hiểm | Giới hạn MIME/kích thước; production thêm scan |
| Brute force login | Production thêm rate limit/lockout/MFA |
