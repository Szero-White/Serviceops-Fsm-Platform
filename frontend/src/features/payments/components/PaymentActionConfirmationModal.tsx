import { Alert, Button, Checkbox, Descriptions, Modal, Space, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { formatCurrency } from '../../../utils/format'

export function PaymentActionConfirmationModal({
  open,
  title,
  description,
  workOrderCode,
  customerName,
  amount,
  confirmationLabel,
  confirmText,
  loading,
  onCancel,
  onConfirm,
}: {
  open: boolean
  title: string
  description: string
  workOrderCode: string
  customerName?: string
  amount: number
  confirmationLabel: string
  confirmText: string
  loading?: boolean
  onCancel: () => void
  onConfirm: () => void
}) {
  const [confirmed, setConfirmed] = useState(false)

  useEffect(() => {
    if (open) setConfirmed(false)
  }, [open, title])

  return (
    <Modal
      open={open}
      title={title}
      onCancel={onCancel}
      width={560}
      destroyOnHidden
      maskClosable={!loading}
      closable={!loading}
      footer={(
        <Space>
          <Button onClick={onCancel} disabled={loading}>Quay lại</Button>
          <Button type="primary" disabled={!confirmed} loading={loading} onClick={onConfirm}>
            {confirmText}
          </Button>
        </Space>
      )}
    >
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        <Alert type="info" showIcon message={description} />
        <Descriptions bordered size="small" column={1}>
          <Descriptions.Item label="Phiếu công việc">{workOrderCode}</Descriptions.Item>
          <Descriptions.Item label="Khách hàng">{customerName || 'Chưa xác định'}</Descriptions.Item>
          <Descriptions.Item label="Số tiền">
            <Typography.Text strong>{formatCurrency(amount)}</Typography.Text>
          </Descriptions.Item>
        </Descriptions>
        <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)}>
          {confirmationLabel}
        </Checkbox>
      </Space>
    </Modal>
  )
}
