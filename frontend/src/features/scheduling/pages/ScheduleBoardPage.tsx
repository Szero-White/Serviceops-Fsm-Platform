import {
  CalendarOutlined,
  LeftOutlined,
  ReloadOutlined,
  RightOutlined,
  ScheduleOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Spin, Tooltip, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { DispatchQueueItem, ScheduleAppointment, Technician, WorkOrderStatus } from '../../../types'
import { techniciansApi } from '../../technicians/api'
import { workOrdersApi } from '../../work-orders/api'
import { scheduleBoardApi } from '../api'
import {
  ScheduleAppointmentModal,
  type ScheduleAppointmentValues,
} from '../components/ScheduleAppointmentModal'

const DAY_COUNT = 7
const RESCHEDULABLE_STATUSES = new Set<WorkOrderStatus>(['OPEN', 'SCHEDULED', 'ASSIGNED', 'REOPENED'])

function startOfWeek(value: Dayjs) {
  const day = value.day()
  const mondayOffset = day === 0 ? -6 : 1 - day
  return value.add(mondayOffset, 'day').startOf('day')
}

function dayKey(value: Dayjs | string) {
  return dayjs(value).format('YYYY-MM-DD')
}

function timeRange(appointment: ScheduleAppointment) {
  return `${dayjs(appointment.startTime).format('HH:mm')}–${dayjs(appointment.endTime).format('HH:mm')}`
}

function isOverdueAppointment(appointment: ScheduleAppointment) {
  return RESCHEDULABLE_STATUSES.has(appointment.status) && dayjs(appointment.endTime).isBefore(dayjs())
}

export function ScheduleBoardPage() {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(dayjs()))
  const [selectedWorkOrder, setSelectedWorkOrder] = useState<{
    id: string
    code: string
    summary: string
    technicianId?: string
    technicianName?: string
    startTime?: string
    endTime?: string
    redispatching?: boolean
  }>()
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [scheduleForm] = Form.useForm<ScheduleAppointmentValues>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const weekEnd = weekStart.add(DAY_COUNT, 'day')
  const from = weekStart.toISOString()
  const to = weekEnd.toISOString()
  const days = useMemo(
    () => Array.from({ length: DAY_COUNT }, (_, index) => weekStart.add(index, 'day')),
    [weekStart],
  )

  const boardQuery = useQuery({
    queryKey: ['schedule-board', from, to],
    queryFn: () => scheduleBoardApi.get(from, to),
  })
  const techniciansQuery = useQuery({
    queryKey: ['technicians'],
    queryFn: () => techniciansApi.list(),
  })

  const appointmentsByTechnicianAndDay = useMemo(() => {
    const grouped = new Map<string, ScheduleAppointment[]>()
    for (const appointment of boardQuery.data?.appointments ?? []) {
      const appointmentStart = dayjs(appointment.startTime)
      const appointmentEnd = dayjs(appointment.endTime)
      for (const day of days) {
        const dayEnd = day.add(1, 'day')
        if (appointmentStart.isBefore(dayEnd) && appointmentEnd.isAfter(day)) {
          const key = `${appointment.technicianId}:${dayKey(day)}`
          const items = grouped.get(key) ?? []
          items.push(appointment)
          grouped.set(key, items)
        }
      }
    }
    for (const items of grouped.values()) {
      items.sort((a, b) => a.startTime.localeCompare(b.startTime))
    }
    return grouped
  }, [boardQuery.data?.appointments, days])

  const scheduleMutation = useMutation({
    mutationFn: (values: ScheduleAppointmentValues) => workOrdersApi.schedule(selectedWorkOrder!.id, {
      technicianId: values.technicianId,
      startTime: values.period[0].toISOString(),
      endTime: values.period[1].toISOString(),
      reason: selectedWorkOrder?.redispatching ? values.reason?.trim() : undefined,
    }),
    onSuccess: (_workOrder, values) => {
      const technicianName = techniciansQuery.data?.find((technician) => technician.id === values.technicianId)?.name ?? 'Kỹ thuật viên'
      const scheduleText = `${dayjs(values.period[0]).format('DD/MM/YYYY HH:mm')}–${dayjs(values.period[1]).format('HH:mm')}`
      const technicianChanged = Boolean(
        selectedWorkOrder?.redispatching
        && selectedWorkOrder.technicianId
        && selectedWorkOrder.technicianId !== values.technicianId,
      )

      notification.success({
        message: `${technicianChanged ? 'Đã điều phối lại' : selectedWorkOrder?.redispatching ? 'Đã cập nhật lịch' : 'Đã xếp lịch'} · ${selectedWorkOrder?.code ?? 'Phiếu công việc'}`,
        description: technicianChanged
          ? `${selectedWorkOrder?.technicianName ?? 'Kỹ thuật viên trước'} → ${technicianName} · ${scheduleText}`
          : `${technicianName} · ${scheduleText}`,
      })
      setScheduleOpen(false)
      setSelectedWorkOrder(undefined)
      scheduleForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
      queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      queryClient.invalidateQueries({ queryKey: ['work-order'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const openQueueItem = (item: DispatchQueueItem) => {
    setSelectedWorkOrder({ id: item.workOrderId, code: item.workOrderCode, summary: item.summary, redispatching: false })
    scheduleForm.resetFields()
    setScheduleOpen(true)
  }

  const openAppointment = (appointment: ScheduleAppointment) => {
    setSelectedWorkOrder({
      id: appointment.workOrderId,
      code: appointment.workOrderCode,
      summary: appointment.summary,
      technicianId: appointment.technicianId,
      technicianName: appointment.technicianName,
      startTime: appointment.startTime,
      endTime: appointment.endTime,
      redispatching: true,
    })
    scheduleForm.resetFields()
    scheduleForm.setFieldsValue({
      technicianId: appointment.technicianId,
      period: [dayjs(appointment.startTime), dayjs(appointment.endTime)],
    })
    setScheduleOpen(true)
  }

  const technicians = techniciansQuery.data ?? []
  const loading = boardQuery.isLoading || techniciansQuery.isLoading
  const isCurrentWeek = weekStart.isSame(startOfWeek(dayjs()), 'day')

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Điều phối hiện trường"
        title="Lịch điều phối"
        description="Theo dõi lịch đội kỹ thuật theo tuần, ưu tiên phiếu chưa xếp lịch và điều chỉnh phân công khi kế hoạch thay đổi."
        actions={(
          <Button icon={<ReloadOutlined />} onClick={() => boardQuery.refetch()} loading={boardQuery.isFetching}>
            Làm mới
          </Button>
        )}
        meta={(
          <>
            <MetaBadge tone="info">{boardQuery.data?.appointments.length ?? 0} lịch trong tuần</MetaBadge>
            <MetaBadge tone={(boardQuery.data?.dispatchQueueTotal ?? 0) > 0 ? 'warning' : 'neutral'}>
              {boardQuery.data?.dispatchQueueTotal ?? 0} phiếu chờ điều phối
            </MetaBadge>
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

      {boardQuery.isError ? (
        <QueryErrorAlert
          title="Chưa tải được lịch điều phối"
          error={boardQuery.error}
          onRetry={() => boardQuery.refetch()}
        />
      ) : null}

      {techniciansQuery.isError ? (
        <QueryErrorAlert
          title="Chưa tải được danh sách kỹ thuật viên"
          error={techniciansQuery.error}
          onRetry={() => techniciansQuery.refetch()}
        />
      ) : null}

      {boardQuery.isError || techniciansQuery.isError ? null : loading ? (
        <div className="schedule-board-loading"><Spin size="large" /></div>
      ) : (
        <div className="schedule-board-layout">
          <aside className="dispatch-queue-panel">
            <div className="dispatch-queue-header">
              <div>
                <Typography.Text strong>Chưa xếp lịch</Typography.Text>
                <Typography.Text type="secondary">Phiếu mở đang chờ phân công</Typography.Text>
              </div>
              <MetaBadge tone="warning">{boardQuery.data?.dispatchQueueTotal ?? 0}</MetaBadge>
            </div>

            <div className="dispatch-queue-list">
              {(boardQuery.data?.dispatchQueue.length ?? 0) === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không có phiếu chờ" />
              ) : boardQuery.data?.dispatchQueue.map((item) => (
                <button key={item.workOrderId} type="button" className="dispatch-queue-item" onClick={() => openQueueItem(item)}>
                  <div className="dispatch-queue-item-topline">
                    <span className="dispatch-queue-code">{item.workOrderCode}</span>
                    <PriorityTag priority={item.priority} />
                  </div>
                  <strong>{item.summary}</strong>
                  <span>{item.customerName}</span>
                  <div className="dispatch-queue-item-footer">
                    <StatusTag status={item.status} />
                    <span>{dayjs(item.createdAt).format('DD/MM HH:mm')}</span>
                  </div>
                </button>
              ))}
            </div>

            {(boardQuery.data?.dispatchQueueTotal ?? 0) > (boardQuery.data?.dispatchQueue.length ?? 0) ? (
              <Typography.Text type="secondary" className="dispatch-queue-limit-note">
                Hiển thị {boardQuery.data?.dispatchQueue.length ?? 0} phiếu ưu tiên đầu tiên.
              </Typography.Text>
            ) : null}
          </aside>

          <section className="schedule-board-panel" aria-label="Lịch kỹ thuật viên theo tuần">
            <div className="schedule-board-scroll">
              <div className="schedule-board-grid">
                <div className="schedule-board-corner">Kỹ thuật viên</div>
                {days.map((day) => (
                  <div key={dayKey(day)} className={`schedule-board-day-header${day.isSame(dayjs(), 'day') ? ' is-today' : ''}`}>
                    <span>{day.format('ddd')}</span>
                    <strong>{day.format('DD/MM')}</strong>
                  </div>
                ))}

                {technicians.map((technician) => (
                  <TechnicianScheduleRow
                    key={technician.id}
                    technician={technician}
                    days={days}
                    appointmentsByTechnicianAndDay={appointmentsByTechnicianAndDay}
                    onOpenAppointment={openAppointment}
                  />
                ))}
              </div>
            </div>

            {technicians.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có kỹ thuật viên hoạt động" />
            ) : null}
          </section>
        </div>
      )}

      <ScheduleAppointmentModal
        open={scheduleOpen}
        workOrderCode={selectedWorkOrder?.code}
        workOrderSummary={selectedWorkOrder?.summary}
        form={scheduleForm}
        technicians={technicians}
        pending={scheduleMutation.isPending}
        redispatching={Boolean(selectedWorkOrder?.redispatching)}
        onClose={() => {
          setScheduleOpen(false)
          setSelectedWorkOrder(undefined)
          scheduleForm.resetFields()
        }}
        onSubmit={(values) => scheduleMutation.mutate(values)}
      />
    </div>
  )
}

function TechnicianScheduleRow({
  technician,
  days,
  appointmentsByTechnicianAndDay,
  onOpenAppointment,
}: {
  technician: Technician
  days: Dayjs[]
  appointmentsByTechnicianAndDay: Map<string, ScheduleAppointment[]>
  onOpenAppointment: (appointment: ScheduleAppointment) => void
}) {
  return (
    <>
      <div className="schedule-board-technician">
        <div className="schedule-board-technician-icon"><ScheduleOutlined /></div>
        <div>
          <strong>{technician.name}</strong>
          <span>{technician.skills || 'Chưa khai báo kỹ năng'}</span>
        </div>
      </div>
      {days.map((day) => {
        const appointments = appointmentsByTechnicianAndDay.get(`${technician.id}:${dayKey(day)}`) ?? []
        return (
          <div key={`${technician.id}:${dayKey(day)}`} className={`schedule-board-cell${day.isSame(dayjs(), 'day') ? ' is-today' : ''}`}>
            {appointments.length === 0 ? (
              <span className="schedule-board-empty-slot" aria-label="Không có lịch" />
            ) : appointments.map((appointment) => {
              const overdue = isOverdueAppointment(appointment)
              return (
                <Tooltip
                  key={appointment.appointmentId}
                  title={overdue
                    ? 'Lịch hẹn đã qua nhưng công việc chưa bắt đầu. Bấm để điều phối lại.'
                    : 'Bấm để đổi kỹ thuật viên hoặc thời gian'}
                >
                  <button
                    type="button"
                    className={`schedule-appointment-card${overdue ? ' is-overdue' : ''}`}
                    onClick={() => onOpenAppointment(appointment)}
                  >
                    <div className="schedule-appointment-topline">
                      <span>{timeRange(appointment)}</span>
                      <div className="schedule-appointment-badges">
                        {overdue ? <span className="schedule-overdue-badge">Quá hạn</span> : null}
                        <PriorityTag priority={appointment.priority} />
                      </div>
                    </div>
                    <strong>{appointment.workOrderCode}</strong>
                    <span className="schedule-appointment-summary">{appointment.summary}</span>
                    <span className="schedule-appointment-customer">{appointment.customerName}</span>
                  </button>
                </Tooltip>
              )
            })}
          </div>
        )
      })}
    </>
  )
}
