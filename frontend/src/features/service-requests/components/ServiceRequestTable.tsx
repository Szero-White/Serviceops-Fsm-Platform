import { CloseCircleOutlined, EditOutlined, SwapOutlined } from '@ant-design/icons'
import { Button, Empty, Popconfirm, Space, Table, Tooltip, Typography } from 'antd'
import type { TableProps } from 'antd'
import { ChannelTag, PriorityTag, StatusTag } from '../../../components/StatusTag'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { PageResponse, ServiceChannel, ServiceRequest } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'

interface ServiceRequestTableProps {
  data?: PageResponse<ServiceRequest>
  loading: boolean
  hasError: boolean
  page: number
  sort: TableSortState
  canConvert: boolean
  channelMap: Map<string, ServiceChannel>
  onPageChange: (page: number) => void
  onSortChange: (sort: TableSortState) => void
  onEdit: (request: ServiceRequest) => void
  onConvert: (request: ServiceRequest) => void
  onCancel: (requestId: string) => void
}

const DEFAULT_SORT: TableSortState = { sortBy: 'createdAt', sortDir: 'desc' }

export function ServiceRequestTable({
  data,
  loading,
  hasError,
  page,
  sort,
  canConvert,
  channelMap,
  onPageChange,
  onSortChange,
  onEdit,
  onConvert,
  onCancel,
}: ServiceRequestTableProps) {
  const handleChange: TableProps<ServiceRequest>['onChange'] = (pagination, _filters, sorter) => {
    onPageChange(Math.max((pagination.current ?? 1) - 1, 0))
    onSortChange(resolveTableSort(sorter, DEFAULT_SORT))
  }

  return (
    <Table<ServiceRequest>
      rowKey="id"
      loading={loading}
      dataSource={hasError ? [] : (data?.content ?? [])}
      className="content-table"
      scroll={{ x: 1180 }}
      pagination={{
        current: page + 1,
        pageSize: LIST_PAGE_SIZE,
        total: hasError ? 0 : (data?.totalElements ?? 0),
        showSizeChanger: false,
        showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} yêu cầu`,
      }}
      onChange={handleChange}
      locale={{ emptyText: <Empty description={hasError ? 'Không thể tải dữ liệu yêu cầu dịch vụ' : 'Chưa có yêu cầu phù hợp'} /> }}
      columns={[
        {
          title: 'Yêu cầu',
          width: 280,
          ...serverSortable(sort, 'title'),
          render: (_, record) => (
            <div className="table-primary-cell">
              <Typography.Text strong>{record.title}</Typography.Text>
              <Typography.Text type="secondary" ellipsis>{record.description}</Typography.Text>
            </div>
          ),
        },
        { title: 'Khách hàng', dataIndex: 'customerName', width: 175, ellipsis: true, ...serverSortable(sort, 'customerName') },
        { title: 'Thiết bị', dataIndex: 'assetLabel', width: 180, ellipsis: true, ...serverSortable(sort, 'assetLabel'), render: (value) => value || EMPTY_VALUE },
        { title: 'Ưu tiên', dataIndex: 'priority', width: 100, ...serverSortable(sort, 'priority'), render: (value) => <PriorityTag priority={value} /> },
        {
          title: 'Kênh',
          dataIndex: 'channel',
          width: 120,
          ...serverSortable(sort, 'channel'),
          render: (value) => {
            const channel = channelMap.get(value)
            return <ChannelTag channel={value} label={channel?.name} color={channel?.color} />
          },
        },
        { title: 'Trạng thái', dataIndex: 'status', width: 170, ...serverSortable(sort, 'status'), render: (value) => <StatusTag status={value} label={value === 'OPEN' ? 'Chờ chuyển điều phối' : undefined} /> },
        { title: 'Tiếp nhận', dataIndex: 'createdAt', width: 150, ...serverSortable(sort, 'createdAt'), render: formatDateTime },
        {
          title: 'Thao tác',
          width: 118,
          fixed: 'right',
          align: 'center',
          className: 'service-request-actions',
          render: (_, record) => {
            const isOpen = record.status === 'OPEN'
            return (
              <Space size={2} wrap={false}>
                <Tooltip title={isOpen ? 'Kiểm tra / chỉnh sửa' : 'Chỉ chỉnh sửa được yêu cầu đang chờ chuyển điều phối'}>
                  <Button
                    aria-label="Kiểm tra / chỉnh sửa yêu cầu"
                    type="text"
                    disabled={!isOpen}
                    icon={<EditOutlined />}
                    onClick={() => onEdit(record)}
                  />
                </Tooltip>

                {isOpen && canConvert ? (
                  <Tooltip title="Tạo phiếu công việc và chuyển sang Điều phối">
                    <Button
                      aria-label="Tạo phiếu công việc và chuyển sang Điều phối"
                      type="text"
                      className="table-action-primary"
                      icon={<SwapOutlined />}
                      onClick={() => onConvert(record)}
                    />
                  </Tooltip>
                ) : null}

                {isOpen ? (
                  <Popconfirm
                    title="Hủy yêu cầu này?"
                    description="Yêu cầu sẽ được lưu trong lịch sử với trạng thái Đã hủy."
                    okText="Hủy yêu cầu"
                    cancelText="Giữ lại"
                    onConfirm={() => onCancel(record.id)}
                  >
                    <Tooltip title="Hủy yêu cầu">
                      <Button aria-label="Hủy yêu cầu" type="text" danger icon={<CloseCircleOutlined />} />
                    </Tooltip>
                  </Popconfirm>
                ) : null}
              </Space>
            )
          },
        },
      ]}
    />
  )
}
