import { DeleteOutlined, DownOutlined, DownloadOutlined, EditOutlined, FileExcelOutlined, PlusOutlined, SearchOutlined, UploadOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Dropdown, Empty, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Typography, Upload } from 'antd'
import { useEffect, useState, type ReactNode } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { customersApi } from '../../customers/api'
import { CsvImportPreviewModal } from '../../../components/CsvImportPreviewModal'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { BinaryStatusTag, MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { Customer, CustomerImportResult, CustomerImportRowResult } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { EMPTY_VALUE, formatDate } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useAuth } from '../../auth/AuthContext'

import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

type CustomerStatusFilter = 'all' | 'active' | 'inactive'

const CUSTOMER_STATUS_FILTER_OPTIONS = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Hoạt động' },
  { value: 'inactive', label: 'Ngừng hoạt động' },
] satisfies Array<{ value: CustomerStatusFilter; label: string }>

export function CustomersPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'OWNER' || user?.role === 'CUSTOMER_SERVICE'
  const [searchInput, setSearchInput] = useState('')
  const [statusFilter, setStatusFilter] = useState<CustomerStatusFilter>('all')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const activeFilter = statusFilter === 'all' ? undefined : statusFilter === 'active'
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Customer>()
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportFile, setBulkImportFile] = useState<File>()
  const [bulkImportResult, setBulkImportResult] = useState<CustomerImportResult>()
  const [form] = Form.useForm()
  const handleFormValidationFailed = useFormValidationFeedback()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const customersQuery = useQuery({
    queryKey: ['customers', { search, statusFilter, page, size: LIST_PAGE_SIZE }],
    queryFn: () => customersApi.list(search, page, LIST_PAGE_SIZE, activeFilter),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = customersQuery

  useEffect(() => {
    setPage(0)
  }, [search])

  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])

  const refreshRelatedViews = () => {
    queryClient.invalidateQueries({ queryKey: ['customers'] })
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
    queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => editing ? customersApi.update(editing.id, values) : customersApi.create(values),
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật khách hàng' : 'Đã tạo khách hàng')
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      refreshRelatedViews()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => customersApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá khách hàng')
      refreshRelatedViews()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const previewImport = useMutation({
    mutationFn: (file: File) => customersApi.importCsv(file, false),
    onSuccess: (result, file) => {
      setBulkImportFile(file)
      setBulkImportResult(result)
      setBulkImportOpen(true)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const commitImport = useMutation({
    mutationFn: () => customersApi.importCsv(bulkImportFile!, true),
    onSuccess: (result) => {
      setBulkImportResult(result)
      if (result.committed) {
        notification.success({
          message: 'Import khách hàng hoàn tất',
          description: `Đã thêm ${result.importedRows} khách hàng vào hệ thống.`,
        })
        setBulkImportOpen(false)
        setBulkImportFile(undefined)
        setBulkImportResult(undefined)
        refreshRelatedViews()
      }
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const exportCsv = async () => {
    try {
      downloadBlob(await customersApi.exportCsv(searchInput.trim()), 'serviceops-customers.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const downloadTemplate = async () => {
    try {
      downloadBlob(await customersApi.importTemplate(), 'serviceops-customers-template.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

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

  const customerActions = (
    <Space size={10} wrap>
      <Dropdown
        trigger={['click']}
        menu={{
          items: [
            {
              key: 'export',
              icon: <DownloadOutlined />,
              label: 'Xuất CSV',
              onClick: exportCsv,
            },
            {
              key: 'template',
              icon: <FileExcelOutlined />,
              label: 'Tải mẫu import',
              onClick: downloadTemplate,
            },
            {
              key: 'import',
              icon: <UploadOutlined />,
              label: (
                <Upload
                  accept=".csv,text/csv"
                  showUploadList={false}
                  beforeUpload={(file) => {
                    previewImport.mutate(file)
                    return Upload.LIST_IGNORE
                  }}
                >
                  <span>Nhập CSV</span>
                </Upload>
              ),
            },
          ],
        }}
      >
        <Button icon={<FileExcelOutlined />} loading={previewImport.isPending}>
          Dữ liệu <DownOutlined />
        </Button>
      </Dropdown>
      <Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm khách hàng</Button>
    </Space>
  )

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Quản lý khách hàng"
        title="Khách hàng"
        description="Quản lý liên hệ, địa chỉ phục vụ và trạng thái khách hàng trong một danh sách dễ quét."
        actions={canManage ? customerActions : undefined}
        meta={
          <>
            <MetaBadge>{customersQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} hồ sơ`}</MetaBadge>
            <MetaBadge tone={statusFilter === 'all' ? 'neutral' : 'info'}>
              {CUSTOMER_STATUS_FILTER_OPTIONS.find((option) => option.value === statusFilter)?.label}
            </MetaBadge>
          </>
        }
      />

      <CardlessTableToolbar>
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Tìm tên, mã, số điện thoại hoặc email"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
        <Select
          aria-label="Lọc trạng thái khách hàng"
          value={statusFilter}
          options={CUSTOMER_STATUS_FILTER_OPTIONS}
          onChange={(value) => {
            setStatusFilter(value)
            setPage(0)
          }}
        />
      </CardlessTableToolbar>

      {customersQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách khách hàng"
          error={customersQuery.error}
          onRetry={() => customersQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={customersQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: customersQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} khách hàng`,
        }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description={customersQuery.isError ? 'Không thể tải dữ liệu khách hàng' : 'Chưa có khách hàng phù hợp'} /> }}
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
          { title: 'Trạng thái', dataIndex: 'active', width: 130, render: (value: boolean) => <BinaryStatusTag active={value} inactiveLabel="Ngừng hoạt động" /> },
          { title: 'Ngày tạo', dataIndex: 'createdAt', width: 130, render: formatDate },
          {
            title: '',
            width: 92,
            hidden: !canManage,
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

      <Modal title={editing ? 'Cập nhật khách hàng' : 'Thêm khách hàng'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} okText={editing ? 'Lưu thay đổi' : 'Thêm khách hàng'} width={680} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
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

      <CsvImportPreviewModal<CustomerImportRowResult>
        title="Kiểm tra file nhập khách hàng"
        open={bulkImportOpen}
        result={bulkImportResult}
        committing={commitImport.isPending}
        onCancel={() => setBulkImportOpen(false)}
        onCommit={() => commitImport.mutate()}
        columns={[
          { title: 'Mã', dataIndex: 'code', width: 140 },
          { title: 'Tên khách hàng', dataIndex: 'name', ellipsis: true },
        ]}
      />
    </div>
  )
}

function CardlessTableToolbar({ children }: { children: ReactNode }) {
  return <div className="table-toolbar toolbar-row">{children}</div>
}
