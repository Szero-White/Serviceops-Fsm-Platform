# ServiceOps FSM

**Phần mềm quản lý dịch vụ bảo trì và sửa chữa**

ServiceOps FSM giúp doanh nghiệp quản lý toàn bộ quá trình xử lý một yêu cầu sửa chữa: từ lúc khách hàng báo sự cố, tạo phiếu công việc, phân công kỹ thuật viên, cấp phụ tùng, theo dõi tiến độ, xác nhận hoàn thành, thanh toán đến khi đóng hồ sơ.

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

## Live demo

**ServiceOps:** https://serviceops-fsm.centralindia.cloudapp.azure.com

Bản demo có sẵn các tài khoản theo đúng vai trò trong quy trình làm việc:

| Vai trò | Tài khoản | Mật khẩu | Trách nhiệm chính |
| --- | --- | --- | --- |
| Chủ hệ thống | `owner` | `Demo@2026` | Theo dõi hoạt động, quản lý người dùng, cấu hình và nhật ký hệ thống |
| Chăm sóc khách hàng | `customer-service` | `Demo@2026` | Quản lý khách hàng, thiết bị, yêu cầu dịch vụ, thanh toán và đóng hồ sơ |
| Điều phối viên | `dispatcher` | `Demo@2026` | Phân công kỹ thuật viên, sắp xếp lịch và theo dõi phiếu công việc |
| Kỹ thuật viên | `technician` | `Demo@2026` | Xem lịch, cập nhật tiến độ, yêu cầu phụ tùng và hoàn thành công việc |
| Nhân viên kho | `warehouse` | `Demo@2026` | Xử lý yêu cầu phụ tùng, cấp/trả hàng, kiểm kê và theo dõi tồn kho |

### Gợi ý xem demo trong 5 phút

1. Đăng nhập `customer-service` để xem Khách hàng, Thiết bị và Yêu cầu dịch vụ.
2. Chuyển một Yêu cầu dịch vụ thành Phiếu công việc.
3. Đăng nhập `dispatcher` để phân công và xếp lịch cho kỹ thuật viên.
4. Đăng nhập `technician` để xem lịch và cập nhật tiến độ công việc.
5. Đăng nhập `warehouse` để xem yêu cầu phụ tùng và tồn kho.
6. Đăng nhập `owner` để xem Dashboard, Audit Log và toàn bộ hoạt động của hệ thống.

## Phần mềm quản lý những gì?

Quy trình chính của hệ thống:

```text
Khách hàng báo sự cố
        ↓
Chăm sóc khách hàng tiếp nhận
        ↓
Yêu cầu dịch vụ
        ↓
Phiếu công việc
        ↓
Phân công kỹ thuật viên và xếp lịch
        ↓
Yêu cầu / cấp / sử dụng / trả phụ tùng
        ↓
Kỹ thuật viên hoàn thành công việc
        ↓
Khách hàng xác nhận
        ↓
Thanh toán
        ↓
Phát hành biên nhận
        ↓
Đóng phiếu công việc
        ↓
Lưu lịch sử, thông báo và nhật ký thao tác
```

Một Yêu cầu dịch vụ không tự tạo Phiếu công việc. Nhân viên chăm sóc khách hàng kiểm tra thông tin trước khi chuyển sang Phiếu công việc. Điều phối viên phân công kỹ thuật viên và xếp lịch. Kỹ thuật viên cập nhật tiến độ và sử dụng phụ tùng. Nhân viên kho xử lý việc cấp/trả phụ tùng. Khi công việc hoàn thành và khách hàng xác nhận, hệ thống tiếp tục xử lý thanh toán, biên nhận và đóng hồ sơ.

## Ví dụ một quy trình sử dụng

