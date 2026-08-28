import { ClearOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Empty, Input, Select, Table } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { AUDIT_ACTION_LABELS, AuditActionTag, MetaBadge } from '../../../components/PresentationBadge'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { auditApi } from '../api'

const { RangePicker } = DatePicker
const auditEntityLabels: Record<string, string> = {
  SERVICE_CHANNEL: 'Kênh tiếp nhận',
  SERVICE_REQUEST: 'Yêu cầu dịch vụ',
  WORK_ORDER: 'Phiếu công việc',
  CUSTOMER: 'Khách hàng',
  ASSET: 'Thiết bị',
  TECHNICIAN_PROFILE: 'Hồ sơ kỹ thuật viên',
  SPARE_PART: 'Phụ tùng',
  INVENTORY: 'Kho phụ tùng',
  USER_ACCOUNT: 'Tài khoản người dùng',
  ATTACHMENT: 'File đính kèm',
  AI: 'Trợ lý AI',
  SYSTEM: 'Hệ thống',
}

const auditActionOptions = Object.entries(AUDIT_ACTION_LABELS)
  .map(([value, label]) => ({ value, label }))
  .sort((left, right) => left.label.localeCompare(right.label, 'vi'))

const auditEntityOptions = Object.entries(auditEntityLabels)
  .map(([value, label]) => ({ value, label }))
  .sort((left, right) => left.label.localeCompare(right.label, 'vi'))

function recentDateRange(days: number): [Dayjs, Dayjs] {
  return [dayjs().subtract(days - 1, 'day').startOf('day'), dayjs().endOf('day')]
}

const auditDatePresets: Array<{ label: string; value: [Dayjs, Dayjs] }> = [
  { label: 'Hôm nay', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
  { label: '7 ngày gần nhất', value: recentDateRange(7) },
  { label: '30 ngày gần nhất', value: recentDateRange(30) },
  { label: '90 ngày gần nhất', value: recentDateRange(90) },
]

function formatAuditEntityType(value?: string) {
  if (!value) return EMPTY_VALUE
  if (auditEntityLabels[value]) return auditEntityLabels[value]
  const normalized = value.toLowerCase().replaceAll('_', ' ')
  return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}

export function AuditPage() {
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [actorInput, setActorInput] = useState('')
  const [action, setAction] = useState<string>()
  const [entityType, setEntityType] = useState<string>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const search = useDebouncedValue(searchInput.trim())
  const actor = useDebouncedValue(actorInput.trim())

  useEffect(() => {
    setPage(0)
  }, [search, actor])

  const from = dateRange?.[0].startOf('day').toISOString()
  const to = dateRange?.[1].endOf('day').toISOString()

  const queryKey = useMemo(() => [
    'audit',
    { page, size: LIST_PAGE_SIZE, search, actor, action, entityType, from, to },
  ], [page, search, actor, action, entityType, from, to])

  const auditQuery = useQuery({
    queryKey,
    queryFn: () => auditApi.list({ page, size: LIST_PAGE_SIZE, query: search, actor, action, entityType, from, to }),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = auditQuery

  const rangeLabel = dateRange
    ? `${dateRange[0].format('DD/MM/YYYY')} – ${dateRange[1].format('DD/MM/YYYY')}`
    : 'Toàn bộ thời gian'

  const resetFilters = () => {
    setSearchInput('')
    setActorInput('')
    setAction(undefined)
    setEntityType(undefined)
    setDateRange(null)
    setPage(0)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Quản trị & kiểm soát"
        title="Nhật ký hệ thống"
        description="Tra cứu thao tác quan trọng theo thời gian, người thực hiện và nghiệp vụ. Có thể lọc nhanh theo khoảng ngày hoặc xem toàn bộ lịch sử theo phân trang."
        meta={<><MetaBadge>{auditQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} sự kiện phù hợp`}</MetaBadge><MetaBadge tone="info">{rangeLabel}</MetaBadge></>}
      />

      {auditQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được nhật ký hệ thống"
          error={auditQuery.error}
          onRetry={() => auditQuery.refetch()}
        />
      )}

      <div className="table-toolbar toolbar-row audit-toolbar">
        <Input
          allowClear
          className="audit-search-input"
          prefix={<SearchOutlined />}
          placeholder="Tìm mã phiếu, nội dung, hành động..."
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
        <RangePicker
          allowClear
          presets={auditDatePresets}
          format="DD/MM/YYYY"
          value={dateRange}
          onChange={(dates) => {
            setDateRange(dates?.[0] && dates?.[1] ? [dates[0], dates[1]] : null)
            setPage(0)
          }}
        />
        <Input
          allowClear
          className="audit-actor-input"
          prefix={<UserOutlined />}
          placeholder="Người thao tác"
          value={actorInput}
          onChange={(event) => setActorInput(event.target.value)}
        />
        <Select
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder="Tất cả hành động"
          value={action}
          onChange={(value) => { setAction(value); setPage(0) }}
          options={auditActionOptions}
        />
        <Select
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder="Tất cả đối tượng"
          value={entityType}
          onChange={(value) => { setEntityType(value); setPage(0) }}
          options={auditEntityOptions}
        />
        <Button icon={<ClearOutlined />} onClick={resetFilters}>Đặt lại</Button>
      </div>

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={auditQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1160 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: auditQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} sự kiện`,
        }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description={auditQuery.isError ? 'Không thể tải dữ liệu audit' : 'Không có sự kiện phù hợp bộ lọc'} /> }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 180, render: formatDateTime },
          { title: 'Người thao tác', dataIndex: 'actorUsername', width: 160, render: (value: string) => <span className="audit-actor">{value}</span> },
          { title: 'Hành động', dataIndex: 'action', width: 170, render: (value: string) => <AuditActionTag action={value} /> },
          { title: 'Đối tượng', dataIndex: 'entityType', width: 190, render: (value: string) => <span className="audit-entity-label">{formatAuditEntityType(value)}</span> },
          { title: 'Chi tiết', dataIndex: 'details', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Mã đối tượng', dataIndex: 'entityId', width: 230, render: (value) => value ? <span className="entity-code" title={value}>{value}</span> : EMPTY_VALUE },
        ]}
      />
    </div>
  )
}
