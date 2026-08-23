# Verification Results

## Current feature-branch checkpoint — `docs/final-portfolio-polish` (2026-08-23)

The feature branch is intentionally still uncommitted while the final ownership/test-safety cleanup is being validated. The previous merged `main` CI baseline remains historical evidence only; current branch behavior must be revalidated before merge.

Current local-safe verification evidence before push:

- Backend suite: **99 tests, 0 failures, 0 errors, 22 skipped**. The skipped tests are Testcontainers integration suites because Docker is unavailable locally; they were not executed and are not counted as passes.
- Frontend TypeScript/UI policy lint: PASS.
- Frontend production build: PASS (**3250 modules transformed** in the latest local run).
- `git diff --check`: PASS; Windows LF→CRLF messages are line-ending warnings, not diff errors.
- Playwright suite: **11 browser tests across 3 spec files** on the current feature branch.
- Mutating Playwright E2E is deliberately **not run against the developer `:3000` environment**. `playwright.config.ts` requires an explicit `E2E_BASE_URL` and refuses localhost/127.0.0.1 development ports 3000/5173.
- GitHub Actions remains the clean-environment browser gate: it creates an isolated Docker Compose **Nginx → Spring Boot → PostgreSQL** stack and runs Playwright against `http://127.0.0.1:8088`.
- Current feature-branch GitHub CI/Playwright result is **pending until this branch is committed and pushed**. Do not copy the previous PR's green CI result onto this branch.

These results are evidence for the current checkpoint, not a claim that the application can never contain a bug. Any behavior-changing commit must be revalidated.

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
