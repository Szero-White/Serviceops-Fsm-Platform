# ServiceOps FSM

ServiceOps FSM là hệ thống quản lý vận hành dịch vụ hiện trường dành cho doanh nghiệp bảo trì và sửa chữa.

Hệ thống quản lý xuyên suốt một hồ sơ dịch vụ từ khi tiếp nhận yêu cầu, lập phiếu công việc, phân công kỹ thuật viên, sử dụng phụ tùng, hoàn thành công việc, xác nhận khách hàng, thanh toán, phát hành biên nhận đến lưu lịch sử truy vết.

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

## Live demo

**ServiceOps:**  
https://serviceops-fsm.centralindia.cloudapp.azure.com

Bản demo có sẵn tài khoản cho năm vai trò nghiệp vụ chính.

| Vai trò | Tài khoản | Mật khẩu | Chức năng chính |
| --- | --- | --- | --- |
| Chủ hệ thống | `owner` | `Demo@2026` | Dashboard, người dùng, nhật ký hệ thống và giám sát vận hành |
| Điều phối viên | `dispatcher` | `Demo@2026` | Phiếu công việc, phân công kỹ thuật viên và xếp lịch |
| Chăm sóc khách hàng | `customer-service` | `Demo@2026` | Khách hàng, thiết bị, yêu cầu dịch vụ và tạo Work Order |
| Kỹ thuật viên | `technician` | `Demo@2026` | Lịch cá nhân, công việc được giao và thực hiện công việc hiện trường |
| Nhân viên kho | `warehouse` | `Demo@2026` | Yêu cầu phụ tùng, xuất/trả kho, kiểm kê và lịch sử tồn kho |

## Luồng nghiệp vụ chính

```text
Khách hàng / Thiết bị
        ↓
Yêu cầu dịch vụ
        ↓
Phiếu công việc
        ↓
Phân công / Xếp lịch
        ↓
Kỹ thuật viên thực hiện
        ↓
Phụ tùng
        ↓
Hoàn thành công việc
        ↓
Khách hàng xác nhận
        ↓
Thanh toán
        ↓
Biên nhận / Đóng phiếu
        ↓
Lịch sử / Audit / Notification
```

Service Request không tự tạo Work Order. `CUSTOMER_SERVICE` tiếp nhận và chuyển đổi khi hồ sơ đủ điều kiện; `DISPATCHER` phân công/xếp lịch; `TECHNICIAN` thực hiện công việc; `WAREHOUSE_STAFF` xử lý vật tư; `OWNER` quản trị và giám sát.

## Quy tắc nghiệp vụ cốt lõi

- Service Request: `OPEN → CONVERTED/CANCELLED`, không hard delete.
- Technician chỉ thao tác Work Order được gán cho mình.
- Dispatcher bị giới hạn redispatch/reschedule sau khi field work đã bắt đầu.
- Inventory: `REQUESTED → ISSUE → USED → RETURN`; `ISSUE` mới giảm stock, `USED` không giảm lần hai.
- Customer acceptance tạo billing snapshot; thay đổi catalog/return về sau không sửa số tiền đã xác nhận.
- Payment phải đi qua đúng pending state trước `SETTLED`; receipt chỉ phát hành sau settlement hợp lệ.
- Closure là use case riêng, không phải generic status transition.
- Business code do backend/database sinh theo tenant + loại + ngày:
  - `KH-YYYYMMDD-NNN`
  - `WO-YYYYMMDD-NNN`
  - `PT-YYYYMMDD-NNN`
  - `BN-YYYYMMDD-NNN`

Chi tiết: [docs/BUSINESS_FLOW.md](docs/BUSINESS_FLOW.md).

## Kiến trúc

Backend là modular monolith Spring Boot, tách theo module nghiệp vụ và các lớp `web` / `application` / `domain`. Frontend tổ chức theo feature và dùng TanStack Query cho server state.

```text
React + TypeScript
      ↓ /api/v1
Spring Boot REST API
      ↓
Application / Domain
      ↓
JPA / JdbcTemplate
      ↓
PostgreSQL
```

- JWT + RBAC + tenant scope bảo vệ truy cập.
- Flyway quản lý schema; JPA chỉ `validate`.
- Transaction/locking bảo vệ các workflow schedule, inventory, payment và business code.
- Audit và notification tách khỏi presentation logic của UI.
- AI chỉ hỗ trợ thao tác/hướng dẫn, không thay business rule.

Chi tiết: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Công nghệ

**Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Springdoc OpenAPI, Maven, Testcontainers.

**Frontend:** React 19, TypeScript 5.9, Vite 8, Ant Design 6, TanStack Query 5, React Router 7, Axios, Playwright.

**Vận hành:** Docker Compose, Nginx, GitHub Actions, Actuator.

## Chạy local

Yêu cầu: Java 21, Node.js 22+, npm, Git và PostgreSQL 17 (native hoặc Docker).

```powershell
Copy-Item .env.example .env
.\scripts\dev-start.ps1 -StartPostgres
```

Nếu PostgreSQL đã chạy:

```powershell
.\scripts\dev-start.ps1
```

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Swagger local: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Xem [RUN_LOCAL.md](RUN_LOCAL.md) khi cần setup hoặc troubleshoot chi tiết.

## Kiểm thử

Local gate:

```powershell
.\scripts\check-local.ps1
```

GitHub Actions kiểm tra backend test/package, frontend lint/build, production-like Docker stack, health/login smoke và Playwright E2E.

Playwright có mutation guard; chỉ chạy trên dữ liệu disposable/isolated.

Checklist release: [docs/UAT_CHECKLIST.md](docs/UAT_CHECKLIST.md).

## Database và migration

Flyway là nguồn quản lý schema. Repository hiện có migration `V1` đến `V20`; migration mới phải append-only.

`V20__standardize_business_codes.sql` chuẩn hóa business code và counter cho generator concurrency-safe. Không dùng `MAX()+1` và không để client tự cấp các mã `KH/WO/PT/BN`.

Production phải backup database trước migration mới.

## Cấu trúc repository

```text
backend/                  Spring Boot application + tests
frontend/                 React application + Playwright E2E
scripts/                  Local/production helper scripts
docs/                     Kiến trúc, nghiệp vụ, deploy, UAT
.github/workflows/        CI
```

## Tài liệu

- [RUN_LOCAL.md](RUN_LOCAL.md) — chạy local
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — kiến trúc, database, security, runtime
- [docs/BUSINESS_FLOW.md](docs/BUSINESS_FLOW.md) — workflow và role ownership
- [docs/PRODUCTION_DEPLOYMENT.md](docs/PRODUCTION_DEPLOYMENT.md) — deploy production-like
- [docs/UAT_CHECKLIST.md](docs/UAT_CHECKLIST.md) — smoke/UAT

## Giới hạn hiện tại

Phiên bản hiện tại ưu tiên một flow field-service end-to-end trên single-node deployment. Local file storage, in-memory login throttling và một số query chưa được benchmark ở tải lớn; chưa có load-test benchmark nên không tuyên bố khả năng chịu tải ở quy mô lớn.

Chỉ thêm cache, object storage hoặc kiến trúc phân tán khi có yêu cầu tải/topology cụ thể.
