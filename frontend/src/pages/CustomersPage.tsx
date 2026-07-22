import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import { apiErrorMessage } from '../api/http'
import { customersApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import type { Customer } from '../types'
import { EMPTY_VALUE, formatDate } from '../utils/format'

export function CustomersPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Customer>()
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['customers', search], queryFn: () => customersApi.list(search) })

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => editing ? customersApi.update(editing.id, values) : customersApi.create(values),
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật khách hàng' : 'Đã tạo khách hàng')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => customersApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá khách hàng')
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ code: `KH-${Date.now().toString().slice(-5)}`, active: true })
    setOpen(true)
  }

  const showEdit = (record: Customer) => {
    setEditing(record)
    form.setFieldsValue(record)
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Customer operations"
        title="Khách hàng"
        description="Quản lý liên hệ, địa chỉ phục vụ và trạng thái khách hàng trong một danh sách dễ quét."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm khách hàng</Button>}
        meta={<Tag color="blue">{data?.totalElements ?? 0} hồ sơ</Tag>}
      />

      <CardlessTableToolbar>
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm tên, mã, số điện thoại hoặc email"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </CardlessTableToolbar>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có khách hàng phù hợp" /> }}
        columns={[
          {
            title: 'Khách hàng',
            dataIndex: 'name',
            width: 280,
            render: (value: string, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{value}</Typography.Text>
                <Typography.Text type="secondary" code>{record.code}</Typography.Text>
              </div>
            ),
          },
          {
            title: 'Liên hệ',
            width: 240,
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{record.phone || EMPTY_VALUE}</span>
                <Typography.Text type="secondary">{record.email || EMPTY_VALUE}</Typography.Text>
              </div>
            ),
          },
          { title: 'Địa chỉ', dataIndex: 'address', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Trạng thái', dataIndex: 'active', width: 130, render: (value: boolean) => <Tag color={value ? 'green' : 'default'}>{value ? 'Hoạt động' : 'Ngừng'}</Tag> },
          { title: 'Ngày tạo', dataIndex: 'createdAt', width: 130, render: formatDate },
          {
            title: '',
            width: 92,
            fixed: 'right',
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa khách hàng" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xoá khách hàng này?"
                  description="Chỉ xoá được khi khách hàng chưa được dùng trong dữ liệu nghiệp vụ."
                  okText="Xoá"
                  cancelText="Huỷ"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(record.id)}
                >
                  <Button aria-label="Xoá khách hàng" type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật khách hàng' : 'Thêm khách hàng'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={680} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Mã khách hàng" name="code" rules={[{ required: true, message: 'Nhập mã khách hàng' }]}><Input /></Form.Item>
            <Form.Item label="Tên khách hàng" name="name" rules={[{ required: true, message: 'Nhập tên khách hàng' }]}><Input /></Form.Item>
            <Form.Item label="Số điện thoại" name="phone"><Input /></Form.Item>
            <Form.Item label="Email" name="email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input /></Form.Item>
          </div>
          <Form.Item label="Địa chỉ" name="address"><Input /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item label="Đang hoạt động" name="active" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function CardlessTableToolbar({ children }: { children: ReactNode }) {
  return <div className="table-toolbar">{children}</div>
}
