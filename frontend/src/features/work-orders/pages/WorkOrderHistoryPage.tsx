import { DeleteOutlined, DownloadOutlined, EyeOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Drawer, Empty, Input, Popconfirm, Select, Space, Table, Timeline, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { WorkOrder, WorkOrderStatus } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useAuth } from '../../auth/AuthContext'
import { workOrdersApi } from '../api'

const historyStatusOptions: Array<{ value: Extract<WorkOrderStatus, 'CLOSED' | 'CANCELLED'>; label: string }> = [
  { value: 'CLOSED', label: 'Đã đóng' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

export function WorkOrderHistoryPage() {
  const { user } = useAuth()
  const canDelete = ['OWNER', 'DISPATCHER'].includes(user?.role ?? '')
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [status, setStatus] = useState<Extract<WorkOrderStatus, 'CLOSED' | 'CANCELLED'>>()
  const [selectedId, setSelectedId] = useState<string>()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const historyQuery = useQuery({
    queryKey: ['work-order-history', { search, status, page, size: LIST_PAGE_SIZE }],
    queryFn: () => workOrdersApi.history(search, status, page, LIST_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = historyQuery

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

  const remove = useMutation({
    mutationFn: (id: string) => workOrdersApi.deleteFromHistory(id),
    onSuccess: () => {
      message.success('Đã xóa phiếu khỏi lịch sử tra cứu')
      setSelectedId(undefined)
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const exportInvoice = async (workOrder: WorkOrder) => {
    try {
      downloadBlob(await workOrdersApi.invoice(workOrder.id), `hoa-don-dich-vu-${workOrder.code}.html`)
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Lưu trữ dịch vụ"
        title="Lịch sử phiếu công việc"
        description="Tra cứu phiếu đã đóng hoặc đã hủy, xem lại tiến trình xử lý và tải hóa đơn cho phiếu đã đóng khi cần đối soát."
        meta={<MetaBadge>{historyQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} phiếu lưu trữ`}</MetaBadge>}
      />

      <div className="table-toolbar toolbar-row">
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm mã phiếu, nội dung, khách hàng, serial hoặc kỹ thuật viên"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
        <Select
          allowClear
          placeholder="Tất cả trạng thái"
          value={status}
          onChange={(value) => { setStatus(value); setPage(0) }}
          options={historyStatusOptions}
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
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        onRow={(record) => ({ onDoubleClick: () => setSelectedId(record.id) })}
        locale={{ emptyText: <Empty description={historyQuery.isError ? 'Không thể tải dữ liệu lịch sử phiếu' : 'Chưa có phiếu lịch sử phù hợp'} /> }}
        columns={[
          {
            title: 'Phiếu',
            width: 320,
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
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{record.customerName}</span>
                <Typography.Text type="secondary">{record.assetLabel || EMPTY_VALUE}</Typography.Text>
              </div>
            ),
          },
          { title: 'Kỹ thuật viên', dataIndex: 'technicianName', width: 180, render: (value) => value || EMPTY_VALUE },
          { title: 'Trạng thái', dataIndex: 'status', width: 150, render: (value) => <StatusTag status={value} /> },
          { title: 'Hoàn thành', dataIndex: 'completedAt', width: 170, render: formatDateTime },
          { title: 'Ngày tạo', dataIndex: 'createdAt', width: 170, render: formatDateTime },
          {
            title: 'Thao tác',
            width: canDelete ? 168 : 116,
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Xem chi tiết" type="text" icon={<EyeOutlined />} onClick={() => setSelectedId(record.id)} />
                {record.status === 'CLOSED' && (
                  <Button aria-label="Tải hóa đơn" type="text" icon={<DownloadOutlined />} onClick={() => exportInvoice(record)} />
                )}
                {canDelete && (
                  <Popconfirm
                    title="Xóa phiếu khỏi lịch sử?"
                    description="Phiếu chỉ được ẩn khỏi danh sách tra cứu. Dữ liệu audit và liên kết nghiệp vụ vẫn được giữ trong hệ thống."
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
        onClose={() => setSelectedId(undefined)}
        width={720}
        loading={detailLoading}
        extra={detail ? (
          <Space>
            {detail.status === 'CLOSED' && (
              <Button icon={<DownloadOutlined />} onClick={() => exportInvoice(detail)}>Xuất hóa đơn</Button>
            )}
            {canDelete && (
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
        {detail ? (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Descriptions className="detail-descriptions" column={2} bordered size="small">
              <Descriptions.Item label="Trạng thái"><StatusTag status={detail.status} /></Descriptions.Item>
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
              <h3 className="detail-section-title">Tiến trình xử lý</h3>
              {detail.history?.length ? (
                <Timeline className="detail-timeline" items={detail.history.map((item) => ({
                  color: item.toStatus === 'CANCELLED' ? '#9c5050' : item.toStatus === 'CLOSED' || item.toStatus === 'COMPLETED' ? '#4b7968' : '#47789f',
                  children: (
                    <div className="timeline-entry">
                      <div className="timeline-entry-head"><StatusTag status={item.toStatus} /><Typography.Text className="timeline-actor">{item.changedBy}</Typography.Text></div>
                      <div className="timeline-note">{item.note ?? 'Không có ghi chú'}</div>
                      <Typography.Text className="timeline-time">{formatDateTime(item.createdAt)}</Typography.Text>
                    </div>
                  ),
                }))} />
              ) : <Empty description="Chưa có lịch sử trạng thái" />}
            </section>
          </Space>
        ) : null}
      </Drawer>
    </div>
  )
}
