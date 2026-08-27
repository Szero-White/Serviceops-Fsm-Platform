# ServiceOps — Field Service Operations Platform

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

**Live demo:** Not deployed publicly yet. The repository includes a complete local setup and production-like Docker validation path.

ServiceOps is a full-stack operations platform for a **field-service maintenance and repair business**. It coordinates the departments that receive customer issues, manage customer equipment, plan field visits, perform technical work, control spare-parts stock and oversee the service lifecycle.

The project is intentionally modeled as a connected business process rather than a collection of isolated CRUD screens. A customer issue becomes a service request, then a work order, then a scheduled technician job; field execution coordinates requested/issued/used/returned spare parts, and the completed job remains traceable through billing acceptance, payment reconciliation, receipt, closure, history, notifications, inventory ledger and audit records.

## Business problem

A field-service company has to keep several departments synchronized around the same service case. Customer Service needs accurate customer and equipment context. Dispatchers need assignable technicians and conflict-free schedules. Technicians need only the jobs assigned to them and a clear execution workflow. Warehouse staff need reliable stock balances and spare-part lifecycle controls. Management needs visibility, accountability and a durable history of what happened.

ServiceOps provides one operational record that follows the work across those handoffs so the organization does not have to coordinate the same job through disconnected spreadsheets, chat messages or department-specific records.

## Who uses the system

| Real-world responsibility | ServiceOps role | Main responsibilities in the system |
| --- | --- | --- |
| Business owner / operations management | `OWNER` | User administration, overall operations, dashboard, audit, work-order management and oversight |
| Dispatch / service coordination | `DISPATCHER` | Work orders, technician resources, assignment, scheduling/rescheduling and operational history |
| Customer service / service desk | `CUSTOMER_SERVICE` | Customers, customer equipment, intake channels, service requests and Service Request → Work Order handoff |
| Field technician | `TECHNICIAN` | Personal schedule, assigned work, field progress, diagnosis/resolution, evidence and spare-part consumption |
| Warehouse / spare-parts staff | `WAREHOUSE_STAFF` | Spare-parts catalog, stock receiving, stocktake/reconciliation, returns and movement traceability |

The frontend hides routes and actions that are outside a role's responsibility, while the backend remains the authoritative authorization boundary.

## End-to-end operating story

Consider a customer reporting that an air conditioner is no longer cooling properly. The same case moves through ServiceOps as it would through a real service organization:

1. **Customer Service receives the issue.** The agent finds or creates the customer, records the customer's equipment and selects the configured intake channel such as phone or email. If a technical identifier such as the serial number is not available during the first call, the equipment can still be registered and the serial can be completed later after verification.
2. **A Service Request is opened.** The request keeps the customer, optional equipment, issue description, priority and intake channel together. Asset selection is scoped to the selected customer, and the backend rejects a mismatched customer/asset relationship.
3. **The request becomes a Work Order.** The operational job is created from the request while preserving the source customer and equipment relationship.
4. **Dispatch plans the visit.** A Dispatcher selects a technician and schedules or reschedules the work. Scheduling uses overlap detection and locking so the same technician is not silently double-booked.
5. **The technician receives the assignment.** The technician sees the job through the personal schedule derived from the authenticated account, not from a client-supplied technician identifier.
6. **Field execution begins.** The technician progresses the assigned job through field states such as `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` and `COMPLETED`. Management-only transitions remain unavailable to the technician.
7. **Spare parts participate in the same job.** The assigned Technician creates a `REQUEST` without changing stock. Warehouse either marks the request unavailable or physically hands over the exact requested quantity and records `ISSUE`, which is the stock-out event. The Technician later records actual `USED` quantity without reducing stock again. Any unused issued quantity can be physically received back by Warehouse as `RETURN`, including after the Work Order is closed; the inventory ledger remains the stock authority. Legacy `CONSUME` rows remain readable for historical compatibility, but the current UI does not create them.
8. **The service result and customer charge are frozen.** Diagnosis, resolution notes and evidence stay attached to the job. Through `COMPLETED`, the assigned Technician records actual used parts, labor and any explained incidental fee. Customer acceptance then freezes an immutable billing snapshot so later catalog-price changes or part returns cannot silently rewrite what the customer accepted.
9. **Payment is reconciled before closure.** After `CUSTOMER_ACCEPTED`, the Technician can show the Owner-configured company bank/QR in read-only form and record that the customer reported a transfer, optionally with evidence, or that cash is being held for handover. Customer Service verifies the actual transfer or cash handover and moves the separate payment state to `SETTLED`. Only then can Customer Service issue the official service-payment receipt and close the Work Order.
10. **The organization can trace the result.** Work-order history and the unified timeline tell the business story from request/issue/used through completion, acceptance, payment, receipt, closure and any post-closure return. Inventory Movements remains the stock ledger; Audit keeps detailed system traceability; notifications remain attention-only rather than duplicating those histories.

