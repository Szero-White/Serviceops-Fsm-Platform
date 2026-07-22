import { InboxOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { inventoryApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/PageHeader'
import type { SparePart } from '../types'
import { formatCurrency, formatDateTime, formatNumber } from '../utils/format'

export function InventoryPage() {
  const { user } = useAuth()
  const canManageStock = ['OWNER', 'WAREHOUSE_STAFF'].includes(user?.role ?? '')
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [importing, setImporting] = useState<SparePart>()
  const [createForm] = Form.useForm()
  const [importForm] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['spare-parts', search], queryFn: () => inventoryApi.list(search) })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const create = useMutation({
    mutationFn: (values: Record<string, unknown>) => inventoryApi.create(values),
    onSuccess: () => {
      message.success('Đã tạo phụ tùng')
      setCreateOpen(false)
      createForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const importStock = useMutation({
    mutationFn: (values: { quantity: number; note: string }) => inventoryApi.importStock(importing!.id, values),
    onSuccess: () => {
      message.success('Đã nhập kho')
      setImporting(undefined)
      importForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Inventory control"
        title="Kho phụ tùng"
        description="Theo dõi tồn kho, mức đặt hàng và nhập bổ sung phụ tùng phục vụ work order."
        actions={canManageStock ? <Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.setFieldsValue({ unit: 'cái', initialStock: 0, reorderLevel: 3, unitPrice: 0, active: true }); setCreateOpen(true) }}>Thêm phụ tùng</Button> : undefined}
        meta={<Space size={[8, 8]} wrap><Tag color="blue">{data?.totalElements ?? 0} SKU</Tag><Tag color="red">{data?.content.filter((part) => part.lowStock).length ?? 0} sắp hết</Tag></Space>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm theo SKU hoặc tên phụ tùng" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        rowClassName={(record) => record.lowStock ? 'low-stock-row' : ''}
        locale={{ emptyText: <Empty description="Chưa có phụ tùng phù hợp" /> }}
        columns={[
          {
            title: 'Phụ tùng',
            width: 320,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.name}</Typography.Text>
                <Typography.Text type="secondary" code>{record.sku}</Typography.Text>
              </div>
            ),
          },
          {
            title: 'Tồn kho',
            width: 180,
            render: (_, record) => (
              <Space size={8} wrap>
                <strong>{formatNumber(record.stockQuantity)}</strong>
                <span>{record.unit}</span>
                {record.lowStock && <Tag color="red">Sắp hết</Tag>}
              </Space>
            ),
          },
          { title: 'Mức đặt hàng', dataIndex: 'reorderLevel', width: 150, render: (value, record) => `${formatNumber(value)} ${record.unit}` },
          { title: 'Đơn giá', dataIndex: 'unitPrice', width: 150, render: formatCurrency },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
          ...(canManageStock ? [{
            title: '',
            fixed: 'right' as const,
            width: 120,
            render: (_: unknown, record: SparePart) => (
              <Button icon={<InboxOutlined />} onClick={() => { setImporting(record); importForm.setFieldsValue({ note: 'Nhập bổ sung kho' }) }}>
                Nhập kho
              </Button>
            ),
          }] : []),
        ]}
      />

      <Modal title="Thêm phụ tùng" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => createForm.submit()} confirmLoading={create.isPending} width={680} destroyOnHidden>
        <Form form={createForm} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="SKU" name="sku" rules={[{ required: true, message: 'Nhập SKU' }]}><Input /></Form.Item>
            <Form.Item label="Tên phụ tùng" name="name" rules={[{ required: true, message: 'Nhập tên phụ tùng' }]}><Input /></Form.Item>
            <Form.Item label="Đơn vị" name="unit" rules={[{ required: true, message: 'Nhập đơn vị' }]}><Input /></Form.Item>
            <Form.Item label="Tồn ban đầu" name="initialStock" rules={[{ required: true, message: 'Nhập tồn ban đầu' }]}><InputNumber min={0} precision={3} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Mức đặt hàng lại" name="reorderLevel" rules={[{ required: true, message: 'Nhập mức đặt hàng' }]}><InputNumber min={0} precision={3} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Đơn giá" name="unitPrice" rules={[{ required: true, message: 'Nhập đơn giá' }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="VND" /></Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal title={`Nhập kho · ${importing?.sku ?? ''}`} open={Boolean(importing)} onCancel={() => setImporting(undefined)} onOk={() => importForm.submit()} confirmLoading={importStock.isPending} destroyOnHidden>
        <Form form={importForm} layout="vertical" onFinish={(values) => importStock.mutate(values)} requiredMark={false}>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} style={{ width: '100%' }} addonAfter={importing?.unit} /></Form.Item>
          <Form.Item label="Ghi chú" name="note" rules={[{ required: true, message: 'Nhập ghi chú' }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
