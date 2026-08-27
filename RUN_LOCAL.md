# Hướng dẫn chạy ServiceOps trên Windows

## 1. Yêu cầu môi trường

Bắt buộc:

- Java JDK 21
- Node.js 22 LTS và npm
- Git
- PostgreSQL 17, theo **một** trong hai cách:
  - PostgreSQL cài trực tiếp trên Windows; hoặc
  - Docker Desktop chạy PostgreSQL container của repository.

Maven cài toàn cục không bắt buộc vì repository có Maven Wrapper.

Kiểm tra phần bắt buộc:

```powershell
java -version
.\backend\mvnw.cmd -version
node -v
npm -v
```

Chỉ khi chọn Docker Desktop:

```powershell
docker version
```

## 2. Tạo cấu hình local một lần

Từ thư mục gốc repository:

```powershell
Copy-Item .env.example .env
```

Mặc định `.env.example` dùng:

```env
POSTGRES_DB=serviceops
POSTGRES_USER=serviceops
POSTGRES_PASSWORD=serviceops
POSTGRES_PORT=5432
DEMO_PASSWORD=Demo@2026
```

Nếu PostgreSQL native trên máy bạn dùng tài khoản khác, chỉ sửa file `.env` local. Không commit `.env`.

`DEMO_PASSWORD` được `scripts/dev-start.ps1` truyền đồng thời cho backend và frontend để quick-login không lệch mật khẩu.

## 3. Chuẩn bị PostgreSQL

### Cách A — Docker Desktop

Bạn có thể khởi động riêng PostgreSQL:

```powershell
.\scripts\start-postgres.ps1
```

hoặc dùng quick start ở bước 4 với `-StartPostgres`.

### Cách B — PostgreSQL đã cài trên Windows

Nếu dùng đúng bộ mặc định `serviceops/serviceops`, chạy bằng tài khoản quản trị PostgreSQL:

```sql
CREATE USER serviceops WITH PASSWORD 'serviceops';
CREATE DATABASE serviceops OWNER serviceops;
GRANT ALL PRIVILEGES ON DATABASE serviceops TO serviceops;
```

Nếu database/user đã tồn tại hoặc bạn dùng credential khác, chỉ cần cập nhật `.env` cho khớp. Không cần Docker.

## 4. Cách chạy hằng ngày — khuyến nghị

PostgreSQL đã chạy:

```powershell
.\scripts\dev-start.ps1
```

Nếu dùng Docker và muốn bật PostgreSQL trước:

```powershell
.\scripts\dev-start.ps1 -StartPostgres
```

Script tự xác định repository root nên không phụ thuộc đường dẫn kiểu `D:\Study\...`. Nó mở hai cửa sổ CMD riêng cho Spring Boot và Vite. Nếu frontend chưa có `node_modules`, script chạy `npm ci` trước `npm run dev`.

Mở:

- Frontend: http://localhost:3000
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## 5. Chạy thủ công khi cần troubleshoot

### Backend

Mở PowerShell tại repository root. Đọc giá trị từ `.env` của bạn và đặt cùng bộ `POSTGRES_*` + `DEMO_PASSWORD`, ví dụ với credential mặc định:

