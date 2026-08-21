# Production / Public Demo Deployment

This deployment keeps the existing ServiceOps/FSM modular monolith and hardens it for a single-node public demo. No business module is removed, and Kubernetes/Kafka/microservices are intentionally not introduced without a concrete requirement.

## 1. Environment

Copy `.env.production.example` to a server-only `.env.production` file and replace every `CHANGE_ME` value.

Public demo defaults:

- `SPRING_PROFILES_ACTIVE=prod,demo`
- `DEMO_MODE=true`
- `JWT_SECRET` is mandatory, Base64 encoded, and must decode to at least 32 bytes.
- `DEMO_PASSWORD` must be at least 8 characters and cannot be `123456` or a shipped placeholder value; startup fails instead of silently exposing a known demo password.
- `JWT_ACCESS_TOKEN_MINUTES=30` affects production/demo only; local development keeps its existing behavior.
- `AI_ENABLED=false` in production unless a server-side Gemini key is intentionally configured.
- `SWAGGER_ENABLED=false` by default. Enable it only for an intentional API-review environment; the recruiter demo does not require public Swagger.
- `MAX_TENANT_STORAGE_BYTES=104857600` limits each tenant to 100 MiB on the local storage adapter in the public demo. Use `0` for unlimited storage in a controlled environment.

Generate a JWT secret on Linux:

```bash
openssl rand -base64 48
```

For a private non-demo production deployment, use:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DEMO_MODE=false
SWAGGER_ENABLED=false
```

If API documentation is temporarily required in a controlled review environment, set `SWAGGER_ENABLED=true` explicitly and disable it again afterward.

## 2. Build and start

```bash
cp .env.production.example .env.production
# edit .env.production

docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
```

Verify container health:

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml ps
curl -f http://127.0.0.1:8088/actuator/health/readiness
```

The compose stack deliberately does **not** publish PostgreSQL. The frontend container is bound only to `127.0.0.1:${HTTP_PORT:-8088}` so a host TLS reverse proxy can own public ports 80/443.

## 3. HTTPS and host firewall

Do not expose a recruiter demo over plain HTTP on the public Internet. Recommended layout:

```text
Internet
  -> HTTPS host reverse proxy
  -> 127.0.0.1:8088 frontend Nginx
  -> Spring Boot
  -> PostgreSQL private Docker network
```

The frontend Nginx preserves the original forwarded HTTPS scheme when proxying to Spring Boot.

On a single VM, expose only what is required (normally SSH and HTTP/HTTPS), use SSH key authentication, and never publish PostgreSQL port 5432.

## 4. Demo-mode behavior

`DEMO_MODE=true` keeps normal application workflows usable while protecting only the seeded data required to keep the public demo recoverable:

- recruiter-created customers, assets, service requests, work orders, inventory records, attachments and custom service channels keep their normal role-based create/update/delete behavior;
- seeded demo identities are protected by service-level policy from deletion, deactivation, credential changes and destructive role changes;
- technician profile updates remain usable, while profiles backed by the protected seeded identities cannot be used to bypass that identity protection;
- system-defined service channels are protected from update/delete, while custom channels created during the demo support normal CRUD.

Authorization and business invariants remain enforced exactly as in non-demo mode. Set `DEMO_MODE=false` for a private deployment that does not need seeded-data protection.

Known seeded demo accounts are re-synchronized to the configured `DEMO_PASSWORD` when the demo profile starts. This prevents a reused local demo volume from silently retaining the local `123456` password.

Set `DEMO_MODE=false` and omit the `demo` profile to restore normal production behavior.

## 5. Attachment safety

The local storage adapter now enforces:

- MIME allowlist for JPG/PNG/WEBP/PDF;
- magic-byte signature verification;
- normalized tenant-scoped paths with traversal rejection;
- optional per-tenant storage quota;
- rollback cleanup when DB persistence fails after a file write;
- physical deletion only after the DB delete commits.

For horizontal scaling, move the existing storage abstraction to S3-compatible object storage rather than sharing local container disks.

## 6. Backup

Load server environment variables and run:

```bash
set -a
. ./.env.production
set +a
./scripts/production/backup-postgres.sh
```

The script writes a private temporary SQL dump first, checks `pg_dump` success, then compresses it. Default retention is seven days. Override with `RETENTION_DAYS` and `BACKUP_DIR`.

## 7. Restore drill

A restore is intentionally protected from accidental execution. Confirm the target DB, then run:

```bash
set -a
. ./.env.production
set +a
RESTORE_CONFIRM=serviceops-restore \
  ./scripts/production/restore-postgres.sh ./backups/serviceops_YYYYMMDDTHHMMSSZ.sql.gz
```

The script validates gzip integrity, temporarily stops the backend, restores with `psql -v ON_ERROR_STOP=1`, and restarts the backend. Run application smoke tests after every restore drill.

## 8. Deployment acceptance gate

Before publishing a new revision:

```bash
cd backend
./mvnw --batch-mode clean test

cd ../frontend
npm ci
npm run lint
VITE_API_URL=/api/v1 npm run build

cd ..
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
curl -f http://127.0.0.1:8088/actuator/health/readiness
```

Then smoke-test login, cross-tenant access rejection, work-order workflow, concurrent scheduling, inventory consumption and attachment upload/download.

Use Flyway migrations through application startup. Never run schema recreation, destructive reset, or local fresh-seed commands against a persistent production database.
