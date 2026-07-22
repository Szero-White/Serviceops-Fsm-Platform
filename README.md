# ServiceOps — Field Service, Maintenance & Warranty Management SaaS

ServiceOps là hệ thống quản lý yêu cầu dịch vụ, thiết bị khách hàng, phiếu công việc, lịch kỹ thuật viên và phụ tùng dành cho doanh nghiệp bảo trì/sửa chữa. Repository được thiết kế **local-first**: PostgreSQL chạy bằng Docker, backend và frontend chạy trực tiếp trên máy để dễ debug; chỉ triển khai cloud sau khi flow local ổn định.

## Giá trị nghiệp vụ

- Tiếp nhận yêu cầu từ khách hàng và chuyển thành work order.
- Quản lý thiết bị theo serial number và lịch sử bảo trì.
- Xếp lịch kỹ thuật viên, ngăn lịch chồng lấn trong transaction.
- Quản lý vòng đời work order bằng state transition có kiểm soát.
- Ghi nhận phụ tùng, không cho tồn kho âm và tạo sổ giao dịch kho.
- Upload ảnh/PDF minh chứng; lưu local qua abstraction để thay bằng MinIO/S3 sau này.
- Audit log, notification và dashboard vận hành.
- Phân quyền OWNER, DISPATCHER, CUSTOMER_SERVICE, TECHNICIAN, WAREHOUSE_STAFF.
- Multi-tenant bằng `tenant_id` ở toàn bộ dữ liệu nghiệp vụ.

## Công nghệ

### Backend
- Java 17, Spring Boot 3.5
- Spring Security JWT, Spring Data JPA, PostgreSQL
- Flyway, Bean Validation, Problem Details, Swagger/OpenAPI
- JUnit 5, Mockito, Testcontainers-ready

### Frontend
- React 19, TypeScript, Vite
- Ant Design, TanStack Query, Axios, React Router

### Local infrastructure
- PostgreSQL 17 bằng Docker Compose
- File local trong `backend/data/uploads`
- GitHub Actions build/test

## Chạy nhanh

Xem hướng dẫn chi tiết tại [RUN_LOCAL.md](RUN_LOCAL.md).

```powershell
# 1. PostgreSQL
Copy-Item .env.example .env
docker compose -f docker-compose.local.yml up -d

# 2. Backend — cửa sổ PowerShell thứ nhất
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# 3. Frontend — cửa sổ PowerShell thứ hai
cd frontend
npm install
npm run dev
```

Mở:
- Frontend: http://localhost:3000
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Tài khoản demo

| Vai trò | Username | Password |
|---|---|---|
| Owner | `owner` | `123456` |
| Điều phối | `dispatcher` | `123456` |
| CSKH | `customer-service` | `123456` |
| Kỹ thuật viên | `technician` | `123456` |
| Kho | `warehouse` | `123456` |

> Mật khẩu trên chỉ dành cho profile local và dữ liệu seed.

## Luồng demo chính

1. Đăng nhập bằng `owner` hoặc `dispatcher`.
2. Xem/tạo khách hàng và thiết bị.
3. Tạo yêu cầu dịch vụ.
4. Chuyển yêu cầu thành work order.
5. Xếp lịch và phân công kỹ thuật viên.
6. Đăng nhập tài khoản `technician`, cập nhật `ON_THE_WAY` → `IN_PROGRESS`.
7. Ghi nhận phụ tùng đã dùng và upload ảnh/PDF.
8. Nhập chẩn đoán, giải pháp, hoàn thành công việc.
9. Xác nhận khách hàng, đóng phiếu và xem audit log/dashboard.

## Tài liệu

- [Kiến trúc](docs/ARCHITECTURE.md)
- [Quy trình nghiệp vụ](docs/BUSINESS_FLOW.md)
- [Bảo mật](docs/SECURITY.md)
- [Thiết kế dữ liệu](docs/DATABASE.md)
- [Danh mục API](docs/API.md)
- [Quy trình phát triển](docs/DEVELOPMENT_PROCESS.md)
- [Hướng dẫn sử dụng demo](docs/USER_GUIDE.md)
- [UAT checklist](docs/UAT_CHECKLIST.md)
- [Roadmap](docs/ROADMAP.md)
- [Kết quả kiểm tra trước khi đóng gói](VERIFY_RESULTS.md)

## Trạng thái phiên bản

Đây là **MVP local-first** có vertical slice hoàn chỉnh để học, demo và tiếp tục phát triển. Các tích hợp MinIO, Redis, RabbitMQ, email/SMS và triển khai cloud được cố ý để ở roadmap nhằm giữ bản đầu dễ chạy và dễ kiểm chứng.