```powershell
cd backend
$env:POSTGRES_HOST="localhost"
$env:POSTGRES_PORT="5432"
$env:POSTGRES_DB="serviceops"
$env:POSTGRES_USER="serviceops"
$env:POSTGRES_PASSWORD="serviceops"
$env:DEMO_PASSWORD="Demo@2026"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Flyway tự migrate schema. Seeder tạo bộ dữ liệu demo khi database chưa có account `owner`.

> Local profile không tự đổi mật khẩu của demo accounts đã tồn tại. Nếu bạn đổi `DEMO_PASSWORD` sau lần seed đầu tiên, hãy dùng mật khẩu hiện có của database hoặc chủ động reset database local nếu dữ liệu đó không cần giữ.

### Frontend

Mở terminal khác:

```powershell
cd frontend
Copy-Item .env.example .env -ErrorAction SilentlyContinue
npm ci
npm run dev
```

`frontend/.env.example` dùng cùng quick-login password `Demo@2026`.

## 6. Dừng hệ thống

Dừng backend/frontend bằng `Ctrl + C` trong hai terminal.

Nếu dùng Docker PostgreSQL và muốn dừng container nhưng giữ dữ liệu:

```powershell
docker compose -f docker-compose.local.yml stop
```

Chỉ khi **thật sự muốn xóa toàn bộ database local**:

```powershell
docker compose -f docker-compose.local.yml down -v
```

Không dùng `down -v` chỉ để dọn vài record test.

## 7. Reset database local an toàn

Khi dữ liệu UAT/manual test đã quá rối và bạn muốn tạo lại database local từ đầu, dùng script có backup guard:

```powershell
.\scripts\reset-local-db.ps1
```

Script:

- đọc `POSTGRES_HOST/PORT/DB/USER/PASSWORD` từ process environment hoặc `.env`;
- từ chối chạy nếu `POSTGRES_HOST` không phải `localhost`, `127.0.0.1` hoặc `::1`;
- kiểm tra role owner tồn tại **trước** khi drop database;
- mặc định tạo custom-format backup vào `db-backups/` và kiểm tra archive bằng `pg_restore -l`;
- yêu cầu gõ lại đúng tên database trước khi `DROP DATABASE`;
- tạo lại database với owner đúng theo `POSTGRES_USER`;
- không tự chạy Flyway: sau khi reset thành công, chạy `.\scripts\dev-start.ps1`; backend sẽ migrate V1 → latest và seeder local sẽ tạo lại dữ liệu demo.

Nếu PostgreSQL binaries chưa nằm trong `PATH`, chỉ rõ thư mục `bin`:

```powershell
.\scripts\reset-local-db.ps1 -PostgresBin "D:\PostgreSQL\bin"
```

Nếu account ứng dụng không có quyền drop/create database, truyền role quản trị và nhập password khi script hỏi:

```powershell
.\scripts\reset-local-db.ps1 -AdminUser postgres
```

`db-backups/` là dữ liệu local và đã được Git ignore. Chỉ dùng `-SkipBackup` khi bạn **chủ động chấp nhận** không giữ snapshot trước reset.

## 8. E2E và dữ liệu local

Không chạy mutating Playwright E2E vào frontend/backend developer local hoặc database `serviceops` đang dùng để UAT.

Playwright yêu cầu `E2E_BASE_URL` rõ ràng và CI chạy nó trên stack Docker cô lập. Dữ liệu `E2E-*` được tạo bởi browser workflow là stateful test data; không nên dùng script xóa hàng loạt trên database local nếu chưa kiểm tra quan hệ Work Order, appointment, history, inventory và attachment.

## 9. Lỗi thường gặp

### Cổng 5432 đã được dùng

Sửa `POSTGRES_PORT` trong `.env` và bảo đảm backend dùng cùng giá trị.

### Frontend không gọi được backend

Mặc định frontend dùng `VITE_API_URL=/api/v1` và Vite proxy `/api` sang `http://localhost:8080`.

### Quick-login báo sai mật khẩu

Kiểm tra ba giá trị có đồng bộ không:

- `DEMO_PASSWORD` trong `.env`/backend startup;
- `VITE_DEMO_PASSWORD` được frontend nhận;
- mật khẩu thực tế của demo accounts trong database đã seed trước đó.

Với database mới theo tài liệu này, giá trị thống nhất là `Demo@2026`.

## 10. Kiểm tra trước khi commit

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm ci
npm run lint
npm run build

git status
```

Docker/Testcontainers có thể được skip khi Docker không khả dụng; đọc đúng summary test và không gọi skipped tests là passed.

Không commit `.env`, `node_modules`, `dist`, `target` hoặc thư mục upload local.
