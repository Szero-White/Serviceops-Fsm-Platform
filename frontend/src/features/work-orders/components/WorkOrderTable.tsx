import { EyeOutlined } from '@ant-design/icons'
import { Button, Empty, Table, Typography } from 'antd'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { WorkOrder } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'

export function WorkOrderTable({
  workOrders,
  loading,
  onSelect,
}: {
  workOrders: WorkOrder[]
  loading: boolean
  onSelect: (id: string) => void
}) {
  return (
    <Table
      rowKey="id"
      loading={loading}
      dataSource={workOrders}
      className="content-table"
      scroll={{ x: 1120 }}
      pagination={{ pageSize: 12, showSizeChanger: false }}
      onRow={(record) => ({ onDoubleClick: () => onSelect(record.id) })}
      locale={{ emptyText: <Empty description="Chưa có phiếu công việc phù hợp" /> }}
      columns={[
        {
          title: 'Phiếu',
          width: 280,
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
          title: 'Bên liên quan',
          width: 220,
          render: (_, record) => (
            <div className="table-secondary-stack">
              <span>{record.customerName}</span>
              <Typography.Text type="secondary">{record.technicianName || 'Chưa phân công'}</Typography.Text>
            </div>
          ),
        },
        { title: 'Thiết bị', dataIndex: 'assetLabel', width: 180, ellipsis: true, render: (value) => value || EMPTY_VALUE },
        { title: 'Trạng thái', dataIndex: 'status', width: 145, render: (value) => <StatusTag status={value} /> },
        { title: 'Bắt đầu', dataIndex: 'scheduledStart', width: 145, render: formatDateTime },
        { title: 'Kết thúc', dataIndex: 'scheduledEnd', width: 145, render: formatDateTime },
        { title: '', width: 56, render: (_, record) => <Button aria-label="Xem chi tiết" type="text" icon={<EyeOutlined />} onClick={() => onSelect(record.id)} /> },
      ]}
    />
  )
}
