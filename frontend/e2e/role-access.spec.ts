import { expect, test } from '@playwright/test'
import { ALLOWED_ROUTES, dashboardHeading, defaultHeading, defaultRoute, login, ROUTES, SIDEBAR_NAVIGATION, type DemoUser, watchRuntime } from './support/serviceops'

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

for (const username of Object.keys(SIDEBAR_NAVIGATION) as DemoUser[]) {
  test(`${username}: sidebar follows the role workflow`, async ({ page }) => {
    const assertRuntimeClean = watchRuntime(page)
    await login(page, username)

    const menu = page.getByRole('menu', { name: 'Điều hướng chính' })
    await expect(menu).toBeVisible()

    const sectionTitles = (await menu.locator('.ant-menu-item-group-title').allTextContents()).map((value) => value.trim()).filter(Boolean)
    const itemLabels = (await menu.locator('.ant-menu-item').allTextContents()).map((value) => value.trim()).filter(Boolean)

    expect(sectionTitles).toEqual(SIDEBAR_NAVIGATION[username].sections)
    expect(itemLabels).toEqual(SIDEBAR_NAVIGATION[username].items)
    await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()

    assertRuntimeClean()
  })
}
