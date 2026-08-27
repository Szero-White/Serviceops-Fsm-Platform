import { Form, Input, InputNumber, Modal, Select, Space, Typography } from 'antd'
import { useEffect } from 'react'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { PageResponse, SparePart, WorkOrderPartRequest } from '../../../types'
import { formatCompactDecimalInput, formatCurrency, formatQuantityWithUnit } from '../../../utils/format'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

export type WorkOrderPartRequestValues = {
  sparePartId: string
  quantity: number
  note: string
}

export function WorkOrderPartRequestModal({
  open,
  request,
  parts,
  pending,
  onClose,
  onSubmit,
}: {
  open: boolean
  request?: WorkOrderPartRequest
  parts?: PageResponse<SparePart>
  pending: boolean
  onClose: () => void
  onSubmit: (values: WorkOrderPartRequestValues) => void
}) {
  const [form] = Form.useForm<WorkOrderPartRequestValues>()
  const handleFormValidationFailed = useFormValidationFeedback()
  const selectedPartId = Form.useWatch('sparePartId', form)
  const selectedPart = parts?.content.find((item) => item.id === selectedPartId)

  useEffect(() => {
    if (!open) return
    form.resetFields()
    form.setFieldsValue(request
      ? { sparePartId: request.sparePartId, quantity: request.requestedQuantity, note: request.note }
      : { quantity: 1, note: '' })
  }, [form, open, request])

  return (
    <Modal
      title={request ? `Sửa yêu cầu · ${request.sparePartSku}` : 'Yêu cầu phụ tùng'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={pending}
      okText={request ? 'Lưu thay đổi' : 'Gửi yêu cầu'}
      width={620}
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
        {request ? (
          <Space direction="vertical" size={4} style={{ width: '100%', marginBottom: 16 }}>
            <Typography.Text strong>{request.sparePartName}</Typography.Text>
            <Typography.Text type="secondary" code>{request.sparePartSku}</Typography.Text>
          </Space>
        ) : (
          <Form.Item label="Phụ tùng" name="sparePartId" rules={[{ required: true, message: 'Chọn phụ tùng' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Chọn phụ tùng cần cấp"
              options={parts?.content
                .filter((part) => part.active)
                .map((part) => ({
                  value: part.id,
                  label: `${part.sku} · ${part.name} · Tồn ${formatQuantityWithUnit(part.stockQuantity, part.unit)}`,
                }))}
            />
          </Form.Item>
        )}
        <Form.Item
          label="Số lượng yêu cầu"
          name="quantity"
          rules={[{ required: true, message: 'Nhập số lượng yêu cầu' }]}
        >
          <InputNumber min={0.001} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          label="Mục đích sử dụng"
          name="note"
          rules={[
            { required: true, whitespace: true, message: 'Nhập mục đích sử dụng phụ tùng' },
            { max: 300, message: 'Mục đích tối đa 300 ký tự' },
          ]}
        >
          <Input.TextArea
            rows={3}
            maxLength={300}
            showCount
            placeholder="Ví dụ: Thay van cấp nước bị hỏng sau khi kiểm tra tại hiện trường."
          />
        </Form.Item>
        {!request && selectedPart ? (
          <MetaBadge>Đơn giá tham khảo: {formatCurrency(selectedPart.unitPrice)}</MetaBadge>
        ) : null}
      </Form>
    </Modal>
  )
}
