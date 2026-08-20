import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Select } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { WorkOrderStatus } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { formatQuantity } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { assetsApi } from '../../assets/api'
import { attachmentsApi } from '../../attachments/api'
import { useAuth } from '../../auth/AuthContext'
import { customersApi } from '../../customers/api'
import { inventoryApi } from '../../inventory/api'
import { techniciansApi } from '../../technicians/api'
import { workOrdersApi } from '../api'
import { WorkOrderDetailDrawer } from '../components/WorkOrderDetailDrawer'
import {
  WorkOrderDialogs,
  type CompleteWorkOrderValues,
  type ConsumePartValues,
  type CreateWorkOrderValues,
  type ScheduleWorkOrderValues,
} from '../components/WorkOrderDialogs'
import { WorkOrderTable } from '../components/WorkOrderTable'
import { availableWorkOrderTransitions, WORK_ORDER_STATUS_OPTIONS } from '../model/workOrderPresentation'
import { workOrderPermissions } from '../model/workOrderPermissions'

export function WorkOrdersPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const permissions = workOrderPermissions(user?.role)
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [status, setStatus] = useState<WorkOrderStatus>()
  const [selectedId, setSelectedId] = useState<string | undefined>(() => searchParams.get('open') ?? undefined)
  const [createOpen, setCreateOpen] = useState(false)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [completeOpen, setCompleteOpen] = useState(false)
  const [consumeOpen, setConsumeOpen] = useState(false)
  const [createForm] = Form.useForm<CreateWorkOrderValues>()
  const [scheduleForm] = Form.useForm<ScheduleWorkOrderValues>()
  const [completeForm] = Form.useForm<CompleteWorkOrderValues>()
  const [consumeForm] = Form.useForm<ConsumePartValues>()
  const watchedCreateCustomerId = Form.useWatch('customerId', createForm)
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const selectWorkOrder = (id?: string) => {
    setSelectedId(id)
    const next = new URLSearchParams(searchParams)
    if (id) next.set('open', id)
    else next.delete('open')
    setSearchParams(next, { replace: true })
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
  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['work-order', selectedId],
    queryFn: () => workOrdersApi.get(selectedId!),
    enabled: Boolean(selectedId),
  })
  const { data: customers } = useQuery({
    queryKey: ['customers', 'all'],
    queryFn: () => customersApi.list('', 0, 100),
    enabled: permissions.canCreate,
  })
  const { data: assets, isFetching: assetsLoading } = useQuery({
    queryKey: ['assets', 'work-order-customer', watchedCreateCustomerId],
    queryFn: () => assetsApi.list('', 0, 100, watchedCreateCustomerId),
    enabled: permissions.canCreate && Boolean(watchedCreateCustomerId),
  })
  const { data: technicians } = useQuery({
    queryKey: ['technicians'],
    queryFn: () => techniciansApi.list(),
    enabled: permissions.canSchedule,
  })
  const { data: parts } = useQuery({
    queryKey: ['spare-parts', 'all'],
    queryFn: () => inventoryApi.list('', 0, 100),
    enabled: permissions.canConsumePart,
  })
  const { data: attachments } = useQuery({
    queryKey: ['attachments', selectedId],
    queryFn: () => attachmentsApi.list('WORK_ORDER', selectedId!),
    enabled: Boolean(selectedId),
  })

  const refreshOperations = () => {
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const refreshAttachments = () => {
    queryClient.invalidateQueries({ queryKey: ['attachments', selectedId] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const create = useMutation({
    mutationFn: (values: CreateWorkOrderValues) => workOrdersApi.create(values),
    onSuccess: (workOrder) => {
      notification.success({
        message: `Đã tạo ${workOrder.code}`,
        description: workOrder.summary,
      })
      setCreateOpen(false)
      createForm.resetFields()
      refreshOperations()
      selectWorkOrder(workOrder.id)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const schedule = useMutation({
    mutationFn: (values: ScheduleWorkOrderValues) => workOrdersApi.schedule(selectedId!, {
      technicianId: values.technicianId,
      startTime: values.period[0].toISOString(),
      endTime: values.period[1].toISOString(),
    }),
    onSuccess: (workOrder, values) => {
      const technicianName = technicians?.find((technician) => technician.id === values.technicianId)?.name ?? workOrder.technicianName ?? 'Kỹ thuật viên'
      notification.success({
        message: `Đã phân công · ${workOrder.code}`,
        description: `${technicianName} · ${dayjs(values.period[0]).format('DD/MM/YYYY HH:mm')}–${dayjs(values.period[1]).format('HH:mm')}`,
      })
      setScheduleOpen(false)
      scheduleForm.resetFields()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const transition = useMutation({
    mutationFn: ({ targetStatus, note }: { targetStatus: WorkOrderStatus; note?: string }) => workOrdersApi.transition(selectedId!, { targetStatus, note }),
    onSuccess: (workOrder) => {
      const statusLabel = WORK_ORDER_STATUS_OPTIONS.find((option) => option.value === workOrder.status)?.label ?? workOrder.status
      notification.success({
        message: `Đã cập nhật ${workOrder.code}`,
        description: `Trạng thái hiện tại: ${statusLabel}.`,
      })
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const complete = useMutation({
    mutationFn: (values: CompleteWorkOrderValues) => workOrdersApi.transition(selectedId!, { targetStatus: 'COMPLETED', ...values }),
    onSuccess: (workOrder) => {
      notification.success({
        message: `Đã hoàn thành ${workOrder.code}`,
        description: workOrder.summary,
      })
      setCompleteOpen(false)
      completeForm.resetFields()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const consume = useMutation({
    mutationFn: (values: ConsumePartValues) => workOrdersApi.consumePart(selectedId!, values),
    onSuccess: (part, values) => {
      notification.success({
        message: `Đã ghi nhận phụ tùng · ${part.sku}`,
        description: `${part.name} · Đã dùng ${formatQuantity(values.quantity)} ${part.unit} · Tồn còn ${formatQuantity(part.stockQuantity)} ${part.unit}${detail?.code ? ` · ${detail.code}` : ''}.`,
      })
      setConsumeOpen(false)
      consumeForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const uploadFile = async (options: UploadRequestOption) => {
    try {
      await attachmentsApi.upload('WORK_ORDER', selectedId!, options.file as File)
      message.success('Đã tải file lên')
      options.onSuccess?.({})
      refreshAttachments()
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  const exportInvoice = async () => {
    if (!detail) return
    try {
      downloadBlob(await workOrdersApi.invoice(detail.id), `hoa-don-dich-vu-${detail.code}.html`)
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const openSchedule = () => {
    if (!detail) return
    scheduleForm.setFieldsValue({
      technicianId: detail.technicianId,
      period: detail.scheduledStart && detail.scheduledEnd
        ? [dayjs(detail.scheduledStart), dayjs(detail.scheduledEnd)]
        : undefined,
    })
    setScheduleOpen(true)
  }

  const transitions = useMemo(
    () => detail ? availableWorkOrderTransitions(detail.status) : [],
    [detail],
  )

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Bảng điều phối"
        title="Phiếu công việc"
        description="Điều phối, theo dõi trạng thái, lịch kỹ thuật viên, phụ tùng và bằng chứng hoàn thành."
        actions={permissions.canCreate ? (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              createForm.setFieldsValue({ priority: 'NORMAL' })
              setCreateOpen(true)
            }}
          >
            Tạo phiếu công việc
          </Button>
        ) : undefined}
        meta={<><MetaBadge>{workOrdersQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} phiếu`}</MetaBadge><MetaBadge tone={status ? 'info' : 'neutral'}>{status ? 'Đang lọc' : 'Tất cả trạng thái'}</MetaBadge></>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phiếu, nội dung, khách hàng, serial hoặc kỹ thuật viên" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={(value) => { setStatus(value); setPage(0) }} options={WORK_ORDER_STATUS_OPTIONS} />
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
        permissions={permissions}
        transitions={transitions}
        transitionPending={transition.isPending}
        onClose={() => selectWorkOrder(undefined)}
        onSchedule={openSchedule}
        onComplete={() => setCompleteOpen(true)}
        onConsumePart={() => setConsumeOpen(true)}
        onTransition={(targetStatus, note) => transition.mutate({ targetStatus, note })}
        onExportInvoice={exportInvoice}
        onUpload={uploadFile}
        onAttachmentsChanged={refreshAttachments}
      />

      <WorkOrderDialogs
        create={{ open: createOpen, form: createForm, pending: create.isPending, onClose: () => setCreateOpen(false), onSubmit: (values) => create.mutate(values) }}
        schedule={{ open: scheduleOpen, form: scheduleForm, pending: schedule.isPending, onClose: () => setScheduleOpen(false), onSubmit: (values) => schedule.mutate(values) }}
        complete={{ open: completeOpen, form: completeForm, pending: complete.isPending, onClose: () => setCompleteOpen(false), onSubmit: (values) => complete.mutate(values) }}
        consume={{ open: consumeOpen, form: consumeForm, pending: consume.isPending, onClose: () => setConsumeOpen(false), onSubmit: (values) => consume.mutate(values) }}
        customers={customers}
        assets={assets}
        assetsLoading={assetsLoading}
        technicians={technicians}
        parts={parts}
      />
    </div>
  )
}
