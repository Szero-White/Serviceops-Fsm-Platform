import { expect, test } from '@playwright/test'
import { apiJson, login, modalByTitle, submitModal, watchRuntime } from './support/serviceops'

test('Customer Service can receive a service request and convert it to a work order', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'customer-service')

  const suffix = Date.now().toString().slice(-8)
  const customerCode = `E2E-SR-${suffix}`
  const title = `Yêu cầu E2E ${suffix}`

  const customer = await apiJson<{ id: string; code: string; name: string }>(page, 'POST', '/customers', {
    code: customerCode,
    name: `Khách SR E2E ${suffix}`,
    phone: '0909888777',
    active: true,
  })
  expect(customer.status).toBe(200)

  await page.goto('/service-requests')
  await page.getByRole('button', { name: /Tiếp nhận yêu cầu/ }).click()
  const modal = modalByTitle(page, 'Tiếp nhận yêu cầu dịch vụ')

  await modal.getByLabel('Khách hàng').click()
  await page.getByRole('option', { name: new RegExp(customerCode) }).click()
  await modal.getByLabel('Tiêu đề').fill(title)
  await modal.getByLabel('Mô tả chi tiết').fill('Kiểm thử luồng tiếp nhận và chuyển đổi bằng trình duyệt thật.')
  await submitModal(page, 'Tiếp nhận yêu cầu dịch vụ')
  await expect(page.getByText('Đã tiếp nhận yêu cầu dịch vụ').last()).toBeVisible()

  await page.getByPlaceholder('Tìm tiêu đề, mô tả, khách hàng hoặc serial').fill(title)
  const requestRow = page.locator('tbody tr').filter({ hasText: title })
  await expect(requestRow).toBeVisible()
  await requestRow.getByRole('button', { name: 'Tạo phiếu công việc' }).click()
  await expect(page.getByText(/Đã tạo WO-/).last()).toBeVisible()

  await page.goto('/work-orders')
  await page.getByPlaceholder('Tìm mã phiếu, nội dung, khách hàng, serial hoặc kỹ thuật viên').fill(title)
  await expect(page.locator('tbody tr').filter({ hasText: title })).toBeVisible()

  assertRuntimeClean()
})

test('Technician sees only technician transitions and backend rejects management transitions', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'owner')

  const suffix = Date.now().toString().slice(-8)
  const customer = await apiJson<{ id: string }>(page, 'POST', '/customers', {
    code: `E2E-WO-${suffix}`,
    name: `Khách WO E2E ${suffix}`,
    active: true,
  })
  expect(customer.status).toBe(200)

  const workOrder = await apiJson<{ id: string; code: string }>(page, 'POST', '/work-orders', {
    customerId: customer.body.id,
    summary: `Technician policy E2E ${suffix}`,
    description: 'Browser E2E verifies UI and backend transition policy.',
    priority: 'NORMAL',
  })
  expect(workOrder.status).toBe(200)

  const technicians = await apiJson<Array<{ id: string; username: string }>>(page, 'GET', '/technicians?activeOnly=true')
  expect(technicians.status).toBe(200)
  const technician = technicians.body.find((item) => item.username === 'technician')
  expect(technician).toBeTruthy()

  const start = new Date(Date.now() + 21 * 24 * 60 * 60 * 1000)
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
  const scheduled = await apiJson(page, 'POST', `/work-orders/${workOrder.body.id}/schedule`, {
    technicianId: technician!.id,
    startTime: start.toISOString(),
    endTime: end.toISOString(),
  })
  expect(scheduled.status).toBe(200)

  await login(page, 'technician')
  await page.goto(`/work-orders?open=${workOrder.body.id}`)
  await expect(page.getByText(workOrder.body.code).last()).toBeVisible()
  await expect(page.getByRole('button', { name: 'Bắt đầu di chuyển' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Huỷ phiếu' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Khách xác nhận' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Đóng phiếu' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Mở lại' })).toHaveCount(0)

  const forbidden = await apiJson(page, 'POST', `/work-orders/${workOrder.body.id}/transition`, {
    targetStatus: 'CANCELLED',
    note: 'E2E must be forbidden for technician',
  })
  expect(forbidden.status).toBe(403)

  await page.getByRole('button', { name: 'Bắt đầu di chuyển' }).click()
  await expect(page.getByText(new RegExp(`Đã cập nhật ${workOrder.body.code}`)).last()).toBeVisible()
  await expect(page.getByRole('button', { name: 'Bắt đầu / tiếp tục' })).toBeVisible()

  assertRuntimeClean()
})
