import { Form, Input, Modal, Typography } from 'antd'
import type { FormInstance } from 'antd'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

export type CompleteWorkOrderValues = {
  diagnosis: string
  resolution: string
  note?: string
}

export function CompleteWorkOrderModal({
  open,
  form,
  pending,
  hasPreviousResult,
  onClose,
  onSubmit,
}: {
  open: boolean
  form: FormInstance<CompleteWorkOrderValues>
  pending: boolean
  hasPreviousResult: boolean
  onClose: () => void
  onSubmit: (values: CompleteWorkOrderValues) => void
}) {
  const handleFormValidationFailed = useFormValidationFeedback()

  return (
    <Modal
      title="Hoàn thành công việc"
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={pending}
      okText="Hoàn thành công việc"
      width={680}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        onFinishFailed={handleFormValidationFailed}
        scrollToFirstError
        requiredMark
      >
        {hasPreviousResult ? (
          <Typography.Paragraph type="secondary">
            Kết quả xử lý gần nhất đã được điền sẵn. Giữ nguyên nếu vẫn đúng hoặc cập nhật theo lần xử lý hiện tại; mỗi lần hoàn thành sẽ được lưu riêng trong Tiến trình.
          </Typography.Paragraph>
        ) : null}
        <Form.Item label="Chẩn đoán / nguyên nhân" name="diagnosis" rules={[{ required: true, message: 'Vui lòng nhập chẩn đoán / nguyên nhân' }]}>
          <Input.TextArea rows={4} />
        </Form.Item>
        <Form.Item label="Giải pháp đã thực hiện" name="resolution" rules={[{ required: true, message: 'Vui lòng nhập giải pháp đã thực hiện' }]}>
          <Input.TextArea rows={4} />
        </Form.Item>
        <Form.Item label="Ghi chú bàn giao" name="note"><Input /></Form.Item>
      </Form>
    </Modal>
  )
}