This produces one continuous business chain instead of separate records for each department:

```text
Customer reports an issue
        ↓
Customer Service
Customer → Asset → Service Request
        ↓
Work Order
        ↓
Dispatcher
Technician assignment → Schedule / Reschedule
        ↓
Technician
ON_THE_WAY → IN_PROGRESS
        ↓
        ├── REQUEST part ─────→ Warehouse request queue (no stock movement)
        │       ↓
        │     ISSUE ───────────→ stock decreases exactly once
        │       ↓
        │     USED ────────────→ actual customer usage (no stock movement)
        ↓
Diagnosis → Resolution → Evidence → COMPLETED
        ↓
Billing draft → Customer Acceptance → frozen billing snapshot
        ↓
Customer payment action → CSKH reconciliation → SETTLED
        ↓
Official receipt → CSKH CLOSED
        ↓
Warehouse may RETURN unused outstanding parts → stock increases
        ↓
History / Timeline / Inventory Ledger / Notifications / Audit
```

## Core workflow and business rules

The primary lifecycle is:

```text
Customer
  → Asset
  → Service Request
  → Work Order
  → Technician Scheduling
  → Service Execution
  → Part Request / Issue / Actual Used / Return
  → Completion
  → Customer Acceptance + Billing Snapshot
  → Payment Reconciliation
  → Receipt
  → Closure
```

The Work Order state machine also supports controlled branches such as `WAITING_FOR_PARTS`, `REOPENED` and `CANCELLED`. Invalid or unauthorized transitions are rejected by the backend. Customer/asset consistency, technician ownership, schedule conflicts, inventory balance and tenant scope are also enforced server-side rather than relying only on frontend visibility.

## Demo accounts

The login screen exposes **five quick-login cards**, one for each business role.

| Role | Username | Password | Main area to review |
| --- | --- | --- | --- |
| Owner | `owner` | `Demo@2026` | User administration, dashboard, audit and overall operations |
| Dispatcher | `dispatcher` | `Demo@2026` | Work orders, technician assignment and weekly scheduling |
| Customer Service | `customer-service` | `Demo@2026` | Customers, assets, service requests and request-to-work-order flow |
| Technician | `technician` | `Demo@2026` | Personal schedule, assigned work and field execution |
| Warehouse | `warehouse` | `Demo@2026` | Part-request queue, spare parts, ISSUE/RETURN, stocktake and inventory movement history |

`technician-2` is an additional seeded technician account and also uses `Demo@2026` in the current local portfolio environment. It is intentionally **not** a sixth quick-login card. It is used to verify isolation between two individual technicians who share the `TECHNICIAN` role, especially for `/my-schedule` and assigned work.

> `Demo@2026` is the disposable credential used by the current local/demo portfolio setup. Production database, JWT and infrastructure secrets must remain separate and must not be committed.

## Recruiter walkthrough

For a review, use **one service case across every role** instead of demonstrating unrelated CRUD records:

