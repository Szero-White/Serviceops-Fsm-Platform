import { RollbackOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Table, Typography } from 'antd'
import { useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import type { OutstandingPart, ReturnablePart } from '../../../types'
import { formatDateTime, formatQuantity, formatQuantityWithUnit } from '../../../utils/format'
import { compareDate, compareNumber, compareText } from '../../../utils/tableSort'
import { inventoryApi } from '../api'
import { ReturnPartModal, type ReturnPartValues } from './ReturnPartModal'

type OutstandingPartsTableProps = {
  search: string
  canReturn: boolean
}

export function OutstandingPartsTable({ search, canReturn }: OutstandingPartsTableProps) {
  const [returnable, setReturnable] = useState<ReturnablePart>()
  const [openingKey, setOpeningKey] = useState<string>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['part-outstanding', search],
    queryFn: () => inventoryApi.outstandingParts(search),
  })

  const openReturn = async (item: OutstandingPart) => {
    const key = `${item.workOrderId}:${item.sparePartId}`
    setOpeningKey(key)
    try {
      const current = await inventoryApi.returnable(item.workOrderId, item.sparePartId)
      if (Number(current.returnableQuantity) <= 0) {
        message.info('Phụ tùng này không còn số lượng có thể hoàn trả.')
        queryClient.invalidateQueries({ queryKey: ['part-outstanding'] })
        return
      }
      setReturnable(current)
    } catch (error) {
      message.error(apiErrorMessage(error))
    } finally {
      setOpeningKey(undefined)
    }
  }

  const returnPart = useMutation({
    mutationFn: (values: ReturnPartValues) => inventoryApi.returnPart(
      returnable!.workOrderId,
      returnable!.sparePartId,
      values,
    ),
    onSuccess: (result, values) => {
      notification.success({
        message: `Đã nhận hoàn trả · ${result.sparePartSku}`,
        description: `${result.workOrderCode} · +${formatQuantity(values.quantity)} ${result.unit} vào kho · Còn kỹ thuật viên giữ ${formatQuantity(result.returnableQuantity)} ${result.unit}.`,
      })
      setReturnable(undefined)
      queryClient.invalidateQueries({ queryKey: ['part-outstanding'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-part-usage'] })
      queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
      queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
      queryClient.invalidateQueries({ queryKey: ['work-order'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-timeline'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div style={{ marginTop: 28 }}>
      <div className="section-heading-row">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>Vật tư đang do kỹ thuật viên giữ</Typography.Title>
          <Typography.Text type="secondary">
            Đây là hàng đợi xử lý hoàn trả. Chỉ các phần đã cấp nhưng chưa sử dụng hoặc chưa trả hết mới xuất hiện; khi đã hoàn hết, dòng sẽ tự biến mất.
          </Typography.Text>
        </div>
      </div>
      <Table<OutstandingPart>
        rowKey={(item) => `${item.workOrderId}:${item.sparePartId}`}
        loading={query.isLoading || query.isFetching}
        dataSource={query.isError ? [] : (query.data ?? [])}
        pagination={false}
        className="content-table"
        scroll={{ x: canReturn ? 1200 : 1100 }}
        locale={{ emptyText: <Empty description="Không còn vật tư nào đang chờ hoàn trả" /> }}
        columns={[
          { title: 'Kỹ thuật viên', width: 190, sorter: (a, b) => compareText(a.technicianName, b.technicianName), render: (_, item) => item.technicianName ?? 'Chưa xác định' },
          { title: 'Phiếu', width: 190, sorter: (a, b) => compareText(a.workOrderCode, b.workOrderCode), render: (_, item) => <div><Typography.Text code>{item.workOrderCode}</Typography.Text><br /><Typography.Text type="secondary" ellipsis={{ tooltip: item.workOrderSummary }}>{item.workOrderSummary}</Typography.Text></div> },
          { title: 'Phụ tùng', width: 230, sorter: (a, b) => compareText(a.sparePartName, b.sparePartName), render: (_, item) => <div><Typography.Text strong>{item.sparePartName}</Typography.Text><br /><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
          { title: 'Đã cấp', width: 110, sorter: (a, b) => compareNumber(a.issuedQuantity, b.issuedQuantity), render: (_, item) => formatQuantityWithUnit(item.issuedQuantity, item.unit) },
          { title: 'Đã dùng', width: 110, sorter: (a, b) => compareNumber(a.usedQuantity, b.usedQuantity), render: (_, item) => formatQuantityWithUnit(item.usedQuantity, item.unit) },
          { title: 'Đã trả', width: 110, sorter: (a, b) => compareNumber(a.returnedQuantity, b.returnedQuantity), render: (_, item) => formatQuantityWithUnit(item.returnedQuantity, item.unit) },
          { title: 'Đang giữ', width: 120, sorter: (a, b) => compareNumber(a.outstandingQuantity, b.outstandingQuantity), render: (_, item) => <Typography.Text strong>{formatQuantityWithUnit(item.outstandingQuantity, item.unit)}</Typography.Text> },
          { title: 'Từ lúc', width: 170, sorter: (a, b) => compareDate(a.since, b.since), render: (_, item) => formatDateTime(item.since) },
          ...(canReturn ? [{
            title: 'Thao tác',
            width: 120,
            fixed: 'right' as const,
            render: (_: unknown, item: OutstandingPart) => (
              <Button
                size="small"
                icon={<RollbackOutlined />}
                onClick={() => void openReturn(item)}
                loading={openingKey === `${item.workOrderId}:${item.sparePartId}`}
                disabled={Number(item.outstandingQuantity) <= 0}
              >
                Hoàn trả
              </Button>
            ),
          }] : []),
        ]}
      />

      <ReturnPartModal
        returnable={returnable}
        pending={returnPart.isPending}
        onClose={() => setReturnable(undefined)}
        onSubmit={(values) => returnPart.mutate(values)}
      />
    </div>
  )
}
