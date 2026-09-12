import { Button, Modal, Space, Typography } from 'antd'
import { PriorityTag } from '../../../components/StatusTag'
import type { ServiceRequest } from '../../../types'

interface ServiceRequestConversionModalProps {
  request?: ServiceRequest
  loading: boolean
  onCancel: () => void
  onReview: (request: ServiceRequest) => void
  onConfirm: (requestId: string) => void
}

export function ServiceRequestConversionModal({
  request,
  loading,
  onCancel,
  onReview,
  onConfirm,
}: ServiceRequestConversionModalProps) {
  return (
    <Modal
      title="Xác nhận tạo phiếu công việc"
      open={Boolean(request)}
      onCancel={onCancel}
      footer={[
        <Button key="review" onClick={() => request && onReview(request)}>
          Quay lại kiểm tra
        </Button>,
        <Button key="convert" type="primary" loading={loading} onClick={() => request && onConfirm(request.id)}>
          Tạo phiếu & chuyển điều phối
        </Button>,
      ]}
      width={620}
      destroyOnHidden
    >
      {request ? (
        <Space orientation="vertical" size={10} style={{ width: '100%' }}>
          <Typography.Text>
            Vui lòng kiểm tra lại thông tin chính trước khi chuyển yêu cầu sang bộ phận Điều phối.
          </Typography.Text>
          <div className="service-request-convert-summary">
            <div><span>Yêu cầu</span><strong>{request.title}</strong></div>
            <div><span>Khách hàng</span><strong>{request.customerName}</strong></div>
            <div><span>Thiết bị</span><strong>{request.assetLabel || 'Chưa xác định'}</strong></div>
            <div><span>Ưu tiên</span><strong><PriorityTag priority={request.priority} /></strong></div>
          </div>
          <Typography.Text type="secondary">
            Sau khi xác nhận, yêu cầu sẽ được khóa và hệ thống tạo phiếu công việc để Điều phối viên tiếp tục xử lý.
          </Typography.Text>
        </Space>
      ) : null}
    </Modal>
  )
}