1. Sign in as **Customer Service**, create or inspect a customer and equipment record, then create a Service Request.
2. Convert that exact request into a Work Order and keep its generated code as the trace identifier for the rest of the demo.
3. Sign in as **Dispatcher**, assign a technician and demonstrate schedule/reschedule behavior.
4. Sign in as **Technician**, confirm the same Work Order appears in the personal schedule, start field execution and create a part request if material is needed.
5. Sign in as **Warehouse**, open **Yêu cầu phụ tùng**, verify the Technician's requested quantity and record `ISSUE` only when the physical part is handed over. Return to **Technician** to record actual `USED`, diagnosis/resolution, complete the job, enter the real service charges and record **Khách xác nhận**.
6. Still as **Technician**, demonstrate the payment handoff: show the company bank/QR read-only and report a customer transfer (with optional evidence) or record cash custody. Sign in as **Customer Service**, reconcile the actual payment, move it to `SETTLED`, issue the official receipt and close the Work Order.
7. If the Technician still holds an unused issued part, sign in as **Warehouse** after `CLOSED` and record the physical `RETURN`; verify stock/outstanding change while the Work Order remains closed.
8. Finish as **Owner** by reviewing payment settings, history, timeline, dashboard and audit data for the same operational story, then switch roles/open protected routes directly to verify frontend and backend role ownership remain aligned.

## Product capabilities

- Customer and customer-equipment management, including equipment whose serial is not yet known at service intake.
- Configurable service-request intake channels.
- Service Request → Work Order conversion with customer/asset consistency checks.
- Work-order lifecycle with controlled role-aware transitions and history.
- Technician assignment, overlap-safe scheduling and weekly dispatcher schedule board.
- Personal technician schedule derived from the authenticated account.
- Spare-parts catalog, configurable minimum-stock thresholds, stock transactions, discontinue/reactivate lifecycle and negative-stock protection.
- Safe hard-delete behavior for pristine spare parts while preserving inventory history for used parts.
- Technician part requests, Warehouse ISSUE/RETURN, actual-used tracking and outstanding-material visibility, with inventory movement history as the stock ledger.
- Warehouse stocktake/reconciliation and editable minimum-stock thresholds; threshold changes are audited and can raise low-stock alerts when current stock becomes newly low.
- Customer-accepted immutable billing snapshots based on actual `USED` quantities, catalog unit-price snapshots, labor and explained incidental fees.
- Separate payment reconciliation for transfer/cash, Owner-managed company bank/QR, optional transfer evidence, official receipt after `SETTLED`, and Customer Service closure.
- CSV import/export for customers, assets and spare parts; bulk asset import keeps serial as a stable required identifier.
- Work-order evidence attachments with MIME/signature/path validation and tenant-scoped storage.
- Official service-payment receipt derived from the frozen billing/payment snapshot after settlement.
- Persistent notifications, audit trail and operational dashboard.
- Shared-schema multi-tenancy with tenant-scoped data access.
- Five business roles: `OWNER`, `DISPATCHER`, `CUSTOMER_SERVICE`, `TECHNICIAN`, `WAREHOUSE_STAFF`.
- Optional AI-assisted service-request drafting and a role-aware in-app help assistant.

## Architecture

```text
Browser
  │
  ▼
React 19 + TypeScript + Ant Design
  │  /api/v1
  ▼
Vite proxy (development) / Nginx (production-like)
  │
  ▼
Spring Boot 3.5 modular monolith
  ├── identity / security / tenant
  ├── customer / asset
  ├── service request / channel
  ├── work order
  ├── technician / scheduling
  ├── inventory
  ├── attachment
  ├── notification
  ├── audit / dashboard
  └── AI assistance
        │
        ├── PostgreSQL 17
        ├── filesystem-backed attachment storage
        └── Gemini API (optional, server-side)
```

ServiceOps intentionally remains a **modular monolith**. The current requirements benefit from explicit business-module boundaries and transactional use cases without the operational overhead of a distributed architecture.

The role-aware AI help assistant is constrained to product guidance and does not receive raw customer, work-order or inventory runtime records. Spring Security and the application backend remain the authorization boundary.

## Technology stack

### Backend

- Java **21**
- Spring Boot **3.5.16**
- Spring MVC and Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL **17**
- Spring Security with JWT authentication and method-level authorization
- Flyway database migrations
- JUnit 5, Mockito and Testcontainers

### Frontend

- React **19.2.7**
- TypeScript **5.9.3**
- Vite **8.1.5**
- Ant Design **6.5.1**
- TanStack Query **5.101.3**
- React Router **7.18.2**
- Axios
- Playwright Chromium E2E

### Operations

