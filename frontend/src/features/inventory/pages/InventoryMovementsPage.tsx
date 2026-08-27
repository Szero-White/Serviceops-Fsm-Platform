import { RollbackOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, DatePicker, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, Typography } from 'antd'
import type { Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { actorRoleLabel } from '../../../constants/userRoles'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import type { InventoryTransaction, InventoryTransactionType, ReturnablePart } from '../../../types'
import { formatCompactDecimalInput, formatDateTime, formatQuantity, formatQuantityWithUnit } from '../../../utils/format'
import { inventoryApi } from '../api'

import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
const { RangePicker } = DatePicker

type ReturnValues = { quantity: number; note: string }

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
  const [type, setType] = useState<InventoryTransactionType | undefined>()
  const [period, setPeriod] = useState<[Dayjs, Dayjs] | null>(null)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<InventoryTransaction>()
  const [returnable, setReturnable] = useState<ReturnablePart>()
  const [form] = Form.useForm<ReturnValues>()
  const handleFormValidationFailed = useFormValidationFeedback()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const transactionsQuery = useQuery({
    queryKey: ['inventory-transactions', { search, type, period: period?.map((value) => value.toISOString()), page, size: LIST_PAGE_SIZE }],
    queryFn: () => inventoryApi.transactions({
      search,
      type,
      fromTime: period?.[0].startOf('day').toISOString(),
      toTime: period?.[1].endOf('day').toISOString(),
      page,
      size: LIST_PAGE_SIZE,
    }),
    placeholderData: keepPreviousData,
  })
  const data = transactionsQuery.data

  useEffect(() => setPage(0), [search, type, period])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0))
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['stocktake-parts'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
  }

  const openReturn = async (movement: InventoryTransaction) => {
    if (!movement.workOrderId) return
    try {
      const result = await inventoryApi.returnable(movement.workOrderId, movement.sparePartId)
      if (Number(result.returnableQuantity) <= 0) {
        message.info('Phụ tùng này không còn số lượng có thể hoàn trả cho phiếu công việc.')
        return
      }
      setSelected(movement)
      setReturnable(result)
      form.setFieldsValue({ quantity: Number(result.returnableQuantity), note: 'Hoàn trả phụ tùng chưa sử dụng' })
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const returnPart = useMutation({
    mutationFn: (values: ReturnValues) => inventoryApi.returnPart(selected!.workOrderId!, selected!.sparePartId, values),
    onSuccess: (result, values) => {
      notification.success({
        message: `Đã hoàn trả · ${result.sparePartSku}`,
        description: `${result.workOrderCode} · +${formatQuantity(values.quantity)} ${result.unit} vào kho · Còn có thể hoàn ${formatQuantity(result.returnableQuantity)} ${result.unit}.`,
      })
      setSelected(undefined)
      setReturnable(undefined)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div className="page-stack">
      <PageHeader
        title="Lịch sử biến động kho"
        description="Theo dõi nhập kho, cấp cho kỹ thuật viên, dữ liệu legacy sử dụng, hoàn trả và điều chỉnh kiểm kê. Dòng Cấp hoặc Sử dụng còn số lượng khả dụng sẽ có nút Hoàn trả ở cột Thao tác."
        meta={<><MetaBadge>{data?.totalElements ?? 0} giao dịch</MetaBadge><MetaBadge tone="info">Theo thời gian thực</MetaBadge></>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm SKU, tên, mã WO, họ tên người thực hiện hoặc mục đích" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <Select<InventoryTransactionType> allowClear placeholder="Loại giao dịch" value={type} onChange={setType} style={{ minWidth: 190 }} options={Object.entries(TYPE_LABELS).map(([value, label]) => ({ value: value as InventoryTransactionType, label }))} />
        <RangePicker value={period} onChange={(value) => setPeriod(value as [Dayjs, Dayjs] | null)} format="DD/MM/YYYY" />
      </div>

      {transactionsQuery.isError && <QueryErrorAlert title="Chưa tải được lịch sử kho" error={transactionsQuery.error} onRetry={() => transactionsQuery.refetch()} />}

      <Table<InventoryTransaction>
        rowKey="id"
        loading={transactionsQuery.isLoading || transactionsQuery.isFetching}
        dataSource={transactionsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1480 }}
        pagination={{ current: page + 1, pageSize: LIST_PAGE_SIZE, total: transactionsQuery.isError ? 0 : (data?.totalElements ?? 0), showSizeChanger: false }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description="Chưa có giao dịch kho phù hợp" /> }}
        columns={[
          { title: 'Thời gian', dataIndex: 'createdAt', width: 165, render: formatDateTime },
          { title: 'Loại', width: 150, render: (_, record) => <MetaBadge tone={isIncrease(record.type) ? 'success' : ['ISSUE', 'CONSUME'].includes(record.type) ? 'info' : 'neutral'}>{TYPE_LABELS[record.type]}</MetaBadge> },
          { title: 'Phụ tùng', width: 240, render: (_, record) => <div className="table-primary-cell"><Typography.Text strong>{record.sparePartName}</Typography.Text><Typography.Text type="secondary" code>{record.sparePartSku}</Typography.Text></div> },
          { title: 'Biến động', width: 140, render: (_, record) => <Typography.Text strong type={isIncrease(record.type) ? 'success' : undefined}>{isIncrease(record.type) ? '+' : '-'}{formatQuantityWithUnit(record.quantity, record.unit)}</Typography.Text> },
          { title: 'Tồn sau', width: 130, render: (_, record) => formatQuantityWithUnit(record.balanceAfter, record.unit) },
          { title: 'Phiếu công việc', width: 220, render: (_, record) => record.workOrderCode ? <div className="table-primary-cell"><Typography.Text code>{record.workOrderCode}</Typography.Text><Typography.Text type="secondary" ellipsis={{ tooltip: record.workOrderSummary }}>{record.workOrderSummary ?? 'Nghiệp vụ theo phiếu công việc'}</Typography.Text></div> : '—' },
          { title: 'Người thực hiện', width: 230, render: (_, record) => <div className="table-primary-cell"><Typography.Text strong>{record.actorDisplayName || record.createdBy}</Typography.Text><Typography.Text type="secondary">{actorRoleLabel(record.actorRole)}</Typography.Text></div> },
          { title: 'Mục đích / ghi chú', dataIndex: 'note', width: 260, ellipsis: true, render: (value: string | undefined, record) => value || record.workOrderSummary || '—' },
          { title: 'Thao tác', width: 120, fixed: 'right', render: (_, record) => ['ISSUE', 'CONSUME'].includes(record.type) && record.workOrderId ? <Button size="small" icon={<RollbackOutlined />} onClick={() => void openReturn(record)}>Hoàn trả</Button> : null },
        ]}
      />

      <Modal
        title={`Hoàn trả phụ tùng · ${returnable?.sparePartSku ?? ''}`}
        open={Boolean(selected && returnable)}
        onCancel={() => { setSelected(undefined); setReturnable(undefined); form.resetFields() }}
        onOk={() => form.submit()}
        okText="Xác nhận hoàn trả"
        confirmLoading={returnPart.isPending}
        destroyOnHidden
      >
        {returnable && <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text>Phiếu <Typography.Text code>{returnable.workOrderCode}</Typography.Text> · Có thể hoàn tối đa <strong>{formatQuantityWithUnit(returnable.returnableQuantity, returnable.unit)}</strong>.</Typography.Text>
          <Form form={form} layout="vertical" onFinish={(values) => returnPart.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
            <Form.Item label="Số lượng hoàn trả" name="quantity" rules={[{ required: true, message: 'Nhập số lượng hoàn trả' }]}>
              <InputNumber min={0.001} max={Number(returnable.returnableQuantity)} precision={3} formatter={formatCompactDecimalInput} addonAfter={returnable.unit} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Lý do" name="note" rules={[{ required: true, message: 'Nhập lý do hoàn trả' }, { max: 300 }]}>
              <Input placeholder="Ví dụ: Kỹ thuật viên không sử dụng hết" />
            </Form.Item>
          </Form>
        </Space>}
      </Modal>
    </div>
  )
}
