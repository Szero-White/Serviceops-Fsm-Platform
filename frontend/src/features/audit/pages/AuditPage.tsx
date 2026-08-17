import { useQuery } from '@tanstack/react-query'
import { Empty, Table } from 'antd'
import { auditApi } from '../api'
import { PageHeader } from '../../../components/PageHeader'
import { AuditActionTag, MetaBadge } from '../../../components/PresentationBadge'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'

const auditEntityLabels: Record<string, string> = {
  SERVICE_CHANNEL: 'Kênh tiếp nhận',
  SERVICE_REQUEST: 'Yêu cầu dịch vụ',
  WORK_ORDER: 'Phiếu công việc',
  CUSTOMER: 'Khách hàng',
  ASSET: 'Thiết bị',
  TECHNICIAN: 'Kỹ thuật viên',
  SPARE_PART: 'Phụ tùng',
  INVENTORY: 'Kho phụ tùng',
  USER: 'Người dùng',
  ATTACHMENT: 'Tệp đính kèm',
  AI: 'Trợ lý AI',
}

function formatAuditEntityType(value?: string) {
  if (!value) return EMPTY_VALUE
  if (auditEntityLabels[value]) return auditEntityLabels[value]
  const normalized = value.toLowerCase().replaceAll('_', ' ')
  return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}

export function AuditPage() {
  const { data, isLoading } = useQuery({ queryKey: ['audit'], queryFn: () => auditApi.list() })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Quản trị & kiểm soát"
        title="Nhật ký hệ thống"
        description="Truy vết thao tác quan trọng để kiểm soát vận hành, hỗ trợ khách hàng và audit nội bộ."
        meta={<MetaBadge>{data?.totalElements ?? 0} sự kiện</MetaBadge>}
      />
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        pagination={{ pageSize: 15, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có sự kiện audit" /> }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 180, render: formatDateTime },
          { title: 'Người thao tác', dataIndex: 'actorUsername', width: 160, render: (value: string) => <span className="audit-actor">{value}</span> },
          { title: 'Hành động', dataIndex: 'action', width: 160, render: (value: string) => <AuditActionTag action={value} /> },
          { title: 'Đối tượng', dataIndex: 'entityType', width: 170, render: (value: string) => <span className="audit-entity-label">{formatAuditEntityType(value)}</span> },
          { title: 'Chi tiết', dataIndex: 'details', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Mã đối tượng', dataIndex: 'entityId', width: 230, render: (value) => value ? <span className="entity-code" title={value}>{value}</span> : EMPTY_VALUE },
        ]}
      />
    </div>
  )
}
