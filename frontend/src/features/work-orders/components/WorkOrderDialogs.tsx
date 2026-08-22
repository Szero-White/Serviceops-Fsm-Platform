import { DatePicker, Form, Input, InputNumber, Modal, Select, Typography } from 'antd'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { FormInstance } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import type { Asset, Customer, PageResponse, SparePart, Technician } from '../../../types'
import { formatCompactDecimalInput, formatCurrency, formatQuantityWithUnit } from '../../../utils/format'
import { PRIORITY_OPTIONS } from '../model/workOrderPresentation'

const { RangePicker } = DatePicker

export type CreateWorkOrderValues = {
  customerId: string
  assetId?: string
  summary: string
  description?: string
  priority: string
}

export type ScheduleWorkOrderValues = {
  technicianId: string
  period: [Dayjs, Dayjs]
}

export type CompleteWorkOrderValues = {
  diagnosis: string
  resolution: string
  note?: string
}

export type ConsumePartValues = {
  sparePartId: string
  quantity: number
  note?: string
}

export function WorkOrderDialogs({
  create,
  schedule,
  complete,
  consume,
  customers,
  assets,
  assetsLoading,
  technicians,
  parts,
}: {
  create: { open: boolean; form: FormInstance<CreateWorkOrderValues>; pending: boolean; onClose: () => void; onSubmit: (values: CreateWorkOrderValues) => void }
  schedule: { open: boolean; form: FormInstance<ScheduleWorkOrderValues>; pending: boolean; onClose: () => void; onSubmit: (values: ScheduleWorkOrderValues) => void }
  complete: { open: boolean; form: FormInstance<CompleteWorkOrderValues>; pending: boolean; onClose: () => void; onSubmit: (values: CompleteWorkOrderValues) => void }
  consume: { open: boolean; form: FormInstance<ConsumePartValues>; pending: boolean; onClose: () => void; onSubmit: (values: ConsumePartValues) => void }
  customers?: PageResponse<Customer>
  assets?: PageResponse<Asset>
  assetsLoading?: boolean
  technicians?: Technician[]
  parts?: PageResponse<SparePart>
}) {
  const selectedCustomerId = Form.useWatch('customerId', create.form)
  const selectedPartId = Form.useWatch('sparePartId', consume.form)
  const selectedPart = parts?.content.find((item) => item.id === selectedPartId)

  return (
    <>
      <Modal title="Tạo phiếu công việc" open={create.open} onCancel={create.onClose} onOk={() => create.form.submit()} confirmLoading={create.pending} width={760} destroyOnHidden>
        <Form form={create.form} layout="vertical" onFinish={create.onSubmit} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="Chọn khách hàng"
                options={customers?.content.map((customer) => ({ value: customer.id, label: `${customer.code} · ${customer.name}` }))}
                onChange={(customerId) => create.form.setFieldsValue({ customerId, assetId: undefined })}
              />
            </Form.Item>
            <Form.Item label="Thiết bị (không bắt buộc)" name="assetId">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                disabled={!selectedCustomerId}
                loading={assetsLoading}
                placeholder={selectedCustomerId ? 'Chọn thiết bị của khách hàng' : 'Chọn khách hàng trước'}
                notFoundContent={selectedCustomerId && !assetsLoading ? 'Khách hàng này chưa có thiết bị' : undefined}
                options={assets?.content.map((asset) => ({
                  value: asset.id,
                  label: `${asset.serialNumber ?? 'Chưa xác định serial'} · ${[asset.brand, asset.model].filter(Boolean).join(' ') || asset.category}`,
                }))}
              />
            </Form.Item>
          </div>
          <Form.Item label="Nội dung công việc" name="summary" rules={[{ required: true, message: 'Nhập nội dung công việc' }]}><Input /></Form.Item>
          <Form.Item label="Mô tả" name="description"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={PRIORITY_OPTIONS} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Phân công và xếp lịch" open={schedule.open} onCancel={schedule.onClose} onOk={() => schedule.form.submit()} confirmLoading={schedule.pending} width={620} destroyOnHidden>
        <Form form={schedule.form} layout="vertical" onFinish={schedule.onSubmit} requiredMark={false}>
          <Form.Item label="Kỹ thuật viên" name="technicianId" rules={[{ required: true, message: 'Chọn kỹ thuật viên' }]}>
            <Select showSearch optionFilterProp="label" options={technicians?.map((technician) => ({ value: technician.id, label: `${technician.name} · ${technician.skills ?? ''}` }))} />
          </Form.Item>
          <Form.Item label="Thời gian thực hiện" name="period" rules={[{ required: true, message: 'Chọn thời gian thực hiện' }]}>
            <RangePicker showTime format="DD/MM/YYYY HH:mm" style={{ width: '100%' }} disabledDate={(date) => date.isBefore(dayjs().startOf('day'))} />
          </Form.Item>
          <Typography.Text type="secondary">Hệ thống sẽ cảnh báo nếu kỹ thuật viên đã có lịch trùng với khoảng thời gian này.</Typography.Text>
        </Form>
      </Modal>

      <Modal title="Hoàn thành công việc" open={complete.open} onCancel={complete.onClose} onOk={() => complete.form.submit()} confirmLoading={complete.pending} width={680} destroyOnHidden>
        <Form form={complete.form} layout="vertical" onFinish={complete.onSubmit} requiredMark={false}>
          <Form.Item label="Chẩn đoán / nguyên nhân" name="diagnosis" rules={[{ required: true, message: 'Nhập chẩn đoán' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Giải pháp đã thực hiện" name="resolution" rules={[{ required: true, message: 'Nhập giải pháp' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Ghi chú bàn giao" name="note"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Ghi nhận phụ tùng sử dụng" open={consume.open} onCancel={consume.onClose} onOk={() => consume.form.submit()} confirmLoading={consume.pending} width={620} destroyOnHidden>
        <Form form={consume.form} layout="vertical" onFinish={consume.onSubmit} requiredMark={false}>
          <Form.Item label="Phụ tùng" name="sparePartId" rules={[{ required: true, message: 'Chọn phụ tùng' }]}>
            <Select showSearch optionFilterProp="label" options={parts?.content.filter((part) => part.active).map((part) => ({ value: part.id, label: `${part.sku} · ${part.name} · Tồn ${formatQuantityWithUnit(part.stockQuantity, part.unit)}` }))} />
          </Form.Item>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Ghi chú" name="note"><Input placeholder="Ví dụ: Thay tụ máy nén" /></Form.Item>
          {selectedPart ? <MetaBadge>Đơn giá tham khảo: {formatCurrency(selectedPart.unitPrice)}</MetaBadge> : null}
        </Form>
      </Modal>
    </>
  )
}
