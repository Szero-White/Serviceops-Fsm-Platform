import {
  CalendarOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
} from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { Button, Descriptions, Drawer, Input, Popconfirm, Space, Tabs, Typography, Upload } from 'antd'
import { AttachmentList } from '../../attachments/components/AttachmentList'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { AttachmentItem, UserRole, WorkOrder, WorkOrderStatus } from '../../../types'
import { useState } from 'react'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { TRANSITION_LABELS } from '../model/workOrderPresentation'
import type { WorkOrderPermissions } from '../model/workOrderPermissions'
import { WorkOrderActivityTimeline } from './WorkOrderActivityTimeline'
import { WorkOrderPartsPanel } from '../../inventory/components/WorkOrderPartsPanel'
import { WorkOrderBillingPanel } from '../../payments/components/WorkOrderBillingPanel'
import { WorkOrderPaymentPanel } from '../../payments/components/WorkOrderPaymentPanel'

export function WorkOrderDetailDrawer({
  workOrder,
  attachments,
  open,
  loading,
  error,
  onRetry,
  attachmentsError,
  onRetryAttachments,
  permissions,
  transitions,
  transitionPending,
  activeTabKey,
  onTabChange,
  onClose,
  onSchedule,
  onComplete,
  role,
  onTransition,
  onUpload,
  onAttachmentsChanged,
}: {
  workOrder?: WorkOrder
  attachments?: AttachmentItem[]
  open: boolean
  loading: boolean
  error?: unknown
  onRetry: () => void
  attachmentsError?: unknown
  onRetryAttachments: () => void
  permissions: WorkOrderPermissions
  transitions: WorkOrderStatus[]
  transitionPending: boolean
  activeTabKey?: string
  onTabChange?: (key: string) => void
  onClose: () => void
  onSchedule: () => void
  onComplete: () => void
  role?: UserRole
  onTransition: (targetStatus: WorkOrderStatus, note?: string) => void
  onUpload: (options: UploadRequestOption) => Promise<void>
  onAttachmentsChanged: () => void
}) {
  const [cancelReason, setCancelReason] = useState('')
  const canScheduleCurrent = permissions.canSchedule
    && workOrder
    && ['OPEN', 'SCHEDULED', 'ASSIGNED', 'REOPENED'].includes(workOrder.status)

  return (
    <Drawer
      rootClassName="serviceops-detail-drawer"
      title={workOrder ? (
        <div className="detail-drawer-title">
          <span className="detail-drawer-code">{workOrder.code}</span>
          <span className="detail-drawer-summary">{workOrder.summary}</span>
        </div>
      ) : 'Chi tiết phiếu công việc'}
      open={open}
      onClose={onClose}
      width={720}
      loading={loading}
      extra={canScheduleCurrent ? (
        <Button type="primary" icon={<CalendarOutlined />} onClick={onSchedule}>
          {workOrder?.technicianId ? 'Điều phối lại' : 'Phân công / xếp lịch'}
        </Button>
      ) : undefined}
    >
      {error ? (
        <QueryErrorAlert
          title="Chưa tải được chi tiết phiếu công việc"
          error={error}
          onRetry={onRetry}
        />
      ) : workOrder ? (
        <>
          <div className="work-order-actions">
            <Space wrap>
              {permissions.canTransition && transitions
                .filter((target) => target !== 'CUSTOMER_ACCEPTED' && target !== 'CLOSED')
                .map((target) => target === 'COMPLETED' ? (
                  <Button key={target} type="primary" icon={<CheckCircleOutlined />} onClick={onComplete}>{TRANSITION_LABELS[target]}</Button>
                ) : target === 'CANCELLED' ? (
                  <Popconfirm
                    key={target}
                    title="Hủy phiếu công việc này?"
                    description={(
                      <Space direction="vertical" size={8}>
                        <Typography.Text type="secondary">Lý do hủy sẽ được lưu vào lịch sử phiếu.</Typography.Text>
                        <Input.TextArea
                          value={cancelReason}
                          onChange={(event) => setCancelReason(event.target.value)}
                          placeholder="Ví dụ: Khách hàng thông báo thiết bị đã hoạt động bình thường và không còn nhu cầu dịch vụ."
                          autoSize={{ minRows: 3, maxRows: 5 }}
                          maxLength={1000}
                          showCount
                        />
                      </Space>
                    )}
                    okText="Xác nhận hủy"
                    cancelText="Giữ lại"
                    okButtonProps={{ danger: true, disabled: !cancelReason.trim(), loading: transitionPending }}
                    onConfirm={() => {
                      const reason = cancelReason.trim()
                      if (!reason) return
                      onTransition(target, reason)
                      setCancelReason('')
                    }}
                    onCancel={() => setCancelReason('')}
                  >
                    <Button danger>{TRANSITION_LABELS[target]}</Button>
                  </Popconfirm>
                ) : (
                  <Button key={target} onClick={() => onTransition(target)} loading={transitionPending}>{TRANSITION_LABELS[target]}</Button>
                ))}
              {permissions.canTransition && transitions.includes('CUSTOMER_ACCEPTED') && (
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  loading={transitionPending}
                  onClick={() => onTransition('CUSTOMER_ACCEPTED')}
                >
                  {TRANSITION_LABELS.CUSTOMER_ACCEPTED}
                </Button>
              )}
              {permissions.canTransition && transitions.includes('CLOSED') && (
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  loading={transitionPending}
                  onClick={() => onTransition('CLOSED')}
                >
                  {TRANSITION_LABELS.CLOSED}
                </Button>
              )}
              <Upload customRequest={onUpload} showUploadList={false} accept="image/jpeg,image/png,image/webp,application/pdf">
                <Button icon={<CloudUploadOutlined />}>Tải ảnh / PDF</Button>
              </Upload>
            </Space>
          </div>

          <Tabs
            activeKey={activeTabKey}
            onChange={onTabChange}
            items={[
            {
              key: 'overview',
              label: 'Tổng quan',
              children: (
                <Descriptions className="detail-descriptions" column={2} bordered size="small">
                  <Descriptions.Item label="Trạng thái"><StatusTag status={workOrder.status} /></Descriptions.Item>
                  <Descriptions.Item label="Ưu tiên"><PriorityTag priority={workOrder.priority} /></Descriptions.Item>
                  <Descriptions.Item label="Khách hàng">{workOrder.customerName}</Descriptions.Item>
                  <Descriptions.Item label="Thiết bị">{workOrder.assetLabel ?? 'Chưa xác định'}</Descriptions.Item>
                  <Descriptions.Item label="Kỹ thuật viên">{workOrder.technicianName ?? 'Chưa phân công'}</Descriptions.Item>
                  <Descriptions.Item label="Lịch hẹn">{formatDateTime(workOrder.scheduledStart)} - {formatDateTime(workOrder.scheduledEnd)}</Descriptions.Item>
                  {workOrder.status === 'ASSIGNED' ? (
                    <Descriptions.Item label="Bước tiếp theo" span={2}>
                      Đang chờ kỹ thuật viên được phân công bắt đầu di chuyển hoặc thực hiện công việc.
                      Điều phối viên hoặc Owner có thể điều phối lại trước khi kỹ thuật viên bắt đầu.
                    </Descriptions.Item>
                  ) : null}
                  <Descriptions.Item label="Mô tả" span={2}>{workOrder.description ?? EMPTY_VALUE}</Descriptions.Item>
                  <Descriptions.Item label="Chẩn đoán" span={2}>{workOrder.diagnosis ?? EMPTY_VALUE}</Descriptions.Item>
                  <Descriptions.Item label="Giải pháp" span={2}>{workOrder.resolution ?? EMPTY_VALUE}</Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'parts',
              label: 'Phụ tùng',
              children: <WorkOrderPartsPanel workOrder={workOrder} role={role} />,
            },
            ...((role && ['OWNER', 'CUSTOMER_SERVICE', 'TECHNICIAN'].includes(role)) ? [
              {
                key: 'billing',
                label: 'Chi phí',
                children: <WorkOrderBillingPanel workOrder={workOrder} role={role} />,
              },
              {
                key: 'payment',
                label: 'Thanh toán',
                children: <WorkOrderPaymentPanel workOrder={workOrder} role={role} attachments={attachments} onViewBilling={() => onTabChange?.('billing')} />,
              },
            ] : []),
            {
              key: 'timeline',
              label: 'Tiến trình',
              children: (
                <WorkOrderActivityTimeline
                  workOrderId={workOrder.id}
                  activities={workOrder.activities}
                  history={workOrder.history}
                />
              ),
            },
            {
              key: 'attachments',
              label: `File đính kèm (${attachments?.length ?? 0})`,
              children: attachmentsError ? (
                <QueryErrorAlert
                  title="Chưa tải được file đính kèm"
                  error={attachmentsError}
                  onRetry={onRetryAttachments}
                />
              ) : <AttachmentList attachments={attachments} onChanged={onAttachmentsChanged} />,
            },
            ]}
          />
        </>
      ) : null}
    </Drawer>
  )
}
