import { DownOutlined, DownloadOutlined, FileExcelOutlined, InboxOutlined, PlusOutlined, SearchOutlined, UploadOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Dropdown, Empty, Form, Input, InputNumber, Modal, Space, Table, Typography, Upload } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { inventoryApi } from '../../inventory/api'
import { useAuth } from '../../auth/AuthContext'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { SparePart, SparePartImportResult, SparePartImportRowResult } from '../../../types'
import { formatCompactDecimalInput, formatCurrency, formatDateTime, formatQuantity, formatQuantityWithUnit } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'

function downloadBlob(blob: Blob, filename: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(objectUrl)
}

export function InventoryPage() {
  const { user } = useAuth()
  const canManageStock = ['OWNER', 'WAREHOUSE_STAFF'].includes(user?.role ?? '')
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [createOpen, setCreateOpen] = useState(false)
  const [importing, setImporting] = useState<SparePart>()
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportFile, setBulkImportFile] = useState<File>()
  const [bulkImportResult, setBulkImportResult] = useState<SparePartImportResult>()
  const [createForm] = Form.useForm()
  const [importForm] = Form.useForm()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const inventoryQuery = useQuery({
    queryKey: ['spare-parts', { search, page, size: LIST_PAGE_SIZE }],
    queryFn: () => inventoryApi.list(search, page, LIST_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = inventoryQuery

  useEffect(() => {
    setPage(0)
  }, [search])

  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const create = useMutation({
    mutationFn: (values: Record<string, unknown>) => inventoryApi.create(values),
    onSuccess: () => {
      message.success('Đã tạo phụ tùng')
      setCreateOpen(false)
      createForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const importStock = useMutation({
    mutationFn: (values: { quantity: number; note: string }) => inventoryApi.importStock(importing!.id, values),
    onSuccess: (part, values) => {
      notification.success({
        message: `Đã nhập kho · ${part.sku}`,
        description: `${part.name} · +${formatQuantity(values.quantity)} ${part.unit} · Tồn hiện tại ${formatQuantity(part.stockQuantity)} ${part.unit}.`,
      })
      setImporting(undefined)
      importForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const previewImport = useMutation({
    mutationFn: (file: File) => inventoryApi.importCsv(file, false),
    onSuccess: (result, file) => {
      setBulkImportFile(file)
      setBulkImportResult(result)
      setBulkImportOpen(true)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const commitImport = useMutation({
    mutationFn: () => inventoryApi.importCsv(bulkImportFile!, true),
    onSuccess: (result) => {
      setBulkImportResult(result)
      if (result.committed) {
        notification.success({
          message: 'Import danh mục phụ tùng hoàn tất',
          description: `Đã thêm ${result.importedRows} SKU vào kho phụ tùng.`,
        })
        setBulkImportOpen(false)
        setBulkImportFile(undefined)
        setBulkImportResult(undefined)
        refresh()
      }
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const exportCsv = async () => {
    try {
      downloadBlob(await inventoryApi.exportCsv(searchInput.trim()), 'serviceops-spare-parts.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const downloadTemplate = async () => {
    try {
      downloadBlob(await inventoryApi.importTemplate(), 'serviceops-spare-parts-template.csv')
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const inventoryActions = (
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
            ...(canManageStock ? [
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
            ] : []),
          ],
        }}
      >
        <Button icon={<FileExcelOutlined />} loading={previewImport.isPending}>
          Dữ liệu <DownOutlined />
        </Button>
      </Dropdown>
      {canManageStock && (
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.setFieldsValue({ unit: 'cái', initialStock: 0, reorderLevel: 3, unitPrice: 0, active: true }); setCreateOpen(true) }}>
          Thêm phụ tùng
        </Button>
      )}
    </Space>
  )

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Quản lý tồn kho"
        title="Kho phụ tùng"
        description="Theo dõi tồn kho, mức đặt hàng và nhập bổ sung phụ tùng phục vụ phiếu công việc."
        actions={inventoryActions}
        meta={<><MetaBadge>{inventoryQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} SKU`}</MetaBadge><MetaBadge tone={search ? 'info' : 'neutral'}>{search ? 'Đang lọc' : 'Tất cả phụ tùng'}</MetaBadge></>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm SKU, tên hoặc đơn vị phụ tùng" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
      </div>

      {inventoryQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách phụ tùng"
          error={inventoryQuery.error}
          onRetry={() => inventoryQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={inventoryQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 980 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: inventoryQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} phụ tùng`,
        }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        rowClassName={(record) => record.lowStock ? 'low-stock-row' : ''}
        locale={{ emptyText: <Empty description={inventoryQuery.isError ? 'Không thể tải dữ liệu phụ tùng' : 'Chưa có phụ tùng phù hợp'} /> }}
        columns={[
          {
            title: 'Phụ tùng',
            width: 320,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.name}</Typography.Text>
                <Typography.Text type="secondary" code>{record.sku}</Typography.Text>
              </div>
            ),
          },
          {
            title: 'Tồn kho',
            width: 180,
            render: (_, record) => (
              <Space size={8} wrap>
                <strong>{formatQuantity(record.stockQuantity)}</strong>
                <span>{record.unit}</span>
                {record.lowStock && <MetaBadge tone="danger">Sắp hết</MetaBadge>}
              </Space>
            ),
          },
          { title: 'Mức đặt hàng', dataIndex: 'reorderLevel', width: 150, render: (value, record) => formatQuantityWithUnit(value, record.unit) },
          { title: 'Đơn giá', dataIndex: 'unitPrice', width: 150, render: formatCurrency },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
          ...(canManageStock ? [{
            title: '',
            width: 120,
            render: (_: unknown, record: SparePart) => (
              <Button icon={<InboxOutlined />} onClick={() => { setImporting(record); importForm.setFieldsValue({ note: 'Nhập bổ sung kho' }) }}>
                Nhập kho
              </Button>
            ),
          }] : []),
        ]}
      />

      <Modal title="Thêm phụ tùng" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => createForm.submit()} confirmLoading={create.isPending} width={680} destroyOnHidden>
        <Form form={createForm} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="SKU" name="sku" rules={[{ required: true, message: 'Nhập SKU' }]}><Input /></Form.Item>
            <Form.Item label="Tên phụ tùng" name="name" rules={[{ required: true, message: 'Nhập tên phụ tùng' }]}><Input /></Form.Item>
            <Form.Item label="Đơn vị" name="unit" rules={[{ required: true, message: 'Nhập đơn vị' }]}><Input /></Form.Item>
            <Form.Item label="Tồn ban đầu" name="initialStock" rules={[{ required: true, message: 'Nhập tồn ban đầu' }]}><InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Mức đặt hàng lại" name="reorderLevel" rules={[{ required: true, message: 'Nhập mức đặt hàng' }]}><InputNumber min={0} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="Đơn giá" name="unitPrice" rules={[{ required: true, message: 'Nhập đơn giá' }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="VND" /></Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal title={`Nhập kho · ${importing?.sku ?? ''}`} open={Boolean(importing)} onCancel={() => setImporting(undefined)} onOk={() => importForm.submit()} confirmLoading={importStock.isPending} destroyOnHidden>
        <Form form={importForm} layout="vertical" onFinish={(values) => importStock.mutate(values)} requiredMark={false}>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} formatter={formatCompactDecimalInput} style={{ width: '100%' }} addonAfter={importing?.unit} /></Form.Item>
          <Form.Item label="Ghi chú" name="note" rules={[{ required: true, message: 'Nhập ghi chú' }]}><Input /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="Kiểm tra file nhập phụ tùng"
        open={bulkImportOpen}
        onCancel={() => setBulkImportOpen(false)}
        onOk={() => commitImport.mutate()}
        okText="Xác nhận nhập"
        cancelText="Đóng"
        confirmLoading={commitImport.isPending}
        okButtonProps={{ disabled: !bulkImportResult || bulkImportResult.errorRows > 0 }}
        width={820}
        destroyOnHidden
      >
        {bulkImportResult && (
          <Space direction="vertical" size={14} style={{ width: '100%' }}>
            <Alert
              type={bulkImportResult.errorRows > 0 ? 'warning' : 'success'}
              showIcon
              message={`${bulkImportResult.validRows}/${bulkImportResult.totalRows} dòng hợp lệ`}
              description={bulkImportResult.errorRows > 0 ? 'File còn dòng lỗi, hệ thống chưa ghi dữ liệu vào kho.' : 'File hợp lệ, bạn có thể xác nhận để ghi dữ liệu vào kho.'}
            />
            <Table<SparePartImportRowResult>
              rowKey="rowNumber"
              size="small"
              dataSource={bulkImportResult.rows}
              pagination={{ pageSize: 8, showSizeChanger: false }}
              columns={[
                { title: 'Dòng', dataIndex: 'rowNumber', width: 80 },
                { title: 'SKU', dataIndex: 'sku', width: 150 },
                { title: 'Tên phụ tùng', dataIndex: 'name', ellipsis: true },
                {
                  title: 'Kết quả',
                  dataIndex: 'valid',
                  width: 130,
                  render: (valid: boolean) => <MetaBadge tone={valid ? 'success' : 'danger'}>{valid ? 'Hợp lệ' : 'Lỗi'}</MetaBadge>,
                },
                { title: 'Ghi chú', dataIndex: 'message', ellipsis: true },
              ]}
            />
          </Space>
        )}
      </Modal>
    </div>
  )
}
