import { AuditOutlined } from '@ant-design/icons'
import { Table, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { auditApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { formatDateTime } from '../utils/format'

const actionColors: Record<string, string> = {
  CREATE: 'green', UPDATE: 'blue', ASSIGN: 'purple', CHANGE_STATUS: 'geekblue',
  CONSUME_PART: 'orange', IMPORT_STOCK: 'cyan', CANCEL: 'red', UPLOAD_FILE: 'magenta', SEED: 'default',
}

export function AuditPage() {
  const { data, isLoading } = useQuery({ queryKey: ['audit'], queryFn: () => auditApi.list() })
  return (
    <div>
      <PageHeader title="Nhật ký hệ thống" description="Truy vết các thao tác quan trọng phục vụ kiểm soát và hỗ trợ khách hàng." />
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 950 }}
        pagination={{ pageSize: 15, showSizeChanger: false }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 170, render: formatDateTime },
          { title: 'Người thao tác', dataIndex: 'actorUsername', width: 160, render: (v: string) => <Typography.Text strong>{v}</Typography.Text> },
          { title: 'Hành động', dataIndex: 'action', width: 160, render: (v: string) => <Tag icon={<AuditOutlined />} color={actionColors[v] ?? 'default'}>{v}</Tag> },
          { title: 'Đối tượng', dataIndex: 'entityType', width: 160 },
          { title: 'Chi tiết', dataIndex: 'details', ellipsis: true, render: (v) => v || '—' },
          { title: 'Entity ID', dataIndex: 'entityId', width: 270, render: (v) => v ? <Typography.Text code>{v}</Typography.Text> : '—' },
        ]}
      />
    </div>
  )
}
