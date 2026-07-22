import { PlusOutlined, SearchOutlined, SwapOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, customersApi, serviceRequestsApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { ChannelTag, PriorityTag, StatusTag } from '../components/StatusTag'
import type { RequestChannel } from '../types'
import { EMPTY_VALUE, formatDateTime } from '../utils/format'

const priorityOptions = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

const channelOptions: Array<{ value: RequestChannel; label: string }> = [
  { value: 'PHONE', label: 'Điện thoại' },
  { value: 'EMAIL', label: 'Email' },
  { value: 'WEBSITE', label: 'Website' },
  { value: 'ZALO', label: 'Zalo' },
  { value: 'WALK_IN', label: 'Trực tiếp' },
  { value: 'INTERNAL', label: 'Nội bộ' },
]

const requestStatusOptions = [
  { value: 'OPEN', label: 'Đang mở' },
  { value: 'CONVERTED', label: 'Đã tạo phiếu' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
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
    onSuccess: () => {
      message.success('Đã tiếp nhận yêu cầu dịch vụ')
      setOpen(false)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const convert = useMutation({
    mutationFn: serviceRequestsApi.convert,
    onSuccess: (workOrder) => {
      message.success(`Đã tạo ${workOrder.code}`)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const cancel = useMutation({
    mutationFn: serviceRequestsApi.cancel,
    onSuccess: () => {
      message.success('Đã huỷ yêu cầu')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Intake queue"
        title="Yêu cầu dịch vụ"
        description="Tiếp nhận sự cố, nhu cầu bảo trì và chuyển thành work order khi đủ thông tin."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={() => { form.setFieldsValue({ priority: 'NORMAL', channel: 'PHONE' }); setOpen(true) }}>Tiếp nhận yêu cầu</Button>}
        meta={<Space size={[8, 8]} wrap><Tag color="blue">{data?.totalElements ?? 0} yêu cầu</Tag><Tag color="orange">{data?.content.filter((request) => request.status === 'OPEN').length ?? 0} đang mở</Tag></Space>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm nội dung, khách hàng hoặc serial" value={search} onChange={(event) => setSearch(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={setStatus} options={requestStatusOptions} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 1120 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có yêu cầu phù hợp" /> }}
        columns={[
          {
            title: 'Yêu cầu',
            width: 340,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.title}</Typography.Text>
                <Typography.Text type="secondary" ellipsis>{record.description}</Typography.Text>
              </div>
            ),
          },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 210, ellipsis: true },
          { title: 'Thiết bị', dataIndex: 'assetLabel', width: 220, ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Ưu tiên', dataIndex: 'priority', width: 120, render: (value) => <PriorityTag priority={value} /> },
          { title: 'Kênh', dataIndex: 'channel', width: 120, render: (value) => <ChannelTag channel={value} /> },
          { title: 'Trạng thái', dataIndex: 'status', width: 140, render: (value) => <StatusTag status={value} /> },
          { title: 'Tiếp nhận', dataIndex: 'createdAt', width: 170, render: formatDateTime },
          {
            title: '',
            fixed: 'right',
            width: 170,
            render: (_, record) => record.status === 'OPEN' ? (
              <Space size={4}>
                <Button type="link" icon={<SwapOutlined />} loading={convert.isPending} onClick={() => convert.mutate(record.id)}>Tạo phiếu</Button>
                <Popconfirm title="Huỷ yêu cầu này?" okText="Huỷ" cancelText="Giữ lại" onConfirm={() => cancel.mutate(record.id)}>
                  <Button type="link" danger>Huỷ</Button>
                </Popconfirm>
              </Space>
            ) : EMPTY_VALUE,
          },
        ]}
      />

      <Modal title="Tiếp nhận yêu cầu dịch vụ" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={create.isPending} width={760} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
              <Select showSearch optionFilterProp="label" options={customers?.content.map((customer) => ({ value: customer.id, label: `${customer.code} · ${customer.name}` }))} />
            </Form.Item>
            <Form.Item label="Thiết bị" name="assetId">
              <Select allowClear showSearch optionFilterProp="label" options={assets?.content.map((asset) => ({ value: asset.id, label: `${asset.serialNumber} · ${asset.customerName}` }))} />
            </Form.Item>
            <Form.Item label="Mức độ ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={priorityOptions} /></Form.Item>
            <Form.Item label="Kênh tiếp nhận" name="channel" rules={[{ required: true, message: 'Chọn kênh tiếp nhận' }]}><Select options={channelOptions} /></Form.Item>
          </div>
          <Form.Item label="Tiêu đề" name="title" rules={[{ required: true, message: 'Nhập tiêu đề yêu cầu' }]}><Input placeholder="Ví dụ: Máy lạnh không đủ lạnh" /></Form.Item>
          <Form.Item label="Mô tả chi tiết" name="description" rules={[{ required: true, message: 'Nhập mô tả chi tiết' }]}><Input.TextArea rows={5} placeholder="Triệu chứng, thời điểm xảy ra, yêu cầu của khách hàng..." /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
