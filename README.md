# ServiceOps — Field Service Operations Platform

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

**Live demo:** Not deployed publicly yet. The repository includes a complete local setup and production-like Docker validation path.

ServiceOps is a full-stack operations platform for a **field-service maintenance and repair business**. It coordinates the departments that receive customer issues, manage customer equipment, plan field visits, perform technical work, control spare-parts stock and oversee the service lifecycle.

The project is intentionally modeled as a connected business process rather than a collection of isolated CRUD screens. A customer issue becomes a service request, then a work order, then a scheduled technician job; field execution can consume warehouse stock, and the completed job remains traceable through acceptance, closure, history, notifications, invoice export and audit records.

## Business problem

A field-service company has to keep several departments synchronized around the same service case. Customer Service needs accurate customer and equipment context. Dispatchers need assignable technicians and conflict-free schedules. Technicians need only the jobs assigned to them and a clear execution workflow. Warehouse staff need reliable stock balances and spare-part lifecycle controls. Management needs visibility, accountability and a durable history of what happened.

ServiceOps provides one operational record that follows the work across those handoffs so the organization does not have to coordinate the same job through disconnected spreadsheets, chat messages or department-specific records.

## Who uses the system

| Real-world responsibility | ServiceOps role | Main responsibilities in the system |
| --- | --- | --- |
| Business owner / operations management | `OWNER` | User administration, overall operations, dashboard, audit, work-order management and oversight |
| Dispatch / service coordination | `DISPATCHER` | Work orders, technician resources, assignment, scheduling/rescheduling, operational history and audit review |
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
7. **Spare parts participate in the same job.** When a repair needs a part, consumption is recorded against the assigned Work Order only while field execution is active (`ASSIGNED`, `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `REOPENED`) and the stock balance is reduced transactionally. The Work Order activity timeline immediately surfaces `CONSUME`/`RETURN` events with part, quantity, actor and time, while inventory transactions remain the source of truth. No new consumption is accepted after completion/customer acceptance. Negative stock is blocked. Inactive/discontinued parts remain historically traceable but cannot be newly consumed.
8. **The service result is documented.** Diagnosis, resolution notes and JPG/PNG/WEBP/PDF evidence stay attached to the job so the service record explains both what was found and what was done.
9. **The result is accepted and the job is closed.** After `COMPLETED`, the assigned Technician can record the customer's on-site acceptance, while Owner has the same acceptance/closure capability as an administrative override. `CUSTOMER_ACCEPTED` exposes the final **Close Work Order** action. If the customer reports that the same issue persists before closure, the Work Order can be `REOPENED`; after `CLOSED`, a later issue starts a new Service Request/Work Order so the original service history remains immutable.
10. **The organization can trace the result.** Work-order history, notifications, invoice export, dashboard data and audit records provide the operational trail after the field visit is finished.

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
        ├── consume part ─────→ Inventory transaction / stock decreases
        │
        ├── unused quantity ────→ Warehouse confirms controlled RETURN
        │                         └── stock increases + ledger trace
        ↓
Diagnosis → Resolution → Evidence → COMPLETED
        ↓
Customer Acceptance
        ↓
CLOSED
        ↓
History / Invoice / Notifications / Dashboard / Audit
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
  → Parts Consumption
  → Completion
  → Customer Acceptance
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
| Warehouse | `warehouse` | `Demo@2026` | Spare parts, stocktake, Work Order part returns and inventory movement history |

`technician-2` is an additional seeded technician account and also uses `Demo@2026` in the current local portfolio environment. It is intentionally **not** a sixth quick-login card. It is used to verify isolation between two individual technicians who share the `TECHNICIAN` role, especially for `/my-schedule` and assigned work.

> `Demo@2026` is the disposable credential used by the current local/demo portfolio setup. Production database, JWT and infrastructure secrets must remain separate and must not be committed.

## Recruiter walkthrough

For a review, use **one service case across every role** instead of demonstrating unrelated CRUD records:

1. Sign in as **Customer Service**, create or inspect a customer and equipment record, then create a Service Request.
2. Convert that exact request into a Work Order and keep its generated code as the trace identifier for the rest of the demo.
3. Sign in as **Dispatcher**, assign a technician and demonstrate schedule/reschedule behavior.
4. Sign in as **Technician**, confirm the same Work Order appears in the personal schedule, start field execution and record the service result.
5. Sign in as **Warehouse** to review the stock movement created by the Work Order, demonstrate a controlled part return when applicable, and run a stocktake/reconciliation example.
6. Complete the Work Order as **Technician**, then use **Khách xác nhận** after the customer agrees and **Đóng phiếu** to move the job into Work Order History. Owner can perform the same acceptance/closure as an admin override; if the same issue persists before closure, reopen the existing job. Invoice quantities use net consumption after returns.
7. Finish as **Owner** by reviewing users, dashboard, history and audit data for the same operational story.
8. Switch roles or open protected routes directly to verify that frontend visibility and backend authorization remain aligned.

## Product capabilities

- Customer and customer-equipment management, including equipment whose serial is not yet known at service intake.
- Configurable service-request intake channels.
- Service Request → Work Order conversion with customer/asset consistency checks.
- Work-order lifecycle with controlled role-aware transitions and history.
- Technician assignment, overlap-safe scheduling and weekly dispatcher schedule board.
- Personal technician schedule derived from the authenticated account.
- Spare-parts catalog, configurable minimum-stock thresholds, stock transactions, discontinue/reactivate lifecycle and negative-stock protection.
- Safe hard-delete behavior for pristine spare parts while preserving inventory history for used parts.
- Warehouse stocktake/reconciliation, editable minimum-stock thresholds, Work Order part returns and a searchable inventory movement ledger; threshold changes are audited and can raise low-stock alerts when the new threshold makes current stock newly low. Invoice quantities use net consumption after returns.
- CSV import/export for customers, assets and spare parts; bulk asset import keeps serial as a stable required identifier.
- Work-order evidence attachments with MIME/signature/path validation and tenant-scoped storage.
- Service invoice/export view derived from work-order and consumed-parts data.
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
- Technician field transitions remain assignment-scoped; after `COMPLETED`, the assigned Technician can record `CUSTOMER_ACCEPTED`, close the Work Order, or reopen it before closure when the same issue persists.
- Customer acceptance and closure belong to the assigned Technician with Owner as an administrative override. Customer Service can reopen/cancel when handling a customer follow-up; `CLOSED` and `CANCELLED` remain terminal.
- Scheduling conflicts use locking plus overlap detection.
- Inventory consumption prevents negative stock.
- Attachment uploads enforce size limits, MIME allowlists, signature checks, normalized paths and configurable tenant quota.
- Login throttling and request correlation IDs are enabled.
- Public-demo mode protects required seeded identities and system-defined service channels while recruiter-created data remains editable according to RBAC.

## Verification and CI

GitHub Actions runs three major verification gates:

1. **Backend** — Maven tests and package build.
2. **Frontend** — TypeScript/UI-policy lint and production build.
3. **Production-like runtime** — Docker Compose starts **Nginx → Spring Boot → PostgreSQL**, verifies readiness/frontend/demo login and runs Playwright Chromium against the Nginx-fronted application.

The current Playwright suite contains **11 browser tests across 3 spec files** and covers:

- route-access policy for all five demo roles;
- Customer CRUD;
- custom Service Channel CRUD;
- Warehouse spare-part creation and stock import;
- Customer Service request intake and Service Request → Work Order conversion;
- Technician UI transition restrictions;
- backend rejection of unauthorized Technician and Dispatcher transitions;
- Warehouse frontend route isolation from Work Order and operational dashboard data.

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
