# ServiceOps — Field Service Operations Platform

[![CI](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Szero-White/Serviceops-Fsm-Platform/actions/workflows/ci.yml)

**Live demo:** _Coming soon — add the deployed URL here before sharing the portfolio._

ServiceOps is a full-stack field-service operations platform built as a **Spring Boot modular monolith** with a **React + TypeScript** operations console.

It models the main field-service lifecycle from customer intake and asset service requests through dispatching, technician execution, spare-parts consumption, customer acceptance, closure, audit and reporting. The project focuses on **business correctness, RBAC, tenant isolation, transactional consistency, concurrency control, automated verification and production-like deployment**.

## Core workflow

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

The work-order state machine also supports controlled branches such as `WAITING_FOR_PARTS`, `REOPENED` and `CANCELLED`. Invalid or unauthorized transitions are rejected by the backend.

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

A short walkthrough demonstrates the end-to-end workflow and role boundaries:

1. Sign in as **Customer Service** and create or inspect a customer and installed asset.
2. Create a **Service Request** and convert it into a **Work Order**.
3. Sign in as **Dispatcher**, open **Lịch điều phối**, assign a technician and schedule or reschedule the work.
4. Sign in as **Technician**, open **Lịch của tôi**, verify the appointment and progress the assigned work through field execution.
5. Record consumed spare parts and upload service evidence.
6. Enter diagnosis/resolution notes and complete the work order.
7. Sign in as **Owner** or **Dispatcher** to perform management-side acceptance/closure and review history, invoice export, notifications, dashboard and audit data.
8. Switch roles or open protected routes directly to verify that frontend visibility and backend authorization remain aligned.

## Product capabilities

- Customer and installed-asset management.
- Configurable service-request intake channels.
- Service Request → Work Order conversion with customer/asset consistency checks.
- Work-order lifecycle with controlled role-aware transitions and history.
- Technician assignment, overlap-safe scheduling and weekly dispatcher schedule board.
- Personal technician schedule derived from the authenticated account.
- Spare-parts catalog, stock transactions and negative-stock protection.
- CSV import/export for customers, assets and spare parts.
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

The current portfolio environment uses `Demo@2026` for both the local PostgreSQL password and demo-user password.

### Prerequisites

- Java JDK 21
- Node.js 22 LTS + npm
- PostgreSQL 17
- Git

Docker Desktop is optional for daily development when PostgreSQL already runs natively.

### Start backend and frontend with one PowerShell command

If PostgreSQL is already running locally with database `serviceops`, user `serviceops` and password `Demo@2026`:

```powershell
Start-Process cmd.exe -WorkingDirectory "D:\Study\Java\ServiceOps FSM\backend" -ArgumentList "/k",'set POSTGRES_HOST=localhost&& set POSTGRES_PORT=5432&& set POSTGRES_DB=serviceops&& set POSTGRES_USER=serviceops&& set POSTGRES_PASSWORD=Demo@2026&& set DEMO_PASSWORD=Demo@2026&& mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"'; Start-Process cmd.exe -WorkingDirectory "D:\Study\Java\ServiceOps FSM\frontend" -ArgumentList "/k","set VITE_DEMO_PASSWORD=Demo@2026&& npm run dev"
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
