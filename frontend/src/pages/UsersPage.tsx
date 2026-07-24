import { DeleteOutlined, EditOutlined, KeyOutlined, PlusOutlined, SearchOutlined, TeamOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { usersApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import type { UserAccount, UserRole } from '../types'
import { formatDateTime } from '../utils/format'

const roleLabels: Record<UserRole, string> = {
  OWNER: 'Chủ sở hữu',
  DISPATCHER: 'Điều phối',
  CUSTOMER_SERVICE: 'CSKH',
  TECHNICIAN: 'Kỹ thuật viên',
  WAREHOUSE_STAFF: 'Nhân viên kho',
}

const roleDescriptions: Record<UserRole, string> = {
  OWNER: 'Quản trị hệ thống, người dùng, danh mục, báo cáo và audit.',
  DISPATCHER: 'Điều phối work order, phân công và theo dõi lịch kỹ thuật viên.',
  CUSTOMER_SERVICE: 'Tiếp nhận yêu cầu, quản lý khách hàng và thiết bị.',
  TECHNICIAN: 'Xem việc được giao, cập nhật tiến độ, ghi nhận vật tư và bằng chứng.',
  WAREHOUSE_STAFF: 'Quản lý phụ tùng, nhập kho và theo dõi tồn.',
}

const roleColors: Record<UserRole, string> = {
  OWNER: 'gold',
  DISPATCHER: 'blue',
  CUSTOMER_SERVICE: 'cyan',
  TECHNICIAN: 'green',
  WAREHOUSE_STAFF: 'purple',
}

const roleOptions = Object.entries(roleLabels).map(([value, label]) => ({
  value,
  label,
}))

function usernameFromName(value: string) {
  const slug = value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^a-zA-Z0-9]+/g, '.')
    .replace(/^\.+|\.+$/g, '')
    .toLowerCase()

  return slug || `user.${Date.now().toString().slice(-5)}`
}

export function UsersPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<UserAccount>()
  const [form] = Form.useForm()
  const selectedRole = Form.useWatch('role', form)
  const { user: currentUser } = useAuth()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data = [], isError, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.list,
  })

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) {
      return data
    }
    return data.filter((account) =>
      [account.displayName, account.username, roleLabels[account.role], account.phone, account.skills].some((value) => value?.toLowerCase().includes(keyword)),
    )
  }, [data, search])

  const ownerCount = data.filter((account) => account.role === 'OWNER' && account.active).length
  const activeCount = data.filter((account) => account.active).length
  const technicianCount = data.filter((account) => account.role === 'TECHNICIAN').length

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = { ...values }
      if (!payload.password) {
        delete payload.password
      }
      return editing ? usersApi.update(editing.id, payload) : usersApi.create(payload)
    },
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật người dùng' : 'Đã tạo người dùng')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá người dùng')
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showCreate = () => {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ role: 'DISPATCHER', active: true })
    setOpen(true)
  }

  const showEdit = (record: UserAccount) => {
    setEditing(record)
    form.setFieldsValue({ ...record, password: undefined })
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Access control"
        title="Người dùng & phân quyền"
        description="OWNER tạo tài khoản cho nhân sự, phân vai trò theo trách nhiệm và kiểm soát trạng thái truy cập."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm người dùng</Button>}
        meta={<Tag color="blue">{data.length} tài khoản</Tag>}
      />

      <div className="channel-summary-grid">
        <MetricCard label="Đang hoạt động" value={activeCount} helper="Có thể đăng nhập hệ thống" icon={<TeamOutlined />} tone="green" />
        <MetricCard label="Chủ sở hữu" value={ownerCount} helper="Tài khoản quản trị cao nhất" icon={<UserSwitchOutlined />} tone="blue" />
        <MetricCard label="Kỹ thuật viên" value={technicianCount} helper="Đồng bộ hồ sơ phân công" icon={<KeyOutlined />} tone="purple" />
      </div>

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm tên, username, vai trò hoặc kỹ năng" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      {isError && (
        <Alert
          showIcon
          type="error"
          message="Chưa tải được danh sách người dùng"
          description="Hãy restart backend để API /api/v1/users mới được nạp, sau đó tải lại trang."
          style={{ marginBottom: 14 }}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={filtered}
        className="content-table"
        scroll={{ x: 1120 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có người dùng phù hợp" /> }}
        columns={[
          {
            title: 'Người dùng',
            width: 300,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.displayName}</Typography.Text>
                <Typography.Text type="secondary">@{record.username}</Typography.Text>
              </div>
            ),
          },
          { title: 'Vai trò', dataIndex: 'role', width: 160, render: (role: UserRole) => <Tag color={roleColors[role]}>{roleLabels[role]}</Tag> },
          { title: 'Phạm vi trách nhiệm', dataIndex: 'role', ellipsis: true, render: (role: UserRole) => roleDescriptions[role] },
          { title: 'Trạng thái', dataIndex: 'active', width: 140, render: (active: boolean) => <Tag color={active ? 'green' : 'default'}>{active ? 'Hoạt động' : 'Tạm ngưng'}</Tag> },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
          {
            title: '',
            width: 92,
            fixed: 'right',
            render: (_, record) => {
              const isSelf = currentUser?.id === record.id
              return (
                <Space size={4}>
                  <Button aria-label="Sửa người dùng" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                  <Popconfirm
                    title="Xoá người dùng này?"
                    description={isSelf ? 'Không thể xóa tài khoản đang đăng nhập.' : 'Chỉ xoá được khi người dùng chưa bị ràng buộc dữ liệu vận hành.'}
                    okText="Xoá"
                    cancelText="Huỷ"
                    okButtonProps={{ danger: true, loading: remove.isPending, disabled: isSelf }}
                    onConfirm={() => remove.mutate(record.id)}
                  >
                    <Button aria-label="Xoá người dùng" type="text" danger disabled={isSelf} icon={<DeleteOutlined />} />
                  </Popconfirm>
                </Space>
              )
            },
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật người dùng' : 'Thêm người dùng'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={760} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Họ tên" name="displayName" rules={[{ required: true, message: 'Nhập họ tên người dùng' }]}>
              <Input
                placeholder="Ví dụ: Lê Thu Điều phối"
                onBlur={(event) => !editing && !form.getFieldValue('username') && form.setFieldValue('username', usernameFromName(event.target.value))}
              />
            </Form.Item>
            <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
              <Input placeholder="le.thu.dieu.phoi" />
            </Form.Item>
            <Form.Item label="Vai trò" name="role" rules={[{ required: true, message: 'Chọn vai trò' }]}>
              <Select options={roleOptions} disabled={editing?.id === currentUser?.id} />
            </Form.Item>
            <Form.Item label={editing ? 'Mật khẩu mới' : 'Mật khẩu'} name="password" rules={editing ? [] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 6, message: 'Mật khẩu tối thiểu 6 ký tự' }]}>
              <Input.Password placeholder={editing ? 'Bỏ trống nếu không đổi' : 'Tối thiểu 6 ký tự'} />
            </Form.Item>
            {selectedRole === 'TECHNICIAN' && (
              <>
                <Form.Item label="Điện thoại kỹ thuật viên" name="phone">
                  <Input placeholder="0909123456" />
                </Form.Item>
                <Form.Item label="Kỹ năng kỹ thuật viên" name="skills">
                  <Input placeholder="Máy lạnh, tủ lạnh, điện dân dụng..." />
                </Form.Item>
              </>
            )}
          </div>
          <Form.Item name="active" valuePropName="checked">
            <Switch disabled={editing?.id === currentUser?.id} checkedChildren="Hoạt động" unCheckedChildren="Tạm ngưng" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
