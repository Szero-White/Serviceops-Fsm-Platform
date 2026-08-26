import { DatePicker, Form, Input, Modal, Select, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import type { FormInstance } from 'antd'
import type { Technician } from '../../../types'

import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
const { RangePicker } = DatePicker

export type ScheduleAppointmentValues = {
  technicianId: string
  period: [Dayjs, Dayjs]
  reason?: string
}

export function ScheduleAppointmentModal({
  open,
  workOrderCode,
  workOrderSummary,
  form,
  technicians,
  pending,
  redispatching = false,
  onClose,
  onSubmit,
}: {
  open: boolean
  workOrderCode?: string
  workOrderSummary?: string
  form: FormInstance<ScheduleAppointmentValues>
  technicians?: Technician[]
  pending: boolean
  redispatching?: boolean
  onClose: () => void
  onSubmit: (values: ScheduleAppointmentValues) => void
}) {
  const handleFormValidationFailed = useFormValidationFeedback()
  return (
    <Modal
      title={workOrderCode ? `${redispatching ? 'Điều phối lại' : 'Xếp lịch'} ${workOrderCode}` : 'Xếp lịch công việc'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={pending}
      okText={redispatching ? 'Lưu điều phối mới' : 'Lưu lịch'}
      width={620}
      destroyOnHidden
    >
      {workOrderSummary ? (
        <Typography.Paragraph type="secondary" className="schedule-modal-summary">
          {workOrderSummary}
        </Typography.Paragraph>
      ) : null}
      {redispatching ? (
        <Typography.Paragraph type="secondary">
          Thay đổi kỹ thuật viên, thời gian hoặc cả hai sẽ được ghi vào Tiến trình của phiếu.
          Kỹ thuật viên liên quan sẽ nhận thông báo phù hợp.
        </Typography.Paragraph>
      ) : null}
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
        <Form.Item label="Kỹ thuật viên" name="technicianId" rules={[{ required: true, message: 'Chọn kỹ thuật viên' }]}>
          <Select
            showSearch
            optionFilterProp="label"
            placeholder="Chọn kỹ thuật viên"
            options={technicians?.map((technician) => ({
              value: technician.id,
              label: technician.skills ? `${technician.name} · ${technician.skills}` : technician.name,
            }))}
          />
        </Form.Item>
        <Form.Item label="Thời gian thực hiện" name="period" rules={[{ required: true, message: 'Chọn thời gian bắt đầu và kết thúc' }]}>
          <RangePicker
            showTime={{ format: 'HH:mm' }}
            format="DD/MM/YYYY HH:mm"
            className="full-width"
            disabledDate={(current) => current && current.endOf('day').isBefore(dayjs())}
          />
        </Form.Item>
        {redispatching ? (
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
              placeholder="Ví dụ: Khách đổi giờ hẹn hoặc kỹ thuật viên hiện tại không thể tiếp tục lịch này."
            />
          </Form.Item>
        ) : null}
      </Form>
    </Modal>
  )
}
