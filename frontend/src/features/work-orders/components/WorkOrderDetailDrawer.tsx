import {
  CalendarOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  DownloadOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { Button, Descriptions, Drawer, Empty, Popconfirm, Space, Tabs, Timeline, Typography, Upload } from 'antd'
import { AttachmentList } from '../../attachments/components/AttachmentList'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { AttachmentItem, WorkOrder, WorkOrderStatus } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { TRANSITION_LABELS } from '../model/workOrderPresentation'
import type { WorkOrderPermissions } from '../model/workOrderPermissions'

export function WorkOrderDetailDrawer({
  workOrder,
  attachments,
  open,
  loading,
  permissions,
  transitions,
  transitionPending,
  onClose,
  onSchedule,
  onComplete,
  onConsumePart,
  onTransition,
  onExportInvoice,
  onUpload,
  onAttachmentsChanged,
}: {
  workOrder?: WorkOrder
  attachments?: AttachmentItem[]
  open: boolean
  loading: boolean
  permissions: WorkOrderPermissions
  transitions: WorkOrderStatus[]
  transitionPending: boolean
  onClose: () => void
  onSchedule: () => void
  onComplete: () => void
  onConsumePart: () => void
  onTransition: (targetStatus: WorkOrderStatus, note?: string) => void
  onExportInvoice: () => void
  onUpload: (options: UploadRequestOption) => Promise<void>
  onAttachmentsChanged: () => void
}) {
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
      extra={canScheduleCurrent ? <Button type="primary" icon={<CalendarOutlined />} onClick={onSchedule}>Phân công / xếp lịch</Button> : undefined}
    >
      {workOrder ? (
        <>
          <div className="work-order-actions">
            <Space wrap>
              {permissions.canTransition && transitions.map((target) => target === 'COMPLETED' ? (
                <Button key={target} type="primary" icon={<CheckCircleOutlined />} onClick={onComplete}>{TRANSITION_LABELS[target]}</Button>
              ) : target === 'CANCELLED' ? (
                <Popconfirm
                  key={target}
                  title="Huỷ phiếu công việc này?"
                  okText="Huỷ"
                  cancelText="Giữ lại"
                  onConfirm={() => onTransition(target, 'Huỷ từ giao diện vận hành')}
                >
                  <Button danger>{TRANSITION_LABELS[target]}</Button>
                </Popconfirm>
              ) : (
                <Button key={target} onClick={() => onTransition(target)} loading={transitionPending}>{TRANSITION_LABELS[target]}</Button>
              ))}
              {permissions.canConsumePart && !['CLOSED', 'CANCELLED'].includes(workOrder.status) && (
                <Button icon={<ToolOutlined />} onClick={onConsumePart}>Dùng phụ tùng</Button>
              )}
              <Button icon={<DownloadOutlined />} onClick={onExportInvoice}>Xuất hóa đơn</Button>
              <Upload customRequest={onUpload} showUploadList={false} accept="image/jpeg,image/png,image/webp,application/pdf">
                <Button icon={<CloudUploadOutlined />}>Tải ảnh / PDF</Button>
              </Upload>
            </Space>
          </div>

          <Tabs items={[
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
                  <Descriptions.Item label="Mô tả" span={2}>{workOrder.description ?? EMPTY_VALUE}</Descriptions.Item>
                  <Descriptions.Item label="Chẩn đoán" span={2}>{workOrder.diagnosis ?? EMPTY_VALUE}</Descriptions.Item>
                  <Descriptions.Item label="Giải pháp" span={2}>{workOrder.resolution ?? EMPTY_VALUE}</Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'timeline',
              label: `Lịch sử (${workOrder.history?.length ?? 0})`,
              children: workOrder.history?.length ? (
                <Timeline className="detail-timeline" items={workOrder.history.map((item) => ({
                  color: item.toStatus === 'CANCELLED' ? '#9c5050' : item.toStatus === 'COMPLETED' || item.toStatus === 'CLOSED' ? '#4b7968' : '#47789f',
                  children: (
                    <div className="timeline-entry">
                      <div className="timeline-entry-head"><StatusTag status={item.toStatus} /><Typography.Text className="timeline-actor">{item.changedBy}</Typography.Text></div>
                      <div className="timeline-note">{item.note ?? 'Không có ghi chú'}</div>
                      <Typography.Text className="timeline-time">{formatDateTime(item.createdAt)}</Typography.Text>
                    </div>
                  ),
                }))} />
              ) : <Empty description="Chưa có lịch sử" />,
            },
            {
              key: 'attachments',
              label: `Tệp đính kèm (${attachments?.length ?? 0})`,
              children: <AttachmentList attachments={attachments} onChanged={onAttachmentsChanged} />,
            },
          ]} />
        </>
      ) : null}
    </Drawer>
  )
}
