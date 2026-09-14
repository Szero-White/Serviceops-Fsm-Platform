# Triển khai ServiceOps

Tài liệu này mô tả **production-like topology được lưu trong repository và được CI kiểm tra**: PostgreSQL + Spring Boot + Nginx frontend qua Docker Compose.

## 1. Chuẩn bị cấu hình

Tạo file server-only:

```bash
cp .env.production.example .env.production
```

Phải thay toàn bộ placeholder/secret trước khi chạy. Các biến quan trọng:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `JWT_SECRET`: Base64, sau decode ít nhất 32 bytes
- `JWT_ISSUER`
- `CORS_ALLOWED_ORIGINS`
- `DEMO_MODE`, `DEMO_PASSWORD` nếu bật demo profile
- `BUSINESS_TIME_ZONE` (mặc định `Asia/Ho_Chi_Minh`)
- `MAX_TENANT_STORAGE_BYTES`
- `AI_ENABLED`, `GEMINI_API_KEY` nếu bật Gemini
- `SWAGGER_ENABLED=false` cho public deployment thông thường

Không commit `.env.production`.

Tạo JWT secret ví dụ:

```bash
openssl rand -base64 48
```

## 2. Backup trước deploy

Nếu đang chạy stack Compose hiện tại:

```bash
set -a
. ./.env.production
set +a
BACKUP_DIR=./backups ./scripts/production/backup-postgres.sh
```

Script dùng `pg_dump`, gzip archive và mặc định giữ 7 ngày. Xác nhận file backup tồn tại trước migration mới.

Không reset/drop production database để deploy.

## 3. Validate và build

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
```

Compose hiện có ba service:

- `postgres`: PostgreSQL 17 + persistent volume;
- `backend`: Spring Boot Java 21 + upload volume;
- `frontend`: Nginx phục vụ SPA và proxy `/api` sang backend.

Backend tự chạy Flyway khi start. JPA chỉ `validate` schema, không tự sửa cấu trúc database.

## 4. Deploy

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Frontend được bind mặc định vào `127.0.0.1:${HTTP_PORT:-8088}`. Với Internet-facing deployment, đặt reverse proxy/TLS ở host phía trước port này; không expose plain HTTP trực tiếp ra Internet.

## 5. Health check

```bash
curl -fsS http://127.0.0.1:8088/actuator/health/readiness
curl -fsS http://127.0.0.1:8088/ > /dev/null
```

Sau đó kiểm login và một workflow ngắn theo [UAT_CHECKLIST.md](UAT_CHECKLIST.md).

Nếu startup lỗi:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 backend
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 postgres
```

Không tiếp tục mở traffic nếu Flyway/health check lỗi.

## 6. File upload

Container backend dùng `/app/data/uploads` và volume `uploads_data`. `MAX_TENANT_STORAGE_BYTES` giới hạn quota tenant; `0` nghĩa là không giới hạn.

Local filesystem phù hợp single-node. Nếu chạy nhiều backend instance, phải chuyển storage sang shared/object storage trước.

## 7. AI

Production mặc định `AI_ENABLED=false`. Khi bật Gemini:

- key chỉ cấu hình server-side;
- không đưa key vào frontend build args;
- giữ timeout có giới hạn;
- kiểm cả response Gemini và fallback nội bộ.

AI failure không được làm hỏng core workflow.

## 8. Restore

Restore là thao tác phá dữ liệu hiện tại, chỉ thực hiện khi đã xác nhận đúng target:

```bash
set -a
. ./.env.production
set +a
RESTORE_CONFIRM=serviceops-restore \
  ./scripts/production/restore-postgres.sh ./backups/serviceops_YYYYMMDDTHHMMSSZ.sql.gz
```

Script dừng backend trong lúc restore và khởi động lại sau khi hoàn tất. Sau restore phải chạy health check + UAT smoke trước khi mở traffic.

## 9. Release gate

Một release chỉ được coi là deploy xong khi:

- Compose config/build thành công;
- Flyway start không lỗi;
- readiness health xanh;
- frontend tải được;
- login đúng role;
- business code mới sinh đúng;
- Service Request → Work Order → field execution → payment/closure smoke chạy được;
- Audit/Notification không lộ raw exception/secret.

CI cũng dựng chính production-like stack này và chạy Playwright trước khi merge.
