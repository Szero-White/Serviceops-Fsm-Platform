import { CheckCircleOutlined, EditOutlined, InboxOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Popconfirm, Select, Space, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { inventoryApi } from '../api'
import { useAuth } from '../../auth/AuthContext'
import { CheckboxFilterSelect } from '../../../components/CheckboxFilterSelect'
import { BulkImportPreviewModal, CreateSparePartModal, ImportStockModal, ReorderLevelModal } from '../components/InventoryModals'
import { InventoryPageActions } from '../components/InventoryPageActions'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import type { SparePart, SparePartImportResult } from '../../../types'
import { formatCurrency, formatDateTime, formatQuantity, formatQuantityWithUnit } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { downloadBlob } from '../../../utils/download'
import { resolveTableSort, serverSortable, type TableSortState } from '../../../utils/tableSort'

export function InventoryPage() {
  const { user } = useAuth()
  const canManageStock = ['OWNER', 'WAREHOUSE_STAFF'].includes(user?.role ?? '')
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TableSortState>({ sortBy: 'createdAt', sortDir: 'desc' })
  const [activeFilters, setActiveFilters] = useState<Array<'active' | 'inactive'>>(['active'])
  const search = useDebouncedValue(searchInput.trim())
  const active = activeFilters.length === 1 ? activeFilters[0] === 'active' : undefined
  const [createOpen, setCreateOpen] = useState(false)
  const [importing, setImporting] = useState<SparePart>()
  const [editingReorderLevel, setEditingReorderLevel] = useState<SparePart>()
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportFile, setBulkImportFile] = useState<File>()
  const [bulkImportResult, setBulkImportResult] = useState<SparePartImportResult>()
  const [createForm] = Form.useForm()
  const [importForm] = Form.useForm<{ quantity: number; note: string }>()
  const [reorderLevelForm] = Form.useForm<{ reorderLevel: number }>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()
  const inventoryQuery = useQuery({
    queryKey: ['spare-parts', { search, active, page, size: LIST_PAGE_SIZE, sort }],
    queryFn: () => inventoryApi.list(search, page, LIST_PAGE_SIZE, active, sort.sortBy, sort.sortDir),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = inventoryQuery

  useEffect(() => {
    setPage(0)
  }, [search, activeFilters])

  useEffect(() => {
    if (data && page > 0 && page >= data.totalPages) {
      setPage(Math.max(data.totalPages - 1, 0))
    }
  }, [data, page])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['stocktake-parts'] })
    queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const create = useMutation({
    mutationFn: (values: Record<string, unknown>) => inventoryApi.create(values),
    onSuccess: () => {
      message.success('Đã tạo phụ tùng')
      setCreateOpen(false)
      createForm.resetFields()
      setPage(0)
      setSort({ sortBy: 'createdAt', sortDir: 'desc' })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const updateReorderLevel = useMutation({
    mutationFn: (values: { reorderLevel: number }) => inventoryApi.updateReorderLevel(editingReorderLevel!.id, values.reorderLevel),
    onSuccess: (part) => {
      const isLowStock = part.active && Number(part.stockQuantity) <= Number(part.reorderLevel)
      notification.success({
        message: 'Đã cập nhật ngưỡng tồn tối thiểu',
        description: isLowStock
          ? `${part.name} (${part.sku}) hiện còn ${formatQuantityWithUnit(part.stockQuantity, part.unit)}, đã chạm hoặc thấp hơn ngưỡng ${formatQuantityWithUnit(part.reorderLevel, part.unit)}.`
          : `${part.name} (${part.sku}) · Ngưỡng mới ${formatQuantityWithUnit(part.reorderLevel, part.unit)}.`,
      })
      setEditingReorderLevel(undefined)
      reorderLevelForm.resetFields()
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

  const setActive = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => inventoryApi.setActive(id, active),
    onSuccess: (part) => {
      notification.success({
        message: part.active ? 'Đã kích hoạt lại phụ tùng' : 'Đã ngừng sử dụng phụ tùng',
        description: part.active
          ? `${part.name} (${part.sku}) có thể tiếp tục dùng cho nghiệp vụ mới.`
          : Number(part.stockQuantity) > 0
            ? `Tồn ${formatQuantity(part.stockQuantity)} ${part.unit} được giữ nguyên để tiếp tục theo dõi; phụ tùng không còn được dùng cho nghiệp vụ mới.`
            : `${part.name} (${part.sku}) đã được đưa khỏi danh mục đang sử dụng.`,
      })
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
          message: 'Nhập danh sách phụ tùng hoàn tất',
          description: `Đã thêm ${result.importedRows} phụ tùng vào kho.`,
        })
        setBulkImportOpen(false)
        setBulkImportFile(undefined)
        setBulkImportResult(undefined)
        setPage(0)
        setSort({ sortBy: 'createdAt', sortDir: 'desc' })
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

  const openCreate = () => {
    createForm.setFieldsValue({ unit: 'cái', initialStock: 0, reorderLevel: 3, unitPrice: 0, active: true })
    setCreateOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Quản lý tồn kho"
        title="Kho phụ tùng"
        description="Theo dõi tồn kho, ngưỡng tồn tối thiểu và nhập bổ sung phụ tùng phục vụ phiếu công việc."
        actions={(
          <InventoryPageActions
            canManageStock={canManageStock}
            importLoading={previewImport.isPending}
            onExport={exportCsv}
            onDownloadTemplate={downloadTemplate}
            onImportFile={(file) => previewImport.mutate(file)}
            onCreate={openCreate}
          />
        )}
        meta={<><MetaBadge>{inventoryQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} phụ tùng`}</MetaBadge><MetaBadge tone={search || activeFilters.length === 1 ? 'info' : 'neutral'}>{activeFilters.length === 1 ? (activeFilters[0] === 'active' ? 'Đang sử dụng' : 'Ngừng sử dụng') : 'Tất cả phụ tùng'}</MetaBadge></>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phụ tùng, tên hoặc đơn vị" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <CheckboxFilterSelect
          ariaLabel="Lọc trạng thái phụ tùng"
          placeholder="Tất cả phụ tùng"
          value={activeFilters}
          onChange={(value) => setActiveFilters(value as Array<'active' | 'inactive'>)}
          options={[
            { value: 'active', label: 'Đang sử dụng' },
            { value: 'inactive', label: 'Ngừng sử dụng' },
          ]}
        />
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
        scroll={{ x: 1120 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: inventoryQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} phụ tùng`,
        }}
        onChange={(pagination, _filters, sorter) => {
          setPage(Math.max((pagination.current ?? 1) - 1, 0))
          const nextSort = resolveTableSort(sorter, { sortBy: 'createdAt', sortDir: 'desc' })
          if (nextSort.sortBy !== sort.sortBy || nextSort.sortDir !== sort.sortDir) {
            setSort(nextSort)
            setPage(0)
          }
        }}
        rowClassName={(record) => record.active && record.lowStock ? 'low-stock-row' : ''}
        locale={{ emptyText: <Empty description={inventoryQuery.isError ? 'Không thể tải dữ liệu phụ tùng' : 'Chưa có phụ tùng phù hợp'} /> }}
        columns={[
          {
            title: 'Phụ tùng',
            width: 320,
            ...serverSortable(sort, 'name'),
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
            ...serverSortable(sort, 'stockQuantity'),
            render: (_, record) => (
              <Space size={8} wrap>
                <strong>{formatQuantity(record.stockQuantity)}</strong>
                <span>{record.unit}</span>
                {record.lowStock && <MetaBadge tone="danger">Sắp hết</MetaBadge>}
              </Space>
            ),
          },
          { title: 'Ngưỡng tồn tối thiểu', dataIndex: 'reorderLevel', width: 190, ...serverSortable(sort, 'reorderLevel'), render: (value, record) => formatQuantityWithUnit(value, record.unit) },
          { title: 'Đơn giá', dataIndex: 'unitPrice', width: 150, ...serverSortable(sort, 'unitPrice'), render: formatCurrency },
          {
            title: 'Trạng thái',
            width: 140,
            ...serverSortable(sort, 'active'),
            render: (_: unknown, record: SparePart) => (
              <MetaBadge tone={record.active ? 'success' : 'neutral'}>
                {record.active ? 'Đang sử dụng' : 'Ngừng sử dụng'}
              </MetaBadge>
            ),
          },
          { title: 'Cập nhật', dataIndex: 'updatedAt', width: 170, ...serverSortable(sort, 'updatedAt'), render: formatDateTime },
          ...(canManageStock ? [{
            title: 'Thao tác',
            width: 390,
            fixed: 'right' as const,
            render: (_: unknown, record: SparePart) => {
              const hasStock = Number(record.stockQuantity) !== 0
              return (
                <Space size={6} wrap>
                  <Button
                    icon={<InboxOutlined />}
                    disabled={!record.active}
                    onClick={() => { setImporting(record); importForm.setFieldsValue({ note: 'Nhập bổ sung kho' }) }}
                  >
                    Nhập kho
                  </Button>

                  <Button
                    icon={<EditOutlined />}
                    onClick={() => {
                      setEditingReorderLevel(record)
                      reorderLevelForm.setFieldsValue({ reorderLevel: record.reorderLevel })
                    }}
                  >
                    Sửa ngưỡng
                  </Button>

                  {record.active ? (
                    <Popconfirm
                      title="Ngừng sử dụng phụ tùng?"
                      description={hasStock
                        ? `Phụ tùng vẫn còn ${formatQuantity(record.stockQuantity)} ${record.unit}. Tồn kho sẽ được giữ nguyên để theo dõi, nhưng phụ tùng sẽ không còn được dùng cho nghiệp vụ mới.`
                        : 'Phụ tùng sẽ không còn được dùng cho nghiệp vụ mới.'}
                      okText="Ngừng sử dụng"
                      cancelText="Hủy"
                      onConfirm={() => setActive.mutate({ id: record.id, active: false })}
                    >
                      <Button icon={<StopOutlined />} loading={setActive.isPending}>
                        Ngừng sử dụng
                      </Button>
                    </Popconfirm>
                  ) : (
                    <Button
                      icon={<CheckCircleOutlined />}
                      loading={setActive.isPending}
                      onClick={() => setActive.mutate({ id: record.id, active: true })}
                    >
                      Kích hoạt lại
                    </Button>
                  )}


                </Space>
              )
            },
          }] : []),
        ]}
      />

      <CreateSparePartModal
        open={createOpen}
        form={createForm}
        loading={create.isPending}
        onCancel={() => setCreateOpen(false)}
        onSubmit={(values) => create.mutate(values)}
      />

      <ReorderLevelModal
        part={editingReorderLevel}
        form={reorderLevelForm}
        loading={updateReorderLevel.isPending}
        onCancel={() => { setEditingReorderLevel(undefined); reorderLevelForm.resetFields() }}
        onSubmit={(values) => updateReorderLevel.mutate(values)}
      />

      <ImportStockModal
        part={importing}
        form={importForm}
        loading={importStock.isPending}
        onCancel={() => { setImporting(undefined); importForm.resetFields() }}
        onSubmit={(values) => importStock.mutate(values)}
      />

      <BulkImportPreviewModal
        open={bulkImportOpen}
        result={bulkImportResult}
        loading={commitImport.isPending}
        onCancel={() => setBulkImportOpen(false)}
        onCommit={() => commitImport.mutate()}
      />
    </div>
  )
}
