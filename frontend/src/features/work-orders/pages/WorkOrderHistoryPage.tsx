import { CheckCircleOutlined, DeleteOutlined, DownloadOutlined, EyeOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Drawer, Empty, Input, Popconfirm, Space, Table, Tooltip, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { WorkOrder, WorkOrderStatus } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'
import { attachmentsApi } from '../../attachments/api'
import { AttachmentList } from '../../attachments/components/AttachmentList'
import { useAuth } from '../../auth/AuthContext'
import { paymentsApi } from '../../payments/api'
import { workOrdersApi } from '../api'
import { WorkOrderActivityTimeline } from '../components/WorkOrderActivityTimeline'

const historyStatusOptions = [
  { value: 'CUSTOMER_ACCEPTED', label: 'Chờ hoàn tất hồ sơ' },
  { value: 'CLOSED', label: 'Đã đóng' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

export function WorkOrderHistoryPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const canDelete = user?.role === 'OWNER'
  const canDownloadReceipt = Boolean(user?.role && ['OWNER', 'CUSTOMER_SERVICE'].includes(user.role))
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>({ sortBy: 'createdAt', sortDir: 'desc' })
  const search = useDebouncedValue(searchInput.trim())
  const [statuses, setStatuses] = useState<WorkOrderStatus[]>([])
  const [selectedId, setSelectedId] = useState<string | undefined>(() => searchParams.get('open') ?? undefined)
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const selectHistoryWorkOrder = (id?: string) => {
    setSelectedId(id)
    const next = new URLSearchParams(searchParams)
    if (id) next.set('open', id)
    else next.delete('open')
    setSearchParams(next, { replace: true })
  }

  const historyQuery = useQuery({
    queryKey: ['work-order-history', { search, statuses, page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => workOrdersApi.history(search, statuses, page, LIST_PAGE_SIZE, sort.sortBy, sort.sortDir),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = historyQuery
  const historySummaryQuery = useQuery({
    queryKey: ['work-order-history-summary', { search }],
    queryFn: () => workOrdersApi.historySummary(search),
  })
  const historySummary = historySummaryQuery.data

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
  const attachmentsQuery = useQuery({
    queryKey: ['attachments', selectedId],
    queryFn: () => attachmentsApi.list('WORK_ORDER', selectedId!),
    enabled: Boolean(selectedId),
  })
  const workAttachments = attachmentsQuery.data?.filter((item) => item.purpose === 'WORK_EVIDENCE')

  const remove = useMutation({
    mutationFn: (id: string) => workOrdersApi.deleteFromHistory(id),
    onSuccess: () => {
      message.success('Đã xóa phiếu khỏi lịch sử tra cứu')
      selectHistoryWorkOrder(undefined)
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-history-summary'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const downloadReceipt = async (workOrder: WorkOrder) => {
    try {
      downloadBlob(await paymentsApi.downloadReceipt(workOrder.id), `bien-nhan-thanh-toan-${workOrder.code}.html`)
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Theo dõi hồ sơ dịch vụ"
        title="Lịch sử phiếu công việc"
        description="Tra cứu hồ sơ chờ hoàn tất, phiếu đã đóng hoặc đã hủy và xem lại toàn bộ tiến trình xử lý."
        meta={(
          <>
            <MetaBadge>{historySummaryQuery.isError ? 'Lỗi tải thống kê' : `${historySummary?.total ?? 0} hồ sơ`}</MetaBadge>
            <MetaBadge tone="warning">{historySummary?.pendingClosure ?? 0} chờ hoàn tất hồ sơ</MetaBadge>
            <MetaBadge tone="success">{historySummary?.closed ?? 0} đã đóng</MetaBadge>
            <MetaBadge tone="danger">{historySummary?.cancelled ?? 0} đã hủy</MetaBadge>
          </>
        )}
      />

      <div className="table-toolbar toolbar-row">
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm mã phiếu, nội dung, khách hàng, số sê-ri hoặc kỹ thuật viên"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
        <CheckboxFilterSelect
          placeholder="Tất cả trạng thái"
          ariaLabel="Lọc trạng thái lịch sử phiếu"
          value={statuses}
          onChange={(value) => { setStatuses(value as WorkOrderStatus[]); setPage(0) }}
          options={historyStatusOptions}
          minWidth={220}
        />
      </div>

      {historyQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được lịch sử phiếu công việc"
          error={historyQuery.error}
          onRetry={() => historyQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={historyQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1160 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: historyQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} phiếu`,
        }}
        onChange={(pagination, _filters, sorter) => {
          setPage(Math.max((pagination.current ?? 1) - 1, 0))
          setSort(resolveTableSort(sorter, { sortBy: 'createdAt', sortDir: 'desc' }))
        }}
        onRow={(record) => ({ onDoubleClick: () => selectHistoryWorkOrder(record.id) })}
        locale={{ emptyText: <Empty description={historyQuery.isError ? 'Không thể tải dữ liệu lịch sử phiếu' : 'Chưa có phiếu lịch sử phù hợp'} /> }}
        columns={[
          {
            title: 'Phiếu',
            width: 320,
            ...serverSortable(sort, 'code'),
            render: (_, record) => (
              <div className="work-order-ticket-cell">
                <div className="work-order-ticket-meta">
                  <span className="work-order-ticket-code">{record.code}</span>
                  <PriorityTag priority={record.priority} />
                </div>
                <Typography.Text className="work-order-ticket-title">{record.summary}</Typography.Text>
              </div>
            ),
          },
          {
            title: 'Khách hàng',
            width: 240,
            ...serverSortable(sort, 'customerName'),
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{record.customerName}</span>
                <Typography.Text type="secondary">{record.assetLabel || EMPTY_VALUE}</Typography.Text>
              </div>
            ),
          },
          { title: 'Kỹ thuật viên', dataIndex: 'technicianName', width: 180, ...serverSortable(sort, 'technicianName'), render: (value) => value || EMPTY_VALUE },
          { title: 'Trạng thái', dataIndex: 'status', width: 175, ...serverSortable(sort, 'status'), render: (value) => value === 'CUSTOMER_ACCEPTED' ? <MetaBadge tone="warning">Chờ hoàn tất hồ sơ</MetaBadge> : <StatusTag status={value} /> },
          { title: 'Hoàn thành', dataIndex: 'completedAt', width: 170, ...serverSortable(sort, 'completedAt'), render: formatDateTime },
          { title: 'Ngày tạo', dataIndex: 'createdAt', width: 170, ...serverSortable(sort, 'createdAt'), render: formatDateTime },
          {
            title: 'Thao tác',
            width: canDelete ? 168 : 116,
            fixed: 'right' as const,
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Xem chi tiết" type="text" icon={<EyeOutlined />} onClick={() => selectHistoryWorkOrder(record.id)} />
                {record.status === 'CUSTOMER_ACCEPTED' && user?.role === 'CUSTOMER_SERVICE' && (
                  <Tooltip title="Thanh toán đã đối soát. Vui lòng đóng phiếu để hoàn tất hồ sơ.">
                    <Button
                      aria-label="Hoàn tất hồ sơ"
                      type="text"
                      icon={<CheckCircleOutlined />}
                      onClick={() => navigate(`/payments?workOrder=${encodeURIComponent(record.code)}`)}
                    />
                  </Tooltip>
                )}
                {record.status === 'CLOSED' && canDownloadReceipt && (
                  <Button aria-label="Tải biên nhận" type="text" icon={<DownloadOutlined />} onClick={() => downloadReceipt(record)} />
                )}
                {canDelete && record.status !== 'CUSTOMER_ACCEPTED' && (
                  <Popconfirm
                    title="Xóa phiếu khỏi lịch sử?"
                    description="Phiếu chỉ được ẩn khỏi danh sách tra cứu. Nhật ký thay đổi và các liên kết nghiệp vụ vẫn được giữ trong hệ thống."
                    okText="Xóa"
                    cancelText="Giữ lại"
                    okButtonProps={{ danger: true, loading: remove.isPending }}
                    onConfirm={() => remove.mutate(record.id)}
                  >
                    <Button aria-label="Xóa khỏi lịch sử" type="text" danger icon={<DeleteOutlined />} />
                  </Popconfirm>
                )}
              </Space>
            ),
          },
        ]}
      />

      <Drawer
        rootClassName="serviceops-detail-drawer"
        title={detail ? (
          <div className="detail-drawer-title">
            <span className="detail-drawer-code">{detail.code}</span>
            <span className="detail-drawer-summary">{detail.summary}</span>
          </div>
        ) : 'Chi tiết phiếu lịch sử'}
        open={Boolean(selectedId)}
        onClose={() => selectHistoryWorkOrder(undefined)}
        width={720}
        loading={detailLoading}
        extra={detail ? (
          <Space>
            {detail.status === 'CLOSED' && canDownloadReceipt && (
              <Button icon={<DownloadOutlined />} onClick={() => downloadReceipt(detail)}>Tải biên nhận</Button>
            )}
            {canDelete && detail.status !== 'CUSTOMER_ACCEPTED' && (
              <Popconfirm
                title="Xóa phiếu khỏi lịch sử?"
                description="Phiếu chỉ được ẩn khỏi danh sách tra cứu."
                okText="Xóa"
                cancelText="Giữ lại"
                okButtonProps={{ danger: true, loading: remove.isPending }}
                onConfirm={() => remove.mutate(detail.id)}
              >
                <Button danger icon={<DeleteOutlined />}>Xóa khỏi lịch sử</Button>
              </Popconfirm>
            )}
          </Space>
        ) : undefined}
      >
        {detailQuery.isError ? (
          <QueryErrorAlert
            title="Chưa tải được chi tiết phiếu lịch sử"
            error={detailQuery.error}
            onRetry={() => detailQuery.refetch()}
          />
        ) : detail ? (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Descriptions className="detail-descriptions" column={2} bordered size="small">
              <Descriptions.Item label="Trạng thái">
                {detail.status === 'CUSTOMER_ACCEPTED'
                  ? <MetaBadge tone="warning">Chờ hoàn tất hồ sơ</MetaBadge>
                  : <StatusTag status={detail.status} />}
              </Descriptions.Item>
              <Descriptions.Item label="Ưu tiên"><PriorityTag priority={detail.priority} /></Descriptions.Item>
              <Descriptions.Item label="Khách hàng">{detail.customerName}</Descriptions.Item>
              <Descriptions.Item label="Thiết bị">{detail.assetLabel ?? 'Chưa xác định'}</Descriptions.Item>
              <Descriptions.Item label="Kỹ thuật viên">{detail.technicianName ?? 'Chưa phân công'}</Descriptions.Item>
              <Descriptions.Item label="Hoàn thành">{formatDateTime(detail.completedAt)}</Descriptions.Item>
              <Descriptions.Item label="Mô tả" span={2}>{detail.description ?? EMPTY_VALUE}</Descriptions.Item>
              <Descriptions.Item label="Chẩn đoán" span={2}>{detail.diagnosis ?? EMPTY_VALUE}</Descriptions.Item>
              <Descriptions.Item label="Giải pháp" span={2}>{detail.resolution ?? EMPTY_VALUE}</Descriptions.Item>
            </Descriptions>

            <section className="detail-section">
              <h3 className="detail-section-title">Hình ảnh & tài liệu</h3>
              {attachmentsQuery.isError ? (
                <QueryErrorAlert
                  title="Chưa tải được hình ảnh và tài liệu"
                  error={attachmentsQuery.error}
                  onRetry={() => attachmentsQuery.refetch()}
                />
              ) : (
                <AttachmentList attachments={workAttachments} />
              )}
            </section>

            <section className="detail-section">
              <h3 className="detail-section-title">Tiến trình xử lý</h3>
              <WorkOrderActivityTimeline
                workOrderId={detail.id}
                activities={detail.activities}
                history={detail.history}
                emptyDescription="Chưa có tiến trình xử lý"
              />
            </section>
          </Space>
        ) : null}
      </Drawer>
    </div>
  )
}