- Multi-stage backend/frontend Docker builds
- Production-like Docker Compose topology: **Nginx → Spring Boot → PostgreSQL**
- PostgreSQL private to the production Compose network
- Health/readiness checks
- Persistent database/upload volumes
- PostgreSQL backup and guarded restore scripts
- GitHub Actions quality gates and production-like runtime validation

## Security and business correctness

- BCrypt password hashing and stateless JWT authentication.
- Backend authorization is authoritative; frontend action hiding is only a UX layer.
- Shared-schema tenant isolation with tenant-scoped repositories and request context.
- Server-side search and pagination for operational lists.
- Pessimistic locking for scheduling, inventory updates and selected owner invariants.
- Optimistic concurrency conflicts mapped to HTTP `409 CONCURRENT_MODIFICATION`.
- Technician `/my-schedule` is resolved from the authenticated user rather than a client-supplied technician ID.
- Technician field transitions remain assignment-scoped; through `COMPLETED`, the assigned Technician owns actual field results/used parts/billing draft, then records customer acceptance and the customer's payment action. Technician cannot settle payment, issue the official receipt or close the Work Order.
- Customer Service owns payment reconciliation and normal closure: `CUSTOMER_ACCEPTED` stays open until payment is `SETTLED`; receipt issuance/closure then remain CSKH responsibilities. Owner supervises and configures company bank/QR instead of impersonating those operational actions.
- Scheduling conflicts use locking plus overlap detection.
- Part `REQUEST` does not move stock; Warehouse `ISSUE` is transactionally/idempotently stock-out, Technician `USED` records actual customer usage without a second stock movement, and Warehouse `RETURN` is the physical stock-in.
- Attachment uploads enforce size limits, MIME allowlists, signature checks, normalized paths and configurable tenant quota.
- Login throttling and request correlation IDs are enabled.
- Public-demo mode protects required seeded identities and system-defined service channels while recruiter-created data remains editable according to RBAC.

## Verification and CI

GitHub Actions runs three major verification gates:

1. **Backend** — Maven tests and package build.
2. **Frontend** — TypeScript/UI-policy lint and production build.
3. **Production-like runtime** — Docker Compose starts **Nginx → Spring Boot → PostgreSQL**, verifies readiness/frontend/demo login and runs Playwright Chromium against the Nginx-fronted application.

The current Playwright suite contains **12 browser tests across 4 spec files** and covers:

- route-access policy for all five demo roles;
- Customer CRUD;
- custom Service Channel CRUD;
- Warehouse spare-part creation and stock import;
- Customer Service request intake and Service Request → Work Order conversion;
- Technician UI transition restrictions;
- backend rejection of unauthorized Technician and Dispatcher transitions;
- Warehouse frontend route isolation from Work Order and operational dashboard data;
- the current full field-service settlement journey: `REQUEST → ISSUE → USED → COMPLETED → CUSTOMER_ACCEPTED → payment → SETTLED → receipt → CLOSED → post-CLOSED RETURN`, including stock/idempotency/freeze/role assertions.

Backend security/integration tests separately exercise Warehouse direct-API denial for Work Order and operational dashboard endpoints.

See [VERIFY_RESULTS.md](VERIFY_RESULTS.md) for the previous verified baseline and the revalidation required after the current release-consistency patch.

## Run locally

### Prerequisites

Required:

- Java JDK 21
- Node.js 22 LTS + npm
- Git

Choose one PostgreSQL option:

- PostgreSQL 17 installed locally; or
- Docker Desktop for the repository-managed PostgreSQL 17 container.

### First-time setup

From the repository root, create your local environment file:

```powershell
Copy-Item .env.example .env
```

The committed example uses disposable local values:

```text
POSTGRES_DB=serviceops
POSTGRES_USER=serviceops
POSTGRES_PASSWORD=serviceops
DEMO_PASSWORD=Demo@2026
```

If your existing native PostgreSQL uses different credentials, edit only your local `.env` file. `.env` is ignored by Git.

For the shortest first run with Docker Desktop, the next command creates/starts PostgreSQL and launches both application terminals:

```powershell
.\scripts\dev-start.ps1 -StartPostgres
```

