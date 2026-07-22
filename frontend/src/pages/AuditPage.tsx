import { AuditOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Empty, Table, Tag, Typography } from 'antd'
import { auditApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { EMPTY_VALUE, formatDateTime } from '../utils/format'

const actionColors: Record<string, string> = {
  CREATE: 'green',
  UPDATE: 'blue',
  ASSIGN: 'purple',
  CHANGE_STATUS: 'geekblue',
  CONSUME_PART: 'orange',
  IMPORT_STOCK: 'cyan',
  CANCEL: 'red',
  UPLOAD_FILE: 'magenta',
  SEED: 'default',
}

export function AuditPage() {
  const { data, isLoading } = useQuery({ queryKey: ['audit'], queryFn: () => auditApi.list() })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Governance"
        title="Nhật ký hệ thống"
        description="Truy vết thao tác quan trọng để kiểm soát vận hành, hỗ trợ khách hàng và audit nội bộ."
        meta={<Tag color="blue">{data?.totalElements ?? 0} sự kiện</Tag>}
      />
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 15, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có sự kiện audit" /> }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 180, render: formatDateTime },
          { title: 'Người thao tác', dataIndex: 'actorUsername', width: 170, render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
          { title: 'Hành động', dataIndex: 'action', width: 170, render: (value: string) => <Tag icon={<AuditOutlined />} color={actionColors[value] ?? 'default'}>{value}</Tag> },
          { title: 'Đối tượng', dataIndex: 'entityType', width: 170 },
          { title: 'Chi tiết', dataIndex: 'details', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Entity ID', dataIndex: 'entityId', width: 270, render: (value) => value ? <Typography.Text code>{value}</Typography.Text> : EMPTY_VALUE },
        ]}
      />
    </div>
  )
}
