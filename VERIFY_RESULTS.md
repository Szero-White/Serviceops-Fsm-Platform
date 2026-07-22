# Verification Results

Verified on 2026-07-21 before packaging.

## Backend

- Java 21 compilation: passed.
- Maven test suite: 7 tests discovered, 6 passed, 1 Testcontainers smoke test skipped because Docker is unavailable in the packaging environment.
- Separate real PostgreSQL smoke test: passed.
  - Spring Boot application context started.
  - Flyway migration V1 applied successfully.
  - Hibernate schema validation passed.
  - Demo data seeding completed.
  - Login API returned a JWT access token.
  - Authenticated dashboard, customer, asset, service request, work order, technician, inventory, audit and notification APIs returned successfully.
  - Technician-scoped work-order access worked and customer access was correctly rejected with HTTP 403.

The included `LocalPostgresSmokeTest` runs automatically when Docker is available.

## Frontend

- TypeScript type check: passed.
- Production build: passed.
- Dependency audit: 0 vulnerabilities reported.

Vite reports one non-blocking warning for a shared vendor chunk above 500 kB. Feature pages are already lazy-loaded; further vendor splitting is an optimization, not a local-run blocker.

## Packaging

Generated folders are intentionally excluded:

- `backend/target`
- `frontend/node_modules`
- `frontend/dist`
- local uploads
- environment secrets
