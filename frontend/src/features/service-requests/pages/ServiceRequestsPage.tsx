import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Input, Space, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { MetaBadge } from '../../../components/PresentationBadge'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import type { ServiceRequest } from '../../../types'
import type { TableSortState } from '../../../utils/tableSort'
import { useAuth } from '../../auth/AuthContext'
import { serviceChannelsApi } from '../../service-channels/api'
import { serviceRequestsApi } from '../api'
import { ServiceRequestConversionModal } from '../components/ServiceRequestConversionModal'
import { ServiceRequestFormModal } from '../components/ServiceRequestFormModal'
import { ServiceRequestTable } from '../components/ServiceRequestTable'

const REQUEST_STATUS_OPTIONS = [
  { value: 'OPEN', label: 'Chờ chuyển điều phối' },
  { value: 'CONVERTED', label: 'Đã chuyển điều phối' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

const DEFAULT_SORT: TableSortState = { sortBy: 'createdAt', sortDir: 'desc' }

export function ServiceRequestsPage() {
  const { message, notification } = App.useApp()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>(DEFAULT_SORT)
  const [statuses, setStatuses] = useState<string[]>(['OPEN'])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<ServiceRequest>()
  const [pendingConversion, setPendingConversion] = useState<ServiceRequest>()
  const search = useDebouncedValue(searchInput.trim())
  const canConvert = user ? ['OWNER', 'CUSTOMER_SERVICE'].includes(user.role) : false

  const serviceRequestsQuery = useQuery({
    queryKey: ['service-requests', { search, statuses, page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => serviceRequestsApi.list(search, statuses, page, LIST_PAGE_SIZE, sort.sortBy, sort.sortDir),
    placeholderData: keepPreviousData,
  })
  const channelsQuery = useQuery({ queryKey: ['service-channels'], queryFn: () => serviceChannelsApi.list(false) })
  const channelMap = useMemo(() => new Map((channelsQuery.data ?? []).map((channel) => [channel.code, channel])), [channelsQuery.data])
  const { data, isLoading, isFetching } = serviceRequestsQuery

  useEffect(() => setPage(0), [search])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const convert = useMutation({
    mutationFn: serviceRequestsApi.convert,
    onSuccess: (workOrder) => {
      setPendingConversion(undefined)
      notification.success({
        message: `Đã chuyển sang điều phối · ${workOrder.code}`,
        description: `${workOrder.summary} đã được tạo thành phiếu công việc và đưa vào hàng chờ điều phối.`,
      })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const cancel = useMutation({
    mutationFn: serviceRequestsApi.cancel,
    onSuccess: () => {
      message.success('Đã hủy yêu cầu')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    setFormOpen(true)
  }

  const showEdit = (request: ServiceRequest) => {
    setEditing(request)
    setFormOpen(true)
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(undefined)
  }

  const handleSaved = (savedRequest: ServiceRequest, wasEditing: boolean) => {
    closeForm()
    if (wasEditing) {
      message.success('Đã cập nhật yêu cầu dịch vụ')
      refresh()
      return
    }

    setPage(0)
    const notificationKey = `service-request-created-${savedRequest.id}`
    notification.success({
      key: notificationKey,
      message: 'Đã tiếp nhận yêu cầu dịch vụ',
      duration: 0,
      description: (
        <Space orientation="vertical" size={4}>
          <Typography.Text><strong>Yêu cầu:</strong> {savedRequest.title}</Typography.Text>
          <Typography.Text><strong>Khách hàng:</strong> {savedRequest.customerName}</Typography.Text>
          <Typography.Text><strong>Thiết bị:</strong> {savedRequest.assetLabel || 'Chưa xác định'}</Typography.Text>
          <Typography.Text type="secondary">Vui lòng kiểm tra lại thông tin; nếu chính xác, hãy tạo phiếu công việc để chuyển sang bộ phận Điều phối.</Typography.Text>
        </Space>
      ),
      actions: (
        <Space>
          <Button size="small" onClick={() => { notification.destroy(notificationKey); showEdit(savedRequest) }}>Kiểm tra</Button>
          <Button size="small" type="primary" onClick={() => { notification.destroy(notificationKey); setPendingConversion(savedRequest) }}>Tạo phiếu công việc</Button>
        </Space>
      ),
    })
    refresh()
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Tiếp nhận dịch vụ"
        title="Yêu cầu dịch vụ"
        description="Tiếp nhận nhu cầu khách hàng, hoàn thiện thông tin và bàn giao yêu cầu đủ điều kiện sang bộ phận điều phối."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Tiếp nhận yêu cầu</Button>}
        meta={(
          <>
            <MetaBadge>{serviceRequestsQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} yêu cầu`}</MetaBadge>
            <MetaBadge tone={statuses.length ? 'info' : 'neutral'}>{statuses.length === 1 ? REQUEST_STATUS_OPTIONS.find((option) => option.value === statuses[0])?.label : statuses.length ? `${statuses.length} trạng thái` : 'Tất cả trạng thái'}</MetaBadge>
          </>
        )}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm tiêu đề, mô tả, khách hàng hoặc số sê-ri" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <CheckboxFilterSelect placeholder="Tất cả trạng thái" ariaLabel="Lọc trạng thái yêu cầu dịch vụ" value={statuses} onChange={(value) => { setStatuses(value); setPage(0) }} options={REQUEST_STATUS_OPTIONS} />
      </div>

      {serviceRequestsQuery.isError ? (
        <QueryErrorAlert title="Chưa tải được danh sách yêu cầu dịch vụ" error={serviceRequestsQuery.error} onRetry={() => serviceRequestsQuery.refetch()} />
      ) : null}

      <ServiceRequestTable
        data={data}
        loading={isLoading || isFetching}
        hasError={serviceRequestsQuery.isError}
        page={page}
        sort={sort}
        canConvert={canConvert}
        channelMap={channelMap}
        onPageChange={setPage}
        onSortChange={setSort}
        onEdit={showEdit}
        onConvert={setPendingConversion}
        onCancel={(requestId) => cancel.mutate(requestId)}
      />

      <ServiceRequestConversionModal
        request={pendingConversion}
        loading={convert.isPending}
        onCancel={() => setPendingConversion(undefined)}
        onReview={(request) => { setPendingConversion(undefined); showEdit(request) }}
        onConfirm={(requestId) => convert.mutate(requestId)}
      />

      <ServiceRequestFormModal open={formOpen} editing={editing} onClose={closeForm} onSaved={handleSaved} />
    </div>
  )
}
