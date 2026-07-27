# Frontend Style Structure

`main.tsx` imports the app style modules directly in cascade order.

- `app/base.css`: browser reset, root text color, app background.
- `app/layout.css`: authenticated app shell, sidebar, header, user menu, notifications, route fallback.
- `app/components.css`: cascade entrypoint for shared app primitives.
- `app/components/`: focused shared modules for shell spacing, page headers, cards, controls, tables, status/cell patterns, and forms.
- `app/dashboard.css`: dashboard-specific hero, health panel, and offline state.
- `app/login.css`: unauthenticated login and demo entry screen.
- `app/responsive.css`: responsive overrides for app and login views.

Landing page styles are intentionally separate under `pages/landing/styles` because the marketing page has its own visual language and `lp-` class namespace.

Maintenance rules:

- Keep `app/components.css` as imports only; place shared UI styles in the matching file under `app/components/`.
- Keep page-specific styles in the matching page style file.
- Do not add new page-specific CSS to `main.tsx`; create or update the matching module instead.
- Remove a selector when its class is no longer referenced by a component, unless it targets Ant Design internals under a referenced wrapper class.
- Prefer a short comment only when a selector group exists to protect layout behavior that is not obvious.
