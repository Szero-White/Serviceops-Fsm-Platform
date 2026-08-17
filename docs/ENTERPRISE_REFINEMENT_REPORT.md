# Enterprise Refinement Report

## Objective

Raise the portfolio from a strong functional hardening baseline to a codebase that is easier for a senior reviewer to navigate, easier to demonstrate to recruiters and safer to evolve without changing the established business scope.

## Principles used

- No intentional business-feature removal.
- No microservice/Kafka/Kubernetes expansion for appearance only.
- Preserve API/business behavior during structural refactors.
- Prefer cohesive feature-owned files over generic helpers.
- Treat line count as a maintainability signal, not a target metric.
- Keep public demo credentials disposable and separate from real secrets.

## Refactored hotspots

### Work-order frontend

Before, `WorkOrdersPage.tsx` owned filters, data queries, RBAC rules, table rendering, detail drawer, transition actions, four dialogs, upload and invoice export in one file.

Now responsibilities are separated into:

- `WorkOrdersPage.tsx` — route-level orchestration;
- `WorkOrderTable.tsx` — list presentation;
- `WorkOrderDetailDrawer.tsx` — detail/timeline/attachment presentation and actions;
- `WorkOrderDialogs.tsx` — create/schedule/complete/parts forms;
- `workOrderPermissions.ts` — role rules;
- `workOrderPresentation.ts` — transition/status/priority presentation metadata.

### Login / demo entry

`LoginPage.tsx` is now route/auth orchestration only. Presentation lives in:

- `LoginHero.tsx`;
- `LoginPanel.tsx`;
- `DemoAccountSelector.tsx`.

Five demo role cards support one-click credential population when `VITE_DEMO_PASSWORD` is configured.

### Frontend type contracts

The previous monolithic `src/types/index.ts` was split into domain files (`auth`, `customer`, `asset`, `service-request`, `work-order`, `inventory`, `operations`, `common`). `index.ts` remains a stable barrel so existing callers do not need disruptive import rewrites.

### Invoice export

Invoice responsibilities are separated into:

- `WorkOrderInvoiceService` — application orchestration and consumed-part query;
- `WorkOrderInvoiceHtmlRenderer` — formatting/escaping/rendering behavior;
- `templates/work-order-invoice.html` — document markup and CSS.

This removes a large HTML document from application-service source code.

### AI help

`AiHelpService` now focuses on provider orchestration, response parsing and auditing. Role/route help topics live in `AiHelpKnowledgeBase`.

### Demo bootstrap

`DemoDataSeeder` now describes the demo scenario and transaction boundary. `DemoDataFactory` owns fixture construction/persistence helpers. This keeps the runner readable without changing seeded business data.

## Enterprise UI refinement

Shared visual tokens were moved toward a calmer operations-SaaS presentation:

- muted blue primary palette;
- neutral application background;
- simplified sidebar/header shadows;
- non-gradient page titles;
- reduced decorative animation;
- consistent white/default secondary buttons;
- clearer recruiter/demo account hierarchy.

Because authenticated screens already share `PageHeader`, table toolbar, table and form conventions, updating shared tokens/styles improves the whole application without duplicating page-level rewrites.

## Production source-size audit

After the pass, no production Java/TypeScript source file exceeds 300 lines. The largest remaining files are cohesive feature/application files in the ~250–292 line range. They should only be split further when responsibility or change patterns justify it.

## Verification note

The hardening baseline was runtime-verified before this pass (59 backend tests, frontend lint/build, zero npm audit findings, healthy Docker Compose runtime and passing GitHub PR checks). This structural revision still requires the normal acceptance gate on a network-enabled workstation/CI runner because the editing container cannot download the Maven/npm dependencies required for a fresh build.

## Product UI coherence follow-up

A later UI pass standardizes the visual system across authenticated pages and the public landing page:

- application navigation is grouped by operational purpose instead of one long flat menu;
- normal business states use neutral presentation while warning/danger/success colors are reserved for semantic meaning;
- table headers/body/action controls share one density and typography model;
- fixed-right action columns are avoided by default to reduce sticky-scrollbar noise;
- dashboard removes duplicated promotional summary content and starts with actionable KPI cards;
- landing content removes unsupported customer/testimonial/pricing/integration claims and separates implemented capability from roadmap capability;
- visual variants are named by semantic intent rather than decorative color names.

This follow-up intentionally changes presentation and documentation, not backend domain behavior. The next product feature should be selected from `docs/ROADMAP.md` only after the UI/refactor branch passes the full acceptance gate.


## Typography consistency audit

The authenticated frontend now uses a six-size productive scale (`10/11/12/14/18/20px`) and shared Ant Design tokens. Page CSS is not allowed to introduce raw `font-size` values; `npm run lint` runs `scripts/check-ui-typography.mjs` to prevent fractional sizes, heavy weights, forced uppercase, or page-specific font-size drift. Public landing typography remains a separate, restrained expressive scale because its reading context differs from operations screens.
