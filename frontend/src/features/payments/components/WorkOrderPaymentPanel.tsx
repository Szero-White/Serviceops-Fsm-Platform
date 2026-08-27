import { CameraOutlined, CheckCircleOutlined, DollarOutlined, PictureOutlined, SwapOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Empty, Popconfirm, Space, Typography, Upload } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { AttachmentItem, UserRole, WorkOrder } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { formatCurrency, formatDateTime } from '../../../utils/format'
import { attachmentsApi } from '../../attachments/api'
import { workOrdersApi } from '../../work-orders/api'
import { paymentsApi } from '../api'
import { PaymentQrImage } from './PaymentQrImage'

const PAYMENT_STATUS_LABELS = {
  UNPAID: 'Chưa thanh toán',
  TRANSFER_PENDING_VERIFICATION: 'Chờ xác minh chuyển khoản',
  CASH_PENDING_HANDOVER: 'KTV đang giữ tiền mặt',
  SETTLED: 'Đã đối soát',
} as const

export function WorkOrderPaymentPanel({
  workOrder,
  role,
  attachments,
  onViewBilling,
}: {
  workOrder: WorkOrder
  role?: UserRole
  attachments?: AttachmentItem[]
  onViewBilling?: () => void
}) {
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const returnToPayments = searchParams.get('from') === 'payments'
  const [evidence, setEvidence] = useState<AttachmentItem>()
  const [evidencePreview, setEvidencePreview] = useState<string>()
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
  const payment = paymentQuery.data
  const profile = profileQuery.data
  const transferEvidence = attachments?.find((item) => item.id === payment?.transferEvidenceAttachmentId)

  useEffect(() => () => {
    if (evidencePreview) URL.revokeObjectURL(evidencePreview)
  }, [evidencePreview])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['work-order-payment', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['payments'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
    queryClient.invalidateQueries({ queryKey: ['work-order', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-timeline', workOrder.id] })
  }

  const reportTransfer = useMutation({
    mutationFn: () => paymentsApi.reportTransfer(workOrder.id, evidence?.id),
    onSuccess: () => {
      notification.success({ message: 'Đã ghi nhận khách báo chuyển khoản', description: 'Khách có thể kết thúc tại đây. CSKH sẽ đối chiếu tiền thực tế vào tài khoản công ty.' })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const collectCash = useMutation({
    mutationFn: () => paymentsApi.collectCash(workOrder.id),
    onSuccess: () => {
      notification.success({ message: 'Đã ghi nhận tiền mặt', description: 'Tiền đang do kỹ thuật viên giữ và cần bàn giao cho CSKH.' })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const settleTransfer = useMutation({
    mutationFn: () => paymentsApi.settleTransfer(payment!.id),
    onSuccess: () => { message.success('Đã đối soát chuyển khoản'); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const settleCash = useMutation({
    mutationFn: () => paymentsApi.settleCash(payment!.id),
    onSuccess: () => { message.success('Đã đối soát tiền mặt'); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const downloadTransferEvidence = useMutation({
    mutationFn: () => attachmentsApi.download(payment!.transferEvidenceAttachmentId!),
    onSuccess: (blob) => downloadBlob(blob, transferEvidence?.originalFilename ?? `bang-chung-chuyen-khoan-${workOrder.code}`),
    onError: (error) => message.error(apiErrorMessage(error)),
  })
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

  const uploadEvidence = async (options: UploadRequestOption) => {
    try {
      const file = options.file as File
      const attachment = await attachmentsApi.upload('WORK_ORDER', workOrder.id, file)
      setEvidence(attachment)
      setEvidencePreview(URL.createObjectURL(file))
      message.success('Đã lưu ảnh giao dịch vào phiếu')
      options.onSuccess?.({})
      queryClient.invalidateQueries({ queryKey: ['attachments', workOrder.id] })
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  if (!paymentReady) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Thanh toán được mở sau khi khách xác nhận kết quả và tổng chi phí." />
  }
  if (!payment) {
    return <Typography.Text type="secondary">Đang tải thông tin thanh toán...</Typography.Text>
  }

  return (
    <Space orientation="vertical" size={18} style={{ width: '100%' }}>
      <div className="section-heading-row">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>Thanh toán dịch vụ</Typography.Title>
          <Typography.Text type="secondary">Số tiền lấy từ snapshot khách đã xác nhận, không thay đổi theo hoàn trả phụ tùng sau này.</Typography.Text>
        </div>
        <MetaBadge tone={payment.status === 'SETTLED' ? 'success' : 'warning'}>{PAYMENT_STATUS_LABELS[payment.status]}</MetaBadge>
      </div>

      <Descriptions bordered size="small" column={1}>
        <Descriptions.Item label="Số tiền"><Typography.Text strong>{formatCurrency(payment.amount)}</Typography.Text></Descriptions.Item>
        {payment.method ? <Descriptions.Item label="Phương thức">{payment.method === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt'}</Descriptions.Item> : null}
        {payment.collectedByDisplayName ? <Descriptions.Item label="Người đang giữ tiền">{payment.collectedByDisplayName}</Descriptions.Item> : null}
        {payment.transferReportedAt ? <Descriptions.Item label="Khách báo chuyển khoản">{formatDateTime(payment.transferReportedAt)}</Descriptions.Item> : null}
        {payment.transferEvidenceAttachmentId ? (
          <Descriptions.Item label="Bằng chứng chuyển khoản">
            <Space wrap>
              <Typography.Text>{transferEvidence?.originalFilename ?? 'Ảnh giao dịch khách cung cấp'}</Typography.Text>
              <Button size="small" loading={downloadTransferEvidence.isPending} onClick={() => downloadTransferEvidence.mutate()}>Tải để kiểm tra</Button>
            </Space>
          </Descriptions.Item>
        ) : null}
        {payment.cashCollectedAt ? <Descriptions.Item label="Nhận tiền mặt">{formatDateTime(payment.cashCollectedAt)}</Descriptions.Item> : null}
        {payment.settledAt ? <Descriptions.Item label="Đối soát">{formatDateTime(payment.settledAt)} · {payment.settledByDisplayName}</Descriptions.Item> : null}
      </Descriptions>

      {role === 'TECHNICIAN' && payment.status === 'UNPAID' ? (
        <>
          <div className="payment-company-card">
            <Typography.Title level={5}>Tài khoản công ty</Typography.Title>
            {profile ? (
              <Space align="start" size={20} wrap>
                <PaymentQrImage attachmentId={profile.qrAttachmentId} />
                <Descriptions size="small" column={1}>
                  <Descriptions.Item label="Ngân hàng">{profile.bankName}</Descriptions.Item>
                  <Descriptions.Item label="Chủ tài khoản">{profile.accountHolder}</Descriptions.Item>
                  <Descriptions.Item label="Số tài khoản"><Typography.Text copyable strong>{profile.accountNumber}</Typography.Text></Descriptions.Item>
                </Descriptions>
              </Space>
            ) : (
              <Typography.Text type="warning">Chủ sở hữu chưa cấu hình tài khoản ngân hàng công ty. Không dùng tài khoản cá nhân.</Typography.Text>
            )}
          </div>

          <Space orientation="vertical" size={10} style={{ width: '100%' }}>
            <Typography.Text strong>Ảnh giao dịch chuyển khoản (khuyến nghị)</Typography.Text>
            <Space wrap>
              <Upload accept="image/*" capture="environment" customRequest={uploadEvidence} showUploadList={false}>
                <Button icon={<CameraOutlined />}>{evidence ? 'Chụp lại' : 'Chụp ảnh'}</Button>
              </Upload>
              <Upload accept="image/*" customRequest={uploadEvidence} showUploadList={false}>
                <Button icon={<PictureOutlined />}>Chọn từ thư viện</Button>
              </Upload>
              {evidence ? <MetaBadge tone="success">Đã chọn: {evidence.originalFilename}</MetaBadge> : null}
            </Space>
            {evidencePreview ? (
              <div>
                <Typography.Text strong>Xem lại ảnh</Typography.Text>
                <div style={{ marginTop: 8 }}><img src={evidencePreview} alt="Ảnh giao dịch vừa chọn" style={{ maxWidth: 280, width: '100%', borderRadius: 10 }} /></div>
              </div>
            ) : null}
            <Typography.Text type="secondary">Ảnh chỉ là bằng chứng hỗ trợ; CSKH vẫn phải kiểm tra tiền thật sự vào tài khoản công ty.</Typography.Text>
          </Space>

          <Space wrap>
            <Popconfirm
              title="Khách báo đã chuyển khoản?"
              description={evidence ? 'Ảnh giao dịch sẽ được liên kết với khoản thanh toán này.' : 'Chưa có ảnh giao dịch. Vẫn có thể ghi nhận nếu khách không thể cung cấp ảnh.'}
              okText="Ghi nhận chuyển khoản"
              cancelText="Chưa"
              onConfirm={() => reportTransfer.mutate()}
            >
              <Button type="primary" icon={<SwapOutlined />} disabled={!profile} loading={reportTransfer.isPending}>Khách báo đã chuyển khoản</Button>
            </Popconfirm>
            <Popconfirm
              title="Đã nhận đủ tiền mặt từ khách?"
              description={`Xác nhận kỹ thuật viên đang giữ ${formatCurrency(payment.amount)} để bàn giao về công ty.`}
              okText="Đã nhận tiền mặt"
              cancelText="Chưa"
              onConfirm={() => collectCash.mutate()}
            >
              <Button icon={<DollarOutlined />} loading={collectCash.isPending}>Đã nhận tiền mặt từ khách</Button>
            </Popconfirm>
          </Space>
        </>
      ) : null}

      {role === 'CUSTOMER_SERVICE' && payment.status === 'TRANSFER_PENDING_VERIFICATION' ? (
        <div className="payment-company-card">
          <Typography.Title level={5}>Đối soát thanh toán</Typography.Title>
          <Typography.Text type="secondary">
            Kiểm tra chi phí khách đã xác nhận, số tiền cần thu và bằng chứng khách cung cấp trước khi xác nhận tiền thực tế đã về tài khoản công ty.
          </Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              <Button onClick={onViewBilling}>Xem chi phí đã xác nhận</Button>
              <Popconfirm
                title="Xác nhận tiền đã về công ty?"
                description={`Bạn đã kiểm tra và xác nhận công ty thực nhận đủ ${formatCurrency(payment.amount)} qua chuyển khoản.`}
                okText="Xác nhận đã đối soát"
                cancelText="Chưa"
                onConfirm={() => settleTransfer.mutate()}
              >
                <Button type="primary" icon={<CheckCircleOutlined />} loading={settleTransfer.isPending}>Xác nhận tiền đã về công ty</Button>
              </Popconfirm>
            </Space>
          </div>
        </div>
      ) : null}
      {role === 'CUSTOMER_SERVICE' && payment.status === 'CASH_PENDING_HANDOVER' ? (
        <div className="payment-company-card">
          <Typography.Title level={5}>Đối soát thanh toán</Typography.Title>
          <Typography.Text type="secondary">
            Kiểm tra chi phí khách đã xác nhận và số tiền kỹ thuật viên đang bàn giao trước khi ghi nhận tiền đã về công ty.
          </Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              <Button onClick={onViewBilling}>Xem chi phí đã xác nhận</Button>
              <Popconfirm
                title="Xác nhận đã nhận bàn giao tiền?"
                description={`Bạn đã nhận và kiểm đủ ${formatCurrency(payment.amount)} từ ${payment.collectedByDisplayName ?? 'kỹ thuật viên'}.`}
                okText="Xác nhận đã đối soát"
                cancelText="Chưa"
                onConfirm={() => settleCash.mutate()}
              >
                <Button type="primary" icon={<CheckCircleOutlined />} loading={settleCash.isPending}>Xác nhận đã nhận bàn giao tiền</Button>
              </Popconfirm>
            </Space>
          </div>
        </div>
      ) : null}

      {payment.status === 'SETTLED' && role && ['CUSTOMER_SERVICE', 'OWNER'].includes(role) ? (
        <div className="payment-company-card">
          <Typography.Title level={5}>Hoàn tất hồ sơ thanh toán</Typography.Title>
          <Typography.Text type="secondary">Biên nhận dùng snapshot chi phí khách đã xác nhận nên không thay đổi khi kho nhận phụ tùng dư sau đó.</Typography.Text>
          <div style={{ marginTop: 12 }}>
            <Space wrap>
              {role === 'CUSTOMER_SERVICE' ? (
                <Button icon={<CheckCircleOutlined />} loading={issueReceipt.isPending} onClick={() => issueReceipt.mutate()}>Phát hành / tải biên nhận</Button>
              ) : workOrder.status === 'CLOSED' ? (
                <Button loading={downloadReceipt.isPending} onClick={() => downloadReceipt.mutate()}>Tải biên nhận</Button>
              ) : (
                <Typography.Text type="secondary">Chờ CSKH hoàn tất hồ sơ thanh toán</Typography.Text>
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
      ) : null}
    </Space>
  )
}
