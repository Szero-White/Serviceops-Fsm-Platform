import { EditOutlined, PhoneOutlined, PlusOutlined, SearchOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Avatar, Button, Empty, Form, Input, Modal, Switch, Table, Tooltip, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { MetricCard } from '../../../components/MetricCard'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { BinaryStatusTag, MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { Technician } from '../../../types'
import { EMPTY_VALUE } from '../../../utils/format'
import { compareDate, compareText, compareNumber } from '../../../utils/tableSort'
import { useAuth } from '../../auth/AuthContext'
import { techniciansApi } from '../api'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

type TechnicianProfileValues = {
  phone?: string
  skills?: string
  active: boolean
}

export function TechniciansPage() {
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Technician>()
  const [form] = Form.useForm<TechnicianProfileValues>()
  const handleFormValidationFailed = useFormValidationFeedback()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const techniciansQuery = useQuery({
    queryKey: ['technicians', 'all'],
    queryFn: () => techniciansApi.list(false),
  })
  const { data = [], isLoading } = techniciansQuery

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    const matches = keyword
      ? data.filter((technician) =>
          [technician.name, technician.username, technician.phone, technician.skills]
            .some((value) => value?.toLowerCase().includes(keyword)),
        )
      : data

    return [...matches].sort((a, b) => compareDate(b.createdAt, a.createdAt))
  }, [data, search])

  const activeCount = data.filter(
    (technician) => technician.active && technician.accountActive,
  ).length
  const pausedCount = data.length - activeCount
  const skilledCount = data.filter((technician) => technician.skills?.trim()).length
  const canManageAccounts = user?.role === 'OWNER'
  const canManageProfiles = user?.role === 'OWNER'

  const updateProfile = useMutation({
    mutationFn: (values: TechnicianProfileValues) => {
      if (!editing) {
        throw new Error('Không có kỹ thuật viên đang được chỉnh sửa')
      }

      return techniciansApi.updateProfile(editing.id, values)
    },
    onSuccess: () => {
      message.success('Đã cập nhật kỹ thuật viên và đồng bộ trạng thái tài khoản')
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      queryClient.invalidateQueries({ queryKey: ['work-order'] })
      queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
      queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
      queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const showEdit = (record: Technician) => {
    setEditing(record)
    form.setFieldsValue({
      phone: record.phone,
      skills: record.skills,
      active: record.active && record.accountActive,
    })
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Nhân sự hiện trường"
        title="Đội ngũ kỹ thuật"
        description="Quản lý hồ sơ nghiệp vụ, kỹ năng và trạng thái hoạt động của đội ngũ kỹ thuật. Trạng thái được đồng bộ hai chiều với Người dùng."
        actions={
          canManageAccounts ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/users?create=technician')}
            >
              Thêm kỹ thuật viên
            </Button>
          ) : undefined
        }
        meta={<MetaBadge>{techniciansQuery.isError ? 'Lỗi tải dữ liệu' : `${data.length} kỹ thuật viên`}</MetaBadge>}
      />

      <div className="channel-summary-grid">
        <MetricCard label="Hoạt động" value={activeCount} helper="Có thể đăng nhập và nhận lịch mới" icon={<UserOutlined />} tone="success" />
        <MetricCard label="Có kỹ năng" value={skilledCount} helper="Đã khai báo năng lực" icon={<ToolOutlined />} tone="primary" />
        <MetricCard label="Tạm ngưng" value={pausedCount} helper="Không đăng nhập và không nhận lịch mới" icon={<PhoneOutlined />} tone="warning" />
      </div>

      <div className="table-toolbar">
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm tên, tên đăng nhập, số điện thoại hoặc kỹ năng"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {techniciansQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách kỹ thuật viên"
          error={techniciansQuery.error}
          onRetry={() => techniciansQuery.refetch()}
        />
      )}

      <Table<Technician>
        rowKey="id"
        loading={isLoading}
        dataSource={techniciansQuery.isError ? [] : filtered}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{ pageSize: LIST_PAGE_SIZE, showSizeChanger: false }}
        locale={{ emptyText: <Empty description={techniciansQuery.isError ? 'Không thể tải dữ liệu kỹ thuật viên' : 'Chưa có kỹ thuật viên phù hợp'} /> }}
        columns={[
          {
            title: 'Kỹ thuật viên',
            width: 320,
            sorter: (a, b) => compareText(a.name, b.name),
            render: (_, record) => (
              <div className="technician-name-cell">
                <Avatar size={42} icon={<UserOutlined />} />
                <div>
                  <Typography.Text strong>{record.name}</Typography.Text>
                  <Typography.Text type="secondary">
                    @{record.username}{record.protectedDemo ? ' · Tài khoản mẫu cố định' : ''}
                  </Typography.Text>
                </div>
              </div>
            ),
          },
          { title: 'Điện thoại', dataIndex: 'phone', width: 150, sorter: (a, b) => compareText(a.phone, b.phone), render: (value) => value || EMPTY_VALUE },
          { title: 'Kỹ năng', dataIndex: 'skills', ellipsis: true, sorter: (a, b) => compareText(a.skills, b.skills), render: (value) => value || EMPTY_VALUE },
          {
            title: 'Trạng thái',
            width: 150,
            sorter: (a, b) => compareNumber(Number(a.active && a.accountActive), Number(b.active && b.accountActive)),
            render: (_, record) => {
              const active = record.active && record.accountActive
              return <BinaryStatusTag active={active} activeLabel="Hoạt động" inactiveLabel="Tạm ngưng" />
            },
          },
          {
            title: 'Thao tác',
            width: canManageProfiles ? 76 : 24,
            fixed: 'right' as const,
            align: 'center' as const,
            render: (_, record) => canManageProfiles ? (
              <Tooltip title={record.protectedDemo ? 'Tài khoản mẫu cố định được bảo vệ' : 'Sửa hồ sơ kỹ thuật viên'}>
                <Button
                  aria-label="Sửa hồ sơ kỹ thuật viên"
                  type="text"
                  icon={<EditOutlined />}
                  disabled={Boolean(record.protectedDemo)}
                  onClick={() => showEdit(record)}
                />
              </Tooltip>
            ) : null,
          },
        ]}
      />

      <Modal
        title="Cập nhật hồ sơ kỹ thuật viên"
        open={Boolean(editing)}
        onCancel={() => {
          setEditing(undefined)
          form.resetFields()
        }}
        onOk={() => form.submit()}
        okText="Lưu thay đổi"
        confirmLoading={updateProfile.isPending}
        width={620}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">
          Tên đăng nhập, mật khẩu và vai trò được quản lý tại màn Người dùng. Thay đổi trạng thái ở một trong hai màn hình sẽ tự cập nhật ở màn còn lại.
        </Typography.Paragraph>

        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => updateProfile.mutate(values)}
          onFinishFailed={handleFormValidationFailed}
          scrollToFirstError
          requiredMark
        >
          <Form.Item label="Điện thoại" name="phone">
            <Input placeholder="0909123456" />
          </Form.Item>
          <Form.Item label="Kỹ năng" name="skills">
            <Input.TextArea rows={3} placeholder="Máy lạnh, tủ lạnh, điện dân dụng, bảo trì định kỳ..." />
          </Form.Item>
          <Form.Item
            label="Trạng thái"
            name="active"
            valuePropName="checked"
            extra="Thay đổi tại đây sẽ đồng thời cập nhật trạng thái tài khoản ở Người dùng. Thay đổi tại Người dùng cũng được phản ánh ngược lại tại đây."
          >
            <Switch
              checkedChildren="Hoạt động"
              unCheckedChildren="Tạm ngưng"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
