import { DeleteOutlined, DownOutlined, DownloadOutlined, EditOutlined, FileExcelOutlined, PlusOutlined, SearchOutlined, UploadOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, DatePicker, Dropdown, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, Upload } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
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

const assetStatusOptions = [
  { value: 'ACTIVE', label: 'Hoạt động' },
  { value: 'IN_SERVICE', label: 'Đang sửa chữa' },
  { value: 'OUT_OF_SERVICE', label: 'Tạm ngưng' },
  { value: 'RETIRED', label: 'Thanh lý' },
]

export function AssetsPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'OWNER' || user?.role === 'CUSTOMER_SERVICE'
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Asset>()
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportFile, setBulkImportFile] = useState<File>()
  const [bulkImportResult, setBulkImportResult] = useState<AssetImportResult>()
  const [form] = Form.useForm()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const assetsQuery = useQuery({
    queryKey: ['assets', { search, page, size: LIST_PAGE_SIZE }],
    queryFn: () => assetsApi.list(search, page, LIST_PAGE_SIZE),
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
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 100), enabled: canManage })

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
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => assetsApi.delete(id),
    onSuccess: () => {
      message.success('Đã xoá thiết bị')
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
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
        queryClient.invalidateQueries({ queryKey: ['assets'] })
        queryClient.invalidateQueries({ queryKey: ['dashboard'] })
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
    form.resetFields()
    form.setFieldsValue({ status: 'ACTIVE' })
    setOpen(true)
  }

  const showEdit = (record: Asset) => {
    setEditing(record)
    form.setFieldsValue({
      ...record,
      installedAt: record.installedAt ? dayjs(record.installedAt) : undefined,
      warrantyUntil: record.warrantyUntil ? dayjs(record.warrantyUntil) : undefined,
    })
    setOpen(true)
  }

  const assetActions = (
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
      <Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm thiết bị</Button>
    </Space>
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
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description={assetsQuery.isError ? 'Không thể tải dữ liệu thiết bị' : 'Chưa có thiết bị phù hợp'} /> }}
        columns={[
          {
            title: 'Thiết bị',
            width: 230,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{[record.brand, record.model].filter(Boolean).join(' ') || record.category}</Typography.Text>
                <Typography.Text type="secondary" code>{record.serialNumber ?? 'Chưa xác định serial'}</Typography.Text>
              </div>
            ),
          },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 180, ellipsis: true },
          { title: 'Loại', dataIndex: 'category', width: 120 },
          {
            title: 'Bảo hành',
            width: 150,
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{formatDate(record.warrantyUntil)}</span>
                <WarrantyTag underWarranty={record.underWarranty} />
              </div>
            ),
          },
          { title: 'Trạng thái', dataIndex: 'status', width: 130, render: (value) => <StatusTag status={value} /> },
          { title: 'Ngày lắp', dataIndex: 'installedAt', width: 110, render: formatDate },
          {
            title: 'Ghi chú',
            dataIndex: 'notes',
            width: 180,
            ellipsis: true,
            render: (value) => value ? <Typography.Text>{value}</Typography.Text> : <Typography.Text type="secondary">Chưa có ghi chú</Typography.Text>,
          },
          {
            title: 'Thao tác',
            width: 76,
            hidden: !canManage,
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa thiết bị" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xoá thiết bị này?"
                  description="Chỉ xoá được khi thiết bị chưa được dùng trong yêu cầu dịch vụ hoặc phiếu công việc."
                  okText="Xoá"
                  cancelText="Huỷ"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(record.id)}
                >
                  <Button aria-label="Xoá thiết bị" type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật thiết bị' : 'Thêm thiết bị'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} width={720} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} requiredMark={false}>
          <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
            <Select showSearch optionFilterProp="label" options={customers?.content.map((customer) => ({ value: customer.id, label: `${customer.code} · ${customer.name}` }))} />
          </Form.Item>
          <div className="form-grid two-cols">
            <Form.Item label="Loại thiết bị" name="category" rules={[{ required: true, message: 'Nhập loại thiết bị' }]}><Input placeholder="Máy lạnh" /></Form.Item>
            <Form.Item label="Serial number (không bắt buộc)" name="serialNumber"><Input placeholder="Có thể bổ sung sau khi xác minh tại hiện trường" /></Form.Item>
            <Form.Item label="Hãng" name="brand"><Input placeholder="Daikin" /></Form.Item>
            <Form.Item label="Model" name="model"><Input /></Form.Item>
            <Form.Item label="Ngày lắp đặt" name="installedAt"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
            <Form.Item label="Bảo hành đến" name="warrantyUntil"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
          </div>
          <Form.Item label="Trạng thái" name="status"><Select options={assetStatusOptions} /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>

      <CsvImportPreviewModal<AssetImportRowResult>
        title="Kiểm tra file nhập thiết bị"
        open={bulkImportOpen}
        result={bulkImportResult}
        committing={commitImport.isPending}
        onCancel={() => setBulkImportOpen(false)}
        onCommit={() => commitImport.mutate()}
        columns={[
          { title: 'Serial', dataIndex: 'serialNumber', width: 180 },
          { title: 'Mã khách hàng', dataIndex: 'customerCode', width: 160 },
        ]}
      />
    </div>
  )
}
