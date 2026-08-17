# Frontend Style Structure

`main.tsx` imports the app style modules directly in cascade order.

- `app/base.css`: browser reset, root text color, app background.
- `app/layout.css`: authenticated app shell, sidebar, header, user menu, notifications, route fallback.
- `app/components.css`: cascade entrypoint for shared app primitives.
- `app/components/`: focused shared modules for shell spacing, page headers, cards, controls, tables, status/cell patterns, and forms.
- `app/dashboard.css`: dashboard-specific health/recent-work and offline state.
- `app/login.css`: unauthenticated login and demo entry screen.
- `app/responsive.css`: responsive overrides for app and login views.

Landing page styles are intentionally separate under `pages/landing/styles` for public-page composition and the `lp-` namespace, while sharing the same restrained product palette, typography and radius principles.

Maintenance rules:

- Keep `app/components.css` as imports only; place shared UI styles in the matching file under `app/components/`.
- Keep page-specific styles in the matching page style file.
- Do not add new page-specific CSS to `main.tsx`; create or update the matching module instead.
- Remove a selector when its class is no longer referenced by a component, unless it targets Ant Design internals under a referenced wrapper class.
- Prefer a short comment only when a selector group exists to protect layout behavior that is not obvious.

## Typography governance

Authenticated product UI uses the productive type tokens declared in `app/base.css`. New component CSS should use those tokens instead of inventing intermediate values such as `11.5px`, `12.75px` or `13.5px`.

- `caption` / `meta`: 11px — code, timestamp and technical metadata.
- `label` / `body`: 12px — table, form, menu, control and normal product copy.
- `body-lg` / `section-title`: 13px — supporting copy and local headings.
- `panel-title`: 15px — drawer/modal hierarchy.
- `auth-title` / `metric`: 20px — login title and KPI value.
- `page-title`: 22px — one page heading per authenticated route.

`npm run lint` also executes `scripts/check-ui-typography.mjs`, which rejects fractional pixel font sizes, 700+ font weights and forced uppercase in authenticated app styles. Authenticated app CSS also routes icon sizes through `--app-icon-*`; raw pixel `font-size` declarations are rejected so typography cannot drift back page-by-page.
