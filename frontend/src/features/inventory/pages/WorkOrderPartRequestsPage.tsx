import { InboxOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { MetaBadge } from '../../../components/PresentationBadge'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import type { WorkOrderPartRequest, WorkOrderPartRequestStatus } from '../../../types'
import { formatDateTime, formatQuantityWithUnit } from '../../../utils/format'
import { useAuth } from '../../auth/AuthContext'
import { inventoryApi } from '../api'
import { PART_REQUEST_STATUS_LABELS, PART_REQUEST_STATUS_OPTIONS } from '../model/workOrderPartPresentation'
import { OutstandingPartsTable } from '../components/OutstandingPartsTable'

function requestTone(status: WorkOrderPartRequestStatus) {
  if (status === 'REQUESTED') return 'warning' as const
  if (status === 'ISSUED') return 'success' as const
  if (status === 'UNAVAILABLE') return 'danger' as const
  return 'neutral' as const
}

export function WorkOrderPartRequestsPage() {
  const { user } = useAuth()
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput.trim())
  const [status, setStatus] = useState<WorkOrderPartRequestStatus | undefined>('REQUESTED')
  const [page, setPage] = useState(0)
  const [unavailableRequest, setUnavailableRequest] = useState<WorkOrderPartRequest>()
  const [form] = Form.useForm<{ reason: string }>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const canFulfill = user?.role === 'WAREHOUSE_STAFF'

  const requestsQuery = useQuery({
    queryKey: ['part-requests', { status, search, page, size: LIST_PAGE_SIZE }],
    queryFn: () => inventoryApi.partRequests({ status, search, page, size: LIST_PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
  const data = requestsQuery.data

  useEffect(() => setPage(0), [search, status])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0))
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['part-requests'] })
    queryClient.invalidateQueries({ queryKey: ['part-outstanding'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-part-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-part-usage'] })
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const issue = useMutation({
    mutationFn: (request: WorkOrderPartRequest) => inventoryApi.issuePartRequest(request.id),
    onSuccess: (request) => {
      notification.success({
        message: `Đã cấp phụ tùng · ${request.sparePartSku}`,
        description: `${request.workOrderCode} · ${formatQuantityWithUnit(request.issuedQuantity ?? request.requestedQuantity, request.unit)} đã bàn giao cho ${request.receivedByDisplayName ?? 'kỹ thuật viên được phân công'}.`,
      })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const unavailable = useMutation({
    mutationFn: (reason: string) => inventoryApi.markPartRequestUnavailable(unavailableRequest!.id, reason),
    onSuccess: (request) => {
      notification.success({
        message: `Đã ghi nhận không thể cấp · ${request.sparePartSku}`,
        description: `${request.workOrderCode} · Yêu cầu được giữ trong lịch sử, không có biến động tồn kho.`,
      })
      setUnavailableRequest(undefined)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Kho phụ tùng"
        title="Yêu cầu phụ tùng"
        description="Xử lý đúng các yêu cầu đang chờ cấp. Tồn kho chỉ giảm khi nhân viên kho xác nhận đã giao phụ tùng thực tế cho kỹ thuật viên."
        meta={<><MetaBadge tone="warning">{status === 'REQUESTED' ? `${data?.totalElements ?? 0} đang chờ` : `${data?.totalElements ?? 0} yêu cầu`}</MetaBadge>{!canFulfill ? <MetaBadge>Chế độ giám sát</MetaBadge> : null}</>}
      />

      <div className="table-toolbar toolbar-row">
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm mã phiếu, phụ tùng hoặc người yêu cầu"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
        <Select<WorkOrderPartRequestStatus>
          allowClear
          placeholder="Tất cả trạng thái"
          value={status}
          onChange={setStatus}
          style={{ minWidth: 190 }}
          options={PART_REQUEST_STATUS_OPTIONS}
        />
      </div>

      {requestsQuery.isError ? (
        <QueryErrorAlert title="Chưa tải được hàng đợi phụ tùng" error={requestsQuery.error} onRetry={() => requestsQuery.refetch()} />
      ) : null}

      <Table<WorkOrderPartRequest>
        rowKey="id"
        loading={requestsQuery.isLoading || requestsQuery.isFetching}
        dataSource={requestsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1260 }}
        pagination={{ current: page + 1, pageSize: LIST_PAGE_SIZE, total: requestsQuery.isError ? 0 : (data?.totalElements ?? 0), showSizeChanger: false }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description={status === 'REQUESTED' ? 'Không có yêu cầu nào đang chờ cấp' : 'Không có yêu cầu phù hợp'} /> }}
        columns={[
          {
            title: 'Phiếu công việc', width: 210,
            render: (_, request) => <div className="table-primary-cell"><Typography.Text code>{request.workOrderCode}</Typography.Text><Typography.Text type="secondary" ellipsis={{ tooltip: request.workOrderSummary }}>{request.workOrderSummary}</Typography.Text></div>,
          },
          { title: 'Phụ tùng', width: 240, render: (_, request) => <div className="table-primary-cell"><Typography.Text strong>{request.sparePartName}</Typography.Text><Typography.Text type="secondary" code>{request.sparePartSku}</Typography.Text></div> },
          { title: 'Số lượng', width: 125, render: (_, request) => formatQuantityWithUnit(request.requestedQuantity, request.unit) },
          { title: 'Người yêu cầu', width: 190, dataIndex: 'requestedByDisplayName' },
          { title: 'Mục đích', width: 260, dataIndex: 'note', ellipsis: true },
          { title: 'Trạng thái', width: 130, render: (_, request) => <MetaBadge tone={requestTone(request.status)}>{PART_REQUEST_STATUS_LABELS[request.status]}</MetaBadge> },
          { title: 'Thời gian', width: 170, render: (_, request) => formatDateTime(request.resolvedAt || request.issuedAt || request.requestedAt) },
          {
            title: 'Thao tác', width: 220, fixed: 'right',
            render: (_, request) => canFulfill && request.status === 'REQUESTED' ? (
              <Space size={6}>
                <Popconfirm
                  title="Xác nhận đã cấp phụ tùng?"
                  description={`Kho sẽ giảm ${formatQuantityWithUnit(request.requestedQuantity, request.unit)} và ghi nhận bàn giao cho kỹ thuật viên.`}
                  okText="Xác nhận cấp"
                  cancelText="Hủy"
                  onConfirm={() => issue.mutate(request)}
                >
                  <Button size="small" type="primary" icon={<InboxOutlined />} loading={issue.isPending}>Xác nhận cấp</Button>
                </Popconfirm>
                <Button size="small" danger icon={<StopOutlined />} onClick={() => { setUnavailableRequest(request); form.resetFields() }}>Không thể cấp</Button>
              </Space>
            ) : null,
          },
        ]}
      />

      <OutstandingPartsTable search={search} />

      <Modal
        title={`Không thể cấp · ${unavailableRequest?.sparePartSku ?? ''}`}
        open={Boolean(unavailableRequest)}
        onCancel={() => { setUnavailableRequest(undefined); form.resetFields() }}
        onOk={() => form.submit()}
        okText="Xác nhận không thể cấp"
        okButtonProps={{ danger: true }}
        confirmLoading={unavailable.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(values) => unavailable.mutate(values.reason.trim())} requiredMark>
          <Form.Item
            label="Lý do"
            name="reason"
            rules={[
              { required: true, whitespace: true, message: 'Nhập lý do không thể cấp' },
              { max: 500, message: 'Lý do tối đa 500 ký tự' },
            ]}
          >
            <Input.TextArea rows={4} maxLength={500} showCount placeholder="Nhập tình trạng thực tế, ví dụ: tồn kho hiện tại không đủ số lượng để cấp." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
