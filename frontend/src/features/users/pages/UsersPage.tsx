import { DeleteOutlined, EditOutlined, KeyOutlined, PlusOutlined, SearchOutlined, TeamOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { usersApi } from '../api'
import { useAuth } from '../../auth/AuthContext'
import { MetricCard } from '../../../components/MetricCard'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { BinaryStatusTag, MetaBadge, RoleTag } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { USER_ROLE_LABELS } from '../../../constants/userRoles'
import type { UserAccount, UserRole } from '../../../types'
import { formatDateTime } from '../../../utils/format'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

const roleDescriptions: Record<UserRole, string> = {
  OWNER: 'Quản trị hệ thống, người dùng, dữ liệu nghiệp vụ, điều phối, kho và audit.',
  DISPATCHER: 'Điều phối phiếu công việc, phân công và theo dõi lịch kỹ thuật viên.',
  CUSTOMER_SERVICE: 'Tiếp nhận yêu cầu, quản lý khách hàng và thiết bị.',
  TECHNICIAN: 'Xem việc được giao, cập nhật tiến độ, ghi nhận vật tư và bằng chứng.',
  WAREHOUSE_STAFF: 'Quản lý phụ tùng, nhập kho và theo dõi tồn.',
}

const roleOptions = Object.entries(USER_ROLE_LABELS).map(([value, label]) => ({
  value,
  label,
}))

type UserStatusFilter = 'all' | 'active' | 'inactive'

const userStatusFilterOptions: Array<{ value: UserStatusFilter; label: string }> = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Hoạt động' },
  { value: 'inactive', label: 'Tạm ngưng' },
]

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
  const [statusFilter, setStatusFilter] = useState<UserStatusFilter>('all')
  const [tablePage, setTablePage] = useState(1)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<UserAccount>()
  const [form] = Form.useForm()
  const handleFormValidationFailed = useFormValidationFeedback()
  const selectedRole = Form.useWatch('role', form)
  const { user: currentUser } = useAuth()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()

  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.list,
  })
  const { data = [], isError, isLoading } = usersQuery

  useEffect(() => {
    if (searchParams.get('create') !== 'technician') {
      return
    }

    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({ role: 'TECHNICIAN', active: true })
    setOpen(true)
    setSearchParams({}, { replace: true })
  }, [form, searchParams, setSearchParams])

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    return data.filter((account) => {
      const statusMatches =
        statusFilter === 'all'
        || (statusFilter === 'active' && account.active)
        || (statusFilter === 'inactive' && !account.active)

      if (!statusMatches) {
        return false
      }

      if (!keyword) {
        return true
      }

      return [account.displayName, account.username, USER_ROLE_LABELS[account.role], account.phone, account.skills]
        .some((value) => value?.toLowerCase().includes(keyword))
    })
  }, [data, search, statusFilter])

  useEffect(() => {
    const totalPages = Math.max(Math.ceil(filtered.length / LIST_PAGE_SIZE), 1)
    if (tablePage > totalPages) {
      setTablePage(totalPages)
    }
  }, [filtered.length, tablePage])

  const ownerCount = data.filter((account) => account.role === 'OWNER' && account.active).length
  const activeCount = data.filter((account) => account.active).length
  const technicianCount = data.filter((account) => account.role === 'TECHNICIAN').length

  const resultCountLabel = search.trim() || statusFilter !== 'all'
    ? `${filtered.length}/${data.length} tài khoản`
    : `${data.length} tài khoản`

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = { ...values }
      if (!payload.password) {
        delete payload.password
      }
      return editing ? usersApi.update(editing.id, payload) : usersApi.create(payload)
    },
    onSuccess: (savedAccount) => {
      notification.success({
        message: editing ? 'Đã cập nhật tài khoản' : 'Đã tạo tài khoản',
        description: savedAccount.role === 'TECHNICIAN' && savedAccount.active
          ? `${savedAccount.displayName} · ${USER_ROLE_LABELS[savedAccount.role]} · Hoạt động. Trạng thái sẵn sàng điều phối của hồ sơ kỹ thuật viên được quản lý riêng.`
          : `${savedAccount.displayName} · ${USER_ROLE_LABELS[savedAccount.role]} · ${savedAccount.active ? 'Hoạt động' : 'Tạm ngưng'}`,
      })
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      queryClient.invalidateQueries({ queryKey: ['work-order'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
      queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: (_, removedId) => {
      const removedAccount = data.find((account) => account.id === removedId)
      notification.success({
        message: 'Đã xóa tài khoản',
        description: removedAccount
          ? `${removedAccount.displayName} · ${USER_ROLE_LABELS[removedAccount.role]}`
          : 'Tài khoản đã được xóa khỏi hệ thống.',
      })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      queryClient.invalidateQueries({ queryKey: ['work-order'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
      queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
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
        eyebrow="Quản trị truy cập"
        title="Người dùng & phân quyền"
        description="OWNER tạo tài khoản cho nhân sự, phân vai trò theo trách nhiệm và kiểm soát trạng thái truy cập."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm người dùng</Button>}
        meta={
          <>
            <MetaBadge>{isError ? 'Lỗi tải dữ liệu' : resultCountLabel}</MetaBadge>
            <MetaBadge tone={statusFilter === 'all' ? 'neutral' : 'info'}>
              {userStatusFilterOptions.find((option) => option.value === statusFilter)?.label}
            </MetaBadge>
          </>
        }
      />

      <div className="channel-summary-grid">
        <MetricCard label="Đang hoạt động" value={activeCount} helper="Có thể đăng nhập hệ thống" icon={<TeamOutlined />} tone="success" />
        <MetricCard label="Chủ sở hữu" value={ownerCount} helper="Tài khoản quản trị cao nhất" icon={<UserSwitchOutlined />} tone="primary" />
        <MetricCard label="Kỹ thuật viên" value={technicianCount} helper="Đồng bộ hồ sơ phân công" icon={<KeyOutlined />} tone="primary" />
      </div>

      <div className="table-toolbar toolbar-row">
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm tên, username, vai trò, điện thoại hoặc kỹ năng"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setTablePage(1)
          }}
        />
        <Select
          aria-label="Lọc trạng thái tài khoản"
          value={statusFilter}
          options={userStatusFilterOptions}
          onChange={(value) => {
            setStatusFilter(value)
            setTablePage(1)
          }}
        />
      </div>

      {isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách người dùng"
          error={usersQuery.error}
          onRetry={() => usersQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={isError ? [] : filtered}
        className="content-table"
        scroll={{ x: 1120 }}
        pagination={{
          current: tablePage,
          pageSize: LIST_PAGE_SIZE,
          showSizeChanger: false,
          onChange: setTablePage,
        }}
        locale={{ emptyText: <Empty description={isError ? 'Không thể tải dữ liệu người dùng' : 'Chưa có người dùng phù hợp'} /> }}
        columns={[
          {
            title: 'Người dùng',
            width: 300,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.displayName}</Typography.Text>
                <Typography.Text type="secondary">
                  @{record.username}{record.protectedDemo ? ' · Demo cố định' : ''}
                </Typography.Text>
              </div>
            ),
          },
          { title: 'Vai trò', dataIndex: 'role', width: 160, render: (role: UserRole) => <RoleTag role={role} /> },
          { title: 'Phạm vi trách nhiệm', dataIndex: 'role', ellipsis: true, render: (role: UserRole) => roleDescriptions[role] },
          { title: 'Trạng thái', dataIndex: 'active', width: 140, render: (active: boolean) => <BinaryStatusTag active={active} /> },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
          {
            title: '',
            width: 92,
            render: (_, record) => {
              const isSelf = currentUser?.id === record.id
              const isProtectedDemo = Boolean(record.protectedDemo)
              const deleteBlocked = isSelf || isProtectedDemo

              return (
                <Space size={4}>
                  <Button
                    aria-label="Sửa người dùng"
                    type="text"
                    icon={<EditOutlined />}
                    disabled={isProtectedDemo}
                    title={isProtectedDemo ? 'Tài khoản demo cố định được bảo vệ' : undefined}
                    onClick={() => showEdit(record)}
                  />
                  <Popconfirm
                    title={isProtectedDemo ? 'Tài khoản demo cố định' : 'Xóa người dùng này?'}
                    description={
                      isProtectedDemo
                        ? 'Tài khoản này cần được giữ nguyên để bảo đảm luồng public demo luôn hoạt động.'
                        : isSelf
                          ? 'Không thể xóa tài khoản đang đăng nhập.'
                          : 'Chỉ xóa được khi người dùng chưa bị ràng buộc dữ liệu vận hành.'
                    }
                    okText="Xóa"
                    cancelText="Hủy"
                    okButtonProps={{ danger: true, loading: remove.isPending, disabled: deleteBlocked }}
                    onConfirm={() => {
                      if (!deleteBlocked) {
                        remove.mutate(record.id)
                      }
                    }}
                  >
                    <Button
                      aria-label="Xóa người dùng"
                      type="text"
                      danger
                      disabled={deleteBlocked}
                      title={isProtectedDemo ? 'Tài khoản demo cố định được bảo vệ' : undefined}
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              )
            },
          },
        ]}
      />

      <Modal
        title={
          editing
            ? 'Cập nhật người dùng'
            : selectedRole === 'TECHNICIAN'
              ? 'Thêm kỹ thuật viên'
              : 'Thêm người dùng'
        }
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        okText={editing ? 'Lưu thay đổi' : 'Tạo người dùng'}
        confirmLoading={save.isPending}
        width={760}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          <div className="form-grid two-cols">
            <Form.Item label="Họ tên" name="displayName" rules={[{ required: true, message: 'Nhập họ tên người dùng' }]}>
              <Input
                placeholder="Ví dụ: Lê Thu Điều phối"
                onBlur={(event) => !editing && !form.getFieldValue('username') && form.setFieldValue('username', usernameFromName(event.target.value))}
              />
            </Form.Item>
            <Form.Item label={editing ? 'Tên đăng nhập (không thể thay đổi)' : 'Tên đăng nhập'} name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
              <Input placeholder="le.thu.dieu.phoi" disabled={Boolean(editing)} />
            </Form.Item>
            <Form.Item label={editing ? 'Vai trò (không thể thay đổi)' : 'Vai trò'} name="role" rules={[{ required: true, message: 'Chọn vai trò' }]}>
              <Select options={roleOptions} disabled={Boolean(editing)} />
            </Form.Item>
            <Form.Item label={editing ? 'Mật khẩu mới' : 'Mật khẩu'} name="password" rules={editing ? [] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
              <Input.Password placeholder={editing ? 'Bỏ trống nếu không đổi' : 'Tối thiểu 8 ký tự'} />
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
