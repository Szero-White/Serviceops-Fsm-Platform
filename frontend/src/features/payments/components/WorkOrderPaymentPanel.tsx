import { useQuery } from '@tanstack/react-query'
import { Empty, Space, Typography } from 'antd'
import type { AttachmentItem, UserRole, WorkOrder } from '../../../types'
import { paymentsApi } from '../api'
import { CustomerServiceSettlementSection } from './CustomerServiceSettlementSection'
import { PaymentCompletionSection } from './PaymentCompletionSection'
import { PaymentSummary } from './PaymentSummary'
import { TechnicianPaymentSection } from './TechnicianPaymentSection'

interface WorkOrderPaymentPanelProps {
  workOrder: WorkOrder
  role?: UserRole
  attachments?: AttachmentItem[]
  onViewBilling?: () => void
}

export function WorkOrderPaymentPanel({
  workOrder,
  role,
  attachments,
  onViewBilling,
}: WorkOrderPaymentPanelProps) {
  const paymentReady = ['CUSTOMER_ACCEPTED', 'CLOSED'].includes(workOrder.status)
  const profileQuery = useQuery({
    queryKey: ['company-payment-profile'],
    queryFn: paymentsApi.companyProfile,
    enabled: Boolean(role && ['OWNER', 'CUSTOMER_SERVICE', 'TECHNICIAN'].includes(role)),
  })
  const paymentQuery = useQuery({
    queryKey: ['work-order-payment', workOrder.id],
    queryFn: () => paymentsApi.workOrderPayment(workOrder.id),
    enabled: paymentReady && Boolean(role && ['OWNER', 'CUSTOMER_SERVICE', 'TECHNICIAN'].includes(role)),
  })

  if (!paymentReady) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Thanh toán được mở sau khi khách xác nhận kết quả và tổng chi phí." />
  }

  const payment = paymentQuery.data
  if (!payment) {
    return <Typography.Text type="secondary">Đang tải thông tin thanh toán...</Typography.Text>
  }

  const profile = profileQuery.data
  const transferEvidence = attachments?.find((item) => item.id === payment.transferEvidenceAttachmentId)

  return (
    <Space orientation="vertical" size={18} style={{ width: '100%' }}>
      <PaymentSummary payment={payment} transferEvidence={transferEvidence} />

      {role === 'TECHNICIAN' && payment.status === 'UNPAID' ? (
        <TechnicianPaymentSection workOrder={workOrder} payment={payment} profile={profile} attachments={attachments} />
      ) : null}

      {role === 'CUSTOMER_SERVICE' ? (
        <CustomerServiceSettlementSection payment={payment} profile={profile} onViewBilling={onViewBilling} />
      ) : null}

      {role && ['CUSTOMER_SERVICE', 'OWNER'].includes(role) ? (
        <PaymentCompletionSection payment={payment} workOrder={workOrder} role={role} />
      ) : null}
    </Space>
  )
}
