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
  reason?: string
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
  schedule: {
    open: boolean
    form: FormInstance<ScheduleWorkOrderValues>
    pending: boolean
    redispatching: boolean
    currentTechnicianName?: string
    onClose: () => void
    onSubmit: (values: ScheduleWorkOrderValues) => void
  }
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
      <Modal
        title={schedule.redispatching ? 'Điều phối lại phiếu công việc' : 'Phân công và xếp lịch'}
        open={schedule.open}
        onCancel={schedule.onClose}
        onOk={() => schedule.form.submit()}
        confirmLoading={schedule.pending}
        okText={schedule.redispatching ? 'Lưu điều phối mới' : 'Lưu lịch'}
        width={620}
        destroyOnHidden
      >
        <Form form={schedule.form} layout="vertical" onFinish={schedule.onSubmit} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          {schedule.redispatching ? (
            <Typography.Paragraph type="secondary">
              Phiếu đang được giao{schedule.currentTechnicianName ? ` cho ${schedule.currentTechnicianName}` : ''}.
              Chỉ điều phối lại khi kỹ thuật viên chưa bắt đầu di chuyển hoặc thực hiện công việc.
              Thay đổi sẽ được ghi nhận trong Tiến trình xử lý và thông báo cho kỹ thuật viên liên quan.
            </Typography.Paragraph>
          ) : null}
          <Form.Item label="Kỹ thuật viên" name="technicianId" rules={[{ required: true, message: 'Chọn kỹ thuật viên' }]}>
            <Select showSearch optionFilterProp="label" options={technicians?.map((technician) => ({ value: technician.id, label: `${technician.name} · ${technician.skills ?? ''}` }))} />
          </Form.Item>
          <Form.Item label="Thời gian thực hiện" name="period" rules={[{ required: true, message: 'Chọn thời gian thực hiện' }]}>
            <RangePicker showTime format="DD/MM/YYYY HH:mm" style={{ width: '100%' }} disabledDate={(date) => date.isBefore(dayjs().startOf('day'))} />
          </Form.Item>
          {schedule.redispatching ? (
            <Form.Item
              label="Lý do điều phối lại"
              name="reason"
              rules={[
                { required: true, whitespace: true, message: 'Nhập lý do điều phối lại' },
                { max: 500, message: 'Lý do tối đa 500 ký tự' },
              ]}
            >
              <Input.TextArea
                rows={3}
                maxLength={500}
                showCount
                placeholder="Ví dụ: Kỹ thuật viên hiện tại chưa thể bắt đầu đúng thời gian dự kiến; cần chuyển người khác để đáp ứng khách hàng."
              />
            </Form.Item>
          ) : null}
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
