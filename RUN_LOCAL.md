# Chạy ServiceOps trên máy local

Tài liệu này hướng dẫn chạy ServiceOps trên máy cá nhân để phát triển và kiểm thử.

## 1. Yêu cầu

- Java JDK 21
- Node.js 22+ và npm
- Git
- PostgreSQL 17 (cài trực tiếp hoặc Docker Desktop)

Kiểm tra nhanh:

```powershell
java -version
.\backend\mvnw.cmd -version
node -v
npm -v
```

Repository đã có Maven Wrapper nên không bắt buộc cài Maven riêng.

## 2. Tạo file cấu hình local

```powershell
Copy-Item .env.example .env
```

`.env.example` đã có giá trị phù hợp để chạy local, gồm database `serviceops`, tài khoản database `serviceops`, timezone `Asia/Ho_Chi_Minh` và mật khẩu demo `Demo@2026`.

Nếu máy của bạn dùng thông tin PostgreSQL khác, chỉ sửa file `.env` trên máy. Không commit `.env` lên Git.

Nếu muốn dùng Gemini khi chạy local:

```powershell
.\scripts\configure-gemini-local.ps1
```

Không có Gemini key thì chức năng AI vẫn dùng phần hướng dẫn nội bộ.

## 3. Khởi động PostgreSQL

Nếu dùng Docker:

```powershell
.\scripts\start-postgres.ps1
```

Nếu dùng PostgreSQL cài trực tiếp, tạo user/database một lần nếu chưa có:

```sql
CREATE USER serviceops WITH PASSWORD 'serviceops';
CREATE DATABASE serviceops OWNER serviceops;
GRANT ALL PRIVILEGES ON DATABASE serviceops TO serviceops;
```

## 4. Chạy ứng dụng

Nếu PostgreSQL đã chạy:

```powershell
.\scripts\dev-start.ps1
```

Nếu muốn script tự bật PostgreSQL container:

```powershell
.\scripts\dev-start.ps1 -StartPostgres
```

Backend tự chạy Flyway khi khởi động. Nếu database mới và chưa có tài khoản `owner`, dữ liệu demo sẽ được tạo.

Sau khi chạy thành công:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## 5. Chạy backend và frontend riêng

Chỉ dùng phần này khi cần kiểm tra lỗi hoặc muốn chạy từng phần riêng.

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

Khi chạy local, Vite chuyển các request `/api` sang backend tại `http://localhost:8080`.

## 6. Reset database local

Chỉ dùng với database local hoặc database dùng riêng cho test:

```powershell
.\scripts\reset-local-db.ps1
```

Script chỉ cho phép host local, tạo backup theo mặc định và yêu cầu xác nhận trước khi tạo lại database.

Nếu cần dùng user quản trị PostgreSQL:

```powershell
.\scripts\reset-local-db.ps1 -AdminUser postgres
```

Không dùng script reset này cho production.

## 7. Chạy test và build

Dừng Vite trước khi chạy `check-local.ps1` vì script có chạy `npm ci`.

```powershell
.\scripts\check-local.ps1
```

Hoặc chạy từng phần:

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm ci
npm run lint
npm run build
```

Nếu Docker không chạy, các integration test dùng Testcontainers có thể bị skip. Khi đó cần xem phần tổng kết test để biết số test đã chạy, fail và skip.

## 8. Chạy Playwright E2E

E2E có thao tác làm thay đổi dữ liệu, vì vậy chỉ chạy trên môi trường test/local có thể tạo lại dữ liệu.

```powershell
cd frontend
$env:E2E_BASE_URL="http://localhost:3000"
$env:E2E_DEMO_PASSWORD="Demo@2026"
$env:E2E_ALLOW_MUTATIONS="true"
npm run e2e
```

`E2E_ALLOW_MUTATIONS=true` là điều kiện bắt buộc để tránh chạy nhầm E2E trên môi trường không phù hợp.

## 9. Lỗi thường gặp

### Frontend không gọi được backend

Kiểm tra backend có đang chạy ở port `8080` và Vite proxy có hoạt động hay không.

### Tài khoản demo không đăng nhập được sau khi đổi mật khẩu

Dữ liệu demo đã tạo trước đó không tự đổi mật khẩu khi sửa `DEMO_PASSWORD`. Dùng mật khẩu cũ hoặc reset database local nếu không cần giữ dữ liệu.

### `npm ci` lỗi vì file đang được sử dụng trên Windows

Dừng Vite hoặc Node dev server rồi chạy lại.
