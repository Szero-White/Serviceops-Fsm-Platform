# ServiceOps — Field Service Operations Platform

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

**Live demo:** _Coming soon — add the deployed URL here before sharing the portfolio._

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
| Warehouse / spare-parts staff | `WAREHOUSE_STAFF` | Spare-parts catalog, stock import, inventory balances and part lifecycle management |

The frontend hides routes and actions that are outside a role's responsibility, while the backend remains the authoritative authorization boundary.

## End-to-end operating story

Consider a customer reporting that an air conditioner is no longer cooling properly. The same case moves through ServiceOps as it would through a real service organization:

1. **Customer Service receives the issue.** The agent finds or creates the customer, records the customer's equipment and selects the configured intake channel such as phone or email. If a technical identifier such as the serial number is not available during the first call, the equipment can still be registered and the serial can be completed later after verification.
2. **A Service Request is opened.** The request keeps the customer, optional equipment, issue description, priority and intake channel together. Asset selection is scoped to the selected customer, and the backend rejects a mismatched customer/asset relationship.
3. **The request becomes a Work Order.** The operational job is created from the request while preserving the source customer and equipment relationship.
4. **Dispatch plans the visit.** A Dispatcher selects a technician and schedules or reschedules the work. Scheduling uses overlap detection and locking so the same technician is not silently double-booked.
5. **The technician receives the assignment.** The technician sees the job through the personal schedule derived from the authenticated account, not from a client-supplied technician identifier.
6. **Field execution begins.** The technician progresses the assigned job through field states such as `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` and `COMPLETED`. Management-only transitions remain unavailable to the technician.
7. **Spare parts participate in the same job.** When a repair needs a part, consumption is recorded against the Work Order and the stock balance is reduced transactionally. Negative stock is blocked. Inactive/discontinued parts remain historically traceable but cannot be newly consumed.
8. **The service result is documented.** Diagnosis, resolution notes and JPG/PNG/WEBP/PDF evidence stay attached to the job so the service record explains both what was found and what was done.
9. **The job is accepted and closed.** After completion, an authorized management role can record customer acceptance and close the Work Order. Reopen/cancel paths remain controlled by the work-order state machine.
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
        ├── needs spare part ──→ Warehouse / Inventory
        │                         ↓
        └──────── consume part ←──┘
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
| Warehouse | `warehouse` | `Demo@2026` | Spare parts, stock transactions and inventory operations |

`technician-2` is an additional seeded technician account and also uses `Demo@2026` in the current local portfolio environment. It is intentionally **not** a sixth quick-login card. It is used to verify isolation between two individual technicians who share the `TECHNICIAN` role, especially for `/my-schedule` and assigned work.

> `Demo@2026` is the disposable credential used by the current local/demo portfolio setup. Production database, JWT and infrastructure secrets must remain separate and must not be committed.

## Recruiter walkthrough

For a review, use **one service case across every role** instead of demonstrating unrelated CRUD records:

1. Sign in as **Customer Service**, create or inspect a customer and equipment record, then create a Service Request.
2. Convert that exact request into a Work Order and keep its generated code as the trace identifier for the rest of the demo.
3. Sign in as **Dispatcher**, assign a technician and demonstrate schedule/reschedule behavior.
4. Sign in as **Technician**, confirm the same Work Order appears in the personal schedule, start field execution and record the service result.
5. Sign in as **Warehouse** when spare-parts preparation or catalog lifecycle needs to be demonstrated; then return to the technician and consume the part through the Work Order.
6. Complete the Work Order and use an authorized management role for customer acceptance and closure.
7. Finish as **Owner** by reviewing users, dashboard, history and audit data for the same operational story.
8. Switch roles or open protected routes directly to verify that frontend visibility and backend authorization remain aligned.

## Product capabilities

- Customer and customer-equipment management, including equipment whose serial is not yet known at service intake.
- Configurable service-request intake channels.
- Service Request → Work Order conversion with customer/asset consistency checks.
- Work-order lifecycle with controlled role-aware transitions and history.
- Technician assignment, overlap-safe scheduling and weekly dispatcher schedule board.
- Personal technician schedule derived from the authenticated account.
- Spare-parts catalog, stock transactions, discontinue/reactivate lifecycle and negative-stock protection.
- Safe hard-delete behavior for pristine spare parts while preserving inventory history for used parts.
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
- Technician field transitions are limited to `ON_THE_WAY`, `IN_PROGRESS`, `WAITING_FOR_PARTS` and `COMPLETED`.
- Management transitions such as `CANCELLED`, `CUSTOMER_ACCEPTED`, `CLOSED` and `REOPENED` remain restricted to management roles.
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

The current Playwright suite contains **10 browser tests across 3 spec files** and covers:

- route-access policy for all five demo roles;
- Customer CRUD;
- custom Service Channel CRUD;
- Warehouse spare-part creation and stock import;
- Customer Service request intake and Service Request → Work Order conversion;
- Technician UI transition restrictions;
- backend rejection of unauthorized Technician management transitions.

See [VERIFY_RESULTS.md](VERIFY_RESULTS.md) for the current verified baseline.

## Run locally

The current local portfolio environment uses PostgreSQL password `123456` and demo-user password `Demo@2026`.

### Prerequisites

- Java JDK 21
- Node.js 22 LTS + npm
- PostgreSQL 17
- Git

Docker Desktop is optional for daily development when PostgreSQL already runs natively.

### Start backend and frontend with one PowerShell command

If PostgreSQL is already running locally with database `serviceops`, user `postgres` and password `123456`:

```powershell
Start-Process cmd.exe -WorkingDirectory "D:\Study\Java\ServiceOps FSM\backend" -ArgumentList "/k",'set POSTGRES_HOST=localhost&& set POSTGRES_PORT=5432&& set POSTGRES_DB=serviceops&& set POSTGRES_USER=postgres&& set POSTGRES_PASSWORD=123456&& set DEMO_PASSWORD=Demo@2026&& mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"'; Start-Process cmd.exe -WorkingDirectory "D:\Study\Java\ServiceOps FSM\frontend" -ArgumentList "/k","set VITE_DEMO_PASSWORD=Demo@2026&& npm run dev"
```

Wait for the backend log to contain `Started ServiceOpsApplication`, then open:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:3000` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

More local setup and troubleshooting notes are in [RUN_LOCAL.md](RUN_LOCAL.md).

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

scripts/production/       Backup and guarded restore utilities
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
