import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { serviceChannelsApi } from '../../service-channels/api'
import { MetricCard } from '../../../components/MetricCard'
import { PageHeader } from '../../../components/PageHeader'
import { BinaryStatusTag, MetaBadge } from '../../../components/PresentationBadge'
import type { ServiceChannel } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'

const colorOptions = [
  { value: 'blue', label: 'Xanh dương' },
  { value: 'green', label: 'Xanh lá' },
  { value: 'cyan', label: 'Xanh ngọc' },
  { value: 'geekblue', label: 'Xanh đậm' },
  { value: 'purple', label: 'Tím' },
  { value: 'orange', label: 'Cam' },
  { value: 'red', label: 'Đỏ' },
  { value: 'default', label: 'Trung tính' },
]

const colorLabels = Object.fromEntries(colorOptions.map((option) => [option.value, option.label])) as Record<string, string>

function buildCode(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toUpperCase()
    .slice(0, 30)
}

export function ServiceChannelsPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ServiceChannel>()
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data = [], isLoading } = useQuery({
    queryKey: ['service-channels'],
    queryFn: () => serviceChannelsApi.list(false),
  })

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) {
      return data
    }
    return data.filter((channel) =>
      [channel.code, channel.name, channel.description].some((value) => value?.toLowerCase().includes(keyword)),
    )
  }, [data, search])

  const activeCount = data.filter((channel) => channel.active).length
  const systemCount = data.filter((channel) => channel.systemDefined).length

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = { ...values, code: buildCode(String(values.code ?? '')) }
      return editing ? serviceChannelsApi.update(editing.id, payload) : serviceChannelsApi.create(payload)
    },
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật kênh tiếp nhận' : 'Đã tạo kênh tiếp nhận')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['service-channels'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => serviceChannelsApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá kênh tiếp nhận')
      queryClient.invalidateQueries({ queryKey: ['service-channels'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ color: 'blue', sortOrder: (data.length + 1) * 10, active: true })
    setOpen(true)
  }

  const showEdit = (record: ServiceChannel) => {
    setEditing(record)
    form.setFieldsValue(record)
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Cấu hình tiếp nhận"
        title="Kênh tiếp nhận"
        description="Quản trị các kênh tiếp nhận để đội chăm sóc khách hàng dùng thống nhất khi tạo yêu cầu dịch vụ."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm kênh</Button>}
        meta={<MetaBadge>{data.length} kênh</MetaBadge>}
      />

      <div className="channel-summary-grid">
        <MetricCard label="Đang dùng" value={activeCount} helper="Hiển thị trong form tiếp nhận" icon={<PlusOutlined />} tone="success" />
        <MetricCard label="Mặc định hệ thống" value={systemCount} helper="Có sẵn khi khởi tạo tenant" icon={<SearchOutlined />} tone="primary" />
        <MetricCard label="Tạm ngưng" value={data.length - activeCount} helper="Giữ lịch sử, không cho chọn mới" icon={<DeleteOutlined />} tone="warning" />
      </div>

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm theo tên, mã hoặc mô tả" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={filtered}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có kênh tiếp nhận phù hợp" /> }}
        columns={[
          {
            title: 'Kênh',
            width: 320,
            render: (_, record) => (
              <div className="channel-name-cell">
                <span className={`channel-color-swatch channel-color-${record.color}`} />
                <div>
                  <Typography.Text strong>{record.name}</Typography.Text>
                  <Typography.Text code>{record.code}</Typography.Text>
                </div>
              </div>
            ),
          },
          { title: 'Mô tả', dataIndex: 'description', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Màu nhận diện', dataIndex: 'color', width: 140, render: (value: string) => <span className="channel-color-value"><span className={`channel-color-dot channel-color-${value}`} />{colorLabels[value] ?? 'Trung tính'}</span> },
          { title: 'Thứ tự', dataIndex: 'sortOrder', width: 110 },
          { title: 'Trạng thái', dataIndex: 'active', width: 140, render: (value: boolean) => <BinaryStatusTag active={value} activeLabel="Đang dùng" /> },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
          {
            title: '',
            width: 92,
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa kênh" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xoá kênh này?"
                  description="Chỉ xoá được khi kênh chưa được dùng trong yêu cầu dịch vụ."
                  okText="Xoá"
                  cancelText="Huỷ"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(record.id)}
                >
                  <Button aria-label="Xoá kênh" type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật kênh tiếp nhận' : 'Thêm kênh tiếp nhận'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={680} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Tên kênh" name="name" rules={[{ required: true, message: 'Nhập tên kênh' }]}>
              <Input onBlur={(event) => !editing && !form.getFieldValue('code') && form.setFieldValue('code', buildCode(event.target.value))} placeholder="Ví dụ: TikTok Lead" />
            </Form.Item>
            <Form.Item label="Mã kênh" name="code" rules={[{ required: !editing, message: 'Nhập mã kênh' }]}>
              <Input disabled={Boolean(editing)} placeholder="TIKTOK_LEAD" onBlur={(event) => form.setFieldValue('code', buildCode(event.target.value))} />
            </Form.Item>
            <Form.Item label="Màu hiển thị" name="color">
              <Select options={colorOptions} />
            </Form.Item>
            <Form.Item label="Thứ tự" name="sortOrder">
              <InputNumber min={0} max={9999} style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item label="Mô tả" name="description">
            <Input.TextArea rows={3} placeholder="Nguồn tiếp nhận, quy trình xử lý hoặc ghi chú vận hành..." />
          </Form.Item>
          <Form.Item name="active" valuePropName="checked">
            <Switch checkedChildren="Đang dùng" unCheckedChildren="Tạm ngưng" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
