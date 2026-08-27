import { EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Space, Table, Typography } from 'antd'
import { useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import type { UserRole, WorkOrder, WorkOrderPartRequest, WorkOrderPartRequestStatus, WorkOrderPartUsage } from '../../../types'
import { formatDateTime, formatQuantityWithUnit } from '../../../utils/format'
import { inventoryApi } from '../api'
import { canConfirmPartUsage, canRequestPart, PART_REQUEST_STATUS_LABELS } from '../model/workOrderPartPresentation'
import { WorkOrderPartRequestModal, type WorkOrderPartRequestValues } from './WorkOrderPartRequestModal'
import { WorkOrderPartUsageModal } from './WorkOrderPartUsageModal'

function requestTone(status: WorkOrderPartRequestStatus) {
  if (status === 'REQUESTED') return 'warning' as const
  if (status === 'ISSUED') return 'success' as const
  if (status === 'UNAVAILABLE') return 'danger' as const
  return 'neutral' as const
}

export function WorkOrderPartsPanel({ workOrder, role }: { workOrder: WorkOrder; role?: UserRole }) {
  const [requestOpen, setRequestOpen] = useState(false)
  const [partSearchInput, setPartSearchInput] = useState('')
  const partSearch = useDebouncedValue(partSearchInput.trim())
  const [editingRequest, setEditingRequest] = useState<WorkOrderPartRequest>()
  const [cancelingRequest, setCancelingRequest] = useState<WorkOrderPartRequest>()
  const [editingUsage, setEditingUsage] = useState<WorkOrderPartUsage>()
  const [cancelForm] = Form.useForm<{ reason: string }>()
  const { message, notification } = App.useApp()
  const queryClient = useQueryClient()

  const canCreateRequest = role === 'TECHNICIAN' && canRequestPart(workOrder.status)
  const canEditUsage = role === 'TECHNICIAN' && canConfirmPartUsage(workOrder.status)

  const requestsQuery = useQuery({
    queryKey: ['work-order-part-requests', workOrder.id],
    queryFn: () => inventoryApi.workOrderPartRequests(workOrder.id),
  })
  const usageQuery = useQuery({
    queryKey: ['work-order-part-usage', workOrder.id],
    queryFn: () => inventoryApi.workOrderPartUsage(workOrder.id),
  })
  const partsQuery = useQuery({
    queryKey: ['spare-parts', 'part-request-catalog', partSearch],
    queryFn: () => inventoryApi.list(partSearch, 0, LIST_PAGE_SIZE, true),
    enabled: canCreateRequest && requestOpen && !editingRequest,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['work-order-part-requests', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['work-order-part-usage', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['part-requests'] })
    queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
    queryClient.invalidateQueries({ queryKey: ['inventory-transactions'] })
    queryClient.invalidateQueries({ queryKey: ['work-order', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['work-order-timeline', workOrder.id] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const saveRequest = useMutation({
    mutationFn: (values: WorkOrderPartRequestValues) => editingRequest
      ? inventoryApi.updatePartRequest(editingRequest.id, { quantity: values.quantity, note: values.note.trim() })
      : inventoryApi.createPartRequest(workOrder.id, { sparePartId: values.sparePartId, quantity: values.quantity, note: values.note.trim() }),
    onSuccess: (request) => {
      notification.success({
        message: editingRequest ? 'Đã cập nhật yêu cầu phụ tùng' : 'Đã gửi yêu cầu phụ tùng',
        description: `${request.sparePartName} · ${formatQuantityWithUnit(request.requestedQuantity, request.unit)} · ${workOrder.code}.`,
      })
      setRequestOpen(false)
      setEditingRequest(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const cancelRequest = useMutation({
    mutationFn: (reason: string) => inventoryApi.cancelPartRequest(cancelingRequest!.id, reason),
    onSuccess: (request) => {
      notification.success({
        message: 'Đã hủy yêu cầu phụ tùng',
        description: `${request.sparePartName} · ${workOrder.code}. Yêu cầu vẫn được giữ trong lịch sử.`,
      })
      setCancelingRequest(undefined)
      cancelForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const updateUsage = useMutation({
    mutationFn: (usedQuantity: number) => inventoryApi.updatePartUsage(workOrder.id, {
      sparePartId: editingUsage!.sparePartId,
      usedQuantity,
    }),
    onSuccess: (usage) => {
      notification.success({
        message: 'Đã cập nhật phụ tùng thực tế sử dụng',
        description: `${usage.sparePartName} · Đã dùng ${formatQuantityWithUnit(usage.usedQuantity, usage.unit)} · ${workOrder.code}.`,
      })
      setEditingUsage(undefined)
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const openCreateRequest = () => {
    setEditingRequest(undefined)
    setPartSearchInput('')
    setRequestOpen(true)
  }

  const requests = requestsQuery.data ?? []
  const usage = usageQuery.data ?? []

  return (
    <Space direction="vertical" size={18} style={{ width: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>Yêu cầu phụ tùng</Typography.Title>
          <Typography.Text type="secondary">Yêu cầu không làm giảm tồn kho; tồn chỉ giảm khi nhân viên kho xác nhận cấp thực tế.</Typography.Text>
        </div>
        {canCreateRequest ? <Button type="primary" icon={<PlusOutlined />} onClick={openCreateRequest}>Yêu cầu phụ tùng</Button> : null}
      </div>

      {requestsQuery.isError ? (
        <QueryErrorAlert title="Chưa tải được yêu cầu phụ tùng" error={requestsQuery.error} onRetry={() => requestsQuery.refetch()} />
      ) : (
        <Table<WorkOrderPartRequest>
          rowKey="id"
          size="small"
          loading={requestsQuery.isLoading || requestsQuery.isFetching}
          dataSource={requests}
          pagination={false}
          scroll={{ x: 980 }}
          locale={{ emptyText: <Empty description="Phiếu chưa có yêu cầu phụ tùng" /> }}
          columns={[
            { title: 'Phụ tùng', width: 230, render: (_, item) => <div className="table-primary-cell"><Typography.Text strong>{item.sparePartName}</Typography.Text><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
            { title: 'Số lượng', width: 120, render: (_, item) => formatQuantityWithUnit(item.requestedQuantity, item.unit) },
            { title: 'Trạng thái', width: 130, render: (_, item) => <MetaBadge tone={requestTone(item.status)}>{PART_REQUEST_STATUS_LABELS[item.status]}</MetaBadge> },
            { title: 'Mục đích / lý do', width: 260, render: (_, item) => item.resolutionReason || item.note },
            { title: 'Thời gian', width: 165, render: (_, item) => formatDateTime(item.resolvedAt || item.issuedAt || item.requestedAt) },
            {
              title: 'Thao tác', width: 170, fixed: 'right',
              render: (_, item) => role === 'TECHNICIAN' && item.status === 'REQUESTED' && canCreateRequest ? (
                <Space size={4}>
                  <Button size="small" icon={<EditOutlined />} onClick={() => { setEditingRequest(item); setRequestOpen(true) }}>Sửa</Button>
                  <Button size="small" danger icon={<StopOutlined />} onClick={() => { setCancelingRequest(item); cancelForm.resetFields() }}>Hủy</Button>
                </Space>
              ) : null,
            },
          ]}
        />
      )}

      <div>
        <Typography.Title level={5} style={{ marginBottom: 4 }}>Phụ tùng đã cấp / thực tế sử dụng</Typography.Title>
        <Typography.Text type="secondary">Số lượng thực tế dùng cho khách không làm thay đổi tồn kho; phần chưa dùng có thể được kho nhận hoàn trả sau.</Typography.Text>
      </div>

      {usageQuery.isError ? (
        <QueryErrorAlert title="Chưa tải được số liệu phụ tùng đã cấp" error={usageQuery.error} onRetry={() => usageQuery.refetch()} />
      ) : (
        <Table<WorkOrderPartUsage>
          rowKey="sparePartId"
          size="small"
          loading={usageQuery.isLoading || usageQuery.isFetching}
          dataSource={usage.filter((item) => Number(item.issuedQuantity) > 0)}
          pagination={false}
          scroll={{ x: 900 }}
          locale={{ emptyText: <Empty description="Kho chưa cấp phụ tùng cho phiếu này" /> }}
          columns={[
            { title: 'Phụ tùng', width: 230, render: (_, item) => <div className="table-primary-cell"><Typography.Text strong>{item.sparePartName}</Typography.Text><Typography.Text type="secondary" code>{item.sparePartSku}</Typography.Text></div> },
            { title: 'Đã cấp', width: 120, render: (_, item) => formatQuantityWithUnit(item.issuedQuantity, item.unit) },
            { title: 'Đã dùng', width: 120, render: (_, item) => formatQuantityWithUnit(item.usedQuantity, item.unit) },
            { title: 'Đã trả', width: 120, render: (_, item) => formatQuantityWithUnit(item.returnedQuantity, item.unit) },
            { title: 'KTV đang giữ', width: 140, render: (_, item) => <Typography.Text strong={Number(item.outstandingQuantity) > 0}>{formatQuantityWithUnit(item.outstandingQuantity, item.unit)}</Typography.Text> },
            {
              title: 'Thao tác', width: 150, fixed: 'right',
              render: (_, item) => canEditUsage ? <Button size="small" onClick={() => setEditingUsage(item)}>Ghi thực tế dùng</Button> : null,
            },
          ]}
        />
      )}

      <WorkOrderPartRequestModal
        open={requestOpen}
        request={editingRequest}
        parts={partsQuery.data}
        partsLoading={partsQuery.isFetching}
        partsError={partsQuery.error}
        pending={saveRequest.isPending}
        onPartSearch={setPartSearchInput}
        onRetryParts={() => partsQuery.refetch()}
        onClose={() => { setRequestOpen(false); setEditingRequest(undefined); setPartSearchInput('') }}
        onSubmit={(values) => saveRequest.mutate(values)}
      />

      <Modal
        title={`Hủy yêu cầu · ${cancelingRequest?.sparePartSku ?? ''}`}
        open={Boolean(cancelingRequest)}
        onCancel={() => { setCancelingRequest(undefined); cancelForm.resetFields() }}
        onOk={() => cancelForm.submit()}
        okText="Xác nhận hủy"
        okButtonProps={{ danger: true }}
        confirmLoading={cancelRequest.isPending}
        destroyOnHidden
      >
        <Form form={cancelForm} layout="vertical" onFinish={(values) => cancelRequest.mutate(values.reason.trim())} requiredMark>
          <Form.Item
            label="Lý do hủy"
            name="reason"
            rules={[
              { required: true, whitespace: true, message: 'Nhập lý do hủy yêu cầu' },
              { max: 500, message: 'Lý do tối đa 500 ký tự' },
            ]}
          >
            <Input.TextArea rows={4} maxLength={500} showCount placeholder="Nhập lý do thực tế, ví dụ: kiểm tra lại thấy phụ tùng hiện tại vẫn sử dụng được." />
          </Form.Item>
        </Form>
      </Modal>

      <WorkOrderPartUsageModal
        usage={editingUsage}
        pending={updateUsage.isPending}
        onClose={() => setEditingUsage(undefined)}
        onSubmit={(usedQuantity) => updateUsage.mutate(usedQuantity)}
      />
    </Space>
  )
}
