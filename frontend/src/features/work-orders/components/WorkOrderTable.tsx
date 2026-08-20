import { EyeOutlined } from '@ant-design/icons'
import { Button, Empty, Table, Typography } from 'antd'
import { PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { WorkOrder } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'

export function WorkOrderTable({
  workOrders,
  loading,
  page,
  pageSize,
  total,
  onPageChange,
  onSelect,
  loadError = false,
}: {
  workOrders: WorkOrder[]
  loading: boolean
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
  onSelect: (id: string) => void
  loadError?: boolean
}) {
  return (
    <Table
      rowKey="id"
      loading={loading}
      dataSource={workOrders}
      className="content-table"
      scroll={{ x: 1120 }}
      pagination={{
        current: page + 1,
        pageSize,
        total,
        showSizeChanger: false,
        showTotal: (count, range) => `${range[0]}–${range[1]} / ${count} phiếu`,
      }}
      onChange={(pagination) => onPageChange(Math.max((pagination.current ?? 1) - 1, 0))}
      onRow={(record) => ({ onDoubleClick: () => onSelect(record.id) })}
      locale={{ emptyText: <Empty description={loadError ? 'Không thể tải dữ liệu phiếu công việc' : 'Chưa có phiếu công việc phù hợp'} /> }}
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
