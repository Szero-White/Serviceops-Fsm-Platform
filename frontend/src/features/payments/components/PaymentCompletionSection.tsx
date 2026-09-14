import { CheckCircleOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { App, Button, Popconfirm, Space, Typography } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import type { Payment, UserRole, WorkOrder } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { workOrdersApi } from '../../work-orders/api'
import { paymentsApi } from '../api'
import { usePaymentRefresh } from '../hooks/usePaymentRefresh'

interface PaymentCompletionSectionProps {
  payment: Payment
  workOrder: WorkOrder
  role: UserRole
}

export function PaymentCompletionSection({ payment, workOrder, role }: PaymentCompletionSectionProps) {
  const { message, notification } = App.useApp()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const refresh = usePaymentRefresh(workOrder.id)
  const returnToPayments = searchParams.get('from') === 'payments'

  const issueReceipt = useMutation({
    mutationFn: () => paymentsApi.issueReceipt(workOrder.id),
    onSuccess: (blob) => {
      downloadBlob(blob, `bien-nhan-thanh-toan-${workOrder.code}.html`)
      message.success('Đã phát hành biên nhận thanh toán')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const downloadReceipt = useMutation({
    mutationFn: () => paymentsApi.downloadReceipt(workOrder.id),
    onSuccess: (blob) => downloadBlob(blob, `bien-nhan-thanh-toan-${workOrder.code}.html`),
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const closeWorkOrder = useMutation({
    mutationFn: () => workOrdersApi.close(workOrder.id),
    onSuccess: () => {
      notification.success({ message: `Đã đóng ${workOrder.code}`, description: 'Phiếu đã hoàn tất quy trình và chuyển sang lịch sử.' })
      refresh()
      navigate(returnToPayments ? '/payments' : `/work-order-history?open=${encodeURIComponent(workOrder.id)}`)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  if (payment.status !== 'SETTLED') return null

  return (
    <div className="payment-company-card">
      <Typography.Title level={5}>Hoàn tất hồ sơ thanh toán</Typography.Title>
      <Typography.Text type="secondary">Biên nhận sử dụng chi phí khách đã xác nhận, nên số tiền không thay đổi khi kho xử lý vật tư dư sau đó.</Typography.Text>
      <div style={{ marginTop: 12 }}>
        <Space wrap>
          {role === 'CUSTOMER_SERVICE' ? (
            <Button icon={<CheckCircleOutlined />} loading={issueReceipt.isPending} onClick={() => issueReceipt.mutate()}>Phát hành / tải biên nhận</Button>
          ) : workOrder.status === 'CLOSED' ? (
            <Button loading={downloadReceipt.isPending} onClick={() => downloadReceipt.mutate()}>Tải biên nhận</Button>
          ) : (
            <Typography.Text type="secondary">Chờ chăm sóc khách hàng hoàn tất hồ sơ thanh toán</Typography.Text>
          )}
          {role === 'CUSTOMER_SERVICE' && workOrder.status === 'CUSTOMER_ACCEPTED' ? (
            <Popconfirm
              title="Đóng phiếu công việc?"
              description="Thanh toán đã được đối soát. Phiếu sẽ chuyển sang lịch sử; vật tư dư vẫn có thể được kho nhận hoàn trả sau."
              okText="Đóng phiếu"
              cancelText="Chưa"
              onConfirm={() => closeWorkOrder.mutate()}
            >
              <Button type="primary" loading={closeWorkOrder.isPending}>Đóng phiếu</Button>
            </Popconfirm>
          ) : null}
        </Space>
      </div>
    </div>
  )
}
