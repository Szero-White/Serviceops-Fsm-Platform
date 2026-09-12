import { DownloadOutlined, EyeOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { App, Button, Descriptions, Modal, Space, Tooltip, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { AttachmentItem, Payment } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { formatCurrency, formatDateTime } from '../../../utils/format'
import { attachmentsApi } from '../../attachments/api'

const PAYMENT_STATUS_LABELS = {
  UNPAID: 'Chưa thanh toán',
  TRANSFER_PENDING_VERIFICATION: 'Chờ xác minh chuyển khoản',
  CASH_PENDING_HANDOVER: 'KTV đang giữ tiền mặt',
  COUNTER_PAYMENT_PENDING: 'Chờ thanh toán tại quầy',
  SETTLED: 'Đã đối soát',
} as const

interface PaymentSummaryProps {
  payment: Payment
  transferEvidence?: AttachmentItem
}

export function PaymentSummary({ payment, transferEvidence }: PaymentSummaryProps) {
  const { message } = App.useApp()
  const [previewUrl, setPreviewUrl] = useState<string>()
  const [previewOpen, setPreviewOpen] = useState(false)

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
  }, [previewUrl])

  const previewEvidence = useMutation({
    mutationFn: () => attachmentsApi.download(payment.transferEvidenceAttachmentId!),
    onSuccess: (blob) => {
      setPreviewUrl((current) => {
        if (current) URL.revokeObjectURL(current)
        return URL.createObjectURL(blob)
      })
      setPreviewOpen(true)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const downloadEvidence = useMutation({
    mutationFn: () => attachmentsApi.download(payment.transferEvidenceAttachmentId!),
    onSuccess: (blob) => downloadBlob(blob, transferEvidence?.originalFilename ?? `bang-chung-chuyen-khoan-${payment.workOrderCode}`),
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const closePreview = () => {
    setPreviewOpen(false)
    setPreviewUrl((current) => {
      if (current) URL.revokeObjectURL(current)
      return undefined
    })
  }

  return (
    <>
      <div className="section-heading-row">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>Thanh toán dịch vụ</Typography.Title>
          <Typography.Text type="secondary">Số tiền đã được khách hàng xác nhận và được giữ cố định để đối soát thanh toán.</Typography.Text>
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
            <Space size={4} wrap>
              <Typography.Text>{transferEvidence?.originalFilename ?? 'Ảnh giao dịch khách cung cấp'}</Typography.Text>
              <Tooltip title="Xem bằng chứng">
                <Button type="text" size="small" shape="circle" aria-label="Xem bằng chứng chuyển khoản" icon={<EyeOutlined />} loading={previewEvidence.isPending} onClick={() => previewEvidence.mutate()} />
              </Tooltip>
              <Tooltip title="Tải xuống">
                <Button type="text" size="small" shape="circle" aria-label="Tải bằng chứng chuyển khoản" icon={<DownloadOutlined />} loading={downloadEvidence.isPending} onClick={() => downloadEvidence.mutate()} />
              </Tooltip>
            </Space>
          </Descriptions.Item>
        ) : null}
        {payment.cashCollectedAt ? <Descriptions.Item label="Nhận tiền mặt">{formatDateTime(payment.cashCollectedAt)}</Descriptions.Item> : null}
        {payment.counterPaymentRequestedAt ? <Descriptions.Item label="Hẹn thanh toán tại quầy">{formatDateTime(payment.counterPaymentRequestedAt)}</Descriptions.Item> : null}
        {payment.settledAt ? <Descriptions.Item label="Đối soát">{formatDateTime(payment.settledAt)} · {payment.settledByDisplayName}</Descriptions.Item> : null}
      </Descriptions>

      <Modal open={previewOpen} title={transferEvidence?.originalFilename ?? 'Bằng chứng chuyển khoản'} footer={null} width={860} destroyOnHidden onCancel={closePreview}>
        {previewUrl ? <img src={previewUrl} alt={transferEvidence?.originalFilename ?? 'Bằng chứng chuyển khoản'} style={{ display: 'block', width: '100%', maxHeight: '72vh', objectFit: 'contain' }} /> : null}
      </Modal>
    </>
  )
}
