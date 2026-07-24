import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined, SwapOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, customersApi, serviceChannelsApi, serviceRequestsApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { ChannelTag, PriorityTag, StatusTag } from '../components/StatusTag'
import type { ServiceRequest } from '../types'
import { EMPTY_VALUE, formatDateTime } from '../utils/format'

const priorityOptions = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
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
  const [editing, setEditing] = useState<ServiceRequest>()
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['service-requests', search, status],
    queryFn: () => serviceRequestsApi.list(search, status),
  })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })
  const { data: assets } = useQuery({ queryKey: ['assets', 'all'], queryFn: () => assetsApi.list('', 0, 300) })
  const { data: channels = [] } = useQuery({ queryKey: ['service-channels'], queryFn: () => serviceChannelsApi.list(false) })

  const channelOptions = useMemo(
    () => channels.filter((channel) => channel.active).map((channel) => ({ value: channel.code, label: channel.name })),
    [channels],
  )
  const channelMap = useMemo(() => new Map(channels.map((channel) => [channel.code, channel])), [channels])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => editing ? serviceRequestsApi.update(editing.id, values) : serviceRequestsApi.create(values),
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật yêu cầu dịch vụ' : 'Đã tiếp nhận yêu cầu dịch vụ')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => serviceRequestsApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá yêu cầu dịch vụ')
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

  const showCreate = () => {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ priority: 'NORMAL', channel: channelOptions[0]?.value ?? 'PHONE' })
    setOpen(true)
  }

  const showEdit = (record: ServiceRequest) => {
    setEditing(record)
    form.setFieldsValue({
      customerId: record.customerId,
      assetId: record.assetId,
      priority: record.priority,
      channel: record.channel,
      title: record.title,
      description: record.description,
    })
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Intake queue"
        title="Yêu cầu dịch vụ"
        description="Tiếp nhận, chỉnh sửa, huỷ hoặc chuyển yêu cầu thành work order khi đủ thông tin."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Tiếp nhận yêu cầu</Button>}
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
        scroll={{ x: 1260 }}
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
          {
            title: 'Kênh',
            dataIndex: 'channel',
            width: 130,
            render: (value) => {
              const channel = channelMap.get(value)
              return <ChannelTag channel={value} label={channel?.name} color={channel?.color} />
            },
          },
          { title: 'Trạng thái', dataIndex: 'status', width: 140, render: (value) => <StatusTag status={value} /> },
          { title: 'Tiếp nhận', dataIndex: 'createdAt', width: 170, render: formatDateTime },
          {
            title: 'Thao tác',
            fixed: 'right',
            width: 220,
            render: (_, record) => {
              const isOpen = record.status === 'OPEN'
              const isConverted = record.status === 'CONVERTED'
              return (
                <Space size={4}>
                  <Tooltip title={isOpen ? 'Sửa yêu cầu' : 'Chỉ sửa được yêu cầu đang mở'}>
                    <Button aria-label="Sửa yêu cầu" type="text" disabled={!isOpen} icon={<EditOutlined />} onClick={() => showEdit(record)} />
                  </Tooltip>

                  {isOpen && (
                    <>
                      <Button type="link" icon={<SwapOutlined />} loading={convert.isPending} onClick={() => convert.mutate(record.id)}>Tạo phiếu</Button>
                      <Popconfirm title="Huỷ yêu cầu này?" okText="Huỷ" cancelText="Giữ lại" onConfirm={() => cancel.mutate(record.id)}>
                        <Button type="link" danger>Huỷ</Button>
                      </Popconfirm>
                    </>
                  )}

                  <Tooltip title={isConverted ? 'Không thể xoá yêu cầu đã tạo work order' : 'Xoá yêu cầu'}>
                    <Popconfirm
                      disabled={isConverted}
                      title="Xoá yêu cầu này?"
                      description="Không thể xoá yêu cầu đã chuyển thành work order."
                      okText="Xoá"
                      cancelText="Huỷ"
                      okButtonProps={{ danger: true, loading: remove.isPending }}
                      onConfirm={() => remove.mutate(record.id)}
                    >
                      <Button aria-label="Xoá yêu cầu" type="text" danger disabled={isConverted} icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </Tooltip>
                </Space>
              )
            },
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật yêu cầu dịch vụ' : 'Tiếp nhận yêu cầu dịch vụ'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={760} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
              <Select showSearch optionFilterProp="label" options={customers?.content.map((customer) => ({ value: customer.id, label: `${customer.code} · ${customer.name}` }))} />
            </Form.Item>
            <Form.Item label="Thiết bị" name="assetId">
              <Select allowClear showSearch optionFilterProp="label" options={assets?.content.map((asset) => ({ value: asset.id, label: `${asset.serialNumber} · ${asset.customerName}` }))} />
            </Form.Item>
            <Form.Item label="Mức độ ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={priorityOptions} /></Form.Item>
            <Form.Item label="Kênh tiếp nhận" name="channel" rules={[{ required: true, message: 'Chọn kênh tiếp nhận' }]}>
              <Select options={channelOptions} placeholder="Chọn kênh tiếp nhận" />
            </Form.Item>
          </div>
          <Form.Item label="Tiêu đề" name="title" rules={[{ required: true, message: 'Nhập tiêu đề yêu cầu' }]}><Input placeholder="Ví dụ: Máy lạnh không đủ lạnh" /></Form.Item>
          <Form.Item label="Mô tả chi tiết" name="description" rules={[{ required: true, message: 'Nhập mô tả chi tiết' }]}><Input.TextArea rows={5} placeholder="Triệu chứng, thời điểm xảy ra, yêu cầu của khách hàng..." /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
