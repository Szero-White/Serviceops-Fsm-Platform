# Frontend Feature Modules

The frontend follows the same business boundaries as the backend modules.

- `auth`: login page, current session API, auth provider, and route guard.
- `dashboard`: operational overview API.
- `users`: account and role management API.
- `customers`: customer CRUD API.
- `assets`: customer asset CRUD API.
- `service-requests`: intake queue API.
- `service-channels`: intake channel settings API.
- `work-orders`: Work Order API, lifecycle UI, shared scheduling action modal, completion and parts-workflow composition.
- `scheduling`: schedule-board/my-schedule views; reuses the Work Order scheduling action owned by `work-orders`.
- `technicians`: technician profile API.
- `inventory`: spare-part catalog, editable minimum-stock thresholds, stocktake/reconciliation, part returns and inventory movement ledger.
- `attachments`: upload, preview, download API, and attachment UI.
- `audit`: audit log API.
- `notifications`: app notification API.

Rules:

- Put domain API calls in `features/<domain>/api.ts`.
- Put domain route pages in `features/<domain>/pages/`.
- Keep transport concerns in `api/http.ts`.
- Keep domain UI under `features/<domain>/components/`.
- Keep only truly shared UI in `components/`.
- Keep shared API contracts in focused files under `types/` (`dashboard.ts`, `audit.ts`, `attachment.ts`, etc.); `types/index.ts` is only the stable barrel.
- Import each feature API from its owning feature module; there is no cross-domain API barrel.

- Remote selectors backed by paginated APIs must search server-side; do not silently cap customer/asset/part choices to the first fixed page.
