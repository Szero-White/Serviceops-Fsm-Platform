import { DatePicker, Form, Input, InputNumber, Modal, Select, Typography } from 'antd'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { FormInstance } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import type { PageResponse, SparePart, Technician } from '../../../types'
import { formatCompactDecimalInput, formatCurrency, formatQuantityWithUnit } from '../../../utils/format'

import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
const { RangePicker } = DatePicker

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
  note: string
}

export function WorkOrderDialogs({
  schedule,
  complete,
  consume,
  technicians,
  parts,
}: {
  schedule: { open: boolean; form: FormInstance<ScheduleWorkOrderValues>; pending: boolean; onClose: () => void; onSubmit: (values: ScheduleWorkOrderValues) => void }
  complete: { open: boolean; form: FormInstance<CompleteWorkOrderValues>; pending: boolean; onClose: () => void; onSubmit: (values: CompleteWorkOrderValues) => void }
  consume: { open: boolean; form: FormInstance<ConsumePartValues>; pending: boolean; onClose: () => void; onSubmit: (values: ConsumePartValues) => void }
  technicians?: Technician[]
  parts?: PageResponse<SparePart>
}) {
  const handleFormValidationFailed = useFormValidationFeedback()
  const selectedPartId = Form.useWatch('sparePartId', consume.form)
  const selectedPart = parts?.content.find((item) => item.id === selectedPartId)

  return (
    <>
      <Modal title="Phân công và xếp lịch" open={schedule.open} onCancel={schedule.onClose} onOk={() => schedule.form.submit()} confirmLoading={schedule.pending} okText="Lưu lịch" width={620} destroyOnHidden>
        <Form form={schedule.form} layout="vertical" onFinish={schedule.onSubmit} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          <Form.Item label="Kỹ thuật viên" name="technicianId" rules={[{ required: true, message: 'Chọn kỹ thuật viên' }]}>
            <Select showSearch optionFilterProp="label" options={technicians?.map((technician) => ({ value: technician.id, label: `${technician.name} · ${technician.skills ?? ''}` }))} />
          </Form.Item>
          <Form.Item label="Thời gian thực hiện" name="period" rules={[{ required: true, message: 'Chọn thời gian thực hiện' }]}>
            <RangePicker showTime format="DD/MM/YYYY HH:mm" style={{ width: '100%' }} disabledDate={(date) => date.isBefore(dayjs().startOf('day'))} />
          </Form.Item>
          <Typography.Text type="secondary">Hệ thống sẽ cảnh báo nếu kỹ thuật viên đã có lịch trùng với khoảng thời gian này.</Typography.Text>
        </Form>
      </Modal>

      <Modal title="Hoàn thành công việc" open={complete.open} onCancel={complete.onClose} onOk={() => complete.form.submit()} confirmLoading={complete.pending} okText="Hoàn thành công việc" width={680} destroyOnHidden>
        <Form form={complete.form} layout="vertical" onFinish={complete.onSubmit} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          <Form.Item label="Chẩn đoán / nguyên nhân" name="diagnosis" rules={[{ required: true, message: 'Vui lòng nhập chẩn đoán / nguyên nhân' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Giải pháp đã thực hiện" name="resolution" rules={[{ required: true, message: 'Vui lòng nhập giải pháp đã thực hiện' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Ghi chú bàn giao" name="note"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Ghi nhận phụ tùng sử dụng" open={consume.open} onCancel={consume.onClose} onOk={() => consume.form.submit()} confirmLoading={consume.pending} okText="Ghi nhận sử dụng" width={620} destroyOnHidden>
        <Form form={consume.form} layout="vertical" onFinish={consume.onSubmit} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          <Form.Item label="Phụ tùng" name="sparePartId" rules={[{ required: true, message: 'Chọn phụ tùng' }]}>
            <Select showSearch optionFilterProp="label" options={parts?.content.filter((part) => part.active).map((part) => ({ value: part.id, label: `${part.sku} · ${part.name} · Tồn ${formatQuantityWithUnit(part.stockQuantity, part.unit)}` }))} />
          </Form.Item>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Mục đích sử dụng" name="note" rules={[{ required: true, message: 'Nhập mục đích sử dụng phụ tùng' }, { max: 300 }]}>
            <Input placeholder="Ví dụ: Thay tụ máy nén cho thiết bị của phiếu công việc" />
          </Form.Item>
          {selectedPart ? <MetaBadge>Đơn giá tham khảo: {formatCurrency(selectedPart.unitPrice)}</MetaBadge> : null}
        </Form>
      </Modal>
    </>
  )
}
