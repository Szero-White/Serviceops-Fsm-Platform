import { useQuery } from '@tanstack/react-query'
import { Empty, Table, Typography } from 'antd'
import type { OutstandingPart } from '../../../types'
import { formatDateTime, formatQuantityWithUnit } from '../../../utils/format'
import { inventoryApi } from '../api'

export function OutstandingPartsTable({ search }: { search: string }) {
  const query = useQuery({
    queryKey: ['part-outstanding', search],
    queryFn: () => inventoryApi.outstandingParts(search),
  })

  return (
    <div style={{ marginTop: 28 }}>
      <div className="section-heading-row">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>Vật tư đang do kỹ thuật viên giữ</Typography.Title>
          <Typography.Text type="secondary">Theo dõi phần đã cấp nhưng chưa ghi nhận sử dụng hoặc hoàn trả. Không chặn đóng phiếu sau khi thanh toán hoàn tất.</Typography.Text>
        </div>
      </div>
      <Table<OutstandingPart>
        rowKey={(item) => `${item.workOrderId}:${item.sparePartId}`}
        loading={query.isLoading || query.isFetching}
        dataSource={query.isError ? [] : (query.data ?? [])}
        pagination={false}
        scroll={{ x: 1100 }}
        locale={{ emptyText: <Empty description="Không có vật tư nào đang do kỹ thuật viên giữ" /> }}
        columns={[
          { title: 'Kỹ thuật viên', width: 190, render: (_, item) => item.technicianName ?? 'Chưa xác định' },
          { title: 'Phiếu', width: 190, render: (_, item) => <div><Typography.Text code>{item.workOrderCode}</Typography.Text><br /><Typography.Text type="secondary" ellipsis={{ tooltip: item.workOrderSummary }}>{item.workOrderSummary}</Typography.Text></div> },
          { title: 'Phụ tùng', width: 230, render: (_, item) => <div><Typography.Text strong>{item.sparePartName}</Typography.Text><br /><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
          { title: 'Đã cấp', width: 110, render: (_, item) => formatQuantityWithUnit(item.issuedQuantity, item.unit) },
          { title: 'Đã dùng', width: 110, render: (_, item) => formatQuantityWithUnit(item.usedQuantity, item.unit) },
          { title: 'Đã trả', width: 110, render: (_, item) => formatQuantityWithUnit(item.returnedQuantity, item.unit) },
          { title: 'Đang giữ', width: 120, render: (_, item) => <Typography.Text strong>{formatQuantityWithUnit(item.outstandingQuantity, item.unit)}</Typography.Text> },
          { title: 'Từ lúc', width: 170, render: (_, item) => formatDateTime(item.since) },
        ]}
      />
    </div>
  )
}
