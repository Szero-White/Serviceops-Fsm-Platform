import {
  CalendarOutlined,
  EnvironmentOutlined,
  LeftOutlined,
  ReloadOutlined,
  RightOutlined,
  ToolOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Empty, Spin, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../../../components/PageHeader'
import { MetaBadge } from '../../../components/PresentationBadge'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { MyScheduleItem } from '../../../types'
import { useAuth } from '../../auth/AuthContext'
import { scheduleBoardApi } from '../api'

const DAY_COUNT = 7

function startOfWeek(value: Dayjs) {
  const day = value.day()
  const mondayOffset = day === 0 ? -6 : 1 - day
  return value.add(mondayOffset, 'day').startOf('day')
}

function dayKey(value: Dayjs | string) {
  return dayjs(value).format('YYYY-MM-DD')
}

function formatTimeRange(item: MyScheduleItem) {
  return `${dayjs(item.startTime).format('HH:mm')}–${dayjs(item.endTime).format('HH:mm')}`
}

export function MySchedulePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [weekStart, setWeekStart] = useState(() => startOfWeek(dayjs()))
  const weekEnd = weekStart.add(DAY_COUNT, 'day')
  const from = weekStart.toISOString()
  const to = weekEnd.toISOString()
  const days = useMemo(
    () => Array.from({ length: DAY_COUNT }, (_, index) => weekStart.add(index, 'day')),
    [weekStart],
  )
  const isCurrentWeek = weekStart.isSame(startOfWeek(dayjs()), 'day')

  const scheduleQuery = useQuery({
    queryKey: ['my-schedule', from, to],
    queryFn: () => scheduleBoardApi.getMine(from, to),
  })

  const appointmentsByDay = useMemo(() => {
    const grouped = new Map<string, MyScheduleItem[]>()
    for (const appointment of scheduleQuery.data?.appointments ?? []) {
      const key = dayKey(appointment.startTime)
      const items = grouped.get(key) ?? []
      items.push(appointment)
      grouped.set(key, items)
    }
    return grouped
  }, [scheduleQuery.data?.appointments])

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Không gian kỹ thuật viên"
        title="Lịch của tôi"
        description="Theo dõi lịch làm việc được phân công cho chính tài khoản của bạn theo từng ngày."
        actions={(
          <Button icon={<ReloadOutlined />} onClick={() => scheduleQuery.refetch()} loading={scheduleQuery.isFetching}>
            Làm mới
          </Button>
        )}
        meta={(
          <>
            <MetaBadge tone="info">{scheduleQuery.data?.appointments.length ?? 0} công việc trong tuần</MetaBadge>
            <MetaBadge>{scheduleQuery.data?.technicianName ?? user?.displayName ?? 'Kỹ thuật viên'}</MetaBadge>
          </>
        )}
      />

      <div className="schedule-board-toolbar">
        <div className="schedule-board-nav">
          <Button icon={<LeftOutlined />} aria-label="Tuần trước" onClick={() => setWeekStart((value) => value.subtract(DAY_COUNT, 'day'))} />
          <Button disabled={isCurrentWeek} onClick={() => setWeekStart(startOfWeek(dayjs()))}>Tuần này</Button>
          <Button icon={<RightOutlined />} aria-label="Tuần sau" onClick={() => setWeekStart((value) => value.add(DAY_COUNT, 'day'))} />
        </div>
        <div className="schedule-board-range">
          <CalendarOutlined />
          <Typography.Text strong>
            {weekStart.format('DD/MM')} – {weekEnd.subtract(1, 'day').format('DD/MM/YYYY')}
          </Typography.Text>
        </div>
      </div>

      {scheduleQuery.isLoading ? (
        <div className="schedule-board-loading"><Spin size="large" /></div>
      ) : scheduleQuery.isError ? (
        <div className="my-schedule-empty">
          <Empty description="Chưa tải được lịch cá nhân" />
          <Button onClick={() => scheduleQuery.refetch()}>Thử lại</Button>
        </div>
      ) : (
        <div className="my-schedule-week">
          {days.map((day) => {
            const appointments = appointmentsByDay.get(dayKey(day)) ?? []
            const today = day.isSame(dayjs(), 'day')
            return (
              <section key={dayKey(day)} className={`my-schedule-day${today ? ' is-today' : ''}`}>
                <header className="my-schedule-day-header">
                  <div>
                    <Typography.Text strong>{today ? 'Hôm nay' : day.format('dddd')}</Typography.Text>
                    <Typography.Text type="secondary">{day.format('DD/MM/YYYY')}</Typography.Text>
                  </div>
                  <MetaBadge tone={appointments.length ? 'info' : 'neutral'}>{appointments.length} lịch</MetaBadge>
                </header>

                {appointments.length === 0 ? (
                  <div className="my-schedule-day-empty">Không có lịch được phân công</div>
                ) : (
                  <div className="my-schedule-day-list">
                    {appointments.map((appointment) => (
                      <article key={appointment.appointmentId} className="my-schedule-card">
                        <div className="my-schedule-time">
                          <CalendarOutlined />
                          <strong>{formatTimeRange(appointment)}</strong>
                        </div>
                        <div className="my-schedule-card-main">
                          <div className="my-schedule-card-topline">
                            <Typography.Text code>{appointment.workOrderCode}</Typography.Text>
                            <div className="my-schedule-tags">
                              <PriorityTag priority={appointment.priority} />
                              <StatusTag status={appointment.status} />
                            </div>
                          </div>
                          <strong className="my-schedule-summary">{appointment.summary}</strong>
                          <div className="my-schedule-meta">
                            <span><EnvironmentOutlined /> {appointment.customerName}</span>
                            {appointment.customerAddress ? <span>{appointment.customerAddress}</span> : null}
                            {appointment.assetLabel ? <span><ToolOutlined /> {appointment.assetLabel}</span> : null}
                          </div>
                          <div className="my-schedule-card-actions">
                            <Button size="small" icon={<ArrowRightOutlined />} onClick={() => navigate(`/work-orders?open=${appointment.workOrderId}`)}>
                              Mở phiếu
                            </Button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}
