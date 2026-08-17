# Verification Results

## Verified merged baseline — Pull Request #2 (2026-08-17)

The enterprise-refinement/design-system baseline currently on `main` was validated before and during Pull Request #2:

- Java 21 runtime: PASS.
- Backend automated suite: **59 tests, 0 failures, 0 errors, 0 skipped**.
- Testcontainers PostgreSQL 17 + Flyway migrations: PASS.
- Frontend TypeScript/UI policy lint: PASS.
- Frontend production build: PASS.
- `npm audit`: 0 known vulnerabilities.
- `npm audit --omit=dev`: 0 known vulnerabilities.
- Production-like Compose build: PASS.
- Production-like runtime: PostgreSQL/backend/frontend healthy.
- Frontend HTTP smoke check through Nginx: `200`.
- GitHub PR checks: backend, frontend and Docker build PASS.

This section records the **merged baseline only**. Future changes must be validated again; prior PASS results are not proof for modified source.

## Release acceptance gate

Run these checks before merging a change that affects application behavior, deployment, dependencies or shared UI infrastructure:

```powershell
cd backend
.\mvnw.cmd clean test

cd ../frontend
npm ci
npm run lint
npm run build
npm audit
npm audit --omit=dev

cd ..
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

The production-like smoke check must verify at minimum:

- PostgreSQL, backend and frontend containers become healthy.
- `http://localhost:8088/` returns HTTP `200`.
- readiness is reachable through the frontend proxy.
- a configured demo account can authenticate.

Manual browser smoke coverage should include login/demo roles, dashboard, service requests, work orders, scheduling, inventory/parts, attachments/invoice, customers/assets, audit/users, role restrictions and demo destructive-action protection.

Use `docker compose ... down` after validation. Do **not** add `-v` unless persistent test data is intentionally being destroyed.

## CI policy

GitHub Actions is the independent clean-environment gate. It must continue to validate:

- backend tests/package;
- frontend type/UI policy lint and production build;
- production-like Docker Compose build/start;
- backend readiness through Nginx;
- frontend HTTP response;
- demo authentication.

If a CI gate fails, fix the cause rather than weakening or skipping the check.
