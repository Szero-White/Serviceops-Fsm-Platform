import { CheckCircleOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Input, Popconfirm, Space, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { MetaBadge } from '../../../components/PresentationBadge'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { businessCodeLabel } from '../../../presentation/businessText'
import type { Payment, PaymentStatus } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { formatCurrency, formatDateTime } from '../../../utils/format'
import { useAuth } from '../../auth/AuthContext'
import { workOrdersApi } from '../../work-orders/api'
import { paymentsApi } from '../api'
import { resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'

const PAYMENT_STATUSES: PaymentStatus[] = [
  'UNPAID',
  'TRANSFER_PENDING_VERIFICATION',
  'CASH_PENDING_HANDOVER',
  'COUNTER_PAYMENT_PENDING',
  'SETTLED',
]

const STATUS_OPTIONS = PAYMENT_STATUSES.map((value) => ({
  value,
  label: businessCodeLabel(value, 'Trạng thái khác'),
}))

function statusLabel(status: PaymentStatus) {
  return businessCodeLabel(status, 'Trạng thái khác')
}

export function PaymentQueuePage() {
  const { user } = useAuth()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const [searchInput, setSearchInput] = useState(() => searchParams.get('workOrder') ?? '')
  const search = useDebouncedValue(searchInput.trim())
  const [statuses, setStatuses] = useState<PaymentStatus[]>([])
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>({ sortBy: 'updatedAt', sortDir: 'desc' })
  const query = useQuery({
    queryKey: ['payments', { search, statuses, page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => paymentsApi.list({ search, statuses, page, size: LIST_PAGE_SIZE, sortBy: sort.sortBy, sortDir: sort.sortDir }),
    placeholderData: keepPreviousData,
  })
  const data = query.data
  const summaryQuery = useQuery({
    queryKey: ['payments', 'summary'],
    queryFn: paymentsApi.summary,
  })
  const summary = summaryQuery.data
  const pendingReconciliationCount = summaryQuery.isPending ? '…' : summaryQuery.isError ? '—' : (summary?.pendingReconciliationCount ?? 0)
  const pendingClosureCount = summaryQuery.isPending ? '…' : summaryQuery.isError ? '—' : (summary?.pendingClosureCount ?? 0)

  useEffect(() => setPage(0), [search, statuses])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0))
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['payments'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-payment'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }
  const issueReceipt = useMutation({
    mutationFn: (payment: Payment) => paymentsApi.issueReceipt(payment.workOrderId).then((blob) => ({ payment, blob })),
    onSuccess: ({ payment, blob }) => {
      downloadBlob(blob, `bien-nhan-thanh-toan-${payment.workOrderCode}.html`)
      message.success('Đã phát hành biên nhận thanh toán')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const downloadReceipt = useMutation({
    mutationFn: (payment: Payment) => paymentsApi.downloadReceipt(payment.workOrderId).then((blob) => ({ payment, blob })),
    onSuccess: ({ payment, blob }) => downloadBlob(blob, `bien-nhan-thanh-toan-${payment.workOrderCode}.html`),
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const closeWorkOrder = useMutation({
    mutationFn: (payment: Payment) => workOrdersApi.close(payment.workOrderId).then(() => payment),
    onSuccess: (payment) => {
      message.success(`Đã đóng ${payment.workOrderCode}`)
      refresh()
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      queryClient.invalidateQueries({ queryKey: ['work-order', payment.workOrderId] })
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-timeline', payment.workOrderId] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })
  const canSettle = user?.role === 'CUSTOMER_SERVICE'
  const openReconciliation = (payment: Payment) => {
    navigate(`/work-orders?open=${encodeURIComponent(payment.workOrderId)}&tab=payment&from=payments`)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Đối soát dịch vụ"
        title="Cần xử lý thanh toán"
        description="Theo dõi khoản chưa thanh toán, chuyển khoản chờ xác minh, tiền mặt chờ bàn giao hoặc khách hẹn thanh toán trực tiếp tại quầy."
        meta={<>
          <MetaBadge tone="warning">{data?.totalElements ?? 0} khoản</MetaBadge>
          <MetaBadge tone={(summary?.pendingReconciliationCount ?? 0) > 0 ? 'warning' : 'neutral'}>
            Chưa đối soát thanh toán: {pendingReconciliationCount}
          </MetaBadge>
          <MetaBadge tone={(summary?.pendingClosureCount ?? 0) > 0 ? 'warning' : 'neutral'}>
            Chưa đóng phiếu: {pendingClosureCount}
          </MetaBadge>
          {user?.role === 'OWNER' ? <MetaBadge>Chế độ giám sát</MetaBadge> : null}
        </>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phiếu, khách hàng hoặc kỹ thuật viên" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <CheckboxFilterSelect placeholder="Tất cả trạng thái" ariaLabel="Lọc trạng thái thanh toán" value={statuses} onChange={(value) => setStatuses(value as PaymentStatus[])} options={STATUS_OPTIONS} minWidth={250} />
      </div>

      {query.isError ? <QueryErrorAlert title="Chưa tải được hàng đợi thanh toán" error={query.error} onRetry={() => query.refetch()} /> : null}

      <Table<Payment>
        rowKey="id"
        loading={query.isLoading || query.isFetching}
        dataSource={query.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1180 }}
        pagination={{ current: page + 1, pageSize: LIST_PAGE_SIZE, total: query.isError ? 0 : (data?.totalElements ?? 0), showSizeChanger: false }}
        onChange={(pagination, _filters, sorter) => {
          setPage(Math.max((pagination.current ?? 1) - 1, 0))
          const nextSort = resolveTableSort(sorter, { sortBy: 'updatedAt', sortDir: 'desc' })
          if (nextSort.sortBy !== sort.sortBy || nextSort.sortDir !== sort.sortDir) {
            setSort(nextSort)
            setPage(0)
          }
        }}
        columns={[
          { title: 'Phiếu', width: 170, ...serverSortable(sort, 'workOrderCode'), render: (_, payment) => <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/work-orders?open=${encodeURIComponent(payment.workOrderId)}`)}>{payment.workOrderCode}</Button> },
          { title: 'Khách hàng', width: 210, dataIndex: 'customerName', ...serverSortable(sort, 'customerName') },
          { title: 'Số tiền', width: 150, align: 'right' as const, ...serverSortable(sort, 'amount'), render: (_, payment) => <Typography.Text strong>{formatCurrency(payment.amount)}</Typography.Text> },
          { title: 'Kỹ thuật viên', width: 190, dataIndex: 'technicianName', ...serverSortable(sort, 'technicianName') },
          { title: 'Trạng thái tiền', width: 220, ...serverSortable(sort, 'status'), render: (_, payment) => <MetaBadge tone={payment.status === 'SETTLED' ? 'success' : 'warning'}>{statusLabel(payment.status)}</MetaBadge> },
          { title: 'Cập nhật', width: 170, ...serverSortable(sort, 'updatedAt'), render: (_, payment) => formatDateTime(payment.updatedAt) },
          {
            title: 'Xử lý', width: 220, fixed: 'right',
            render: (_, payment) => canSettle && ['TRANSFER_PENDING_VERIFICATION', 'CASH_PENDING_HANDOVER', 'COUNTER_PAYMENT_PENDING'].includes(payment.status) ? (
              <Button size="small" type="primary" icon={<SearchOutlined />} onClick={() => openReconciliation(payment)}>
                Đối soát thanh toán
              </Button>
            ) : canSettle && payment.status === 'SETTLED' ? (
              <Space size={8} wrap>
                {payment.workOrderStatus === 'CLOSED' ? (
                  <Button size="small" icon={<DownloadOutlined />} loading={downloadReceipt.isPending} onClick={() => downloadReceipt.mutate(payment)}>Tải biên nhận</Button>
                ) : (
                  <Button size="small" icon={<CheckCircleOutlined />} loading={issueReceipt.isPending} onClick={() => issueReceipt.mutate(payment)}>Phát hành / tải biên nhận</Button>
                )}
                {payment.workOrderStatus === 'CUSTOMER_ACCEPTED' ? (
                  <Popconfirm
                    title="Đóng phiếu công việc?"
                    description="Thanh toán đã được đối soát. Phiếu sẽ chuyển sang lịch sử; vật tư dư vẫn có thể được kho nhận hoàn trả sau."
                    okText="Đóng phiếu"
                    cancelText="Chưa"
                    onConfirm={() => closeWorkOrder.mutate(payment)}
                  >
                    <Button size="small" type="primary" loading={closeWorkOrder.isPending}>Đóng phiếu</Button>
                  </Popconfirm>
                ) : payment.workOrderStatus === 'CLOSED' ? (
                  <Typography.Text type="secondary">Đã đóng phiếu</Typography.Text>
                ) : null}
              </Space>
            ) : payment.status === 'SETTLED' && user?.role === 'OWNER' ? (
              payment.workOrderStatus === 'CLOSED' ? (
                <Button size="small" icon={<DownloadOutlined />} loading={downloadReceipt.isPending} onClick={() => downloadReceipt.mutate(payment)}>Tải biên nhận</Button>
              ) : <Typography.Text type="secondary">Chờ chăm sóc khách hàng hoàn tất hồ sơ</Typography.Text>
            ) : payment.status === 'UNPAID' ? (
              <Typography.Text type="secondary">Chờ khách thanh toán</Typography.Text>
            ) : payment.status === 'COUNTER_PAYMENT_PENDING' ? (
              <Typography.Text type="secondary">Chờ chăm sóc khách hàng thu tại quầy</Typography.Text>
            ) : ['TRANSFER_PENDING_VERIFICATION', 'CASH_PENDING_HANDOVER'].includes(payment.status) ? (
              <Typography.Text type="secondary">Chờ chăm sóc khách hàng đối soát</Typography.Text>
            ) : <Typography.Text type="secondary">Không cần xử lý</Typography.Text>,
          },
        ]}
      />
    </div>
  )
}
