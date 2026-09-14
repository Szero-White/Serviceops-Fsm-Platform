import { DeleteOutlined, EditOutlined, KeyOutlined, PlusOutlined, SearchOutlined, TeamOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Popconfirm, Space, Table, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { usersApi } from '../api'
import { useAuth } from '../../auth/AuthContext'
import { MetricCard } from '../../../components/MetricCard'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { BinaryStatusTag, MetaBadge, RoleTag } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { USER_ROLE_LABELS } from '../../../constants/userRoles'
import type { UserAccount, UserRole } from '../../../types'
import { formatDateTime } from '../../../utils/format'
import { compareDate, compareNumber, compareText } from '../../../utils/tableSort'
import { UserFormModal } from '../components/UserFormModal'

const roleDescriptions: Record<UserRole, string> = {
  OWNER: 'Quản trị hệ thống, người dùng, dữ liệu nghiệp vụ, điều phối, kho và nhật ký hệ thống.',
  DISPATCHER: 'Điều phối phiếu công việc, phân công và theo dõi lịch kỹ thuật viên.',
  CUSTOMER_SERVICE: 'Tiếp nhận yêu cầu, quản lý khách hàng và thiết bị.',
  TECHNICIAN: 'Xem việc được giao, cập nhật tiến độ, ghi nhận vật tư và bằng chứng.',
  WAREHOUSE_STAFF: 'Quản lý phụ tùng, nhập kho và theo dõi tồn.',
}

type UserStatusFilter = 'active' | 'inactive'

const userStatusFilterOptions: Array<{ value: UserStatusFilter; label: string }> = [
  { value: 'active', label: 'Hoạt động' },
  { value: 'inactive', label: 'Tạm ngưng' },
]

export function UsersPage() {
  const [search, setSearch] = useState('')
  const [statusFilters, setStatusFilters] = useState<UserStatusFilter[]>([])
  const [tablePage, setTablePage] = useState(1)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<UserAccount>()
  const [form] = Form.useForm()
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
    const matches = data.filter((account) => {
      const statusMatches =
        statusFilters.length === 0
        || (statusFilters.includes('active') && account.active)
        || (statusFilters.includes('inactive') && !account.active)

      if (!statusMatches) {
        return false
      }

      if (!keyword) {
        return true
      }

      return [account.displayName, account.username, USER_ROLE_LABELS[account.role], account.phone, account.skills]
        .some((value) => value?.toLowerCase().includes(keyword))
    })

    return matches.sort((a, b) => compareDate(b.createdAt, a.createdAt))
  }, [data, search, statusFilters])

  useEffect(() => {
    const totalPages = Math.max(Math.ceil(filtered.length / LIST_PAGE_SIZE), 1)
    if (tablePage > totalPages) {
      setTablePage(totalPages)
    }
  }, [filtered.length, tablePage])

  const ownerCount = data.filter((account) => account.role === 'OWNER' && account.active).length
  const activeCount = data.filter((account) => account.active).length
  const technicianCount = data.filter((account) => account.role === 'TECHNICIAN').length

  const resultCountLabel = search.trim() || statusFilters.length > 0
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
      const technicianReactivated = Boolean(
        editing
        && editing.role === 'TECHNICIAN'
        && !editing.active
        && savedAccount.active,
      )
      notification.success({
        message: editing ? 'Đã cập nhật tài khoản' : 'Đã tạo tài khoản',
        description: technicianReactivated
          ? `${savedAccount.displayName} · Kỹ thuật viên · Hoạt động. Trạng thái đã đồng bộ với Đội ngũ kỹ thuật.`
          : `${savedAccount.displayName} · ${USER_ROLE_LABELS[savedAccount.role]} · ${savedAccount.active ? 'Hoạt động' : 'Tạm ngưng'}`,
      })
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      setTablePage(1)
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
        description="Chủ sở hữu tạo tài khoản cho nhân sự, phân vai trò theo trách nhiệm và kiểm soát trạng thái truy cập."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm người dùng</Button>}
        meta={
          <>
            <MetaBadge>{isError ? 'Lỗi tải dữ liệu' : resultCountLabel}</MetaBadge>
            <MetaBadge tone={statusFilters.length === 1 ? 'info' : 'neutral'}>{statusFilters.length === 1 ? userStatusFilterOptions.find((option) => option.value === statusFilters[0])?.label : 'Tất cả trạng thái'}</MetaBadge>
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
          placeholder="Tìm tên, tên đăng nhập, vai trò, điện thoại hoặc kỹ năng"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setTablePage(1)
          }}
        />
        <CheckboxFilterSelect
          ariaLabel="Lọc trạng thái tài khoản"
          placeholder="Tất cả trạng thái"
          value={statusFilters}
          options={userStatusFilterOptions}
          onChange={(value) => { setStatusFilters(value as UserStatusFilter[]); setTablePage(1) }}
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
            sorter: (a, b) => compareText(a.displayName, b.displayName),
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.displayName}</Typography.Text>
                <Typography.Text type="secondary">
                  @{record.username}{record.protectedDemo ? ' · Tài khoản mẫu cố định' : ''}
                </Typography.Text>
              </div>
            ),
          },
          { title: 'Vai trò', dataIndex: 'role', width: 160, sorter: (a, b) => compareText(a.role, b.role), render: (role: UserRole) => <RoleTag role={role} /> },
          { title: 'Phạm vi trách nhiệm', dataIndex: 'role', ellipsis: true, sorter: (a, b) => compareText(roleDescriptions[a.role], roleDescriptions[b.role]), render: (role: UserRole) => roleDescriptions[role] },
          { title: 'Trạng thái', dataIndex: 'active', width: 140, sorter: (a, b) => compareNumber(Number(a.active), Number(b.active)), render: (active: boolean) => <BinaryStatusTag active={active} /> },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, sorter: (a, b) => compareDate(a.updatedAt, b.updatedAt), render: formatDateTime },
          {
            title: 'Thao tác',
            width: 100,
            fixed: 'right' as const,
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
                    title={isProtectedDemo ? 'Tài khoản mẫu cố định được bảo vệ' : 'Sửa người dùng'}
                    onClick={() => showEdit(record)}
                  />
                  <Popconfirm
                    title={isProtectedDemo ? 'Tài khoản mẫu cố định' : 'Xóa người dùng này?'}
                    description={
                      isProtectedDemo
                        ? 'Tài khoản này cần được giữ nguyên để bảo đảm bản dùng thử công khai luôn hoạt động.'
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
                      title={isProtectedDemo ? 'Tài khoản mẫu cố định được bảo vệ' : 'Xóa người dùng'}
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              )
            },
          },
        ]}
      />

      <UserFormModal
        open={open}
        editing={editing}
        currentUserId={currentUser?.id}
        selectedRole={selectedRole}
        form={form}
        saving={save.isPending}
        onCancel={() => setOpen(false)}
        onSubmit={(values) => save.mutate(values)}
      />
    </div>
  )
}