Khách hàng báo thiết bị bị lỗi. Nhân viên chăm sóc khách hàng tạo Yêu cầu dịch vụ và kiểm tra thông tin khách hàng, thiết bị. Khi đủ thông tin, yêu cầu được chuyển thành Phiếu công việc. Điều phối viên chọn kỹ thuật viên và thời gian thực hiện. Nếu cần thay phụ tùng, kỹ thuật viên gửi yêu cầu cho kho. Sau khi sửa xong, kỹ thuật viên ghi kết quả, khách hàng xác nhận hoàn thành, nhân viên chăm sóc khách hàng xử lý thanh toán và phát hành biên nhận. Cuối cùng Phiếu công việc được đóng và toàn bộ lịch sử vẫn được lưu để tra cứu.

## Chức năng chính

### Khách hàng và thiết bị

- Quản lý thông tin khách hàng.
- Quản lý thiết bị thuộc từng khách hàng.
- Giữ lịch sử ngay cả khi khách hàng hoặc thiết bị không còn hoạt động.

### Yêu cầu dịch vụ

- Tiếp nhận yêu cầu sửa chữa/bảo trì.
- Ghi nội dung, mức ưu tiên và kênh tiếp nhận.
- Chuyển Yêu cầu dịch vụ thành Phiếu công việc khi đủ thông tin.
- Hủy yêu cầu nhưng vẫn giữ lịch sử.

### Phiếu công việc và lịch làm việc

- Tạo mã Phiếu công việc tự động.
- Phân công kỹ thuật viên.
- Sắp xếp và thay đổi lịch làm việc.
- Ngăn việc xếp trùng lịch cho cùng một kỹ thuật viên.
- Theo dõi trạng thái từ lúc bắt đầu đến khi đóng hồ sơ.

### Kỹ thuật viên

- Chỉ làm việc với các Phiếu công việc được giao cho mình.
- Xem lịch cá nhân.
- Cập nhật trạng thái công việc.
- Ghi chẩn đoán, kết quả sửa chữa và số lượng phụ tùng đã dùng.

### Kho và phụ tùng

- Kỹ thuật viên gửi yêu cầu phụ tùng.
- Kho cấp phụ tùng và cập nhật tồn kho.
- Ghi nhận số lượng thực tế đã dùng.
- Trả phần phụ tùng còn dư về kho.
- Kiểm kê và theo dõi lịch sử biến động kho.

### Thanh toán và biên nhận

- Hỗ trợ chuyển khoản, tiền mặt và thanh toán tại quầy.
- Chỉ xác nhận thanh toán khi đúng bước xử lý.
- Chỉ phát hành biên nhận sau khi thanh toán đã được xác nhận.
- Tránh tạo biên nhận hoặc xác nhận thanh toán trùng khi người dùng gửi lại yêu cầu.

### Thông báo và nhật ký hệ thống

- Gửi thông báo đúng người hoặc đúng nhóm cần xử lý.
- Lưu người thực hiện, hành động và đối tượng bị thay đổi.
- Ưu tiên hiển thị mã nghiệp vụ dễ hiểu thay vì chỉ hiển thị UUID hoặc mã kỹ thuật.

## Điểm kỹ thuật nổi bật

ServiceOps không chỉ là các màn hình thêm/sửa/xóa dữ liệu. Backend kiểm soát các quy tắc quan trọng của quy trình:

- Phân quyền theo vai trò và theo người được giao việc.
- Kiểm tra trạng thái trước khi cho phép thao tác tiếp theo.
- Dùng transaction và locking cho các thao tác dễ xảy ra xung đột như xếp lịch, cấp kho và thanh toán.
- Tránh trừ tồn kho hai lần khi phụ tùng chuyển từ trạng thái đã cấp sang đã sử dụng.
- Chốt chi phí tại thời điểm khách hàng xác nhận để giá về sau không làm thay đổi hồ sơ đã chốt.
- Sinh mã khách hàng, Phiếu công việc, phụ tùng và biên nhận ở backend/database để tránh trùng mã.
- Tách dữ liệu theo tenant trong các truy vấn nghiệp vụ.
- AI chỉ hỗ trợ gợi ý/hướng dẫn; AI không được bỏ qua quyền truy cập hoặc quy tắc nghiệp vụ.

