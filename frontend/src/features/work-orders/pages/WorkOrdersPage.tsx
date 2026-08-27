import { SearchOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Form, Input, Select } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { WorkOrderStatus } from '../../../types'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { attachmentsApi } from '../../attachments/api'
import { useAuth } from '../../auth/AuthContext'
import { techniciansApi } from '../../technicians/api'
import { workOrdersApi } from '../api'
import { paymentsApi } from '../../payments/api'
import { WorkOrderDetailDrawer } from '../components/WorkOrderDetailDrawer'
import {
  WorkOrderDialogs,
  type CompleteWorkOrderValues,
  type ScheduleWorkOrderValues,
} from '../components/WorkOrderDialogs'
import { WorkOrderTable } from '../components/WorkOrderTable'
import { ACTIVE_WORK_ORDER_STATUS_OPTIONS, availableWorkOrderTransitions, WORK_ORDER_STATUS_OPTIONS } from '../model/workOrderPresentation'
import { workOrderPermissions } from '../model/workOrderPermissions'

export function WorkOrdersPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const permissions = workOrderPermissions(user?.role)
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [status, setStatus] = useState<WorkOrderStatus>()
  const [selectedId, setSelectedId] = useState<string | undefined>(() => searchParams.get('open') ?? undefined)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [completeOpen, setCompleteOpen] = useState(false)
  const [scheduleForm] = Form.useForm<ScheduleWorkOrderValues>()
  const [completeForm] = Form.useForm<CompleteWorkOrderValues>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const selectWorkOrder = (id?: string) => {
    setSelectedId(id)
    const next = new URLSearchParams(searchParams)
    if (id) {
      next.set('open', id)
      next.delete('tab')
      next.delete('from')
    } else {
      next.delete('open')
      next.delete('tab')
      next.delete('from')
    }
    setSearchParams(next, { replace: true })
  }

  const changeDetailTab = (tab: string) => {
    const next = new URLSearchParams(searchParams)
    next.set('tab', tab)
    setSearchParams(next, { replace: true })
  }

  const closeDetail = () => {
    if (searchParams.get('from') === 'payments') {
      navigate('/payments')
      return
    }
    selectWorkOrder(undefined)
  }

  const workOrdersQuery = useQuery({
    queryKey: ['work-orders', { search, status, page, size: LIST_PAGE_SIZE }],
    queryFn: () => workOrdersApi.list(search, status, page, LIST_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = workOrdersQuery

  useEffect(() => {
    setPage(0)
  }, [search])

  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])
  const detailQuery = useQuery({
    queryKey: ['work-order', selectedId],
    queryFn: () => workOrdersApi.get(selectedId!),
    enabled: Boolean(selectedId),
  })
  const detail = detailQuery.data
  const detailLoading = detailQuery.isLoading

  const techniciansQuery = useQuery({
    queryKey: ['technicians'],
    queryFn: () => techniciansApi.list(),
    enabled: permissions.canSchedule,
  })
  const technicians = techniciansQuery.data

  const attachmentsQuery = useQuery({
    queryKey: ['attachments', selectedId],
    queryFn: () => attachmentsApi.list('WORK_ORDER', selectedId!),
    enabled: Boolean(selectedId),
  })
  const attachments = attachmentsQuery.data

  const refreshOperations = () => {
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-timeline'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
    queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-billing'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-payment'] })
    queryClient.invalidateQueries({ queryKey: ['payments'] })
  }

  const refreshAttachments = () => {
    queryClient.invalidateQueries({ queryKey: ['attachments', selectedId] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const schedule = useMutation({
    mutationFn: (values: ScheduleWorkOrderValues) => workOrdersApi.schedule(selectedId!, {
      technicianId: values.technicianId,
      startTime: values.period[0].toISOString(),
      endTime: values.period[1].toISOString(),
      reason: values.reason?.trim() || undefined,
    }),
    onSuccess: (workOrder, values) => {
      const technicianName = technicians?.find((technician) => technician.id === values.technicianId)?.name ?? workOrder.technicianName ?? 'Kỹ thuật viên'
      const previousTechnicianName = detail?.technicianName
      const technicianChanged = Boolean(detail?.technicianId && detail.technicianId !== values.technicianId)
      const redispatched = Boolean(detail?.technicianId || detail?.scheduledStart || detail?.scheduledEnd)
      const scheduleText = `${dayjs(values.period[0]).format('DD/MM/YYYY HH:mm')}–${dayjs(values.period[1]).format('HH:mm')}`

      if (technicianChanged) {
        notification.success({
          message: `Đã điều phối lại · ${workOrder.code}`,
          description: `Đã chuyển từ ${previousTechnicianName ?? 'kỹ thuật viên trước'} sang ${technicianName} · Kỹ thuật viên hiện trường. Kỹ thuật viên mới đã được thông báo · ${scheduleText}.`,
        })
      } else if (redispatched) {
        notification.success({
          message: `Đã cập nhật lịch · ${workOrder.code}`,
          description: `${technicianName} · Kỹ thuật viên hiện trường đã nhận thông báo lịch mới · ${scheduleText}.`,
        })
      } else {
        notification.success({
          message: `Đã chuyển thông tin đến ${technicianName}`,
          description: `${workOrder.code} · Kỹ thuật viên hiện trường · ${scheduleText}. Phiếu đang chờ kỹ thuật viên tiếp nhận và bắt đầu công việc.`,
        })
      }
      setScheduleOpen(false)
      scheduleForm.resetFields()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const transition = useMutation({
    mutationFn: ({ targetStatus, note }: { targetStatus: WorkOrderStatus; note?: string }) => targetStatus === 'CUSTOMER_ACCEPTED'
      ? paymentsApi.customerAcceptance(selectedId!, note)
      : workOrdersApi.transition(selectedId!, { targetStatus, note }),
    onSuccess: (workOrder) => {
      const statusLabel = WORK_ORDER_STATUS_OPTIONS.find((option) => option.value === workOrder.status)?.label ?? workOrder.status
      if (workOrder.status === 'CUSTOMER_ACCEPTED') {
        notification.success({
          message: `Khách đã xác nhận · ${workOrder.code}`,
          description: 'Chi phí đã được khóa theo xác nhận của khách. Tiếp theo ghi nhận phương thức thanh toán; CSKH sẽ đối soát tiền về công ty.',
        })
      } else if (workOrder.status === 'CLOSED') {
        notification.success({
          message: `Đã đóng ${workOrder.code}`,
          description: 'Phiếu đã chuyển sang Lịch sử phiếu công việc.',
        })
      } else {
        notification.success({
          message: `Đã cập nhật ${workOrder.code}`,
          description: `Trạng thái hiện tại: ${statusLabel}.`,
        })
      }
      refreshOperations()
      if (workOrder.status === 'CLOSED') {
        navigate(`/work-order-history?open=${encodeURIComponent(workOrder.id)}`)
      }
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const closeComplete = () => {
    setCompleteOpen(false)
    completeForm.resetFields()
  }

  const openComplete = () => {
    if (!detail) return
    completeForm.resetFields()
    completeForm.setFieldsValue({
      diagnosis: detail.diagnosis ?? '',
      resolution: detail.resolution ?? '',
      note: undefined,
    })
    setCompleteOpen(true)
  }

  const complete = useMutation({
    mutationFn: (values: CompleteWorkOrderValues) => workOrdersApi.transition(selectedId!, { targetStatus: 'COMPLETED', ...values }),
    onSuccess: (workOrder) => {
      notification.success({
        message: `Đã hoàn thành ${workOrder.code}`,
        description: 'Kết quả đã được lưu. Kỹ thuật viên kiểm tra phụ tùng thực tế và chi phí trước khi ghi nhận khách xác nhận.',
      })
      closeComplete()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const uploadFile = async (options: UploadRequestOption) => {
    try {
      await attachmentsApi.upload('WORK_ORDER', selectedId!, options.file as File, 'WORK_EVIDENCE')
      message.success('Đã lưu hình ảnh / tài liệu vào hồ sơ công việc')
      options.onSuccess?.({})
      refreshAttachments()
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  const submitSchedule = (values: ScheduleWorkOrderValues) => {
    const sameTechnician = detail?.technicianId === values.technicianId
    const sameSchedule = Boolean(
      detail?.scheduledStart
      && detail?.scheduledEnd
      && dayjs(detail.scheduledStart).valueOf() === values.period[0].valueOf()
      && dayjs(detail.scheduledEnd).valueOf() === values.period[1].valueOf(),
    )

    if (sameTechnician && sameSchedule) {
      message.info('Chưa có thay đổi kỹ thuật viên hoặc thời gian thực hiện')
      return
    }

    schedule.mutate(values)
  }

  const openSchedule = () => {
    if (techniciansQuery.isError) {
      message.error('Chưa tải được danh sách kỹ thuật viên. Vui lòng thử lại.')
      void techniciansQuery.refetch()
      return
    }
    if (!detail) return
    scheduleForm.setFieldsValue({
      technicianId: detail.technicianId,
      period: detail.scheduledStart && detail.scheduledEnd
        ? [dayjs(detail.scheduledStart), dayjs(detail.scheduledEnd)]
        : undefined,
      reason: undefined,
    })
    setScheduleOpen(true)
  }

  const transitions = useMemo(
    () => detail ? availableWorkOrderTransitions(detail.status, user?.role) : [],
    [detail, user?.role],
  )

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Vận hành dịch vụ"
        title="Phiếu công việc"
        description="Theo dõi công việc đã được bàn giao từ Customer Service, từ điều phối đến hoàn thành."
        meta={<><MetaBadge>{workOrdersQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} phiếu`}</MetaBadge><MetaBadge tone={status ? 'info' : 'neutral'}>{status ? 'Đang lọc' : 'Tất cả trạng thái'}</MetaBadge></>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phiếu, nội dung, khách hàng, serial hoặc kỹ thuật viên" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={(value) => { setStatus(value); setPage(0) }} options={ACTIVE_WORK_ORDER_STATUS_OPTIONS} />
      </div>

      {workOrdersQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách phiếu công việc"
          error={workOrdersQuery.error}
          onRetry={() => workOrdersQuery.refetch()}
        />
      )}

      <WorkOrderTable
        workOrders={workOrdersQuery.isError ? [] : (data?.content ?? [])}
        loading={isLoading || isFetching}
        page={page}
        pageSize={LIST_PAGE_SIZE}
        total={workOrdersQuery.isError ? 0 : (data?.totalElements ?? 0)}
        onPageChange={setPage}
        onSelect={selectWorkOrder}
        loadError={workOrdersQuery.isError}
      />

      <WorkOrderDetailDrawer
        workOrder={detail}
        attachments={attachments}
        open={Boolean(selectedId)}
        loading={detailLoading}
        error={detailQuery.error}
        onRetry={() => detailQuery.refetch()}
        attachmentsError={attachmentsQuery.error}
        onRetryAttachments={() => attachmentsQuery.refetch()}
        permissions={permissions}
        transitions={transitions}
        transitionPending={transition.isPending}
        activeTabKey={searchParams.get('tab') ?? 'overview'}
        onTabChange={changeDetailTab}
        onClose={closeDetail}
        onSchedule={openSchedule}
        onComplete={openComplete}
        role={user?.role}
        onTransition={(targetStatus, note) => transition.mutate({ targetStatus, note })}
        onUpload={uploadFile}
        onAttachmentsChanged={refreshAttachments}
      />

      <WorkOrderDialogs
        schedule={{
          open: scheduleOpen,
          form: scheduleForm,
          pending: schedule.isPending,
          redispatching: Boolean(detail?.technicianId || detail?.scheduledStart || detail?.scheduledEnd),
          currentTechnicianName: detail?.technicianName,
          onClose: () => setScheduleOpen(false),
          onSubmit: submitSchedule,
        }}
        complete={{ open: completeOpen, form: completeForm, pending: complete.isPending, hasPreviousResult: Boolean(detail?.diagnosis || detail?.resolution), onClose: closeComplete, onSubmit: (values) => complete.mutate(values) }}
        technicians={technicians}
      />
    </div>
  )
}
