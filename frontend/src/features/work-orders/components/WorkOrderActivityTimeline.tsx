import { CalendarOutlined, RollbackOutlined, ToolOutlined } from '@ant-design/icons'
import { Empty, Space, Timeline, Typography } from 'antd'
import dayjs from 'dayjs'
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
    actorDisplayName: item.actorDisplayName,
    actorRole: item.actorRole,
    diagnosis: item.diagnosis,
    resolution: item.resolution,
    createdAt: item.createdAt,
  }))
}

const actorRoleLabels: Record<NonNullable<WorkOrderActivity['actorRole']>, string> = {
  OWNER: 'Chủ sở hữu',
  DISPATCHER: 'Điều phối viên',
  CUSTOMER_SERVICE: 'Chăm sóc khách hàng',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
  SYSTEM: 'Hệ thống',
}

function activityActor(activity: WorkOrderActivity) {
  const displayName = activity.actorDisplayName?.trim() || activity.actor?.trim()
  const roleLabel = activity.actorRole ? actorRoleLabels[activity.actorRole] : undefined

  if (activity.actorRole === 'SYSTEM' || (!activity.actorRole && activity.actor?.toLowerCase() === 'system')) {
    return 'Hệ thống'
  }
  if (roleLabel && displayName) return `${roleLabel} · ${displayName}`
  return displayName || roleLabel || 'Không xác định'
}

function inlineActor(activity: WorkOrderActivity) {
  return <Typography.Text className="timeline-actor-inline">· {activityActor(activity)}</Typography.Text>
}

function completionDetails(activity: WorkOrderActivity) {
  if (activity.status !== 'COMPLETED') return null

  const hasSnapshot = Boolean(activity.diagnosis?.trim() || activity.resolution?.trim())
  if (!hasSnapshot && !activity.note?.trim()) return null

  return (
    <div className="timeline-completion-details">
      {activity.diagnosis?.trim() ? (
        <div className="timeline-note"><Typography.Text strong>Chẩn đoán / nguyên nhân:</Typography.Text> {activity.diagnosis}</div>
      ) : null}
      {activity.resolution?.trim() ? (
        <div className="timeline-note"><Typography.Text strong>Giải pháp đã thực hiện:</Typography.Text> {activity.resolution}</div>
      ) : null}
      {activity.note?.trim() ? (
        <div className="timeline-note"><Typography.Text strong>Ghi chú bàn giao:</Typography.Text> {activity.note}</div>
      ) : null}
    </div>
  )
}

function statusColor(activity: WorkOrderActivity) {
  if (activity.status === 'CANCELLED') return '#9c5050'
  if (activity.status === 'COMPLETED' || activity.status === 'CLOSED') return '#4b7968'
  return '#47789f'
}

type DispatchSummary = {
  title: 'Đã cập nhật lịch' | 'Đã điều phối lại'
  previousTechnician?: string
  technician?: string
  previousStart?: string
  previousEnd?: string
  start?: string
  end?: string
  reason?: string
  technicianChanged: boolean
  scheduleChanged: boolean
}

function dispatchSummary(note?: string): DispatchSummary | undefined {
  if (!note) return undefined

  const detailed = note.match(
    /:\s*(.*?)\s+\[(.*?)\s+-\s+(.*?)\]\s*(?:→|->)\s*(.*?)\s+\[(.*?)\s+-\s+(.*?)\](?:\.\s*Lý do:\s*(.*))?$/,
  )
  if (detailed) {
    const [, previousTechnician, previousStart, previousEnd, technician, start, end, reason] = detailed
    const technicianChanged = previousTechnician.trim() !== technician.trim()
    const scheduleChanged = previousStart.trim() !== start.trim() || previousEnd.trim() !== end.trim()
    return {
      title: technicianChanged ? 'Đã điều phối lại' : 'Đã cập nhật lịch',
      previousTechnician: previousTechnician.trim(),
      technician: technician.trim(),
      previousStart: previousStart.trim(),
      previousEnd: previousEnd.trim(),
      start: start.trim(),
      end: end.trim(),
      reason: reason?.trim(),
      technicianChanged,
      scheduleChanged,
    }
  }

  const technicianOnly = note.match(/đã điều phối lại kỹ thuật viên từ (.*?) sang (.*?)\.\s*Lý do:\s*(.*)$/i)
  if (technicianOnly) {
    return {
      title: 'Đã điều phối lại',
      previousTechnician: technicianOnly[1].trim(),
      technician: technicianOnly[2].trim(),
      reason: technicianOnly[3].trim(),
      technicianChanged: true,
      scheduleChanged: false,
    }
  }

  const scheduleOnly = note.match(/đã điều chỉnh lịch thực hiện cho (.*?)\.\s*Lý do:\s*(.*)$/i)
  if (scheduleOnly) {
    return {
      title: 'Đã cập nhật lịch',
      technician: scheduleOnly[1].trim(),
      reason: scheduleOnly[2].trim(),
      technicianChanged: false,
      scheduleChanged: true,
    }
  }

  return undefined
}

function formatScheduleRange(start?: string, end?: string) {
  if (!start || !end) return undefined
  const from = dayjs(start)
  const to = dayjs(end)
  if (!from.isValid() || !to.isValid()) return undefined
  return from.isSame(to, 'day')
    ? `${from.format('DD/MM/YYYY HH:mm')}–${to.format('HH:mm')}`
    : `${from.format('DD/MM/YYYY HH:mm')} → ${to.format('DD/MM/YYYY HH:mm')}`
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
                  {inlineActor(activity)}
                </div>
                {activity.status === 'COMPLETED' ? (
                  completionDetails(activity)
                ) : (
                  <div className="timeline-note">{activity.note ?? 'Không có ghi chú'}</div>
                )}
                <Typography.Text className="timeline-time">{formatDateTime(activity.createdAt)}</Typography.Text>
              </div>
            ),
          }
        }

        if (activity.type === 'DISPATCH_UPDATED') {
          const dispatch = dispatchSummary(activity.note)
          const previousSchedule = formatScheduleRange(dispatch?.previousStart, dispatch?.previousEnd)
          const currentSchedule = formatScheduleRange(dispatch?.start, dispatch?.end)

          return {
            color: '#47789f',
            children: (
              <div className="timeline-entry">
                <div className="timeline-entry-head">
                  <Space size={6}>
                    <CalendarOutlined />
                    <Typography.Text strong>{dispatch?.title ?? 'Đã điều phối lại'}</Typography.Text>
                  </Space>
                  {inlineActor(activity)}
                </div>
                {dispatch?.technicianChanged ? (
                  <div className="timeline-note">
                    Kỹ thuật viên: {dispatch.previousTechnician} → {dispatch.technician}
                  </div>
                ) : dispatch?.technician ? (
                  <div className="timeline-note">Kỹ thuật viên: {dispatch.technician}</div>
                ) : null}
                {dispatch?.scheduleChanged && currentSchedule ? (
                  <div className="timeline-note">
                    Lịch: {previousSchedule ? `${previousSchedule} → ` : ''}{currentSchedule}
                  </div>
                ) : null}
                {dispatch?.reason ? <div className="timeline-note">Lý do: {dispatch.reason}</div> : null}
                {!dispatch ? (
                  <div className="timeline-note">Thông tin phân công hoặc lịch thực hiện đã được cập nhật.</div>
                ) : null}
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
                {inlineActor(activity)}
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
