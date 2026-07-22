import { PlusOutlined, SearchOutlined, SwapOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, customersApi, serviceRequestsApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { PriorityTag, StatusTag } from '../components/StatusTag'
import { formatDateTime } from '../utils/format'

const priorityOptions = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

const channelOptions = [
  { value: 'PHONE', label: 'Điện thoại' },
  { value: 'EMAIL', label: 'Email' },
  { value: 'WEBSITE', label: 'Website' },
  { value: 'ZALO', label: 'Zalo' },
  { value: 'WALK_IN', label: 'Trực tiếp' },
  { value: 'INTERNAL', label: 'Nội bộ' },
]

export function ServiceRequestsPage() {
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<string>()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['service-requests', search, status], queryFn: () => serviceRequestsApi.list(search, status) })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })
  const { data: assets } = useQuery({ queryKey: ['assets', 'all'], queryFn: () => assetsApi.list('', 0, 300) })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const create = useMutation({
    mutationFn: (values: Record<string, unknown>) => serviceRequestsApi.create(values),
    onSuccess: () => { message.success('Đã tiếp nhận yêu cầu dịch vụ'); setOpen(false); form.resetFields(); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const convert = useMutation({
    mutationFn: serviceRequestsApi.convert,
    onSuccess: (workOrder) => { message.success(`Đã tạo ${workOrder.code}`); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const cancel = useMutation({
    mutationFn: serviceRequestsApi.cancel,
    onSuccess: () => { message.success('Đã hủy yêu cầu'); refresh() },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div>
      <PageHeader title="Yêu cầu dịch vụ" description="Tiếp nhận sự cố, nhu cầu bảo trì và chuyển thành phiếu công việc." actions={<Button type="primary" icon={<PlusOutlined />} onClick={() => { form.setFieldsValue({ priority: 'NORMAL', channel: 'PHONE' }); setOpen(true) }}>Tiếp nhận yêu cầu</Button>} />
      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm nội dung, khách hàng hoặc serial" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={setStatus} options={[{ value: 'OPEN', label: 'Đang mở' }, { value: 'CONVERTED', label: 'Đã tạo phiếu' }, { value: 'CANCELLED', label: 'Đã hủy' }]} />
      </div>
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 1150 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        columns={[
          { title: 'Nội dung', dataIndex: 'title', width: 260, render: (value: string, r) => <div><Typography.Text strong>{value}</Typography.Text><br /><Typography.Text type="secondary" ellipsis>{r.description}</Typography.Text></div> },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 200 },
          { title: 'Thiết bị', dataIndex: 'assetLabel', width: 220, ellipsis: true, render: (v) => v || 'Chưa xác định' },
          { title: 'Ưu tiên', dataIndex: 'priority', width: 110, render: (v) => <PriorityTag priority={v} /> },
          { title: 'Kênh', dataIndex: 'channel', width: 110 },
          { title: 'Trạng thái', dataIndex: 'status', width: 130, render: (v) => <StatusTag status={v} /> },
          { title: 'Tiếp nhận lúc', dataIndex: 'createdAt', width: 160, render: formatDateTime },
          {
            title: 'Thao tác', fixed: 'right', width: 190,
            render: (_, record) => record.status === 'OPEN' ? (
              <Space>
                <Button type="link" icon={<SwapOutlined />} loading={convert.isPending} onClick={() => convert.mutate(record.id)}>Tạo work order</Button>
                <Popconfirm title="Hủy yêu cầu này?" onConfirm={() => cancel.mutate(record.id)}><Button type="link" danger>Hủy</Button></Popconfirm>
              </Space>
            ) : '—',
          },
        ]}
      />

      <Modal title="Tiếp nhận yêu cầu dịch vụ" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={create.isPending} width={760} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={customers?.content.map((c) => ({ value: c.id, label: `${c.code} · ${c.name}` }))} />
            </Form.Item>
            <Form.Item label="Thiết bị" name="assetId">
              <Select allowClear showSearch optionFilterProp="label" options={assets?.content.map((a) => ({ value: a.id, label: `${a.serialNumber} · ${a.customerName}` }))} />
            </Form.Item>
            <Form.Item label="Mức độ ưu tiên" name="priority" rules={[{ required: true }]}><Select options={priorityOptions} /></Form.Item>
            <Form.Item label="Kênh tiếp nhận" name="channel" rules={[{ required: true }]}><Select options={channelOptions} /></Form.Item>
          </div>
          <Form.Item label="Tiêu đề" name="title" rules={[{ required: true }]}><Input placeholder="Ví dụ: Máy lạnh không đủ lạnh" /></Form.Item>
          <Form.Item label="Mô tả chi tiết" name="description" rules={[{ required: true }]}><Input.TextArea rows={5} placeholder="Triệu chứng, thời điểm xảy ra, yêu cầu của khách hàng..." /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
