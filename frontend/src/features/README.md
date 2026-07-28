# Frontend Feature Modules

The frontend follows the same business boundaries as the backend modules.

- `auth`: login page, current session API, auth provider, and route guard.
- `dashboard`: operational overview API.
- `users`: account and role management API.
- `customers`: customer CRUD API.
- `assets`: customer asset CRUD API.
- `service-requests`: intake queue API.
- `service-channels`: intake channel settings API.
- `work-orders`: dispatch, scheduling, status transition, and parts usage API.
- `technicians`: technician profile API.
- `inventory`: spare part and stock API.
- `attachments`: upload, preview, download API, and attachment UI.
- `audit`: audit log API.
- `notifications`: app notification API.

Rules:

- Put domain API calls in `features/<domain>/api.ts`.
- Put domain route pages in `features/<domain>/pages/`.
- Keep transport concerns in `api/http.ts`.
- Keep domain UI under `features/<domain>/components/`.
- Keep only truly shared UI in `components/`.
- Keep cross-domain types in `types/` until a feature owns enough types to justify a local `types.ts`.
- Do not add new domain logic to `api/services.ts`; it is only a compatibility export surface.
