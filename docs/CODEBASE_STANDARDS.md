# ServiceOps Codebase Standards

## Purpose

These conventions define the maintainability bar for ServiceOps. They are intentionally pragmatic: the goal is a codebase that another engineer can navigate, review and extend without introducing architecture ceremony that the current product does not need.

## Core principles

1. **One primary responsibility per file.** A file may coordinate several collaborators, but it should have one clear reason to change.
2. **Business behavior stays in the owning module.** Shared packages contain cross-cutting primitives, not domain-specific shortcuts.
3. **Names describe intent.** Prefer `PaymentReceiptHtmlRenderer` over generic names such as `Helper`, `Utils` or `Manager`.
4. **Controllers/components orchestrate; services/hooks own behavior.** Avoid embedding unrelated workflows inside route/controller/page files.
5. **Do not refactor by line count alone.** Size is a signal. Cohesion, change frequency, testability and responsibility boundaries are the actual decision criteria.
6. **Preserve compatibility during structural refactors.** Public API contracts, persisted data and business transitions require explicit migration/versioning if changed.

## Backend conventions

### Module layout

```text
<module>/
  domain/          entities, value/state types, repository contracts
  application/     use-case/application services
  web/             controllers and HTTP DTOs
  infrastructure/  adapters when a module needs technology-specific implementations
```

### Application services

A service should represent a cohesive business capability. Split when a class mixes independently changing concerns such as lifecycle transitions, rendering/export, scheduling and persistence support.

Example:

```text
WorkOrderService                 core work-order use cases
PaymentReceiptService           payment receipt orchestration
PaymentReceiptHtmlRenderer      HTML presentation concern
```

### Transactions and concurrency

- Put transaction boundaries at application-service operations that enforce business invariants.
- Use pessimistic locking only where concurrent writes can break an invariant.
- Convert expected concurrency conflicts into controlled business/API responses.
- Never depend on frontend checks to protect a backend invariant.

### Naming

- `*Controller`: HTTP entry point only.
- `*Service`: cohesive application/domain use cases.
- `*Repository`: persistence abstraction/query boundary.
- `*Properties`: typed configuration.
- `*Filter`: servlet/security request filter.
- `*Renderer` / `*Exporter`: presentation/export responsibility.

Avoid `CommonService`, `Helper`, `UtilService`, `Manager` and other names that hide responsibility.

## Frontend conventions

### Feature ownership

Feature-specific components, hooks, constants and types belong under the feature whenever they are not reused across unrelated modules.

```text
features/work-orders/
  api.ts
  components/
  model/
  pages/
```

### Page components

A page should primarily compose:

- page header and summary metadata;
- query/filter state;
- feature components;
- top-level mutation orchestration.

Complex drawers, dialogs, tables and role rules should live in focused feature files instead of expanding one page indefinitely.

### Types

Domain contracts are separated by area under `src/types/`. `src/types/index.ts` is only a stable public barrel and must not become a monolithic type declaration file.

### Data selectors and query scale

- A `Select` backed by a paginated/searchable API must debounce user input and search server-side; do not fetch an arbitrary first 100 records and treat it as the complete option set.
- Preserve the currently selected/editing entity even when it falls outside the latest search page.
- Small bounded reference sets (roles, status enums, service channels, a technician roster at current scale) may remain client-filtered when the backend intentionally returns the full set.
- Prefer correctness-first query invalidation after business mutations; introduce shared query-key factories only when they reduce repeated mistakes without hiding domain dependencies.

### Navigation and authorization

- `router/routeAccess.ts` defines frontend route availability; backend `@PreAuthorize`/service guards remain the real security boundary.
- `navigation/navigationConfig.tsx` defines role-focused sidebar grouping/order only. It may intentionally hide auxiliary read-only routes from the main menu, but it must never grant access that `routeAccess` or the backend denies.
- Sidebar labels should describe the user's task/workspace, not database/module names. Keep the navigation registry centralized instead of duplicating role menus inside layout components.
- AI Help and E2E role contracts must be updated whenever a role workspace or navigation label changes.

### UI consistency

Operational pages should use the shared application language:

```text
PageHeader → Filter/Toolbar → DataTable → Drawer/Detail → Focused Modal
```

Use shared status tags, spacing, empty states and form patterns before introducing page-specific alternatives.

### Visual semantics and naming

- Name presentation variants by meaning (`success`, `warning`, `danger`, `neutral`) instead of arbitrary colors (`green`, `purple`, `orange`).
- Keep normal business states visually neutral; reserve saturated semantic color for exceptions and lifecycle states that need attention.
- Do not introduce inline hex colors in feature components when a shared token/class can express the same intent.
- A table should not require a fixed action column by default. Use it only when real width/interaction requirements justify the additional scroll/sticky complexity.
- Public landing claims must be traceable to implemented code or explicit roadmap documentation. Do not invent customers, metrics, prices, integrations or testimonials.

## Demo safety

- Public demo credentials are intentionally disposable and must never be reused for real administrative accounts.
- `DEMO_MODE` protects destructive administration while preserving the core editable service workflow.
- Public demo frontend convenience may expose the public demo password because it is a presentation credential, not a secret. The underlying environment/database/JWT secrets remain private.

## Pull-request acceptance gate

Before merging a structural or production change:

```text
Backend tests             PASS
Frontend type/lint        PASS
Frontend production build PASS
npm audit                 no known vulnerabilities
Docker build              PASS
Production-like health    backend/frontend/postgres healthy
Manual smoke flow         PASS
Working tree              clean
```

A refactor is incomplete if behavior changes unintentionally or verification becomes weaker.

- Queue/list use case phải batch-load dữ liệu liên quan; không gọi repository/service theo từng row. `part-outstanding` tải request + ISSUE/RETURN + USED theo batch để tránh N+1.