Xem chi tiết tại [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) và [docs/BUSINESS_FLOW.md](docs/BUSINESS_FLOW.md).

## Kiến trúc tổng thể

```text
React + TypeScript
        ↓ /api/v1
Spring Boot REST API
        ↓
Các module nghiệp vụ
        ↓
JPA / JdbcTemplate
        ↓
PostgreSQL
```

Backend là một ứng dụng Spring Boot duy nhất nhưng được chia thành các module theo nghiệp vụ như khách hàng, yêu cầu dịch vụ, Phiếu công việc, lịch làm việc, kho, thanh toán, thông báo và audit. Cách tổ chức này giúp dự án dễ phát triển và kiểm thử mà không cần tách thành nhiều service khi chưa có nhu cầu thực tế.

## Công nghệ

**Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Springdoc OpenAPI, Maven, Testcontainers.

**Frontend:** React 19, TypeScript 5.9, Vite 8, Ant Design 6, TanStack Query 5, React Router 7, Axios, Playwright.

**Triển khai và kiểm thử:** Nginx, systemd, Azure VM, Docker Compose, GitHub Actions, Actuator, Playwright.

## Chạy trên máy local

Yêu cầu: Java 21, Node.js 22+, npm, Git và PostgreSQL 17 (cài trực tiếp hoặc chạy bằng Docker).

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
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Hướng dẫn đầy đủ: [RUN_LOCAL.md](RUN_LOCAL.md).

## Kiểm thử

Chạy kiểm tra local:

```powershell
.\scripts\check-local.ps1
```

Pipeline CI kiểm tra backend test/package, frontend lint/build, Docker stack gần giống môi trường triển khai, health/login smoke test và Playwright E2E.

Checklist kiểm tra trước/sau khi deploy: [docs/UAT_CHECKLIST.md](docs/UAT_CHECKLIST.md).

## Database và migration

Flyway quản lý thay đổi cấu trúc database. Repository hiện có migration từ `V1` đến `V20`; migration mới chỉ được thêm tiếp, không sửa lại migration đã chạy.

Các mã sau do backend/database tự sinh:

- Khách hàng: `KH-YYYYMMDD-NNN`
- Phiếu công việc: `WO-YYYYMMDD-NNN`
- Phụ tùng: `PT-YYYYMMDD-NNN`
- Biên nhận: `BN-YYYYMMDD-NNN`

Production phải backup database trước khi chạy migration mới.

## Cấu trúc repository

```text
backend/                  Backend Spring Boot và test
frontend/                 Frontend React và Playwright E2E
scripts/                  Script chạy local / hỗ trợ vận hành
docs/                     Tài liệu kiến trúc, nghiệp vụ, deploy, UAT
.github/workflows/        GitHub Actions CI
```

## Tài liệu

- [RUN_LOCAL.md](RUN_LOCAL.md) — cách chạy trên máy local
- [docs/BUSINESS_FLOW.md](docs/BUSINESS_FLOW.md) — quy trình và quyền của từng vai trò
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — kiến trúc, bảo mật, database và cách xử lý dữ liệu
- [docs/PRODUCTION_DEPLOYMENT.md](docs/PRODUCTION_DEPLOYMENT.md) — cách triển khai và kiểm tra sau deploy
- [docs/UAT_CHECKLIST.md](docs/UAT_CHECKLIST.md) — checklist kiểm tra chức năng

## Giới hạn hiện tại

Phiên bản hiện tại phù hợp với cách triển khai một backend instance. File upload đang lưu trên ổ đĩa của server và giới hạn đăng nhập sai đang lưu trong bộ nhớ của ứng dụng. Nếu chạy nhiều backend instance, các phần này cần chuyển sang nơi lưu trữ dùng chung. Một số truy vấn dashboard/search chưa có benchmark với dữ liệu rất lớn và dự án chưa có load-test chính thức.
