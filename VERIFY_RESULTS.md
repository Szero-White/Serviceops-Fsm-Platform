# Verification Results

## Current merged baseline — Pull Request #6 (2026-08-21)

The current `main` baseline includes Pull Request #6, merged as commit `a682754`, after all three GitHub checks passed.

Current verification evidence:

- Backend clean suite: **88 tests, 0 failures, 0 errors**; 16 Testcontainers integration tests are skipped only when local Docker is intentionally unavailable.
- GitHub backend CI runs in a clean environment with Docker/Testcontainers available and passed before merge.
- Frontend TypeScript/UI policy lint and production build: PASS.
- Production-like Docker Compose topology **Nginx → Spring Boot → PostgreSQL**: build/start and readiness checks PASS in CI.
- Frontend HTTP response and demo authentication through Nginx: PASS in CI.
- Playwright discovery: **10 browser tests across 3 spec files**.
- Playwright Chromium E2E executes against the production-like Nginx endpoint inside the `docker-build` CI job; that job passed before Pull Request #6 was merged.
- Browser coverage includes all five demo-role route policies, Customer CRUD, custom Service Channel CRUD, Warehouse spare-part create/import, Service Request → Work Order conversion, Technician transition UI policy and backend rejection of unauthorized Technician management transitions.
- User-facing Vietnamese text normalization and UTF-8/mojibake checks were completed as part of the Pull Request #6 stabilization work.

These results provide strong evidence for the supported portfolio workflow; they are **not** a claim that the application can never contain a bug. Any behavior-changing commit must be revalidated.

## Release acceptance gate

Run the fast local gates before merging application changes:

```powershell
cd backend
.\mvnw.cmd clean test

cd ../frontend
npm ci
npm run lint
npm run build
npm audit
npm audit --omit=dev
```

For deployment/runtime changes, also validate the production-like stack:

```powershell
cd ..
Copy-Item .env.production.example .env.production
# Replace every CHANGE_ME value before continuing.

docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

The production-like verification must confirm:

- PostgreSQL, backend and frontend containers become healthy.
- `http://localhost:8088/` returns HTTP `200`.
- backend readiness is reachable through the frontend/Nginx proxy.
- a configured demo account can authenticate.
- Playwright recruiter E2E passes against the production-like endpoint when `E2E_DEMO_PASSWORD` is provided.

Example browser run after the production-like stack is ready:

```powershell
cd frontend
$env:E2E_BASE_URL="http://127.0.0.1:8088"
$env:E2E_DEMO_PASSWORD=$env:DEMO_PASSWORD
npm run e2e
```

Use `docker compose ... down` after validation. Do **not** add `-v` unless persistent test data is intentionally being destroyed.

## Final manual smoke

Automated checks cover business behavior and permission boundaries. Before sharing the portfolio with a recruiter, do one short visual smoke pass rather than manually repeating the full automated suite:

- login and navigation for representative roles;
- one end-to-end Service Request → Work Order → schedule → technician execution path;
- inventory/parts and attachment/invoice visibility;
- direct-route permission behavior;
- layout, modal, table, Vietnamese text and responsive usability at a normal laptop viewport.

## CI policy

GitHub Actions remains the independent clean-environment gate. It validates:

- backend tests/package;
- frontend type/UI policy lint and production build;
- production-like Docker Compose build/start;
- backend readiness through Nginx;
- frontend HTTP response;
- demo authentication;
- Playwright Chromium recruiter E2E.

If a CI gate fails, fix the cause rather than weakening or skipping the check.
