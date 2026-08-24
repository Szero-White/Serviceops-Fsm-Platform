import { DatePicker, Form, Modal, Select, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import type { FormInstance } from 'antd'
import type { Technician } from '../../../types'

import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
const { RangePicker } = DatePicker

export type ScheduleAppointmentValues = {
  technicianId: string
  period: [Dayjs, Dayjs]
}

export function ScheduleAppointmentModal({
  open,
  workOrderCode,
  workOrderSummary,
  form,
  technicians,
  pending,
  onClose,
  onSubmit,
}: {
  open: boolean
  workOrderCode?: string
  workOrderSummary?: string
  form: FormInstance<ScheduleAppointmentValues>
  technicians?: Technician[]
  pending: boolean
  onClose: () => void
  onSubmit: (values: ScheduleAppointmentValues) => void
}) {
  const handleFormValidationFailed = useFormValidationFeedback()
  return (
    <Modal
      title={workOrderCode ? `Xếp lịch ${workOrderCode}` : 'Xếp lịch công việc'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={pending}
      okText="Lưu lịch"
      width={620}
      destroyOnHidden
    >
      {workOrderSummary ? (
        <Typography.Paragraph type="secondary" className="schedule-modal-summary">
          {workOrderSummary}
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
      </Form>
    </Modal>
  )
}
