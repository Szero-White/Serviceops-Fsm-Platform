import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Select } from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { MetaBadge } from '../../../components/PresentationBadge'
import type { WorkOrderStatus } from '../../../types'
import { downloadBlob } from '../../../utils/download'
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
  const permissions = workOrderPermissions(user?.role)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<WorkOrderStatus>()
  const [selectedId, setSelectedId] = useState<string>()
  const [createOpen, setCreateOpen] = useState(false)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [completeOpen, setCompleteOpen] = useState(false)
  const [consumeOpen, setConsumeOpen] = useState(false)
  const [createForm] = Form.useForm<CreateWorkOrderValues>()
  const [scheduleForm] = Form.useForm<ScheduleWorkOrderValues>()
  const [completeForm] = Form.useForm<CompleteWorkOrderValues>()
  const [consumeForm] = Form.useForm<ConsumePartValues>()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['work-orders', search, status],
    queryFn: () => workOrdersApi.list(search, status),
  })
  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['work-order', selectedId],
    queryFn: () => workOrdersApi.get(selectedId!),
    enabled: Boolean(selectedId),
  })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })
  const { data: assets } = useQuery({ queryKey: ['assets', 'all'], queryFn: () => assetsApi.list('', 0, 300) })
  const { data: technicians } = useQuery({ queryKey: ['technicians'], queryFn: () => techniciansApi.list() })
  const { data: parts } = useQuery({ queryKey: ['spare-parts', 'all'], queryFn: () => inventoryApi.list('', 0, 300) })
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
      message.success(`Đã tạo ${workOrder.code}`)
      setCreateOpen(false)
      createForm.resetFields()
      refreshOperations()
      setSelectedId(workOrder.id)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const schedule = useMutation({
    mutationFn: (values: ScheduleWorkOrderValues) => workOrdersApi.schedule(selectedId!, {
      technicianId: values.technicianId,
      startTime: values.period[0].toISOString(),
      endTime: values.period[1].toISOString(),
    }),
    onSuccess: () => {
      message.success('Đã phân công và xếp lịch')
      setScheduleOpen(false)
      scheduleForm.resetFields()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const transition = useMutation({
    mutationFn: ({ targetStatus, note }: { targetStatus: WorkOrderStatus; note?: string }) => workOrdersApi.transition(selectedId!, { targetStatus, note }),
    onSuccess: () => {
      message.success('Đã cập nhật trạng thái')
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const complete = useMutation({
    mutationFn: (values: CompleteWorkOrderValues) => workOrdersApi.transition(selectedId!, { targetStatus: 'COMPLETED', ...values }),
    onSuccess: () => {
      message.success('Đã hoàn thành công việc')
      setCompleteOpen(false)
      completeForm.resetFields()
      refreshOperations()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const consume = useMutation({
    mutationFn: (values: ConsumePartValues) => workOrdersApi.consumePart(selectedId!, values),
    onSuccess: () => {
      message.success('Đã ghi nhận phụ tùng sử dụng')
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
        meta={<><MetaBadge>{data?.totalElements ?? 0} phiếu</MetaBadge><MetaBadge tone={status ? 'info' : 'neutral'}>{status ? 'Đang lọc' : 'Tất cả trạng thái'}</MetaBadge></>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phiếu, nội dung, khách hàng hoặc serial" value={search} onChange={(event) => setSearch(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={setStatus} options={WORK_ORDER_STATUS_OPTIONS} />
      </div>

      <WorkOrderTable workOrders={data?.content ?? []} loading={isLoading} onSelect={setSelectedId} />

      <WorkOrderDetailDrawer
        workOrder={detail}
        attachments={attachments}
        open={Boolean(selectedId)}
        loading={detailLoading}
        permissions={permissions}
        transitions={transitions}
        transitionPending={transition.isPending}
        onClose={() => setSelectedId(undefined)}
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
        technicians={technicians}
        parts={parts}
      />
    </div>
  )
}
