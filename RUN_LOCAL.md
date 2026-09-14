# Chạy ServiceOps trên máy local

## 1. Yêu cầu

- Java JDK 21
- Node.js 22+ và npm
- Git
- PostgreSQL 17 (native hoặc Docker Desktop)

Repository có Maven Wrapper.

```powershell
java -version
.\backend\mvnw.cmd -version
node -v
npm -v
```

## 2. Cấu hình

```powershell
Copy-Item .env.example .env
```

`.env.example` đã có cấu hình local mặc định (`serviceops/serviceops`, port 5432, `Demo@2026`, timezone `Asia/Ho_Chi_Minh`). Nếu máy dùng credential khác, chỉ sửa `.env` local. Không commit `.env`.

Nếu cần Gemini local:

```powershell
.\scripts\configure-gemini-local.ps1
```

Không có Gemini key thì AI dùng fallback nội bộ.

## 3. PostgreSQL

Docker:

```powershell
.\scripts\start-postgres.ps1
```

PostgreSQL native, tạo database/user một lần nếu chưa có:

```sql
CREATE USER serviceops WITH PASSWORD 'serviceops';
CREATE DATABASE serviceops OWNER serviceops;
GRANT ALL PRIVILEGES ON DATABASE serviceops TO serviceops;
```

## 4. Chạy ứng dụng

PostgreSQL đã chạy:

```powershell
.\scripts\dev-start.ps1
```

Hoặc để script bật PostgreSQL container:

```powershell
.\scripts\dev-start.ps1 -StartPostgres
```

Backend tự chạy Flyway. Demo seeder tạo dữ liệu mẫu khi database chưa có account `owner`.

- Frontend: `http://localhost:3000`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## 5. Chạy thủ công khi troubleshoot

Backend:

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

Frontend ở terminal khác:

```powershell
cd frontend
npm ci
npm run dev
```

Vite proxy `/api` sang `http://localhost:8080`.

## 6. Reset database local

Chỉ dùng cho database local/disposable:

```powershell
.\scripts\reset-local-db.ps1
```

Script từ chối host không phải loopback, backup theo mặc định, yêu cầu xác nhận tên database và tạo lại database đúng owner. Sau reset, chạy `dev-start.ps1` để Flyway migrate lại.

Nếu cần role quản trị PostgreSQL:

```powershell
.\scripts\reset-local-db.ps1 -AdminUser postgres
```

Không dùng script này cho production.

## 7. Local verification

Dừng Vite trước vì `check-local.ps1` chạy `npm ci`.

```powershell
.\scripts\check-local.ps1
```

Hoặc chạy riêng:

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm ci
npm run lint
npm run build
```

Nếu Docker không khả dụng, Testcontainers integration tests có thể bị skip; đọc đúng test summary.

## 8. Playwright E2E

E2E có mutation nên chỉ chạy trên dữ liệu disposable/isolated:

```powershell
cd frontend
$env:E2E_BASE_URL="http://localhost:3000"
$env:E2E_DEMO_PASSWORD="Demo@2026"
$env:E2E_ALLOW_MUTATIONS="true"
npm run e2e
```

`E2E_ALLOW_MUTATIONS=true` là guard bắt buộc trong `playwright.config.ts`.

## 9. Lỗi thường gặp

- Frontend không gọi backend: kiểm backend port 8080 và Vite proxy.
- Quick login sai mật khẩu: database đã seed không tự đổi password khi sửa `DEMO_PASSWORD`; dùng password cũ hoặc reset local DB nếu dữ liệu không cần giữ.
- `npm ci` lỗi file bị giữ trên Windows: dừng Vite/Node dev server rồi chạy lại.
