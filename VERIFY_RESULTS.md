# Verification Results

## Verified production-hardening baseline

The production-hardening baseline merged through Pull Request #1 was validated on the developer workstation and GitHub CI:

- Java 21 runtime: PASS.
- Backend automated suite: 59 tests, 0 failures, 0 errors, 0 skipped.
- Testcontainers PostgreSQL 17 + Flyway migrations: PASS.
- Frontend TypeScript/lint: PASS.
- Frontend production build: PASS.
- `npm audit`: 0 known vulnerabilities.
- `npm audit --omit=dev`: 0 known vulnerabilities.
- Production backend/frontend Docker image builds: PASS.
- Production-like Compose runtime: PostgreSQL/backend/frontend healthy.
- Manual production-like browser smoke test: PASS.
- GitHub PR checks: backend, frontend and Docker build PASS.

## Enterprise-refinement validation performed on 2026-08-17

After the maintainability/login/work-order/type/invoice refactor was applied, the developer workstation reran the automated gates:

- `mvnw clean test`: **59 tests, 0 failures, 0 errors, 0 skipped — PASS**.
- Testcontainers connected to Docker Desktop and executed the PostgreSQL integration suite — PASS.
- `npm ci`: PASS.
- `npm run lint`: PASS.
- `npm run build`: PASS.
- `npm audit`: 0 known vulnerabilities.
- `npm audit --omit=dev`: 0 known vulnerabilities.

The later typography/layout refinement also passed `npm run lint` and `npm run build` before the final product-UI coherence pass.

## Current final product-UI coherence pass

This uncommitted pass further standardizes semantic tags, table density, application navigation, dashboard hierarchy and public landing-page truthfulness. It does **not** intentionally change backend business behavior or API contracts.

Because these frontend/documentation changes were produced after the verified runs above, they must be revalidated before merge. Do not copy prior PASS results forward as proof for changed source.

## Required acceptance gate before merge

```powershell
cd frontend
npm run lint
npm run build
npm audit
npm audit --omit=dev

cd ../backend
.\mvnw.cmd clean test

cd ..
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Manual smoke coverage must include login/demo roles, dashboard, service requests, work orders, scheduling, inventory/parts, attachments/invoice, customers/assets, audit/users, role restrictions and demo destructive-action protection.
