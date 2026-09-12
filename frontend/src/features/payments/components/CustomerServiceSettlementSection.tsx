import { BankOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { App, Button, Space, Typography } from 'antd'
import { useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import type { CompanyPaymentProfile, Payment, PaymentMethod } from '../../../types'
import { paymentsApi } from '../api'
import { usePaymentRefresh } from '../hooks/usePaymentRefresh'
import { PaymentActionConfirmationModal } from './PaymentActionConfirmationModal'

type SettlementAction = 'TRANSFER' | 'CASH_HANDOVER' | 'COUNTER_TRANSFER' | 'COUNTER_CASH'

interface CustomerServiceSettlementSectionProps {
  payment: Payment
  profile?: CompanyPaymentProfile | null
  onViewBilling?: () => void
}

export function CustomerServiceSettlementSection({ payment, profile, onViewBilling }: CustomerServiceSettlementSectionProps) {
  const { message } = App.useApp()
  const refresh = usePaymentRefresh(payment.workOrderId)
  const [action, setAction] = useState<SettlementAction>()

  const settleTransfer = useMutation({
    mutationFn: () => paymentsApi.settleTransfer(payment.id),
    onSuccess: () => {
      message.success('Đã đối soát chuyển khoản')
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const settleCash = useMutation({
    mutationFn: () => paymentsApi.settleCash(payment.id),
    onSuccess: () => {
      message.success('Đã đối soát tiền mặt')
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const settleCounter = useMutation({
    mutationFn: (method: PaymentMethod) => paymentsApi.settleCounter(payment.id, method),
    onSuccess: () => {
      message.success('Đã ghi nhận thanh toán tại quầy')
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  if (payment.status === 'TRANSFER_PENDING_VERIFICATION') {
    return (
      <>
        <div className="payment-company-card">
          <Typography.Title level={5}>Đối soát thanh toán</Typography.Title>
          <Typography.Text type="secondary">Kiểm tra chi phí khách đã xác nhận, số tiền cần thu và bằng chứng khách cung cấp trước khi xác nhận tiền thực tế đã về tài khoản công ty.</Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              <Button onClick={onViewBilling}>Xem chi phí đã xác nhận</Button>
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => setAction('TRANSFER')}>Xác nhận tiền đã về công ty</Button>
            </Space>
          </div>
        </div>
        <PaymentActionConfirmationModal
          open={action === 'TRANSFER'}
          title="Xác nhận tiền đã về công ty"
          description="Chỉ xác nhận sau khi CSKH đã kiểm tra giao dịch thực tế trên tài khoản công ty."
          workOrderCode={payment.workOrderCode}
          customerName={payment.customerName}
          amount={payment.amount}
          confirmationLabel="Tôi đã kiểm tra và xác nhận công ty thực nhận đủ số tiền chuyển khoản hiển thị."
          confirmText="Xác nhận đã đối soát"
          loading={settleTransfer.isPending}
          onCancel={() => setAction(undefined)}
          onConfirm={() => settleTransfer.mutate()}
        />
      </>
    )
  }

  if (payment.status === 'CASH_PENDING_HANDOVER') {
    return (
      <>
        <div className="payment-company-card">
          <Typography.Title level={5}>Đối soát thanh toán</Typography.Title>
          <Typography.Text type="secondary">Kiểm tra chi phí khách đã xác nhận và số tiền kỹ thuật viên đang bàn giao trước khi ghi nhận tiền đã về công ty.</Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              <Button onClick={onViewBilling}>Xem chi phí đã xác nhận</Button>
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => setAction('CASH_HANDOVER')}>Xác nhận đã nhận bàn giao tiền</Button>
            </Space>
          </div>
        </div>
        <PaymentActionConfirmationModal
          open={action === 'CASH_HANDOVER'}
          title="Xác nhận đã nhận bàn giao tiền"
          description={`Tiền đang được bàn giao từ ${payment.collectedByDisplayName ?? 'kỹ thuật viên'}.`}
          workOrderCode={payment.workOrderCode}
          customerName={payment.customerName}
          amount={payment.amount}
          confirmationLabel="Tôi đã trực tiếp nhận và kiểm đủ số tiền mặt hiển thị trước khi đối soát."
          confirmText="Xác nhận đã đối soát"
          loading={settleCash.isPending}
          onCancel={() => setAction(undefined)}
          onConfirm={() => settleCash.mutate()}
        />
      </>
    )
  }

  if (payment.status === 'COUNTER_PAYMENT_PENDING') {
    return (
      <>
        <div className="payment-company-card">
          <Typography.Title level={5}>Thu thanh toán tại quầy</Typography.Title>
          <Typography.Text type="secondary">Khách chưa thanh toán tại hiện trường và sẽ hoàn tất trực tiếp với CSKH. Chỉ ghi nhận sau khi công ty thực nhận đủ tiền.</Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              <Button onClick={onViewBilling}>Xem chi phí đã xác nhận</Button>
              <Button icon={<BankOutlined />} disabled={!profile} onClick={() => setAction('COUNTER_TRANSFER')}>Đã nhận chuyển khoản tại quầy</Button>
              <Button icon={<DollarOutlined />} onClick={() => setAction('COUNTER_CASH')}>Đã nhận tiền mặt tại quầy</Button>
            </Space>
          </div>
        </div>
        <PaymentActionConfirmationModal
          open={action === 'COUNTER_TRANSFER'}
          title="Xác nhận chuyển khoản tại quầy"
          description="Chỉ xác nhận sau khi CSKH đã kiểm tra và thấy công ty thực nhận đủ tiền chuyển khoản."
          workOrderCode={payment.workOrderCode}
          customerName={payment.customerName}
          amount={payment.amount}
          confirmationLabel="Tôi đã đối chiếu và xác nhận công ty thực nhận đủ số tiền chuyển khoản của khách."
          confirmText="Xác nhận đã nhận chuyển khoản"
          loading={settleCounter.isPending}
          onCancel={() => setAction(undefined)}
          onConfirm={() => settleCounter.mutate('BANK_TRANSFER')}
        />
        <PaymentActionConfirmationModal
          open={action === 'COUNTER_CASH'}
          title="Xác nhận tiền mặt tại quầy"
          description="Chỉ xác nhận sau khi CSKH đã trực tiếp nhận và kiểm đủ tiền mặt từ khách hàng."
          workOrderCode={payment.workOrderCode}
          customerName={payment.customerName}
          amount={payment.amount}
          confirmationLabel="Tôi đã trực tiếp nhận và kiểm đủ số tiền mặt hiển thị từ khách hàng tại quầy."
          confirmText="Xác nhận đã nhận tiền mặt"
          loading={settleCounter.isPending}
          onCancel={() => setAction(undefined)}
          onConfirm={() => settleCounter.mutate('CASH')}
        />
      </>
    )
  }

  return null
}
