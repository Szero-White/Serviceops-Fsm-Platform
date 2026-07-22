import { InboxOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { inventoryApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { useAuth } from '../auth/AuthContext'
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
    onSuccess: () => { message.success('Đã tạo phụ tùng'); setCreateOpen(false); createForm.resetFields(); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const importStock = useMutation({
    mutationFn: (values: { quantity: number; note: string }) => inventoryApi.importStock(importing!.id, values),
    onSuccess: () => { message.success('Đã nhập kho'); setImporting(undefined); importForm.resetFields(); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div>
      <PageHeader title="Kho phụ tùng" description="Theo dõi tồn kho và nhập phụ tùng phục vụ work order." actions={canManageStock ? <Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.setFieldsValue({ unit: 'cái', initialStock: 0, reorderLevel: 3, unitPrice: 0, active: true }); setCreateOpen(true) }}>Thêm phụ tùng</Button> : undefined} />
      <div className="table-toolbar"><Input allowClear prefix={<SearchOutlined />} placeholder="Tìm theo SKU hoặc tên phụ tùng" value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        rowClassName={(record) => record.lowStock ? 'low-stock-row' : ''}
        columns={[
          { title: 'SKU', dataIndex: 'sku', width: 160, render: (v: string) => <Typography.Text code>{v}</Typography.Text> },
          { title: 'Tên phụ tùng', dataIndex: 'name', width: 250, render: (v: string) => <Typography.Text strong>{v}</Typography.Text> },
          { title: 'Tồn hiện tại', width: 150, render: (_, r) => <Space><strong>{formatNumber(r.stockQuantity)}</strong><span>{r.unit}</span>{r.lowStock && <Tag color="red">Sắp hết</Tag>}</Space> },
          { title: 'Mức đặt hàng', dataIndex: 'reorderLevel', width: 130, render: (v, r) => `${formatNumber(v)} ${r.unit}` },
          { title: 'Đơn giá', dataIndex: 'unitPrice', width: 150, render: formatCurrency },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 160, render: formatDateTime },
          ...(canManageStock ? [{ title: '', fixed: 'right' as const, width: 110, render: (_: unknown, r: SparePart) => <Button icon={<InboxOutlined />} onClick={() => { setImporting(r); importForm.setFieldsValue({ note: 'Nhập bổ sung kho' }) }}>Nhập kho</Button> }] : []),
        ]}
      />

      <Modal title="Thêm phụ tùng" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => createForm.submit()} confirmLoading={create.isPending} width={680} destroyOnHidden>
        <Form form={createForm} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="SKU" name="sku" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Tên phụ tùng" name="name" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Đơn vị" name="unit" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Tồn ban đầu" name="initialStock" rules={[{ required: true }]}><InputNumber min={0} precision={3} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Mức đặt hàng lại" name="reorderLevel" rules={[{ required: true }]}><InputNumber min={0} precision={3} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Đơn giá" name="unitPrice" rules={[{ required: true }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="₫" /></Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal title={`Nhập kho · ${importing?.sku ?? ''}`} open={Boolean(importing)} onCancel={() => setImporting(undefined)} onOk={() => importForm.submit()} confirmLoading={importStock.isPending} destroyOnHidden>
        <Form form={importForm} layout="vertical" onFinish={(values) => importStock.mutate(values)} requiredMark={false}>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true }]}><InputNumber min={0.001} precision={3} style={{ width: '100%' }} addonAfter={importing?.unit} /></Form.Item>
          <Form.Item label="Ghi chú" name="note" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
