# Verification Results

## Current release-consistency checkpoint — revalidation required

The previous merged `main` baseline before this release-consistency patch had the following recorded evidence:

- Backend: **99 total tests, 0 failures, 0 errors, 22 skipped** — therefore **77 executed/passed**, not “99 passed”.
- Frontend TypeScript/UI-policy lint: PASS.
- Frontend production build: PASS (~3250 modules in that recorded run).
- PR #7 was merged and its three GitHub checks were green.

This branch now changes authorization, live JWT account-state validation, technician lifecycle locking, attachment-parent delete guards, username immutability, dashboard status accounting, reschedule audit behavior, frontend identity/cache handling, route policy, cross-module query invalidation, local startup tooling and documentation. **The old green result must not be copied onto the changed source.** Run the gates below before merge/release.

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

`playwright.config.ts` requires an explicit `E2E_BASE_URL` and rejects local development ports. GitHub Actions is the intended clean browser gate: it creates an isolated Docker Compose **Nginx → Spring Boot → PostgreSQL** stack and runs Playwright against `http://127.0.0.1:8088`.

For an explicitly isolated production-like stack:

```powershell
$env:E2E_BASE_URL="http://127.0.0.1:8088"
$env:E2E_DEMO_PASSWORD=$env:DEMO_PASSWORD
cd frontend
npm run e2e
```

Only do this when the endpoint is backed by disposable isolated data. Port number alone is not proof of database isolation.

## Regression coverage added/strengthened by this patch

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
- parts consumption and inventory balance;
- Owner acceptance/closure/history/audit/invoice;
- logout/login between roles to confirm no stale cross-account UI cache.

`v1.0.0` must remain uncreated until the changed source passes automated gates and final manual UAT.
