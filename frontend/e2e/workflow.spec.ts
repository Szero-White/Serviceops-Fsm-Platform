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
  const customerOption = page
    .locator('.ant-select-dropdown:visible .ant-select-item-option')
    .filter({ hasText: customerCode })
    .first()
  await expect(customerOption).toBeVisible()
  await customerOption.click()
  await modal.getByLabel('Tiêu đề').fill(title)
  await modal.getByLabel('Mô tả chi tiết').fill('Kiểm thử luồng tiếp nhận và chuyển đổi bằng trình duyệt thật.')
  await submitModal(page, 'Tiếp nhận yêu cầu dịch vụ')
  await expect(page.getByText('Đã tiếp nhận yêu cầu dịch vụ').last()).toBeVisible()

  await page.getByPlaceholder('Tìm tiêu đề, mô tả, khách hàng hoặc serial').fill(title)
  const requestRow = page.locator('tbody tr').filter({ hasText: title })
  await expect(requestRow).toBeVisible()
  await requestRow.getByRole('button', { name: 'Chuyển sang điều phối' }).click()
  await page.locator('.ant-popconfirm').getByRole('button', { name: 'Chuyển sang điều phối', exact: true }).click()
  await expect(page.getByText(/Đã chuyển sang điều phối · WO-/).last()).toBeVisible()

  await page.goto('/work-orders')
  await page.getByPlaceholder('Tìm mã phiếu, nội dung, khách hàng, serial hoặc kỹ thuật viên').fill(title)
  await expect(page.locator('tbody tr').filter({ hasText: title })).toBeVisible()

  assertRuntimeClean()
})

test('Technician and Dispatcher transition boundaries are enforced by UI and backend', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'owner')

  const suffix = Date.now().toString().slice(-8)
  const customer = await apiJson<{ id: string }>(page, 'POST', '/customers', {
    code: `E2E-WO-${suffix}`,
    name: `Khách WO E2E ${suffix}`,
    active: true,
  })
  expect(customer.status).toBe(200)

  const serviceRequest = await apiJson<{ id: string }>(page, 'POST', '/service-requests', {
    customerId: customer.body.id,
    title: `Technician policy E2E ${suffix}`,
    description: 'Browser E2E verifies UI and backend transition policy.',
    priority: 'NORMAL',
    channel: 'PHONE',
  })
  expect(serviceRequest.status).toBe(200)

  const workOrder = await apiJson<{ id: string; code: string }>(
    page,
    'POST',
    `/work-orders/from-service-request/${serviceRequest.body.id}`,
  )
  expect(workOrder.status).toBe(200)

  const technicians = await apiJson<Array<{ id: string; username: string }>>(page, 'GET', '/technicians?activeOnly=true')
  expect(technicians.status).toBe(200)
  const technician = technicians.body.find((item) => item.username === 'technician')
  expect(technician).toBeTruthy()

  let scheduledStatus = 0
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const start = new Date(Date.now() + (21 + attempt) * 24 * 60 * 60 * 1000)
    const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
    const candidate = await apiJson<{ code?: string }>(page, 'POST', `/work-orders/${workOrder.body.id}/schedule`, {
      technicianId: technician!.id,
      startTime: start.toISOString(),
      endTime: end.toISOString(),
    })

    if (candidate.status === 200) {
      scheduledStatus = 200
      break
    }

    expect(candidate.status).toBe(409)
    expect(candidate.body.code).toBe('TECHNICIAN_SCHEDULE_CONFLICT')
  }
  expect(scheduledStatus).toBe(200)

  await login(page, 'dispatcher')
  const dispatcherForbidden = await apiJson(page, 'POST', `/work-orders/${workOrder.body.id}/transition`, {
    targetStatus: 'ON_THE_WAY',
    note: 'Dispatcher must not perform technician field progress',
  })
  expect(dispatcherForbidden.status).toBe(403)

  await login(page, 'technician')
  await page.goto(`/work-orders?open=${workOrder.body.id}`)
  await expect(page.getByText(workOrder.body.code).last()).toBeVisible()
  await expect(page.getByRole('button', { name: 'Bắt đầu di chuyển' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Hủy phiếu' })).toHaveCount(0)
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

test('Dispatcher can inspect customer and asset data but cannot own intake or master-data writes', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'owner')

  const suffix = Date.now().toString().slice(-8)
  const customerCode = `E2E-DISP-${suffix}`
  const serialNumber = `DISP-${suffix}`

  const customer = await apiJson<{ id: string }>(page, 'POST', '/customers', {
    code: customerCode,
    name: `Khách điều phối E2E ${suffix}`,
    phone: '0909777666',
    active: true,
  })
  expect(customer.status).toBe(200)

  const asset = await apiJson<{ id: string }>(page, 'POST', '/assets', {
    customerId: customer.body.id,
    category: 'Máy lạnh',
    serialNumber,
    brand: 'E2E',
    model: 'Dispatcher read-only',
    status: 'ACTIVE',
  })
  expect(asset.status).toBe(200)

  await login(page, 'dispatcher')

  await page.goto('/customers')
  await page.getByPlaceholder('Tìm tên, mã, số điện thoại hoặc email').fill(customerCode)
  const customerRow = page.locator('tbody tr').filter({ hasText: customerCode })
  await expect(customerRow).toBeVisible()
  await expect(page.getByRole('button', { name: /Thêm khách hàng/ })).toHaveCount(0)
  await expect(customerRow.getByRole('button', { name: 'Sửa khách hàng' })).toHaveCount(0)

  const forbiddenCustomerUpdate = await apiJson(page, 'PUT', `/customers/${customer.body.id}`, {
    code: customerCode,
    name: `Không được sửa ${suffix}`,
    phone: '0909777666',
    active: true,
  })
  expect(forbiddenCustomerUpdate.status).toBe(403)

  await page.goto('/assets')
  await page.getByPlaceholder('Tìm serial, loại, hãng, model hoặc mã/tên khách hàng').fill(serialNumber)
  const assetRow = page.locator('tbody tr').filter({ hasText: serialNumber })
  await expect(assetRow).toBeVisible()
  await expect(page.getByRole('button', { name: /Thêm thiết bị/ })).toHaveCount(0)
  await expect(assetRow.getByRole('button', { name: 'Sửa thiết bị' })).toHaveCount(0)

  const forbiddenAssetUpdate = await apiJson(page, 'PUT', `/assets/${asset.body.id}`, {
    customerId: customer.body.id,
    category: 'Máy lạnh',
    serialNumber,
    brand: 'E2E',
    model: 'Không được sửa',
    status: 'ACTIVE',
  })
  expect(forbiddenAssetUpdate.status).toBe(403)

  const forbiddenIntake = await apiJson(page, 'POST', '/service-requests', {
    customerId: customer.body.id,
    title: `Dispatcher không được tiếp nhận ${suffix}`,
    description: 'RBAC browser regression test',
    priority: 'NORMAL',
    channel: 'PHONE',
  })
  expect(forbiddenIntake.status).toBe(403)

  const removedDirectCreate = await apiJson(page, 'POST', '/work-orders', {
    customerId: customer.body.id,
    summary: `Direct work order must not exist ${suffix}`,
    priority: 'NORMAL',
  })
  expect(removedDirectCreate.status).toBe(405)

  assertRuntimeClean()
})
