# Hướng dẫn chạy ServiceOps trên Windows

## 1. Yêu cầu môi trường

- Java JDK 17
- Maven 3.9+ (không bắt buộc nếu dùng Maven Wrapper đi kèm)
- Node.js 22 LTS và npm
- Docker Desktop
- Git

Kiểm tra:

```powershell
java -version
.\backend\mvnw.cmd -version
node -v
npm -v
docker version
```

## 2. Quy tắc thư mục

Giải nén vào đường dẫn không dấu, không chứa `&`, ví dụ:

```text
D:\Study\Java\serviceops-local-first
```

Không nên đặt trong `D:\Học tập\...` hoặc thư mục chứa ký tự `&` vì PowerShell và một số tool có thể hiểu sai đường dẫn.

## 3. Khởi động PostgreSQL

### Cách A — Docker Desktop (khuyến nghị)

Tại thư mục gốc:

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

Kiểm tra container phải ở trạng thái `healthy`.

### Cách B — PostgreSQL đã cài trên Windows

Nếu máy đã có PostgreSQL, mở pgAdmin/psql và chạy từng lệnh bằng tài khoản quản trị:

```sql
CREATE USER serviceops WITH PASSWORD 'serviceops';
CREATE DATABASE serviceops OWNER serviceops;
GRANT ALL PRIVILEGES ON DATABASE serviceops TO serviceops;
```

Không cần chạy Docker. Backend mặc định kết nối `localhost:5432`, database/user/password đều là `serviceops`. Nếu cấu hình khác, đặt biến môi trường `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` trước khi chạy backend.

## 4. Chạy backend

Mở PowerShell mới:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

Khi log có dòng `Started ServiceOpsApplication`, mở:

- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Flyway tự tạo schema và seeder tự thêm dữ liệu demo khi database còn trống.

## 5. Chạy frontend

Mở PowerShell mới:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Mở http://localhost:3000.

## 6. Dừng hệ thống

Dừng backend/frontend bằng `Ctrl + C`.

Dừng PostgreSQL nhưng giữ dữ liệu:

```powershell
docker compose -f docker-compose.local.yml stop
```

Xóa cả container và dữ liệu local để chạy lại từ đầu:

```powershell
docker compose -f docker-compose.local.yml down -v
```

## 7. Lỗi thường gặp

### Cổng 5432 đã được dùng

Sửa `.env`:

```env
POSTGRES_PORT=5433
```

Sau đó backend cũng phải dùng cổng đó:

```powershell
$env:POSTGRES_PORT="5433"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Frontend không gọi được backend

Kiểm tra `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

Khởi động lại `npm run dev` sau khi sửa `.env`.

### Backend báo sai JWT secret

`JWT_SECRET` phải là chuỗi Base64 đủ dài. Bản `.env.example` đã có key local hợp lệ; production phải thay key mới.

### Muốn reset dữ liệu demo

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

## 8. Kiểm tra trước khi commit

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm run lint
npm run build

git status
```

Không commit `.env`, `node_modules`, `dist`, `target` hoặc thư mục upload local.
