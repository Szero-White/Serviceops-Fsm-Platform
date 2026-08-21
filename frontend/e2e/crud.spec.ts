import { expect, test } from '@playwright/test'
import { login, modalByTitle, submitModal, watchRuntime } from './support/serviceops'

test('Owner can create, edit and delete recruiter-created customer data through the UI', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'owner')
  await page.goto('/customers')

  const suffix = Date.now().toString().slice(-8)
  const code = `E2E-C-${suffix}`
  const initialName = `Khách hàng E2E ${suffix}`
  const updatedName = `Khách hàng E2E đã sửa ${suffix}`

  await page.getByRole('button', { name: /Thêm khách hàng/ }).click()
  const createModal = modalByTitle(page, 'Thêm khách hàng')
  await createModal.getByLabel('Mã khách hàng').fill(code)
  await createModal.getByLabel('Tên khách hàng').fill(initialName)
  await createModal.getByLabel('Số điện thoại').fill('0909123456')
  await createModal.getByLabel('Email').fill(`e2e-${suffix}@example.com`)
  await submitModal(page, 'Thêm khách hàng')
  await expect(page.getByText('Đã tạo khách hàng').last()).toBeVisible()

  const search = page.getByPlaceholder('Tìm tên, mã, số điện thoại hoặc email')
  await search.fill(code)
  let row = page.locator('tbody tr').filter({ hasText: code })
  await expect(row).toContainText(initialName)

  await row.getByRole('button', { name: 'Sửa khách hàng' }).click()
  const editModal = modalByTitle(page, 'Cập nhật khách hàng')
  await editModal.getByLabel('Tên khách hàng').fill(updatedName)
  await submitModal(page, 'Cập nhật khách hàng')
  await expect(page.getByText('Đã cập nhật khách hàng').last()).toBeVisible()
  row = page.locator('tbody tr').filter({ hasText: code })
  await expect(row).toContainText(updatedName)

  await row.getByRole('button', { name: 'Xoá khách hàng' }).click()
  await page.getByRole('button', { name: 'Xoá', exact: true }).click()
  await expect(page.getByText('Đã xoá khách hàng').last()).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: code })).toHaveCount(0)

  assertRuntimeClean()
})

test('Owner can CRUD a custom service channel while public-demo seeds remain separate', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'owner')
  await page.goto('/service-channels')

  const suffix = Date.now().toString().slice(-6)
  const code = `E2E_CH_${suffix}`
  const name = `Kênh E2E ${suffix}`
  const updatedName = `Kênh E2E đã sửa ${suffix}`

  await page.getByRole('button', { name: /Thêm kênh/ }).click()
  const createModal = modalByTitle(page, 'Thêm kênh tiếp nhận')
  await createModal.getByLabel('Tên kênh').fill(name)
  await createModal.getByLabel('Mã kênh').fill(code)
  await createModal.getByLabel('Mô tả').fill('Kênh được tạo bởi browser E2E')
  await submitModal(page, 'Thêm kênh tiếp nhận')
  await expect(page.getByText('Đã tạo kênh tiếp nhận').last()).toBeVisible()

  const search = page.getByPlaceholder('Tìm theo tên, mã hoặc mô tả')
  await search.fill(code)
  let row = page.locator('tbody tr').filter({ hasText: code })
  await expect(row).toContainText(name)

  await row.getByRole('button', { name: 'Sửa kênh' }).click()
  const editModal = modalByTitle(page, 'Cập nhật kênh tiếp nhận')
  await editModal.getByLabel('Tên kênh').fill(updatedName)
  await submitModal(page, 'Cập nhật kênh tiếp nhận')
  await expect(page.getByText('Đã cập nhật kênh tiếp nhận').last()).toBeVisible()

  row = page.locator('tbody tr').filter({ hasText: code })
  await expect(row).toContainText(updatedName)
  await row.getByRole('button', { name: 'Xoá kênh' }).click()
  await page.getByRole('button', { name: 'Xoá', exact: true }).click()
  await expect(page.getByText('Đã xoá kênh tiếp nhận').last()).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: code })).toHaveCount(0)

  assertRuntimeClean()
})

test('Warehouse can create a spare part and import stock through the UI', async ({ page }) => {
  const assertRuntimeClean = watchRuntime(page)
  await login(page, 'warehouse')
  await page.goto('/inventory')

  const suffix = Date.now().toString().slice(-8)
  const sku = `E2E-${suffix}`

  await page.getByRole('button', { name: /Thêm phụ tùng/ }).click()
  const modal = modalByTitle(page, 'Thêm phụ tùng')
  await modal.getByLabel('SKU').fill(sku)
  await modal.getByLabel('Tên phụ tùng').fill(`Phụ tùng E2E ${suffix}`)
  await modal.getByLabel('Đơn vị').fill('cái')
  await modal.getByLabel('Tồn ban đầu').fill('2')
  await modal.getByLabel('Mức đặt hàng lại').fill('1')
  await modal.getByLabel('Đơn giá').fill('10000')
  await submitModal(page, 'Thêm phụ tùng')
  await expect(page.getByText('Đã tạo phụ tùng').last()).toBeVisible()

  const search = page.getByPlaceholder('Tìm SKU, tên hoặc đơn vị phụ tùng')
  await search.fill(sku)
  const row = page.locator('tbody tr').filter({ hasText: sku })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: 'Nhập kho' }).click()

  const importModal = modalByTitle(page, new RegExp(`Nhập kho.*${sku}`))
  await importModal.getByLabel('Số lượng').fill('3')
  await importModal.getByLabel('Ghi chú').fill('Browser E2E nhập bổ sung')
  await submitModal(page, new RegExp(`Nhập kho.*${sku}`))
  await expect(page.getByText(new RegExp(`Đã nhập kho.*${sku}`)).last()).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: sku })).toContainText('5')

  assertRuntimeClean()
})
