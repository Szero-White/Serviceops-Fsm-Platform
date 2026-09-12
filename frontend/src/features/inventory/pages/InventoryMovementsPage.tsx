import { SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { DatePicker, Empty, Input, Table, Typography } from 'antd'
import type { Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { actorRoleLabel } from '../../../constants/userRoles'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import type { InventoryTransaction, InventoryTransactionType } from '../../../types'
import { formatDateTime, formatQuantityWithUnit } from '../../../utils/format'
import { resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'
import { inventoryApi } from '../api'

const { RangePicker } = DatePicker

const TYPE_LABELS: Record<InventoryTransactionType, string> = {
  IMPORT: 'Nhập kho',
  ISSUE: 'Cấp cho kỹ thuật viên',
  CONSUME: 'Sử dụng (legacy)',
  RETURN: 'Hoàn trả',
  ADJUSTMENT_IN: 'Điều chỉnh tăng',
  ADJUSTMENT_OUT: 'Điều chỉnh giảm',
}

function isIncrease(type: InventoryTransactionType) {
  return type === 'IMPORT' || type === 'RETURN' || type === 'ADJUSTMENT_IN'
}

export function InventoryMovementsPage() {
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput.trim())
  const [types, setTypes] = useState<InventoryTransactionType[]>([])
  const [period, setPeriod] = useState<[Dayjs, Dayjs] | null>(null)
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>({ sortBy: 'createdAt', sortDir: 'desc' })

  const transactionsQuery = useQuery({
    queryKey: ['inventory-transactions', { search, types, period: period?.map((value) => value.toISOString()), page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => inventoryApi.transactions({
      search,
      types,
      fromTime: period?.[0].startOf('day').toISOString(),
      toTime: period?.[1].endOf('day').toISOString(),
      page,
      size: LIST_PAGE_SIZE,
      sortBy: sort.sortBy,
      sortDir: sort.sortDir,
    }),
    placeholderData: keepPreviousData,
  })
  const data = transactionsQuery.data

  useEffect(() => setPage(0), [search, types, period])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0))
  }, [data, page])

  return (
    <div className="page-stack">
      <PageHeader
        title="Lịch sử biến động kho"
        description="Sổ giao dịch chỉ dùng để truy vết hàng thực tế ra/vào kho. Nghiệp vụ hoàn trả được xử lý tại Yêu cầu phụ tùng → Vật tư đang do kỹ thuật viên giữ."
        meta={<><MetaBadge>{data?.totalElements ?? 0} giao dịch</MetaBadge><MetaBadge tone="info">Theo thời gian thực</MetaBadge></>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm SKU, tên, mã WO, KTV nhận / trả, người thực hiện hoặc mục đích" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <CheckboxFilterSelect placeholder="Loại giao dịch" ariaLabel="Lọc loại giao dịch kho" value={types} onChange={(value) => setTypes(value as InventoryTransactionType[])} minWidth={220} options={Object.entries(TYPE_LABELS).map(([value, label]) => ({ value, label }))} />
        <RangePicker value={period} onChange={(value) => setPeriod(value as [Dayjs, Dayjs] | null)} format="DD/MM/YYYY" />
      </div>

      {transactionsQuery.isError && <QueryErrorAlert title="Chưa tải được lịch sử kho" error={transactionsQuery.error} onRetry={() => transactionsQuery.refetch()} />}

      <Table<InventoryTransaction>
        rowKey="id"
        loading={transactionsQuery.isLoading || transactionsQuery.isFetching}
        dataSource={transactionsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1560 }}
        pagination={{ current: page + 1, pageSize: LIST_PAGE_SIZE, total: transactionsQuery.isError ? 0 : (data?.totalElements ?? 0), showSizeChanger: false }}
        onChange={(pagination, _filters, sorter) => {
          setPage(Math.max((pagination.current ?? 1) - 1, 0))
          const nextSort = resolveTableSort(sorter, { sortBy: 'createdAt', sortDir: 'desc' })
          if (nextSort.sortBy !== sort.sortBy || nextSort.sortDir !== sort.sortDir) {
            setSort(nextSort)
            setPage(0)
          }
        }}
        locale={{ emptyText: <Empty description="Chưa có giao dịch kho phù hợp" /> }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 165, ...serverSortable(sort, 'createdAt'), render: formatDateTime },
          { title: 'Loại', width: 150, ...serverSortable(sort, 'type'), render: (_, record) => <MetaBadge tone={isIncrease(record.type) ? 'success' : ['ISSUE', 'CONSUME'].includes(record.type) ? 'info' : 'neutral'}>{TYPE_LABELS[record.type]}</MetaBadge> },
          { title: 'Phụ tùng', width: 240, ...serverSortable(sort, 'sparePartName'), render: (_, record) => <div className="table-primary-cell"><Typography.Text strong>{record.sparePartName}</Typography.Text><Typography.Text type="secondary" code>{record.sparePartSku}</Typography.Text></div> },
          { title: 'Biến động', width: 140, ...serverSortable(sort, 'quantity'), render: (_, record) => <Typography.Text strong type={isIncrease(record.type) ? 'success' : undefined}>{isIncrease(record.type) ? '+' : '-'}{formatQuantityWithUnit(record.quantity, record.unit)}</Typography.Text> },
          { title: 'Tồn sau', width: 130, ...serverSortable(sort, 'balanceAfter'), render: (_, record) => formatQuantityWithUnit(record.balanceAfter, record.unit) },
          { title: 'Phiếu công việc', width: 220, ...serverSortable(sort, 'workOrderCode'), render: (_, record) => record.workOrderCode ? <div className="table-primary-cell"><Typography.Text code>{record.workOrderCode}</Typography.Text><Typography.Text type="secondary" ellipsis={{ tooltip: record.workOrderSummary }}>{record.workOrderSummary ?? 'Nghiệp vụ theo phiếu công việc'}</Typography.Text></div> : '—' },
          { title: 'Kỹ thuật viên nhận / trả', width: 220, ...serverSortable(sort, 'recipientDisplayName'), render: (_, record) => ['ISSUE', 'RETURN'].includes(record.type) ? (record.recipientDisplayName || '—') : '—' },
          { title: 'Người thực hiện', width: 230, ...serverSortable(sort, 'actorDisplayName'), render: (_, record) => <div className="table-primary-cell"><Typography.Text strong>{record.actorDisplayName || record.createdBy}</Typography.Text><Typography.Text type="secondary">{actorRoleLabel(record.actorRole)}</Typography.Text></div> },
          { title: 'Mục đích / ghi chú', dataIndex: 'note', width: 260, ellipsis: true, ...serverSortable(sort, 'note'), render: (value: string | undefined, record) => value || record.workOrderSummary || '—' },
        ]}
      />
    </div>
  )
}
