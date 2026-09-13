import { DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Popconfirm, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { assetsApi } from '../../assets/api'
import { customersApi } from '../../customers/api'
import { CsvImportPreviewModal } from '../../../components/CsvImportPreviewModal'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge, WarrantyTag } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { StatusTag } from '../../../components/StatusTag'
import type { Asset, AssetImportResult, AssetImportRowResult } from '../../../types'
import { downloadBlob } from '../../../utils/download'
import { formatDate } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useAuth } from '../../auth/AuthContext'
import { compareText, resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'
import { AssetFormModal } from '../components/AssetFormModal'
import { AssetPageActions } from '../components/AssetPageActions'

export function AssetsPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'OWNER' || user?.role === 'CUSTOMER_SERVICE'
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>({ sortBy: 'createdAt', sortDir: 'desc' })
  const search = useDebouncedValue(searchInput.trim())
  const [open, setOpen] = useState(false)
  const [customerOptionSearchInput, setCustomerOptionSearchInput] = useState('')
  const customerOptionSearch = useDebouncedValue(customerOptionSearchInput.trim())
  const [editing, setEditing] = useState<Asset>()
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportFile, setBulkImportFile] = useState<File>()
  const [bulkImportResult, setBulkImportResult] = useState<AssetImportResult>()
  const [form] = Form.useForm()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const assetsQuery = useQuery({
    queryKey: ['assets', { search, page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => assetsApi.list(search, page, LIST_PAGE_SIZE, undefined, sort.sortBy, sort.sortDir),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = assetsQuery

  useEffect(() => {
    setPage(0)
  }, [search])

  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])
  const customersQuery = useQuery({
    queryKey: ['customers', 'active-options', customerOptionSearch],
    queryFn: () => customersApi.list(customerOptionSearch, 0, LIST_PAGE_SIZE, true),
    enabled: canManage && open,
    placeholderData: keepPreviousData,
  })
  const customers = customersQuery.data
  const customerOptions = useMemo(() => {
    const options = (customers?.content ?? []).map((customer) => ({
      value: customer.id,
      label: `${customer.code} · ${customer.name}`,
    }))
    if (editing && !options.some((option) => option.value === editing.customerId)) {
      return [
        {
          value: editing.customerId,
          label: `${editing.customerName} · Khách hàng hiện tại`,
        },
        ...options,
      ]
    }
    return options
  }, [customers, editing])

  const refreshRelatedViews = () => {
    queryClient.invalidateQueries({ queryKey: ['assets'] })
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
    queryClient.invalidateQueries({ queryKey: ['my-schedule'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = {
        ...values,
        serialNumber: typeof values.serialNumber === 'string' ? values.serialNumber.trim() || null : null,
        installedAt: values.installedAt ? dayjs(values.installedAt as dayjs.Dayjs).format('YYYY-MM-DD') : null,
        warrantyUntil: values.warrantyUntil ? dayjs(values.warrantyUntil as dayjs.Dayjs).format('YYYY-MM-DD') : null,
      }
      return editing ? assetsApi.update(editing.id, payload) : assetsApi.create(payload)
    },
    onSuccess: () => {
      message.success(editing ? 'Đã cập nhật thiết bị' : 'Đã tạo thiết bị')
      if (!editing) {
        setPage(0)
        setSort({ sortBy: 'createdAt', sortDir: 'desc' })
      }
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      refreshRelatedViews()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => assetsApi.delete(id),
    onSuccess: () => {
      message.success('Đã xóa thiết bị')
      refreshRelatedViews()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const previewImport = useMutation({
    mutationFn: (file: File) => assetsApi.importCsv(file, false),
    onSuccess: (result, file) => {
      setBulkImportFile(file)
      setBulkImportResult(result)
      setBulkImportOpen(true)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const commitImport = useMutation({
    mutationFn: () => assetsApi.importCsv(bulkImportFile!, true),
    onSuccess: (result) => {
      setBulkImportResult(result)
      if (result.committed) {
        notification.success({
          message: 'Import thiết bị hoàn tất',
          description: `Đã thêm ${result.importedRows} thiết bị vào hệ thống.`,
        })
        setBulkImportOpen(false)
        setBulkImportFile(undefined)
        setBulkImportResult(undefined)
        setPage(0)
        setSort({ sortBy: 'createdAt', sortDir: 'desc' })
        refreshRelatedViews()
      }
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const exportCsv = async () => {
    try {
      downloadBlob(await assetsApi.exportCsv(searchInput.trim()), 'serviceops-assets.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const downloadTemplate = async () => {
    try {
      downloadBlob(await assetsApi.importTemplate(), 'serviceops-assets-template.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const showCreate = () => {
    setEditing(undefined)
    setCustomerOptionSearchInput('')
    form.resetFields()
    form.setFieldsValue({ status: 'ACTIVE' })
    setOpen(true)
  }

  const showEdit = (record: Asset) => {
    setEditing(record)
    setCustomerOptionSearchInput('')
    form.setFieldsValue({
      ...record,
      installedAt: record.installedAt ? dayjs(record.installedAt) : undefined,
      warrantyUntil: record.warrantyUntil ? dayjs(record.warrantyUntil) : undefined,
    })
    setOpen(true)
  }

  const assetActions = (
    <AssetPageActions
      importing={previewImport.isPending}
      onExport={exportCsv}
      onDownloadTemplate={downloadTemplate}
      onImportFile={(file) => previewImport.mutate(file)}
      onCreate={showCreate}
    />
  )

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Danh mục thiết bị"
        title="Thiết bị khách hàng"
        description="Theo dõi serial, bảo hành, vòng đời và tình trạng phục vụ của từng tài sản."
        actions={canManage ? assetActions : undefined}
        meta={<MetaBadge>{assetsQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} thiết bị`}</MetaBadge>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm serial, loại, hãng, model hoặc mã/tên khách hàng" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
      </div>

      {assetsQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách thiết bị"
          error={assetsQuery.error}
          onRetry={() => assetsQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={assetsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1120 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: assetsQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} thiết bị`,
        }}
        onChange={(pagination, _filters, sorter) => {
          setPage(Math.max((pagination.current ?? 1) - 1, 0))
          setSort(resolveTableSort(sorter, { sortBy: 'createdAt', sortDir: 'desc' }))
        }}
        locale={{ emptyText: <Empty description={assetsQuery.isError ? 'Không thể tải dữ liệu thiết bị' : 'Chưa có thiết bị phù hợp'} /> }}
        columns={[
          {
            title: 'Thiết bị',
            width: 230,
            ...serverSortable(sort, 'equipment'),
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{[record.brand, record.model].filter(Boolean).join(' ') || record.category}</Typography.Text>
                <Typography.Text type="secondary" code>{record.serialNumber ?? 'Chưa xác định serial'}</Typography.Text>
              </div>
            ),
          },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 180, ellipsis: true, ...serverSortable(sort, 'customerName') },
          { title: 'Loại', dataIndex: 'category', width: 120, ...serverSortable(sort, 'category') },
          {
            title: 'Bảo hành',
            width: 150,
            ...serverSortable(sort, 'warrantyUntil'),
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{formatDate(record.warrantyUntil)}</span>
                <WarrantyTag underWarranty={record.underWarranty} />
              </div>
            ),
          },
          { title: 'Trạng thái', dataIndex: 'status', width: 130, ...serverSortable(sort, 'status'), render: (value) => <StatusTag status={value} /> },
          { title: 'Ngày lắp', dataIndex: 'installedAt', width: 110, ...serverSortable(sort, 'installedAt'), render: formatDate },
          {
            title: 'Ghi chú',
            dataIndex: 'notes',
            width: 180,
            ...serverSortable(sort, 'notes'),
            ellipsis: true,
            render: (value) => value ? <Typography.Text>{value}</Typography.Text> : <Typography.Text type="secondary">Chưa có ghi chú</Typography.Text>,
          },
          {
            title: 'Thao tác',
            width: 100,
            fixed: 'right' as const,
            hidden: !canManage,
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa thiết bị" title="Sửa thiết bị" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xóa thiết bị này?"
                  description="Chỉ xóa được khi thiết bị chưa được dùng trong yêu cầu dịch vụ hoặc phiếu công việc."
                  okText="Xóa"
                  cancelText="Hủy"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(record.id)}
                >
                  <Button aria-label="Xóa thiết bị" title="Xóa thiết bị" type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <AssetFormModal
        open={open}
        editing={editing}
        form={form}
        saving={save.isPending}
        customerOptions={customerOptions}
        customersLoading={customersQuery.isFetching}
        customersError={customersQuery.error}
        onRetryCustomers={() => { void customersQuery.refetch() }}
        onCustomerSearch={setCustomerOptionSearchInput}
        onCancel={() => setOpen(false)}
        onSubmit={(values) => save.mutate(values)}
      />

      <CsvImportPreviewModal<AssetImportRowResult>
        title="Kiểm tra file nhập thiết bị"
        open={bulkImportOpen}
        result={bulkImportResult}
        committing={commitImport.isPending}
        onCancel={() => setBulkImportOpen(false)}
        onCommit={() => commitImport.mutate()}
        columns={[
          { title: 'Serial', dataIndex: 'serialNumber', width: 180, sorter: (a, b) => compareText(a.serialNumber, b.serialNumber) },
          { title: 'Mã khách hàng', dataIndex: 'customerCode', width: 160, sorter: (a, b) => compareText(a.customerCode, b.customerCode) },
        ]}
      />
    </div>
  )
}
