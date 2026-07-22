# Frontend Style Structure

`main.tsx` imports the app style modules directly in cascade order.

- `app/base.css`: browser reset, root text color, app background.
- `app/layout.css`: authenticated app shell, sidebar, header, user menu, notifications, route fallback.
- `app/components.css`: shared app primitives such as page headers, cards, metrics, tables, forms, technician cards.
- `app/dashboard.css`: dashboard-specific hero, health panel, and offline state.
- `app/login.css`: unauthenticated login and demo entry screen.
- `app/responsive.css`: responsive overrides for app and login views.

Landing page styles are intentionally separate under `pages/landing/styles` because the marketing page has its own visual language and `lp-` class namespace.

Maintenance rules:

- Keep shared UI styles in `app/components.css`; keep page-specific styles in the matching page style file.
- Do not add new page-specific CSS to `main.tsx`; create or update the matching module instead.
- Remove a selector when its class is no longer referenced by a component, unless it targets Ant Design internals under a referenced wrapper class.
- Prefer a short comment only when a selector group exists to protect layout behavior that is not obvious.
