import { RollbackOutlined, ToolOutlined } from '@ant-design/icons'
import { Empty, Space, Timeline, Typography } from 'antd'
import { StatusTag } from '../../../components/StatusTag'
import type { WorkOrderActivity, WorkOrderHistory } from '../../../types'
import { formatDateTime, formatQuantityWithUnit } from '../../../utils/format'

function fallbackActivities(history?: WorkOrderHistory[]): WorkOrderActivity[] {
  return (history ?? []).map((item) => ({
    id: item.id,
    type: 'STATUS_CHANGE',
    status: item.toStatus,
    note: item.note,
    actor: item.changedBy,
    createdAt: item.createdAt,
  }))
}

function activityActor(activity: WorkOrderActivity) {
  return activity.actorDisplayName?.trim() || activity.actor || 'Không xác định'
}

function statusColor(activity: WorkOrderActivity) {
  if (activity.status === 'CANCELLED') return '#9c5050'
  if (activity.status === 'COMPLETED' || activity.status === 'CLOSED') return '#4b7968'
  return '#47789f'
}

export function workOrderActivityCount(activities?: WorkOrderActivity[], history?: WorkOrderHistory[]) {
  return activities?.length ? activities.length : (history?.length ?? 0)
}

export function WorkOrderActivityTimeline({
  activities,
  history,
  emptyDescription = 'Chưa có tiến trình xử lý',
}: {
  activities?: WorkOrderActivity[]
  history?: WorkOrderHistory[]
  emptyDescription?: string
}) {
  const timelineActivities = activities?.length ? activities : fallbackActivities(history)

  if (!timelineActivities.length) {
    return <Empty description={emptyDescription} />
  }

  return (
    <Timeline
      className="detail-timeline"
      items={timelineActivities.map((activity) => {
        if (activity.type === 'STATUS_CHANGE' && activity.status) {
          return {
            color: statusColor(activity),
            children: (
              <div className="timeline-entry">
                <div className="timeline-entry-head">
                  <StatusTag status={activity.status} />
                  <Typography.Text className="timeline-actor">{activityActor(activity)}</Typography.Text>
                </div>
                <div className="timeline-note">{activity.note ?? 'Không có ghi chú'}</div>
                <Typography.Text className="timeline-time">{formatDateTime(activity.createdAt)}</Typography.Text>
              </div>
            ),
          }
        }

        const isReturn = activity.type === 'PART_RETURNED'
        const title = isReturn ? 'Đã hoàn trả phụ tùng' : 'Đã sử dụng phụ tùng'
        const Icon = isReturn ? RollbackOutlined : ToolOutlined

        return {
          color: isReturn ? '#8a6a3f' : '#47789f',
          children: (
            <div className="timeline-entry">
              <div className="timeline-entry-head">
                <Space size={6}>
                  <Icon />
                  <Typography.Text strong>{title}</Typography.Text>
                </Space>
                <Typography.Text className="timeline-actor">{activityActor(activity)}</Typography.Text>
              </div>
              <div className="timeline-note">
                <Typography.Text strong>{activity.sparePartName ?? 'Phụ tùng'}</Typography.Text>
                {activity.sparePartSku ? <> · <Typography.Text code>{activity.sparePartSku}</Typography.Text></> : null}
                {' · '}{formatQuantityWithUnit(activity.quantity, activity.unit)}
              </div>
              {activity.note ? <div className="timeline-note">Ghi chú: {activity.note}</div> : null}
              <Typography.Text className="timeline-time">{formatDateTime(activity.createdAt)}</Typography.Text>
            </div>
          ),
        }
      })}
    />
  )
}
