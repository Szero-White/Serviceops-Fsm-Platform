import { DeleteOutlined, EditOutlined, PhoneOutlined, PlusOutlined, SearchOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Avatar, Button, Empty, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { techniciansApi } from '../api/services'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import type { Technician } from '../types'
import { EMPTY_VALUE } from '../utils/format'

function usernameFromName(value: string) {
  const slug = value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^a-zA-Z0-9]+/g, '.')
    .replace(/^\.+|\.+$/g, '')
    .toLowerCase()

  return slug || `technician.${Date.now().toString().slice(-5)}`
}

export function TechniciansPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Technician>()
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data = [], isLoading } = useQuery({
    queryKey: ['technicians', 'all'],
    queryFn: () => techniciansApi.list(false),
  })

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) {
      return data
    }
    return data.filter((technician) =>
      [technician.name, technician.username, technician.phone, technician.skills].some((value) => value?.toLowerCase().includes(keyword)),
    )
  }, [data, search])

  const activeCount = data.filter((technician) => technician.active && technician.accountActive).length
  const pausedCount = data.length - activeCount
  const skilledCount = data.filter((technician) => technician.skills?.trim()).length

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = { ...values }
      if (!payload.password) {
        delete payload.password
      }
      return editing ? techniciansApi.update(editing.id, payload) : techniciansApi.create(payload)
    },
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật kỹ thuật viên' : 'Đã tạo kỹ thuật viên')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => techniciansApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá kỹ thuật viên')
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ active: true, password: '123456' })
    setOpen(true)
  }

  const showEdit = (record: Technician) => {
    setEditing(record)
    form.setFieldsValue({ ...record, active: record.active && record.accountActive, password: undefined })
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Field workforce"
        title="Đội ngũ kỹ thuật"
        description="Quản lý tài khoản kỹ thuật viên, thông tin liên hệ, kỹ năng phục vụ và trạng thái sẵn sàng tại hiện trường."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm kỹ thuật viên</Button>}
        meta={<Tag color="blue">{data.length} kỹ thuật viên</Tag>}
      />

      <div className="channel-summary-grid">
        <MetricCard label="Sẵn sàng" value={activeCount} helper="Có thể nhận lịch mới" icon={<UserOutlined />} tone="green" />
        <MetricCard label="Có kỹ năng" value={skilledCount} helper="Đã khai báo năng lực" icon={<ToolOutlined />} tone="blue" />
        <MetricCard label="Tạm ngưng" value={pausedCount} helper="Không hiển thị khi phân công" icon={<PhoneOutlined />} tone="orange" />
      </div>

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm tên, username, số điện thoại hoặc kỹ năng" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={filtered}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có kỹ thuật viên phù hợp" /> }}
        columns={[
          {
            title: 'Kỹ thuật viên',
            width: 320,
            render: (_, record) => (
              <div className="technician-name-cell">
                <Avatar size={42} icon={<UserOutlined />} />
                <div>
                  <Typography.Text strong>{record.name}</Typography.Text>
                  <Typography.Text type="secondary">@{record.username}</Typography.Text>
                </div>
              </div>
            ),
          },
          { title: 'Điện thoại', dataIndex: 'phone', width: 150, render: (value) => value || EMPTY_VALUE },
          { title: 'Kỹ năng', dataIndex: 'skills', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          {
            title: 'Trạng thái',
            width: 150,
            render: (_, record) => {
              const active = record.active && record.accountActive
              return <Tag color={active ? 'green' : 'default'}>{active ? 'Sẵn sàng' : 'Tạm ngưng'}</Tag>
            },
          },
          {
            title: '',
            width: 92,
            fixed: 'right',
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa kỹ thuật viên" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xoá kỹ thuật viên này?"
                  description="Chỉ xoá được khi kỹ thuật viên chưa có lịch hoặc work order."
                  okText="Xoá"
                  cancelText="Huỷ"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(record.id)}
                >
                  <Button aria-label="Xoá kỹ thuật viên" type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật kỹ thuật viên' : 'Thêm kỹ thuật viên'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={720} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Họ tên" name="name" rules={[{ required: true, message: 'Nhập họ tên kỹ thuật viên' }]}>
              <Input
                placeholder="Ví dụ: Phạm Quốc Kỹ thuật"
                onBlur={(event) => !editing && !form.getFieldValue('username') && form.setFieldValue('username', usernameFromName(event.target.value))}
              />
            </Form.Item>
            <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
              <Input placeholder="pham.quoc.ky.thuat" />
            </Form.Item>
            <Form.Item label={editing ? 'Mật khẩu mới' : 'Mật khẩu'} name="password" rules={editing ? [] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 6, message: 'Mật khẩu tối thiểu 6 ký tự' }]}>
              <Input.Password placeholder={editing ? 'Bỏ trống nếu không đổi' : '123456'} />
            </Form.Item>
            <Form.Item label="Điện thoại" name="phone">
              <Input placeholder="0909123456" />
            </Form.Item>
          </div>
          <Form.Item label="Kỹ năng" name="skills">
            <Input.TextArea rows={3} placeholder="Ví dụ: Máy lạnh, tủ lạnh, điện dân dụng, bảo trì định kỳ..." />
          </Form.Item>
          <Form.Item name="active" valuePropName="checked">
            <Switch checkedChildren="Sẵn sàng" unCheckedChildren="Tạm ngưng" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
