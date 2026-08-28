import { expect, test } from '@playwright/test'
import { apiJson, login, watchRuntime } from './support/serviceops'

type WorkOrderResponse = {
  id: string
  code: string
  status: string
}

type TechnicianResponse = {
  id: string
  name: string
  username: string
}

type SparePartResponse = {
  id: string
  sku: string
  stockQuantity: number
}

type PageResponse<T> = {
  content: T[]
}

type PartRequestResponse = {
  id: string
  status: string
  requestedQuantity: number
  issuedQuantity: number | null
}

type PartUsageResponse = {
  usedQuantity: number
  outstandingQuantity: number
}

type BillingResponse = {
  frozen: boolean
  partsTotal: number
  totalAmount: number
}

type PaymentResponse = {
  id: string
  workOrderCode: string
  status: string
  method: string | null
  amount: number
}

type ReturnablePartResponse = {
  returnableQuantity: number
}

type InventoryTransactionResponse = {
  type: string
  workOrderCode?: string
  recipientDisplayName?: string
  actorDisplayName?: string
}

type TimelineItem = {
  type: string
  status: string | null
}

function expectStatus(actual: number, expected: number, context: string) {
  expect(actual, context).toBe(expected)
}

test('field-service journey keeps parts, billing, payment, receipt, closure and post-closed return consistent', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  const suffix = `${Date.now()}`.slice(-8)
  const sku = `E2E-FLOW-${suffix}`
  const workTitle = `Luồng dịch vụ E2E ${suffix}`

  await login(page, 'warehouse')
  const part = await apiJson<SparePartResponse>(page, 'POST', '/spare-parts', {
    sku,
    name: `Van kiểm thử ${suffix}`,
    unit: 'cái',
    initialStock: 5,
    reorderLevel: 1,
    unitPrice: 100000,
    active: true,
  })
  expectStatus(part.status, 200, 'Warehouse tạo phụ tùng cho isolated E2E')
  expect(part.body.stockQuantity).toBe(5)

  await login(page, 'customer-service')
  const customer = await apiJson<{ id: string }>(page, 'POST', '/customers', {
    code: `E2E-FLOW-${suffix}`,
    name: `Khách workflow E2E ${suffix}`,
    phone: '0909888666',
    active: true,
  })
  expectStatus(customer.status, 200, 'CSKH tạo khách hàng')

  const serviceRequest = await apiJson<{ id: string }>(page, 'POST', '/service-requests', {
    customerId: customer.body.id,
    title: workTitle,
    description: 'Kiểm thử end-to-end workflow parts, billing, payment, receipt và closure.',
    priority: 'NORMAL',
    channel: 'PHONE',
  })
  expectStatus(serviceRequest.status, 200, 'CSKH tiếp nhận yêu cầu dịch vụ')

  const converted = await apiJson<WorkOrderResponse>(
    page,
    'POST',
    `/work-orders/from-service-request/${serviceRequest.body.id}`,
  )
  expectStatus(converted.status, 200, 'CSKH chuyển SR sang Work Order')
  expect(converted.body.status).toBe('OPEN')
  const workOrderId = converted.body.id
  const workOrderCode = converted.body.code

  await login(page, 'dispatcher')
  const technicians = await apiJson<TechnicianResponse[]>(page, 'GET', '/technicians?activeOnly=true')
  expectStatus(technicians.status, 200, 'Dispatcher đọc danh sách kỹ thuật viên')
  const technician = technicians.body.find((item) => item.username === 'technician')
  expect(technician, 'Demo technician phải tồn tại để chạy E2E local').toBeTruthy()

  let scheduled: WorkOrderResponse | null = null
  for (let attempt = 0; attempt < 8 && !scheduled; attempt += 1) {
    const start = new Date(Date.now() + (45 + attempt) * 24 * 60 * 60 * 1000)
    const end = new Date(start.getTime() + 90 * 60 * 1000)
    const candidate = await apiJson<WorkOrderResponse>(page, 'POST', `/work-orders/${workOrderId}/schedule`, {
      technicianId: technician!.id,
      startTime: start.toISOString(),
      endTime: end.toISOString(),
    })
    if (candidate.status === 200) scheduled = candidate.body
  }
  expect(scheduled, 'Phải tìm được một lịch tương lai không trùng để chạy workflow').not.toBeNull()
  expect(scheduled!.status).toBe('ASSIGNED')

  await login(page, 'technician')
  for (const targetStatus of ['ON_THE_WAY', 'IN_PROGRESS']) {
    const transition = await apiJson<WorkOrderResponse>(page, 'POST', `/work-orders/${workOrderId}/transition`, {
      targetStatus,
      note: `E2E ${targetStatus}`,
    })
    expectStatus(transition.status, 200, `Technician chuyển ${targetStatus}`)
    expect(transition.body.status).toBe(targetStatus)
  }

  const requested = await apiJson<PartRequestResponse>(page, 'POST', `/work-orders/${workOrderId}/part-requests`, {
    sparePartId: part.body.id,
    quantity: 3,
    note: 'Cần 3 cái để thay thế, dự kiến dùng 2 cái.',
  })
  expectStatus(requested.status, 200, 'Technician tạo REQUEST không làm giảm tồn')
  expect(requested.body.status).toBe('REQUESTED')
  expect(requested.body.requestedQuantity).toBe(3)

  const stockAfterRequest = await apiJson<PageResponse<SparePartResponse>>(
    page,
    'GET',
    `/spare-parts?search=${encodeURIComponent(sku)}&active=true&page=0&size=20`,
  )
  expectStatus(stockAfterRequest.status, 200, 'Technician vẫn xem được catalog phụ tùng')
  expect(stockAfterRequest.body.content[0]?.stockQuantity).toBe(5)

  await login(page, 'warehouse')
  const pendingQueue = await apiJson<PageResponse<PartRequestResponse>>(
    page,
    'GET',
    `/part-requests?status=REQUESTED&search=${encodeURIComponent(sku)}&page=0&size=20`,
  )
  expectStatus(pendingQueue.status, 200, 'Warehouse đọc hàng đợi Yêu cầu phụ tùng')
  expect(pendingQueue.body.content.some((item) => item.id === requested.body.id)).toBe(true)

  const issued = await apiJson<PartRequestResponse>(page, 'POST', `/part-requests/${requested.body.id}/issue`)
  expectStatus(issued.status, 200, 'Warehouse ISSUE đúng requested quantity')
  expect(issued.body.status).toBe('ISSUED')
  expect(issued.body.issuedQuantity).toBe(3)

  const duplicateIssue = await apiJson(page, 'POST', `/part-requests/${requested.body.id}/issue`)
  expectStatus(duplicateIssue.status, 409, 'Retry ISSUE không được double-decrement')

  const issueLedger = await apiJson<PageResponse<InventoryTransactionResponse>>(
    page,
    'GET',
    `/inventory-transactions?search=${encodeURIComponent(workOrderCode)}&type=ISSUE&page=0&size=20`,
  )
  expectStatus(issueLedger.status, 200, 'Warehouse đọc ISSUE ledger có snapshot người nhận')
  const issueMovement = issueLedger.body.content.find((item) => item.workOrderCode === workOrderCode && item.type === 'ISSUE')
  expect(issueMovement, 'ISSUE vừa cấp phải xuất hiện trong inventory ledger').toBeTruthy()
  expect(issueMovement!.recipientDisplayName).toBe(technician!.name)
  expect(issueMovement!.actorDisplayName).toBeTruthy()
  expect(issueMovement!.actorDisplayName).not.toBe(issueMovement!.recipientDisplayName)

  const stockAfterIssue = await apiJson<PageResponse<SparePartResponse>>(
    page,
    'GET',
    `/spare-parts?search=${encodeURIComponent(sku)}&active=true&page=0&size=20`,
  )
  expect(stockAfterIssue.body.content[0]?.stockQuantity).toBe(2)

  await login(page, 'technician')
  const usage = await apiJson<PartUsageResponse>(page, 'PUT', `/work-orders/${workOrderId}/part-usage`, {
    sparePartId: part.body.id,
    usedQuantity: 2,
  })
  expectStatus(usage.status, 200, 'Technician ghi actual USED')
  expect(usage.body.usedQuantity).toBe(2)
  expect(usage.body.outstandingQuantity).toBe(1)

  const completed = await apiJson<WorkOrderResponse>(page, 'POST', `/work-orders/${workOrderId}/transition`, {
    targetStatus: 'COMPLETED',
    note: 'Đã chạy thử ổn định.',
    diagnosis: 'Van cũ hoạt động không ổn định.',
    resolution: 'Thay 2 van kiểm thử và xác nhận hệ thống hoạt động bình thường.',
  })
  expectStatus(completed.status, 200, 'Technician hoàn thành với diagnosis/resolution')
  expect(completed.body.status).toBe('COMPLETED')

  const billingDraft = await apiJson<BillingResponse>(page, 'PUT', `/work-orders/${workOrderId}/billing`, {
    laborFee: 150000,
    incidentalFee: 50000,
    incidentalReason: 'Vật tư phụ phát sinh tại hiện trường',
  })
  expectStatus(billingDraft.status, 200, 'Technician cập nhật billing trước customer acceptance')
  expect(billingDraft.body.frozen).toBe(false)
  expect(billingDraft.body.partsTotal).toBe(200000)
  expect(billingDraft.body.totalAmount).toBe(400000)

  const accepted = await apiJson<WorkOrderResponse>(page, 'POST', `/work-orders/${workOrderId}/customer-acceptance`, {
    note: 'Khách đã kiểm tra kết quả và xác nhận chi phí.',
  })
  expectStatus(accepted.status, 200, 'Technician ghi nhận khách xác nhận')
  expect(accepted.body.status).toBe('CUSTOMER_ACCEPTED')

  const frozenBilling = await apiJson<BillingResponse>(page, 'GET', `/work-orders/${workOrderId}/billing`)
  expectStatus(frozenBilling.status, 200, 'Billing snapshot đọc được sau acceptance')
  expect(frozenBilling.body.frozen).toBe(true)
  expect(frozenBilling.body.totalAmount).toBe(400000)

  const forbiddenUsageEdit = await apiJson(page, 'PUT', `/work-orders/${workOrderId}/part-usage`, {
    sparePartId: part.body.id,
    usedQuantity: 1,
  })
  expectStatus(forbiddenUsageEdit.status, 409, 'USED phải khóa sau CUSTOMER_ACCEPTED')

  await login(page, 'customer-service')
  const prematureClose = await apiJson(page, 'POST', `/work-orders/${workOrderId}/close`)
  expectStatus(prematureClose.status, 409, 'Không được CLOSED trước khi payment SETTLED')

  await login(page, 'technician')
  const cash = await apiJson<PaymentResponse>(page, 'POST', `/work-orders/${workOrderId}/payment/collect-cash`)
  expectStatus(cash.status, 200, 'Technician ghi nhận đang giữ tiền mặt')
  expect(cash.body.status).toBe('CASH_PENDING_HANDOVER')
  expect(cash.body.method).toBe('CASH')
  expect(cash.body.amount).toBe(400000)

  await login(page, 'customer-service')
  const paymentQueue = await apiJson<PageResponse<PaymentResponse>>(
    page,
    'GET',
    `/payments?status=CASH_PENDING_HANDOVER&search=${encodeURIComponent(workOrderCode)}&page=0&size=20`,
  )
  expectStatus(paymentQueue.status, 200, 'CSKH thấy payment trong reconciliation queue')
  expect(paymentQueue.body.content.some((item) => item.id === cash.body.id)).toBe(true)

  await page.goto('/payments')
  await page.getByPlaceholder('Tìm mã phiếu, khách hàng hoặc kỹ thuật viên').fill(workOrderCode)
  const paymentRow = page.locator('tr').filter({ hasText: workOrderCode }).last()
  await expect(paymentRow).toContainText('KTV đang giữ tiền mặt')
  await paymentRow.getByRole('button', { name: 'Đối soát thanh toán' }).click()
  await expect(page).toHaveURL(new RegExp(`/work-orders\\?open=${workOrderId}&tab=payment&from=payments`))
  await expect(page.getByRole('tab', { name: 'Thanh toán' })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByRole('heading', { name: 'Đối soát thanh toán' })).toBeVisible()

  await page.getByRole('button', { name: 'Xem chi phí đã xác nhận' }).click()
  await expect(page.getByRole('tab', { name: 'Chi phí' })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByRole('heading', { name: 'Chi phí khách xác nhận' })).toBeVisible()
  await page.getByRole('tab', { name: 'Thanh toán' }).click()

  await page.getByRole('button', { name: 'Xác nhận đã nhận bàn giao tiền' }).click()
  await page.getByRole('button', { name: 'Xác nhận đã đối soát' }).click()
  await expect(page.getByText('Đã đối soát', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Phát hành / tải biên nhận' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Đóng phiếu' })).toBeVisible()

  const receiptDownload = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Phát hành / tải biên nhận' }).click()
  await receiptDownload

  await page.getByRole('button', { name: 'Đóng phiếu' }).click()
  await page.getByRole('button', { name: 'Đóng phiếu', exact: true }).last().click()
  await expect(page).toHaveURL(/\/payments$/)
  await page.getByPlaceholder('Tìm mã phiếu, khách hàng hoặc kỹ thuật viên').fill(workOrderCode)
  const closedPaymentRow = page.locator('tr').filter({ hasText: workOrderCode }).last()
  await expect(closedPaymentRow).toContainText('Đã đóng phiếu')
  await expect(closedPaymentRow.getByRole('button', { name: 'Tải biên nhận' })).toBeVisible()

  await login(page, 'technician')
  const technicianReceipt = await apiJson(page, 'GET', `/work-orders/${workOrderId}/receipt`)
  expectStatus(technicianReceipt.status, 403, 'Technician không được phát hành/tải official receipt')

  await login(page, 'warehouse')
  const returnable = await apiJson<ReturnablePartResponse>(
    page,
    'GET',
    `/work-orders/${workOrderId}/parts/${part.body.id}/returnable`,
  )
  expectStatus(returnable.status, 200, 'Warehouse xem outstanding sau CLOSED')
  expect(returnable.body.returnableQuantity).toBe(1)

  const returned = await apiJson<ReturnablePartResponse>(
    page,
    'POST',
    `/work-orders/${workOrderId}/parts/${part.body.id}/return`,
    { quantity: 1, note: 'Hoàn phần phụ tùng không sử dụng sau khi kết thúc job.' },
  )
  expectStatus(returned.status, 200, 'Warehouse RETURN outstanding sau CLOSED')
  expect(returned.body.returnableQuantity).toBe(0)

  const stockAfterReturn = await apiJson<PageResponse<SparePartResponse>>(
    page,
    'GET',
    `/spare-parts?search=${encodeURIComponent(sku)}&active=true&page=0&size=20`,
  )
  expect(stockAfterReturn.body.content[0]?.stockQuantity).toBe(3)

  await login(page, 'customer-service')
  const finalWorkOrder = await apiJson<WorkOrderResponse>(page, 'GET', `/work-orders/${workOrderId}`)
  expectStatus(finalWorkOrder.status, 200, 'Post-CLOSED RETURN không reopen Work Order')
  expect(finalWorkOrder.body.status).toBe('CLOSED')

  const timeline = await apiJson<TimelineItem[]>(page, 'GET', `/work-orders/${workOrderId}/timeline`)
  expectStatus(timeline.status, 200, 'Timeline đọc được business story cuối cùng')
  const eventTypes = new Set(timeline.body.map((item) => item.type))
  for (const requiredType of [
    'PART_REQUESTED',
    'PART_ISSUED',
    'PART_USED',
    'PAYMENT_REPORTED',
    'PAYMENT_SETTLED',
    'RECEIPT_ISSUED',
    'PART_RETURNED',
  ]) {
    expect(eventTypes.has(requiredType), `Timeline phải có ${requiredType}`).toBe(true)
  }
  const statuses = new Set(timeline.body.map((item) => item.status).filter(Boolean))
  expect(statuses.has('COMPLETED')).toBe(true)
  expect(statuses.has('CUSTOMER_ACCEPTED')).toBe(true)
  expect(statuses.has('CLOSED')).toBe(true)

  assertRuntimeClean()
})
