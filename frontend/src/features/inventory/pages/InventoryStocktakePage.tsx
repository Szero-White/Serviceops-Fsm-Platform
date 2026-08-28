import { AuditOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, InputNumber, Modal, Space, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import type { SparePart } from '../../../types'
import { formatCompactDecimalInput, formatQuantity, formatQuantityWithUnit } from '../../../utils/format'
import { inventoryApi } from '../api'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

type StocktakeValues = { actualQuantity: number; reason: string }

export function InventoryStocktakePage() {
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput.trim())
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<SparePart>()
  const [form] = Form.useForm<StocktakeValues>()
  const handleFormValidationFailed = useFormValidationFeedback()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const partsQuery = useQuery({
    queryKey: ['stocktake-parts', { search, page, size: LIST_PAGE_SIZE }],
    queryFn: () => inventoryApi.list(search, page, LIST_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const data = partsQuery.data

  useEffect(() => setPage(0), [search])
  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0))
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['stocktake-parts'] })
    queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const stocktake = useMutation({
    mutationFn: (values: StocktakeValues) => inventoryApi.stocktake(selected!.id, values),
    onSuccess: (result) => {
      const difference = Number(result.difference)
      notification.success({
        message: `Đã kiểm kê · ${result.sparePart.sku}`,
        description: difference === 0
          ? `Tồn thực tế khớp hệ thống: ${formatQuantity(result.actualQuantity)} ${result.sparePart.unit}.`
          : `Hệ thống ${formatQuantity(result.systemQuantity)} → thực tế ${formatQuantity(result.actualQuantity)} ${result.sparePart.unit} (${difference > 0 ? '+' : ''}${formatQuantity(difference)}).`,
      })
      setSelected(undefined)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div className="page-stack">
      <PageHeader
        title="Kiểm kê tồn kho"
        description="Đối chiếu số lượng trên hệ thống với số lượng thực tế và ghi nhận chênh lệch có lý do."
        meta={<><MetaBadge>{data?.totalElements ?? 0} SKU</MetaBadge><MetaBadge tone="info">Có audit trail</MetaBadge></>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm SKU, tên hoặc đơn vị phụ tùng" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
      </div>

      {partsQuery.isError && <QueryErrorAlert title="Chưa tải được dữ liệu kiểm kê" error={partsQuery.error} onRetry={() => partsQuery.refetch()} />}

      <Table<SparePart>
        rowKey="id"
        loading={partsQuery.isLoading || partsQuery.isFetching}
        dataSource={partsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        pagination={{ current: page + 1, pageSize: LIST_PAGE_SIZE, total: partsQuery.isError ? 0 : (data?.totalElements ?? 0), showSizeChanger: false }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description="Chưa có phụ tùng phù hợp" /> }}
        columns={[
          {
            title: 'Phụ tùng',
            render: (_, record) => <div className="table-primary-cell"><Typography.Text strong>{record.name}</Typography.Text><Typography.Text type="secondary" code>{record.sku}</Typography.Text></div>,
          },
          { title: 'Tồn hệ thống', width: 180, render: (_, record) => formatQuantityWithUnit(record.stockQuantity, record.unit) },
          { title: 'Ngưỡng tồn tối thiểu', width: 200, render: (_, record) => formatQuantityWithUnit(record.reorderLevel, record.unit) },
          { title: 'Trạng thái', width: 150, render: (_, record) => <MetaBadge tone={record.active ? 'success' : 'neutral'}>{record.active ? 'Đang sử dụng' : 'Ngừng sử dụng'}</MetaBadge> },
          {
            title: 'Thao tác', width: 150,
            render: (_, record) => <Button icon={<AuditOutlined />} onClick={() => { setSelected(record); form.setFieldsValue({ actualQuantity: Number(record.stockQuantity), reason: '' }) }}>Kiểm kê</Button>,
          },
        ]}
      />

      <Modal
        title={`Kiểm kê · ${selected?.sku ?? ''}`}
        open={Boolean(selected)}
        onCancel={() => { setSelected(undefined); form.resetFields() }}
        onOk={() => form.submit()}
        okText="Xác nhận kiểm kê"
        confirmLoading={stocktake.isPending}
        destroyOnHidden
      >
        {selected && <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text>Tồn trên hệ thống: <strong>{formatQuantityWithUnit(selected.stockQuantity, selected.unit)}</strong></Typography.Text>
          <Form form={form} layout="vertical" onFinish={(values) => stocktake.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
            <Form.Item label="Số lượng thực tế" name="actualQuantity" rules={[{ required: true, message: 'Nhập số lượng thực tế' }]}>
              <InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} addonAfter={selected.unit} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Lý do / ghi chú kiểm kê" name="reason" rules={[{ required: true, message: 'Nhập lý do kiểm kê' }, { max: 300 }]}>
              <Input placeholder="Ví dụ: Kiểm kê cuối tuần, phát hiện 1 linh kiện hỏng" />
            </Form.Item>
          </Form>
        </Space>}
      </Modal>
    </div>
  )
}
