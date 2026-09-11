import { Form, Input, InputNumber, Modal, Space, Typography } from 'antd'
import { useEffect } from 'react'
import type { ReturnablePart } from '../../../types'
import { formatCompactDecimalInput, formatQuantityWithUnit } from '../../../utils/format'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

export type ReturnPartValues = {
  quantity: number
  note: string
}

type ReturnPartModalProps = {
  returnable?: ReturnablePart
  pending: boolean
  onClose: () => void
  onSubmit: (values: ReturnPartValues) => void
}

export function ReturnPartModal({ returnable, pending, onClose, onSubmit }: ReturnPartModalProps) {
  const [form] = Form.useForm<ReturnPartValues>()
  const handleFormValidationFailed = useFormValidationFeedback()

  useEffect(() => {
    if (!returnable) {
      form.resetFields()
      return
    }
    form.setFieldsValue({
      quantity: Number(returnable.returnableQuantity),
      note: 'Hoàn trả phụ tùng chưa sử dụng',
    })
  }, [form, returnable])

  const maxReturnable = Number(returnable?.returnableQuantity ?? 0)

  return (
    <Modal
      title={`Xác nhận nhận hoàn trả · ${returnable?.sparePartSku ?? ''}`}
      open={Boolean(returnable)}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText="Xác nhận đã nhận hàng"
      confirmLoading={pending}
      destroyOnHidden
    >
      {returnable ? (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text>
            Phiếu <Typography.Text code>{returnable.workOrderCode}</Typography.Text> · {returnable.sparePartName}
          </Typography.Text>
          <Typography.Text type="secondary">
            Chỉ xác nhận sau khi kho đã nhận thực tế phụ tùng từ kỹ thuật viên. Hệ thống sẽ tăng tồn kho và ghi một giao dịch RETURN để truy vết.
          </Typography.Text>
          <Typography.Text>
            Có thể hoàn tối đa <strong>{formatQuantityWithUnit(returnable.returnableQuantity, returnable.unit)}</strong>.
          </Typography.Text>

          <Form
            form={form}
            layout="vertical"
            onFinish={(values) => onSubmit({ ...values, note: values.note.trim() })}
            onFinishFailed={handleFormValidationFailed}
            scrollToFirstError
            requiredMark
          >
            <Form.Item
              label="Số lượng thực nhận"
              name="quantity"
              rules={[
                { required: true, message: 'Nhập số lượng kho thực nhận' },
                {
                  validator: async (_, value) => {
                    if (value == null) return
                    if (Number(value) <= 0) throw new Error('Số lượng hoàn trả phải lớn hơn 0')
                    if (Number(value) > maxReturnable) {
                      throw new Error(`Không được vượt quá ${formatQuantityWithUnit(maxReturnable, returnable.unit)}`)
                    }
                  },
                },
              ]}
            >
              <InputNumber
                min={0.001}
                max={maxReturnable}
                precision={3}
                formatter={formatCompactDecimalInput}
                addonAfter={returnable.unit}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item
              label="Lý do / ghi chú"
              name="note"
              rules={[
                { required: true, whitespace: true, message: 'Nhập lý do hoàn trả' },
                { max: 300, message: 'Ghi chú tối đa 300 ký tự' },
              ]}
            >
              <Input placeholder="Ví dụ: Kỹ thuật viên không sử dụng hết và đã bàn giao lại cho kho" maxLength={300} />
            </Form.Item>
          </Form>
        </Space>
      ) : null}
    </Modal>
  )
}
