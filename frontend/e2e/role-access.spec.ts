import { expect, test } from '@playwright/test'
import { ALLOWED_ROUTES, dashboardHeading, defaultHeading, defaultRoute, login, ROUTES, type DemoUser, watchRuntime } from './support/serviceops'

for (const username of Object.keys(ALLOWED_ROUTES) as DemoUser[]) {
  test(`${username}: route access matches the role policy`, async ({ page }) => {
    const assertRuntimeClean = watchRuntime(page)
    await login(page, username)

    const allowed = new Set(ALLOWED_ROUTES[username])
    for (const [path, heading] of Object.entries(ROUTES)) {
      await page.goto(path)
      if (allowed.has(path)) {
        const expectedHeading = path === '/' ? dashboardHeading(username) : heading
        await expect(page.getByRole('heading', { level: 1, name: expectedHeading })).toBeVisible()
      } else {
        await expect(page).toHaveURL(new RegExp(`${defaultRoute(username).replace('/', '\\/')}$`))
        await expect(page.getByRole('heading', { level: 1, name: defaultHeading(username) })).toBeVisible()
      }
    }

    assertRuntimeClean()
  })
}
