# ServiceOps — Field Service Operations Platform

ServiceOps is a production-oriented field-service management platform built as a **Spring Boot modular monolith** with a React operations console. The project models an end-to-end service workflow from customer intake to technician execution, parts consumption, acceptance and closure while preserving tenant isolation, role-based access, transactional consistency and auditability.

> Portfolio focus: realistic business workflow, maintainable application structure, security/concurrency controls, production-like deployment and a recruiter-friendly public demo — not feature-count maximization.

## Product scope

ServiceOps supports the core operational chain:

**Customer → Asset → Service Request → Work Order → Technician Scheduling → Service Execution → Parts Consumption → Completion → Customer Acceptance → Closure**

Key capabilities:

- Customer and installed-asset management.
- Configurable service-request intake channels.
- Work-order lifecycle with controlled state transitions.
- Technician assignment and overlap-safe scheduling.
- Spare-parts inventory with transaction ledger and negative-stock protection.
- Work-order attachments for image/PDF evidence.
- Service invoice/export view derived from work-order data and consumed parts.
- Notifications, audit trail and operational dashboard.
- OWNER, DISPATCHER, CUSTOMER_SERVICE, TECHNICIAN and WAREHOUSE_STAFF roles.
- Shared-schema multi-tenancy with tenant-scoped application access.

## Engineering highlights

### Backend

- Java 21, Spring Boot 3.5, Spring Data JPA and PostgreSQL 17.
- Spring Security with JWT authentication and RBAC.
- Flyway-managed schema migrations.
- Pessimistic locking for scheduling, inventory and tenant-level owner invariants.
- Controlled optimistic-lock conflict handling (`409 CONCURRENT_MODIFICATION`).
- Login throttling, request correlation IDs and hardened production configuration.
- Attachment MIME/signature validation, path-boundary protection and tenant storage quota.
- JUnit 5, Mockito and Testcontainers-backed PostgreSQL integration coverage.

### Frontend

- React 19, TypeScript, Vite, Ant Design and TanStack Query.
- Feature-oriented module structure.
- Shared enterprise UI primitives for page headers, filters, tables, statuses and forms.
- Role-aware actions and recruiter-friendly demo account selector.
- Production bundle served by Nginx with API proxying.

### Operations

- Multi-stage backend/frontend Docker builds.
- Docker Compose production topology: **Nginx → Spring Boot → PostgreSQL**.
- PostgreSQL is private to the Compose network.
- Health/readiness checks and persistent database/upload volumes.
- Backup and guarded restore scripts.
- GitHub Actions for backend, frontend and Docker build validation.

## Demo accounts

The login screen exposes five demo roles so reviewers can quickly inspect permission boundaries and business workflows.

| Role | Username | Recommended review area |
|---|---|---|
| Owner | `owner` | Full administration and operational oversight |
| Dispatcher | `dispatcher` | Work orders, technician assignment and scheduling |
| Customer Service | `customer-service` | Customers, assets and service-request intake |
| Technician | `technician` | Field execution and work-order lifecycle |
| Warehouse | `warehouse` | Spare parts and inventory operations |

Local development uses the seeded password `123456`. A public deployment **must** provide a separate strong `DEMO_PASSWORD`; the production frontend receives that public demo credential only as a build-time demo value so role cards can populate the login form. Never reuse an administrative/production secret as a demo password.

## Recommended recruiter walkthrough

1. Sign in as **Customer Service** and review/create a customer, asset and service request.
2. Convert the request into a work order.
3. Sign in as **Dispatcher**, assign a technician and schedule the work.
4. Sign in as **Technician**, progress the work order through field execution.
5. Record used parts and upload service evidence.
6. Complete the work order with diagnosis and resolution notes.
7. Review the invoice/export, audit trail and dashboard.
8. Switch roles to confirm actions are permission-aware.

## Run locally

Detailed instructions are in [RUN_LOCAL.md](RUN_LOCAL.md).

```powershell
# PostgreSQL
Copy-Item .env.example .env
docker compose -f docker-compose.local.yml up -d

# Backend
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Frontend
cd ../frontend
npm ci
npm run dev
```

Local endpoints:

- Frontend: `http://localhost:3000`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Production-like validation

```powershell
Copy-Item .env.production.example .env.production
# Replace every CHANGE_ME value before continuing.

docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

The local production-like frontend is exposed on `http://localhost:8088` by default. Use `docker compose ... down` when finished; do not add `-v` unless you intentionally want to delete persistent database/upload volumes.

## Repository structure

```text
backend/                 Spring Boot modular monolith
frontend/                React operations console
frontend/src/features/   Feature-oriented frontend modules
frontend/src/types/      Domain-specific TypeScript contracts
scripts/production/      Backup and restore utilities
docs/                    Architecture, security, business and operations docs
.github/workflows/       CI validation
docker-compose.prod.yml  Production-like single-node topology
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Business flow](docs/BUSINESS_FLOW.md)
- [Security model](docs/SECURITY.md)
- [Database design](docs/DATABASE.md)
- [API reference](docs/API.md)
- [Production deployment](docs/PRODUCTION_DEPLOYMENT.md)
- [Codebase standards](docs/CODEBASE_STANDARDS.md)
- [UI design system](docs/UI_DESIGN_SYSTEM.md)
- [Product & engineering roadmap](docs/ROADMAP.md)
- [Development process](docs/DEVELOPMENT_PROCESS.md)
- [User guide](docs/USER_GUIDE.md)
- [UAT checklist](docs/UAT_CHECKLIST.md)
- [Senior hardening report](docs/SENIOR_HARDENING_REPORT.md)
- [Verification results](VERIFY_RESULTS.md)

## Product direction

The existing feature set is intentionally a cohesive field-service workflow rather than a collection of CRUD screens. The highest-value next product increments are a **dispatcher schedule board**, **SLA/promised service windows**, and **preventive-maintenance agreements**. They are documented in [docs/ROADMAP.md](docs/ROADMAP.md) and should be implemented one at a time with tests and deployment impact reviewed.

## Architecture direction

ServiceOps intentionally remains a **modular monolith**. Microservices, Kafka, Redis, Kubernetes and external object storage are not introduced merely for portfolio complexity. Those technologies should be added only when an explicit scaling, reliability or integration requirement justifies the operational cost.

The current design prioritizes cohesive modules, explicit business invariants, testability, transactional boundaries and a deployment model that a small team can operate safely.