If you use native PostgreSQL, create the `serviceops` database/user once (or point `.env` at your existing database); the exact SQL is in [RUN_LOCAL.md](RUN_LOCAL.md).

### Daily quick start — backend + frontend together

If PostgreSQL is already running:

```powershell
.\scripts\dev-start.ps1
```

If you use Docker Desktop and want the script to start the repository PostgreSQL container first:

```powershell
.\scripts\dev-start.ps1 -StartPostgres
```

`dev-start.ps1` is repository-relative: it works regardless of where the repository was cloned. It opens separate backend and frontend terminals, passes the same `DEMO_PASSWORD` to both sides, and runs `npm ci` automatically when `frontend/node_modules` does not exist.

Wait for the backend log to contain `Started ServiceOpsApplication`, then open:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:3000` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

For manual backend/frontend startup, native PostgreSQL setup and troubleshooting, see [RUN_LOCAL.md](RUN_LOCAL.md).

### Playwright against the local development stack

The E2E suite mutates business data, so it refuses `localhost:3000`/`5173` by default. For local development, point ServiceOps at a **disposable PostgreSQL database** (not data you want to keep), start backend/frontend normally, then opt in explicitly:

```powershell
cd frontend
$env:E2E_BASE_URL = "http://localhost:3000"
$env:E2E_DEMO_PASSWORD = "Demo@2026"
$env:E2E_ALLOW_LOCAL_MUTATIONS = "true"
npm run e2e
```

No Docker is required for this local E2E path. Remove the environment variables after the run if you do not want them reused in the current terminal.

## Production-like validation

Create `.env.production` from the provided example and replace every placeholder before starting the stack:

```powershell
Copy-Item .env.production.example .env.production

docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Default local production-like entry point:

```text
http://localhost:8088
```

Use `docker compose ... down` when finished. Do **not** add `-v` unless persistent PostgreSQL/upload volumes are intentionally being deleted.

For an Internet-facing demo, follow [docs/PRODUCTION_DEPLOYMENT.md](docs/PRODUCTION_DEPLOYMENT.md).

## Repository structure

```text
backend/                  Spring Boot modular monolith
  src/main/java/          Application and business modules
  src/main/resources/     Configuration and Flyway migrations
  src/test/java/          Unit and PostgreSQL integration tests

frontend/                 React operations console
  src/features/           Feature-oriented frontend modules
  e2e/                    Playwright browser E2E

scripts/                  Local developer helpers
  dev-start.ps1           One-command backend + frontend startup
  start-postgres.ps1      Optional local PostgreSQL container startup
  check-local.ps1         Local backend/frontend verification
  production/             Backup and guarded restore utilities
docs/                     Architecture, security, business and operations docs
.github/workflows/        CI pipeline
docker-compose.local.yml  Optional local PostgreSQL container
docker-compose.prod.yml   Production-like Nginx → backend → PostgreSQL stack
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Business flow](docs/BUSINESS_FLOW.md)
- [Security model](docs/SECURITY.md)
- [Database design](docs/DATABASE.md)
- [API reference](docs/API.md)
- [Production deployment](docs/PRODUCTION_DEPLOYMENT.md)
- [User guide](docs/USER_GUIDE.md)
- [UAT checklist](docs/UAT_CHECKLIST.md)
- [Verification results](VERIFY_RESULTS.md)
- [Roadmap](docs/ROADMAP.md)
- [Codebase standards](docs/CODEBASE_STANDARDS.md)
- [UI design system](docs/UI_DESIGN_SYSTEM.md)
- [Development process](docs/DEVELOPMENT_PROCESS.md)

## Scope and design decisions

The current portfolio baseline is intentionally **feature-frozen around the end-to-end field-service workflow**.

Further work should prioritize verified bugs, security/business correctness, automated coverage, documentation accuracy, deployment reliability and recruiter/demo usability.

Microservices, Kafka, Redis, Kubernetes, Elasticsearch and similar infrastructure are intentionally not added merely to make the portfolio appear more complex. They should be introduced only when a concrete scaling, availability or integration requirement justifies their operational cost.

Possible future additions such as SLA/service windows, preventive-maintenance agreements and technician mobile/PWA support remain optional roadmap items rather than unfinished requirements of the current portfolio baseline.
