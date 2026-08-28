# Verification Results

## Recorded local release-candidate baseline

The P1/P2 baseline at commit `3c8b5f9` was locally verified on 2026-08-27 with:

- Backend Maven suite: **242 total tests, 0 failures, 0 errors, 25 skipped**. The skipped tests are Docker/Testcontainers suites and are **not** counted as passes.
- Frontend TypeScript/UI-policy lint: **PASS**.
- Frontend production build: **PASS** with **3269 modules transformed** in that run.
- Flyway reset/boot drill on a clean local `serviceops` database: migrations **V1 → V15** applied successfully and the backend started normally.

This is local evidence, not a substitute for the repository CI. Before merge/release, the GitHub Actions backend/frontend jobs and isolated production-like Docker + Playwright job must also be green, followed by the manual UAT gate below.

The Playwright suite currently expands to **17 browser tests across 4 spec files**: 7 directly declared CRUD/workflow/settlement tests plus 10 per-role route/sidebar checks generated for the five demo roles.

## Required fast local gates

```powershell
cd backend
.\mvnw.cmd clean test

cd ..\frontend
npm ci
npm run lint
npm run build
```

Interpret Maven results exactly. If Docker is unavailable, Testcontainers suites may be skipped; skipped tests are not passes.

## Stateful Playwright policy

Do **not** run mutating Playwright against the developer `:3000`/`:5173` environment or the local PostgreSQL database used for manual UAT.

`playwright.config.ts` requires both an explicit `E2E_BASE_URL` and `E2E_ALLOW_MUTATIONS=true` because the suite changes business data. GitHub Actions sets that opt-in only for its isolated Docker Compose **Nginx → Spring Boot → PostgreSQL** stack at `http://127.0.0.1:8088`.

For an explicitly isolated local production-like stack:

```powershell
$env:E2E_BASE_URL="http://127.0.0.1:8088"
$env:E2E_DEMO_PASSWORD=$env:DEMO_PASSWORD
$env:E2E_ALLOW_MUTATIONS="true"
cd frontend
npm run e2e
```

Only do this when the endpoint is backed by disposable isolated data. Port number or hostname alone is not proof of database isolation; every mutating E2E target requires the explicit opt-in above.

## Regression coverage currently enforced

- Work Order/Dashboard controller role contracts exclude Warehouse.
- Warehouse direct API checks cover Work Order list/history and dashboard.
- Dispatcher cannot perform technician field transitions.
- Technician account/profile cannot be deactivated while active operational assignments remain.
- Asset and Service Request hard-delete is blocked while attachments still reference the parent.
- Dispatcher technician profile editing is blocked; profile updates are Owner-only.
- Work Order history archive/delete is Owner-only.
- E2E route policy treats Warehouse home as `/inventory`, not the operational dashboard.
- E2E workflow verifies Dispatcher field-transition denial in addition to Technician transition boundaries.
- JWT validation rejects inactive/deleted/stale user identities even while an old access token is otherwise unexpired.
- Username is immutable after account creation so audit/attachment ownership strings cannot drift.
- Technician deactivation/profile pause uses the same pessimistic technician row lock as scheduling to close the schedule-vs-deactivate race.
- Scheduling/rescheduling locks the Work Order row and records `RESCHEDULE` audit details instead of fake `ASSIGNED → ASSIGNED` status history.
- Dashboard counts include SCHEDULED, ON_THE_WAY, CUSTOMER_ACCEPTED and REOPENED states used by the completion KPI.
- `check-local.ps1` fails immediately when Maven/npm native commands return non-zero.

## Production-like validation

For deployment/runtime changes:

```powershell
Copy-Item .env.production.example .env.production
# Replace every CHANGE_ME value before continuing.

docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Confirm PostgreSQL/backend/frontend health, frontend HTTP 200, backend readiness through Nginx and demo authentication. Use `docker compose ... down` afterward. Do **not** add `-v` unless the isolated persistent data is intentionally being destroyed.

## Final manual UAT gate

Before `v1.0.0`, perform the real-world role sequence from `docs/UAT_CHECKLIST.md`, including:

- Customer Service intake → Work Order handoff;
- Dispatcher scheduling/rescheduling and authorization boundaries;
- Technician/technician-2 isolation and field execution;
- Warehouse inventory-only behavior;
- cancellation side effects;
- part request → Warehouse `ISSUE` → Technician actual `USED` → Warehouse `RETURN`, including inventory balance;
- Technician customer acceptance/payment action, Customer Service reconciliation/receipt/closure, and Owner oversight/history/audit;
- logout/login between roles to confirm no stale cross-account UI cache.

`v1.0.0` must remain uncreated until the changed source passes automated gates and final manual UAT.
