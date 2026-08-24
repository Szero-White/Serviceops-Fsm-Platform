import { expect, type Page } from '@playwright/test'

export const DEMO_PASSWORD = process.env.E2E_DEMO_PASSWORD?.trim()

if (!DEMO_PASSWORD) {
  throw new Error('E2E_DEMO_PASSWORD is required')
}

export type DemoUser = 'owner' | 'dispatcher' | 'customer-service' | 'technician' | 'warehouse'
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export type ApiResult<T> = {
  status: number
  body: T
}

export const ROUTES: Record<string, string> = {
  '/': 'Tổng quan vận hành',
  '/users': 'Người dùng & phân quyền',
  '/customers': 'Khách hàng',
  '/assets': 'Thiết bị khách hàng',
  '/service-requests': 'Yêu cầu dịch vụ',
  '/service-channels': 'Kênh tiếp nhận',
  '/work-orders': 'Phiếu công việc',
  '/schedule': 'Lịch điều phối',
  '/my-schedule': 'Lịch của tôi',
  '/work-order-history': 'Lịch sử phiếu công việc',
  '/technicians': 'Đội ngũ kỹ thuật',
  '/inventory': 'Kho phụ tùng',
  '/audit': 'Nhật ký hệ thống',
}

export const ALLOWED_ROUTES: Record<DemoUser, string[]> = {
  owner: ['/', '/users', '/customers', '/assets', '/service-requests', '/service-channels', '/work-orders', '/schedule', '/work-order-history', '/technicians', '/inventory', '/audit'],
  dispatcher: ['/', '/customers', '/assets', '/work-orders', '/schedule', '/work-order-history', '/technicians', '/audit'],
  'customer-service': ['/', '/customers', '/assets', '/service-requests', '/service-channels', '/work-orders', '/work-order-history'],
  technician: ['/', '/work-orders', '/my-schedule', '/work-order-history', '/inventory'],
  warehouse: ['/inventory'],
}

export function dashboardHeading(username: DemoUser) {
  return username === 'technician' ? 'Tổng quan công việc của tôi' : 'Tổng quan vận hành'
}

export function defaultRoute(username: DemoUser) {
  return username === 'warehouse' ? '/inventory' : '/'
}

export function defaultHeading(username: DemoUser) {
  return username === 'warehouse' ? ROUTES['/inventory'] : dashboardHeading(username)
}

export function watchRuntime(page: Page) {
  const failures: string[] = []
  page.on('pageerror', (error) => failures.push(`pageerror: ${error.message}`))
  page.on('response', (response) => {
    if (response.status() >= 500) {
      failures.push(`${response.status()} ${response.request().method()} ${response.url()}`)
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      console.error(`[browser console] ${message.text()}`)
    }
  })
  return () => expect(failures, 'Không được có page error hoặc HTTP 5xx').toEqual([])
}

async function resetSession(page: Page) {
  await page.goto('/landing')
  await page.evaluate(() => localStorage.clear())
}

export async function login(page: Page, username: DemoUser) {
  await resetSession(page)
  await page.goto('/login')
  await page.getByLabel('Tên đăng nhập').fill(username)
  await page.getByLabel('Mật khẩu').fill(DEMO_PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page).toHaveURL(new RegExp(`${defaultRoute(username).replace('/', '\\/')}$`))
  await expect(page.getByRole('heading', { level: 1, name: defaultHeading(username) })).toBeVisible()
}

export async function apiJson<T>(page: Page, method: HttpMethod, path: string, payload?: unknown): Promise<ApiResult<T>> {
  return page.evaluate(async ({ method, path, payload }) => {
    const token = localStorage.getItem('serviceops.accessToken')
    const response = await fetch(`/api/v1${path}`, {
      method,
      headers: {
        ...(payload === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: payload === undefined ? undefined : JSON.stringify(payload),
    })
    const text = await response.text()
    let body: unknown = null
    if (text) {
      try {
        body = JSON.parse(text)
      } catch {
        body = text
      }
    }
    return { status: response.status, body }
  }, { method, path, payload }) as Promise<ApiResult<T>>
}

export function modalByTitle(page: Page, title: string | RegExp) {
  return page.locator('.ant-modal').filter({ has: page.getByText(title) }).last()
}

export async function submitModal(page: Page, title: string | RegExp) {
  const modal = modalByTitle(page, title)
  await expect(modal).toBeVisible()
  await modal.locator('.ant-modal-footer .ant-btn-primary').click()
}
