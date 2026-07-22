import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Modal, Space, Switch, Table, Tag, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { customersApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import type { Customer } from '../types'
import { formatDate } from '../utils/format'

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
      setOpen(false); setEditing(undefined); form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    form.setFieldsValue({ code: `KH-${Date.now().toString().slice(-5)}`, active: true })
    setOpen(true)
  }

  const showEdit = (record: Customer) => {
    setEditing(record)
    form.setFieldsValue(record)
    setOpen(true)
  }

  return (
    <div>
      <PageHeader title="Khách hàng" description="Quản lý thông tin liên hệ và địa chỉ phục vụ." actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm khách hàng</Button>} />
      <div className="table-toolbar"><Input allowClear prefix={<SearchOutlined />} placeholder="Tìm theo tên, mã hoặc số điện thoại" value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 950 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        columns={[
          { title: 'Mã', dataIndex: 'code', width: 120, render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
          { title: 'Tên khách hàng', dataIndex: 'name', width: 220, render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
          { title: 'Điện thoại', dataIndex: 'phone', width: 140, render: (v) => v || '—' },
          { title: 'Email', dataIndex: 'email', width: 220, render: (v) => v || '—' },
          { title: 'Địa chỉ', dataIndex: 'address', ellipsis: true },
          { title: 'Trạng thái', dataIndex: 'active', width: 120, render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? 'Hoạt động' : 'Ngừng'}</Tag> },
          { title: 'Ngày tạo', dataIndex: 'createdAt', width: 120, render: formatDate },
          { title: '', width: 60, fixed: 'right', render: (_, record) => <Button type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} /> },
        ]}
      />

      <Modal title={editing ? 'Cập nhật khách hàng' : 'Thêm khách hàng'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={680} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Mã khách hàng" name="code" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Tên khách hàng" name="name" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="Số điện thoại" name="phone"><Input /></Form.Item>
            <Form.Item label="Email" name="email" rules={[{ type: 'email' }]}><Input /></Form.Item>
          </div>
          <Form.Item label="Địa chỉ" name="address"><Input /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item label="Đang hoạt động" name="active" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
