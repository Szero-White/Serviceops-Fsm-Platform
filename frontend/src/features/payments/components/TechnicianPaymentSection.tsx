import { CameraOutlined, DeleteOutlined, DollarOutlined, PictureOutlined, ShopOutlined, SwapOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Space, Typography, Upload } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { AttachmentItem, CompanyPaymentProfile, Payment, WorkOrder } from '../../../types'
import { attachmentsApi } from '../../attachments/api'
import { paymentsApi } from '../api'
import { usePaymentRefresh } from '../hooks/usePaymentRefresh'
import { PaymentActionConfirmationModal } from './PaymentActionConfirmationModal'
import { PaymentQrImage } from './PaymentQrImage'

type TechnicianPaymentAction = 'TRANSFER' | 'CASH' | 'COUNTER'

interface TechnicianPaymentSectionProps {
  workOrder: WorkOrder
  payment: Payment
  profile?: CompanyPaymentProfile | null
  attachments?: AttachmentItem[]
}

export function TechnicianPaymentSection({ workOrder, payment, profile, attachments }: TechnicianPaymentSectionProps) {
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const refresh = usePaymentRefresh(workOrder.id)
  const [evidence, setEvidence] = useState<AttachmentItem>()
  const [evidencePreview, setEvidencePreview] = useState<string>()
  const [action, setAction] = useState<TechnicianPaymentAction>()
  const draftEvidence = attachments?.find((item) => item.purpose === 'PAYMENT_EVIDENCE' && item.manageable)
  const selectedEvidence = evidence ?? draftEvidence

  useEffect(() => () => {
    if (evidencePreview) URL.revokeObjectURL(evidencePreview)
  }, [evidencePreview])

  useEffect(() => {
    setEvidence(undefined)
    setEvidencePreview(undefined)
    setAction(undefined)
  }, [workOrder.id])

  const reportTransfer = useMutation({
    mutationFn: () => paymentsApi.reportTransfer(workOrder.id, selectedEvidence?.id),
    onSuccess: () => {
      notification.success({ message: 'Đã ghi nhận khách báo chuyển khoản', description: 'Bộ phận chăm sóc khách hàng đã được thông báo để đối soát tiền thực tế vào tài khoản công ty.' })
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const collectCash = useMutation({
    mutationFn: () => paymentsApi.collectCash(workOrder.id),
    onSuccess: () => {
      notification.success({ message: 'Đã ghi nhận tiền mặt', description: 'Tiền đang do kỹ thuật viên giữ và cần bàn giao cho bộ phận chăm sóc khách hàng.' })
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const payAtCounter = useMutation({
    mutationFn: () => paymentsApi.payAtCounter(workOrder.id),
    onSuccess: () => {
      notification.success({
        message: 'Đã chuyển thanh toán về quầy',
        description: 'Khách sẽ thanh toán trực tiếp với bộ phận chăm sóc khách hàng tại quầy. Bộ phận phụ trách đã được thông báo để theo dõi khoản này.',
      })
      setAction(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const uploadEvidence = async (options: UploadRequestOption) => {
    try {
      const file = options.file as File
      const previousEvidence = selectedEvidence
      const attachment = await attachmentsApi.upload('WORK_ORDER', workOrder.id, file, 'PAYMENT_EVIDENCE')
      setEvidence(attachment)
      if (evidencePreview) URL.revokeObjectURL(evidencePreview)
      setEvidencePreview(URL.createObjectURL(file))
      if (previousEvidence?.manageable && previousEvidence.id !== attachment.id) {
        try {
          await attachmentsApi.delete(previousEvidence.id)
        } catch {
          message.warning('Ảnh mới đã được lưu nhưng ảnh nháp trước chưa xóa được')
        }
      }
      message.success('Đã lưu ảnh giao dịch để chờ ghi nhận chuyển khoản')
      options.onSuccess?.({})
      queryClient.invalidateQueries({ queryKey: ['attachments', workOrder.id] })
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  const removeDraftEvidence = async () => {
    if (!selectedEvidence?.manageable) return
    try {
      await attachmentsApi.delete(selectedEvidence.id)
      setEvidence(undefined)
      if (evidencePreview) URL.revokeObjectURL(evidencePreview)
      setEvidencePreview(undefined)
      message.success('Đã bỏ ảnh giao dịch nháp')
      queryClient.invalidateQueries({ queryKey: ['attachments', workOrder.id] })
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  return (
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
            <Button icon={<CameraOutlined />}>{selectedEvidence ? 'Chụp lại' : 'Chụp ảnh'}</Button>
          </Upload>
          <Upload accept="image/*" customRequest={uploadEvidence} showUploadList={false}>
            <Button icon={<PictureOutlined />}>Chọn từ thư viện</Button>
          </Upload>
          {selectedEvidence ? <MetaBadge tone="success">Đã chọn: {selectedEvidence.originalFilename}</MetaBadge> : null}
          {selectedEvidence?.manageable ? <Button danger type="text" icon={<DeleteOutlined />} onClick={() => void removeDraftEvidence()}>Bỏ ảnh</Button> : null}
        </Space>
        {evidencePreview ? (
          <div>
            <Typography.Text strong>Xem lại ảnh</Typography.Text>
            <div style={{ marginTop: 8 }}><img src={evidencePreview} alt="Ảnh giao dịch vừa chọn" style={{ maxWidth: 280, width: '100%', borderRadius: 10 }} /></div>
          </div>
        ) : null}
        <Typography.Text type="secondary">Ảnh giao dịch chỉ hỗ trợ đối soát; thanh toán chỉ hoàn tất sau khi bộ phận chăm sóc khách hàng xác minh công ty đã thực nhận tiền.</Typography.Text>
      </Space>

      <Space orientation="vertical" size={8} style={{ width: '100%' }}>
        <div>
          <Typography.Text strong>Chọn phương án thanh toán thực tế</Typography.Text><br />
          <Typography.Text type="secondary">Mỗi lựa chọn đều có bước xác nhận lại để tránh ghi nhận nhầm. Sau khi lưu, trạng thái thanh toán chỉ thay đổi theo đúng quy trình đối soát.</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<SwapOutlined />} disabled={!profile} onClick={() => setAction('TRANSFER')}>Khách đã chuyển khoản</Button>
          <Button icon={<DollarOutlined />} onClick={() => setAction('CASH')}>Đã nhận tiền mặt</Button>
          <Button icon={<ShopOutlined />} onClick={() => setAction('COUNTER')}>Hẹn thanh toán tại quầy</Button>
        </Space>
      </Space>

      <PaymentActionConfirmationModal
        open={action === 'TRANSFER'}
        title="Xác nhận khách đã chuyển khoản"
        description={selectedEvidence ? 'Ảnh giao dịch đã chọn sẽ được khóa và liên kết với khoản thanh toán này.' : 'Chưa có ảnh giao dịch. Chỉ tiếp tục khi khách đã thực sự báo hoàn tất chuyển khoản.'}
        workOrderCode={workOrder.code}
        customerName={payment.customerName}
        amount={payment.amount}
        confirmationLabel="Tôi đã kiểm tra đúng phiếu, đúng số tiền và khách hàng đã xác nhận đã thực hiện chuyển khoản."
        confirmText="Ghi nhận đã chuyển khoản"
        loading={reportTransfer.isPending}
        onCancel={() => setAction(undefined)}
        onConfirm={() => reportTransfer.mutate()}
      />
      <PaymentActionConfirmationModal
        open={action === 'CASH'}
        title="Xác nhận đã nhận tiền mặt"
        description="Sau khi ghi nhận, hệ thống sẽ xác định kỹ thuật viên đang giữ tiền và phải bàn giao đủ cho bộ phận chăm sóc khách hàng."
        workOrderCode={workOrder.code}
        customerName={payment.customerName}
        amount={payment.amount}
        confirmationLabel="Tôi đã trực tiếp nhận đủ số tiền mặt hiển thị từ khách hàng và chịu trách nhiệm bàn giao về công ty."
        confirmText="Ghi nhận đã nhận tiền mặt"
        loading={collectCash.isPending}
        onCancel={() => setAction(undefined)}
        onConfirm={() => collectCash.mutate()}
      />
      <PaymentActionConfirmationModal
        open={action === 'COUNTER'}
        title="Xác nhận hẹn thanh toán tại quầy"
        description="Lựa chọn này dùng khi kỹ thuật viên chưa thu tiền. Khoản thanh toán sẽ được chuyển sang danh sách chờ của bộ phận chăm sóc khách hàng để thu trực tiếp tại quầy."
        workOrderCode={workOrder.code}
        customerName={payment.customerName}
        amount={payment.amount}
        confirmationLabel="Tôi xác nhận chưa thu tiền từ khách và đã hướng dẫn khách đến quầy chăm sóc khách hàng để hoàn tất thanh toán."
        confirmText="Chuyển thanh toán về quầy"
        loading={payAtCounter.isPending}
        onCancel={() => setAction(undefined)}
        onConfirm={() => payAtCounter.mutate()}
      />
    </>
  )
}
