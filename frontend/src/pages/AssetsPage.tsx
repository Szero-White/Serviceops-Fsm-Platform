import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Form, Input, Modal, Select, Table, Tag, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, customersApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { formatDate } from '../utils/format'

export function AssetsPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['assets', search], queryFn: () => assetsApi.list(search) })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => assetsApi.create({
      ...values,
      installedAt: values.installedAt ? dayjs(values.installedAt as dayjs.Dayjs).format('YYYY-MM-DD') : null,
      warrantyUntil: values.warrantyUntil ? dayjs(values.warrantyUntil as dayjs.Dayjs).format('YYYY-MM-DD') : null,
    }),
    onSuccess: () => {
      message.success('Đã tạo thiết bị')
      setOpen(false); form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div>
      <PageHeader title="Thiết bị khách hàng" description="Theo dõi serial, bảo hành và lịch sử tài sản cần phục vụ." actions={<Button type="primary" icon={<PlusOutlined />} onClick={() => { form.setFieldsValue({ status: 'ACTIVE' }); setOpen(true) }}>Thêm thiết bị</Button>} />
      <div className="table-toolbar"><Input allowClear prefix={<SearchOutlined />} placeholder="Tìm serial, hãng, model hoặc khách hàng" value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 1000 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        columns={[
          { title: 'Serial', dataIndex: 'serialNumber', width: 190, render: (v: string) => <Typography.Text code>{v}</Typography.Text> },
          { title: 'Thiết bị', width: 220, render: (_, r) => <div><Typography.Text strong>{r.brand ?? ''} {r.model ?? ''}</Typography.Text><br /><Typography.Text type="secondary">{r.category}</Typography.Text></div> },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 220 },
          { title: 'Ngày lắp', dataIndex: 'installedAt', width: 120, render: formatDate },
          { title: 'Bảo hành đến', dataIndex: 'warrantyUntil', width: 140, render: (v, r) => <div>{formatDate(v)}<br /><Tag color={r.underWarranty ? 'green' : 'red'}>{r.underWarranty ? 'Còn bảo hành' : 'Hết bảo hành'}</Tag></div> },
          { title: 'Trạng thái', dataIndex: 'status', width: 140, render: (v) => <Tag color={v === 'ACTIVE' ? 'blue' : 'default'}>{v}</Tag> },
          { title: 'Ghi chú', dataIndex: 'notes', ellipsis: true, render: (v) => v || '—' },
        ]}
      />

      <Modal title="Thêm thiết bị" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={720} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={customers?.content.map((c) => ({ value: c.id, label: `${c.code} · ${c.name}` }))} />
          </Form.Item>
          <div className="form-grid two-cols">
            <Form.Item label="Loại thiết bị" name="category" rules={[{ required: true }]}><Input placeholder="Máy lạnh" /></Form.Item>
            <Form.Item label="Serial number" name="serialNumber" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Hãng" name="brand"><Input placeholder="Daikin" /></Form.Item>
            <Form.Item label="Model" name="model"><Input /></Form.Item>
            <Form.Item label="Ngày lắp đặt" name="installedAt"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
            <Form.Item label="Bảo hành đến" name="warrantyUntil"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
          </div>
          <Form.Item label="Trạng thái" name="status"><Select options={[{ value: 'ACTIVE', label: 'Hoạt động' }, { value: 'IN_SERVICE', label: 'Đang sửa chữa' }, { value: 'OUT_OF_SERVICE', label: 'Ngừng hoạt động' }, { value: 'RETIRED', label: 'Thanh lý' }]} /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
