import { Form, InputNumber, Modal, Space, Typography } from 'antd'
import { useEffect } from 'react'
import type { WorkOrderPartUsage } from '../../../types'
import { formatCompactDecimalInput, formatQuantityWithUnit } from '../../../utils/format'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

export function WorkOrderPartUsageModal({
  usage,
  pending,
  onClose,
  onSubmit,
}: {
  usage?: WorkOrderPartUsage
  pending: boolean
  onClose: () => void
  onSubmit: (usedQuantity: number) => void
}) {
  const [form] = Form.useForm<{ usedQuantity: number }>()
  const handleFormValidationFailed = useFormValidationFeedback()
  const usableMaximum = usage ? Math.max(Number(usage.issuedQuantity) - Number(usage.returnedQuantity), 0) : 0

  useEffect(() => {
    if (usage) form.setFieldsValue({ usedQuantity: Number(usage.usedQuantity) })
  }, [form, usage])

  return (
    <Modal
      title={`Phụ tùng thực tế sử dụng · ${usage?.sparePartSku ?? ''}`}
      open={Boolean(usage)}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText="Lưu số lượng thực tế"
      confirmLoading={pending}
      destroyOnHidden
    >
      {usage ? (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text strong>{usage.sparePartName}</Typography.Text>
          <Typography.Text type="secondary">
            Kho đã cấp {formatQuantityWithUnit(usage.issuedQuantity, usage.unit)} · Đã hoàn trả {formatQuantityWithUnit(usage.returnedQuantity, usage.unit)}.
          </Typography.Text>
          <Form
            form={form}
            layout="vertical"
            onFinish={(values) => onSubmit(values.usedQuantity)}
            onFinishFailed={handleFormValidationFailed}
            scrollToFirstError
            requiredMark
          >
            <Form.Item
              label="Số lượng thực tế đã dùng cho khách"
              name="usedQuantity"
              rules={[{ required: true, message: 'Nhập số lượng thực tế đã sử dụng' }]}
            >
              <InputNumber
                min={0}
                max={usableMaximum}
                precision={3}
                formatter={formatCompactDecimalInput}
                addonAfter={usage.unit}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Form>
        </Space>
      ) : null}
    </Modal>
  )
}
