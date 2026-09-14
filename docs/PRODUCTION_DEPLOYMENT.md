# Triển khai ServiceOps

Tài liệu này mô tả hai cách chạy ServiceOps:

1. **Live demo hiện tại:** Azure VM + Nginx + Spring Boot chạy bằng systemd + PostgreSQL.
2. **Docker Compose:** cấu hình có sẵn trong repository để dựng môi trường gần production và dùng trong CI/self-host.

Không đưa secret, password database, JWT secret hoặc API key vào Git.

## 1. Live demo hiện tại trên Azure VM

Mô hình đang chạy:

```text
Public HTTPS
    ↓
Nginx
  ├─ Frontend React: /var/www/serviceops
  └─ /api/v1 → 127.0.0.1:8080
                  ↓
          serviceops.service
                  ↓
       /opt/serviceops/serviceops.jar
                  ↓
              PostgreSQL
```

Domain demo:

`https://serviceops-fsm.centralindia.cloudapp.azure.com`

### Kiểm tra service

```bash
sudo systemctl status serviceops
sudo systemctl is-active serviceops
```

### Kiểm tra backend

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

### Kiểm tra Nginx

```bash
sudo nginx -t
sudo systemctl status nginx
```

Sau khi deploy cần mở domain public và chạy một vòng smoke test theo [UAT_CHECKLIST.md](UAT_CHECKLIST.md).

## 2. Nguyên tắc trước khi deploy

- Backup database trước migration mới.
- Không drop/reset production database chỉ để deploy code.
- Không ghi secret vào repository hoặc command history nếu có thể tránh.
- Backend và frontend phải được build/test trước khi thay artifact đang chạy.
- Chỉ mở lại traffic khi backend health và frontend đều hoạt động bình thường.

## 3. Backend trên Azure VM

Backend production là file JAR tại:

```text
/opt/serviceops/serviceops.jar
```

Service systemd:

```text
serviceops.service
```

Sau khi thay JAR, kiểm tra:

```bash
sudo systemctl restart serviceops
sudo systemctl is-active serviceops
curl -fsS http://127.0.0.1:8080/actuator/health
```

Nếu service không lên:

```bash
sudo journalctl -u serviceops -n 200 --no-pager
```

Flyway chạy khi backend khởi động. Nếu migration lỗi, không tiếp tục deploy frontend hoặc mở traffic như bình thường cho đến khi nguyên nhân được xử lý.

## 4. Frontend trên Azure VM

Frontend production được build với API path tương đối:

```powershell
$env:VITE_API_URL="/api/v1"
npm run build
```

Static files được đặt tại:

```text
/var/www/serviceops
```

Nginx phục vụ frontend và chuyển `/api/v1` sang backend ở `127.0.0.1:8080`.

Không build frontend production với URL localhost.

Sau khi thay frontend:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Mở domain public để kiểm tra trang đăng nhập và các màn hình chính.

## 5. Database và Flyway

Flyway là nguồn quản lý schema. JPA chỉ `validate`.

Trước migration mới:

- tạo backup;
- kiểm tra migration mới là append-only;
- không sửa lại file migration đã chạy trên production;
- theo dõi log startup để chắc chắn Flyway hoàn thành.

Không reset database production để đồng bộ version code.

## 6. Docker Compose trong repository

Repository có `docker-compose.prod.yml` để dựng ba service:

- `postgres`: PostgreSQL 17 với volume lưu dữ liệu;
- `backend`: Spring Boot Java 21 với volume upload;
- `frontend`: Nginx phục vụ React và proxy `/api` sang backend.

Chuẩn bị file cấu hình:

```bash
cp .env.production.example .env.production
```

Các biến quan trọng cần cấu hình:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `JWT_SECRET`, `JWT_ISSUER`
- `CORS_ALLOWED_ORIGINS`
- `DEMO_MODE`, `DEMO_PASSWORD` nếu bật demo
- `BUSINESS_TIME_ZONE`
- `MAX_TENANT_STORAGE_BYTES`
- `AI_ENABLED`, `GEMINI_API_KEY` nếu dùng Gemini
- `SWAGGER_ENABLED`

Không commit `.env.production`.

Tạo JWT secret ví dụ:

```bash
openssl rand -base64 48
```

### Build và chạy

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

### Kiểm tra health

```bash
curl -fsS http://127.0.0.1:8088/actuator/health/readiness
curl -fsS http://127.0.0.1:8088/ > /dev/null
```

Nếu có lỗi:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 backend
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 postgres
```

## 7. Backup và restore khi dùng Docker Compose

Backup:

```bash
set -a
. ./.env.production
set +a
BACKUP_DIR=./backups ./scripts/production/backup-postgres.sh
```

Xác nhận file backup đã được tạo trước khi chạy migration mới.

Restore là thao tác ghi đè dữ liệu hiện tại, chỉ thực hiện khi đã xác nhận đúng file và đúng môi trường:

```bash
set -a
. ./.env.production
set +a
RESTORE_CONFIRM=serviceops-restore \
  ./scripts/production/restore-postgres.sh ./backups/serviceops_YYYYMMDDTHHMMSSZ.sql.gz
```

Sau restore phải chạy health check và smoke test trước khi mở lại hệ thống cho người dùng.

## 8. File upload

Docker backend lưu file tại `/app/data/uploads` qua volume `uploads_data`.

Bản chạy một backend instance có thể dùng filesystem. Nếu triển khai nhiều backend instance, cần chuyển file sang object storage hoặc storage dùng chung.

## 9. AI

Production nên để `AI_ENABLED=false` nếu chưa cấu hình Gemini.

Khi bật Gemini:

- API key chỉ đặt ở backend/server;
- không đưa key vào frontend build;
- giữ timeout có giới hạn;
- kiểm tra cả trường hợp Gemini hoạt động và trường hợp fallback nội bộ.

Lỗi AI không được làm hỏng các chức năng chính như Yêu cầu dịch vụ, Phiếu công việc, kho hoặc thanh toán.

## 10. Kiểm tra sau deploy

Một bản deploy chỉ được xem là hoàn tất khi:

- backend service đang chạy;
- health endpoint trả trạng thái tốt;
- frontend tải được qua HTTPS;
- đăng nhập được bằng các vai trò cần kiểm tra;
- Flyway không báo lỗi;
- tạo mới dữ liệu có mã nghiệp vụ đúng;
- chạy được luồng ngắn từ Yêu cầu dịch vụ đến Phiếu công việc;
- các phần thay đổi trong release được kiểm tra trực tiếp;
- log không có lỗi lặp liên tục hoặc làm lộ secret.

Chi tiết: [UAT_CHECKLIST.md](UAT_CHECKLIST.md).
