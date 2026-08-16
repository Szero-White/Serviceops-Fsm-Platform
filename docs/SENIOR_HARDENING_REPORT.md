# Senior Hardening Report

## Scope rule

No business feature was intentionally removed. This pass hardens the existing ServiceOps/FSM modular monolith around production safety, public-demo safety, transaction consistency and regression coverage. Optional or debatable features remain in the repository and are listed under **Deferred** instead of being deleted.

## Implemented

### Runtime/build consistency

- Standardized Maven/CI target on Java 21.
- Added explicit `prod` and `demo` Spring profiles while preserving the existing local profile behavior.
- Added production Dockerfiles and a single-node Docker Compose topology for frontend/backend/PostgreSQL.
- Added Prometheus registry support for the already-exposed Actuator metrics endpoint.
- Frontend production build uses `/api/v1`; Nginx serves the SPA and proxies API/health/OpenAPI routes.
- Public compose binds frontend only to host loopback so a host TLS proxy owns Internet-facing ports.

### Authentication/security

- Production JWT secret has no fallback and must be valid Base64 with at least 256 bits after decoding.
- Production/demo access-token default is 30 minutes; local development remains unchanged.
- Added layered login failure throttling for normalized username + remote-address pairs, aggregate account failures across IPs, and aggregate source-IP failures across usernames.
- The in-memory limiter removes expired state and has a hard cap to prevent unbounded key growth on the planned single-node demo.
- AI integration defaults off in production but remains available and is not removed.
- Added sanitized `X-Request-ID` correlation IDs, MDC logging context and server-side logging for unexpected exceptions.
- API-created user/technician passwords now require at least 8 characters; local seeded `123456` remains local-only convenience and is not prefilled/exposed by the production frontend.

### Public demo safety

- Added `DEMO_MODE` without removing endpoints.
- Demo mode blocks all deletes and administrative writes for users, technicians and service channels while keeping the core service workflow writable.
- Protected path matching is segment-safe so similarly named unrelated endpoints are not accidentally blocked.
- The existing “keep at least one active OWNER” invariant is now serialized with a pessimistic tenant-row lock so two owners cannot concurrently disable each other and leave a tenant ownerless.
- Spare-part consumption now locks the target work order before checking its editable status, preventing a close/cancel race. Optimistic-lock conflicts are returned as controlled HTTP 409 `CONCURRENT_MODIFICATION` responses instead of generic 500 errors.
- Demo seed password is externalized and rejects the local `123456` value plus shipped placeholder values when public demo protection is enabled.
- Known seeded demo accounts are re-synchronized to `DEMO_PASSWORD` when the demo profile restarts.

### Attachment/storage hardening

- Retained the existing JPG/PNG/WEBP/PDF MIME allowlist.
- Added magic-byte/file-signature validation.
- Added strict nested tenant-folder validation and normalized path-boundary checks.
- Added optional per-tenant storage quota; public demo defaults to 100 MiB, local/private environments can configure another value or `0` for unlimited.
- Uploads written before DB persistence are removed if the surrounding DB transaction rolls back.
- Physical attachment deletion occurs only after the DB transaction commits.
- Invalid rename paths are converted to a controlled 400 business error instead of leaking an unchecked path exception.

### Database/operations

- PostgreSQL remains private to the Docker network.
- Added production health/readiness configuration and persistent DB/upload volumes.
- Backup script no longer hides a failed `pg_dump` behind a successful gzip pipeline.
- Restore script validates the archive, requires an explicit confirmation token, stops the backend during destructive restore and uses `ON_ERROR_STOP`.
- Added deployment, HTTPS, backup and restore guidance.

### Regression coverage

The backend test source now contains **60 targeted `@Test` methods** covering substantially more than the original baseline, including:

- work-order lifecycle/state invariants;
- spare-part stock invariants;
- JWT secret validation;
- login throttling;
- demo safety behavior;
- attachment signatures, traversal protection and tenant quota;
- RBAC and critical read endpoints;
- cross-tenant customer isolation;
- concurrent inventory consumption;
- concurrent technician scheduling;
- concurrent last-owner protection.

The PostgreSQL integration suite remains Testcontainers-based and automatically skips when Docker is unavailable.

## Explicitly retained

These features remain in place:

- AI/Gemini integration;
- local filesystem attachment adapter;
- Swagger/OpenAPI;
- all existing business modules and CRUD endpoints;
- notification and audit behavior;
- shared-schema multi-tenancy;
- current work-order workflow/state machine;
- pessimistic locking for inventory/scheduling;
- local demo credentials/behavior when public demo protection is not enabled.

## Deferred — do not delete; decide after stable P0

1. **Refresh-token rotation/revocation** — current production access token is shortened instead of adding a second authentication lifecycle in this pass.
2. **Distributed rate limiting** — current limiter is single-node/in-memory. Move it to Redis only if multiple backend instances are introduced.
3. **S3-compatible object storage** — local adapter is suitable for one VM; switch through the existing storage abstraction before horizontal scaling.
4. **Transactional outbox / Kafka** — add only when notifications or integrations become truly asynchronous/external and dual-write reliability is required.
5. **PostgreSQL Row-Level Security** — useful tenant-isolation defense-in-depth, but should be introduced with a migration plan and dedicated RLS tests.
6. **Automated public-demo reset** — recommended after backup/restore and deployment are proven; do not add an unattended destructive reset prematurely.
7. **Malware scanner** — magic bytes block simple content-type spoofing, but a commercial/public file service should scan uploaded content before distribution.
8. **Kubernetes/microservices** — not justified for the current one-node portfolio workload and intentionally not added.
9. **Load/performance benchmark** — run realistic PostgreSQL datasets with `EXPLAIN ANALYZE` and API load tests after functional acceptance is green.
10. **Observability expansion** — structured JSON logs, centralized log aggregation, dashboards and tracing remain P1 improvements; request correlation plus the existing Actuator/Prometheus foundation are already in place.
11. **Browser token storage** — the SPA still keeps its access token client-side. Moving to HttpOnly secure cookies/BFF + CSRF protection is a threat-model/architecture decision and was deliberately deferred instead of changing authentication semantics during this hardening pass.

## Verification status for this edited package

Static/config checks completed in the editing environment:

- Java runtime available: Java 21.
- Spring YAML files parse successfully.
- Docker Compose YAML files parse successfully.
- Maven `pom.xml` parses successfully.
- production backup/restore shell scripts pass `sh -n` syntax validation.
- frontend `npm run lint` passed before an attempted clean dependency restore modified the bundled `node_modules` snapshot.

Not claimed as green in this environment:

- backend Maven test execution, because this container has no Maven distribution/cache and outbound package-registry DNS is unavailable;
- frontend production build after clean restore, because outbound npm access is unavailable and the uploaded `node_modules` snapshot had incomplete React type packages;
- Docker image build/Testcontainers execution, because Docker CLI/daemon is unavailable here.

Therefore **CI on a normal network-enabled runner is the final acceptance gate before merge/deploy**. Do not present this package as runtime-verified until the commands in `PRODUCTION_DEPLOYMENT.md` pass.
