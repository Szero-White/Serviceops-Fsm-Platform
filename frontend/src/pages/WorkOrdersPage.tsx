import {
  CalendarOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  EyeOutlined,
  PlusOutlined,
  SearchOutlined,
  ToolOutlined,
} from '@ant-design/icons'
import type { UploadRequestOption } from '@rc-component/upload/es/interface'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Typography,
  Upload,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, attachmentsApi, customersApi, inventoryApi, techniciansApi, workOrdersApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/PageHeader'
import { PriorityTag, StatusTag } from '../components/StatusTag'
import type { WorkOrder, WorkOrderStatus } from '../types'
import { EMPTY_VALUE, formatCurrency, formatDateTime, formatNumber } from '../utils/format'

const { RangePicker } = DatePicker

const statusOptions = [
  { value: 'OPEN', label: 'Đang mở' },
  { value: 'SCHEDULED', label: 'Đã lên lịch' },
  { value: 'ASSIGNED', label: 'Đã phân công' },
  { value: 'ON_THE_WAY', label: 'Đang di chuyển' },
  { value: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { value: 'WAITING_FOR_PARTS', label: 'Chờ phụ tùng' },
  { value: 'COMPLETED', label: 'Đã hoàn thành' },
  { value: 'CUSTOMER_ACCEPTED', label: 'Khách xác nhận' },
  { value: 'CLOSED', label: 'Đã đóng' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
  { value: 'REOPENED', label: 'Mở lại' },
]

const priorityOptions = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

const transitionLabels: Partial<Record<WorkOrderStatus, string>> = {
  ON_THE_WAY: 'Bắt đầu di chuyển',
  IN_PROGRESS: 'Bắt đầu / tiếp tục',
  WAITING_FOR_PARTS: 'Chờ phụ tùng',
  COMPLETED: 'Hoàn thành',
  CUSTOMER_ACCEPTED: 'Khách xác nhận',
  CLOSED: 'Đóng phiếu',
  REOPENED: 'Mở lại',
  CANCELLED: 'Huỷ phiếu',
}

function availableTransitions(status: WorkOrderStatus): WorkOrderStatus[] {
  const map: Partial<Record<WorkOrderStatus, WorkOrderStatus[]>> = {
    ASSIGNED: ['ON_THE_WAY', 'CANCELLED'],
    ON_THE_WAY: ['IN_PROGRESS', 'CANCELLED'],
    IN_PROGRESS: ['WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED'],
    WAITING_FOR_PARTS: ['IN_PROGRESS', 'CANCELLED'],
    COMPLETED: ['CUSTOMER_ACCEPTED', 'REOPENED'],
    CUSTOMER_ACCEPTED: ['CLOSED', 'REOPENED'],
    REOPENED: ['IN_PROGRESS', 'CANCELLED'],
  }
  return map[status] ?? []
}

export function WorkOrdersPage() {
  const { user } = useAuth()
  const canCreate = ['OWNER', 'CUSTOMER_SERVICE', 'DISPATCHER'].includes(user?.role ?? '')
  const canSchedule = ['OWNER', 'DISPATCHER'].includes(user?.role ?? '')
  const canTransition = ['OWNER', 'DISPATCHER', 'TECHNICIAN'].includes(user?.role ?? '')
  const canConsumePart = ['OWNER', 'WAREHOUSE_STAFF', 'TECHNICIAN'].includes(user?.role ?? '')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<WorkOrderStatus>()
  const [selectedId, setSelectedId] = useState<string>()
  const [createOpen, setCreateOpen] = useState(false)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [completeOpen, setCompleteOpen] = useState(false)
  const [consumeOpen, setConsumeOpen] = useState(false)
  const [createForm] = Form.useForm()
  const [scheduleForm] = Form.useForm()
  const [completeForm] = Form.useForm()
  const [consumeForm] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({ queryKey: ['work-orders', search, status], queryFn: () => workOrdersApi.list(search, status) })
  const { data: detail, isLoading: detailLoading } = useQuery({ queryKey: ['work-order', selectedId], queryFn: () => workOrdersApi.get(selectedId!), enabled: Boolean(selectedId) })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })
  const { data: assets } = useQuery({ queryKey: ['assets', 'all'], queryFn: () => assetsApi.list('', 0, 300) })
  const { data: technicians } = useQuery({ queryKey: ['technicians'], queryFn: () => techniciansApi.list() })
  const { data: parts } = useQuery({ queryKey: ['spare-parts', 'all'], queryFn: () => inventoryApi.list('', 0, 300) })
  const { data: attachments } = useQuery({
    queryKey: ['attachments', selectedId],
    queryFn: () => attachmentsApi.list('WORK_ORDER', selectedId!),
    enabled: Boolean(selectedId),
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
  }

  const create = useMutation({
    mutationFn: (values: Record<string, unknown>) => workOrdersApi.create(values),
    onSuccess: (workOrder) => {
      message.success(`Đã tạo ${workOrder.code}`)
      setCreateOpen(false)
      createForm.resetFields()
      refresh()
      setSelectedId(workOrder.id)
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const schedule = useMutation({
    mutationFn: (values: { technicianId: string; period: [Dayjs, Dayjs] }) => workOrdersApi.schedule(selectedId!, {
      technicianId: values.technicianId,
      startTime: values.period[0].toISOString(),
      endTime: values.period[1].toISOString(),
    }),
    onSuccess: () => {
      message.success('Đã phân công và xếp lịch')
      setScheduleOpen(false)
      scheduleForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const transition = useMutation({
    mutationFn: ({ targetStatus, note }: { targetStatus: WorkOrderStatus; note?: string }) => workOrdersApi.transition(selectedId!, { targetStatus, note }),
    onSuccess: () => {
      message.success('Đã cập nhật trạng thái')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const complete = useMutation({
    mutationFn: (values: { diagnosis: string; resolution: string; note?: string }) => workOrdersApi.transition(selectedId!, { targetStatus: 'COMPLETED', ...values }),
    onSuccess: () => {
      message.success('Đã hoàn thành công việc')
      setCompleteOpen(false)
      completeForm.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const consume = useMutation({
    mutationFn: (values: { sparePartId: string; quantity: number; note?: string }) => workOrdersApi.consumePart(selectedId!, values),
    onSuccess: () => {
      message.success('Đã ghi nhận phụ tùng sử dụng')
      setConsumeOpen(false)
      consumeForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['spare-parts'] })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const uploadFile = async (options: UploadRequestOption) => {
    try {
      await attachmentsApi.upload('WORK_ORDER', selectedId!, options.file as File)
      message.success('Đã tải file lên')
      options.onSuccess?.({})
      queryClient.invalidateQueries({ queryKey: ['attachments', selectedId] })
      queryClient.invalidateQueries({ queryKey: ['audit'] })
    } catch (error) {
      message.error(apiErrorMessage(error))
      options.onError?.(error as Error)
    }
  }

  const transitionButtons = useMemo(() => detail ? availableTransitions(detail.status) : [], [detail])

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Dispatch board"
        title="Work order"
        description="Điều phối, theo dõi trạng thái, lịch kỹ thuật viên, phụ tùng và bằng chứng hoàn thành."
        actions={canCreate ? <Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.setFieldsValue({ priority: 'NORMAL' }); setCreateOpen(true) }}>Tạo work order</Button> : undefined}
        meta={<Space size={[8, 8]} wrap><Tag color="blue">{data?.totalElements ?? 0} phiếu</Tag><Tag color="purple">{status ? 'Đang lọc' : 'Tất cả trạng thái'}</Tag></Space>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm mã phiếu, nội dung, khách hàng hoặc serial" value={search} onChange={(event) => setSearch(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={setStatus} options={statusOptions} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 1260 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        onRow={(record) => ({ onDoubleClick: () => setSelectedId(record.id) })}
        locale={{ emptyText: <Empty description="Chưa có work order phù hợp" /> }}
        columns={[
          {
            title: 'Phiếu',
            width: 330,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Space size={8} wrap><Typography.Text strong code>{record.code}</Typography.Text><PriorityTag priority={record.priority} /></Space>
                <Typography.Text strong>{record.summary}</Typography.Text>
              </div>
            ),
          },
          {
            title: 'Bên liên quan',
            width: 260,
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{record.customerName}</span>
                <Typography.Text type="secondary">{record.technicianName || 'Chưa phân công'}</Typography.Text>
              </div>
            ),
          },
          { title: 'Thiết bị', dataIndex: 'assetLabel', width: 220, ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Trạng thái', dataIndex: 'status', width: 170, render: (value) => <StatusTag status={value} /> },
          { title: 'Bắt đầu', dataIndex: 'scheduledStart', width: 170, render: formatDateTime },
          { title: 'Kết thúc', dataIndex: 'scheduledEnd', width: 170, render: formatDateTime },
          { title: '', fixed: 'right', width: 72, render: (_, record) => <Button aria-label="Xem chi tiết" type="text" icon={<EyeOutlined />} onClick={() => setSelectedId(record.id)} /> },
        ]}
      />

      <Drawer
        title={detail ? `${detail.code} · ${detail.summary}` : 'Chi tiết work order'}
        open={Boolean(selectedId)}
        onClose={() => setSelectedId(undefined)}
        width={760}
        loading={detailLoading}
        extra={canSchedule && detail && ['OPEN', 'SCHEDULED', 'ASSIGNED'].includes(detail.status) ? <Button type="primary" icon={<CalendarOutlined />} onClick={() => { scheduleForm.setFieldsValue({ technicianId: detail.technicianId, period: detail.scheduledStart && detail.scheduledEnd ? [dayjs(detail.scheduledStart), dayjs(detail.scheduledEnd)] : undefined }); setScheduleOpen(true) }}>Phân công / xếp lịch</Button> : undefined}
      >
        {detail ? (
          <>
            <div className="work-order-actions">
              <Space wrap>
                {canTransition && transitionButtons.map((target) => target === 'COMPLETED' ? (
                  <Button key={target} type="primary" icon={<CheckCircleOutlined />} onClick={() => setCompleteOpen(true)}>{transitionLabels[target]}</Button>
                ) : target === 'CANCELLED' ? (
                  <Popconfirm key={target} title="Huỷ work order này?" okText="Huỷ" cancelText="Giữ lại" onConfirm={() => transition.mutate({ targetStatus: target, note: 'Huỷ từ giao diện vận hành' })}>
                    <Button danger>{transitionLabels[target]}</Button>
                  </Popconfirm>
                ) : (
                  <Button key={target} onClick={() => transition.mutate({ targetStatus: target })} loading={transition.isPending}>{transitionLabels[target]}</Button>
                ))}
                {canConsumePart && !['CLOSED', 'CANCELLED'].includes(detail.status) && <Button icon={<ToolOutlined />} onClick={() => setConsumeOpen(true)}>Dùng phụ tùng</Button>}
                <Upload customRequest={uploadFile} showUploadList={false} accept="image/jpeg,image/png,image/webp,application/pdf">
                  <Button icon={<CloudUploadOutlined />}>Tải ảnh / PDF</Button>
                </Upload>
              </Space>
            </div>

            <Tabs
              items={[
                {
                  key: 'overview',
                  label: 'Tổng quan',
                  children: (
                    <Descriptions column={2} bordered size="small">
                      <Descriptions.Item label="Trạng thái"><StatusTag status={detail.status} /></Descriptions.Item>
                      <Descriptions.Item label="Ưu tiên"><PriorityTag priority={detail.priority} /></Descriptions.Item>
                      <Descriptions.Item label="Khách hàng">{detail.customerName}</Descriptions.Item>
                      <Descriptions.Item label="Thiết bị">{detail.assetLabel ?? 'Chưa xác định'}</Descriptions.Item>
                      <Descriptions.Item label="Kỹ thuật viên">{detail.technicianName ?? 'Chưa phân công'}</Descriptions.Item>
                      <Descriptions.Item label="Lịch hẹn">{formatDateTime(detail.scheduledStart)} - {formatDateTime(detail.scheduledEnd)}</Descriptions.Item>
                      <Descriptions.Item label="Mô tả" span={2}>{detail.description ?? EMPTY_VALUE}</Descriptions.Item>
                      <Descriptions.Item label="Chẩn đoán" span={2}>{detail.diagnosis ?? EMPTY_VALUE}</Descriptions.Item>
                      <Descriptions.Item label="Giải pháp" span={2}>{detail.resolution ?? EMPTY_VALUE}</Descriptions.Item>
                    </Descriptions>
                  ),
                },
                {
                  key: 'timeline',
                  label: `Lịch sử (${detail.history?.length ?? 0})`,
                  children: detail.history?.length ? (
                    <Timeline items={detail.history.map((item) => ({
                      color: item.toStatus === 'CANCELLED' ? 'red' : item.toStatus === 'COMPLETED' || item.toStatus === 'CLOSED' ? 'green' : 'blue',
                      children: <div><Space><StatusTag status={item.toStatus} /><Typography.Text strong>{item.changedBy}</Typography.Text></Space><div>{item.note ?? 'Không có ghi chú'}</div><Typography.Text type="secondary">{formatDateTime(item.createdAt)}</Typography.Text></div>,
                    }))} />
                  ) : <Empty description="Chưa có lịch sử" />,
                },
                {
                  key: 'attachments',
                  label: `Tệp đính kèm (${attachments?.length ?? 0})`,
                  children: attachments?.length ? (
                    <List dataSource={attachments} renderItem={(item) => <List.Item><List.Item.Meta avatar={<CloudUploadOutlined />} title={item.originalFilename} description={`${item.contentType} · ${formatNumber(item.fileSize / 1024, 1)} KB · ${item.uploadedBy}`} /></List.Item>} />
                  ) : <Empty description="Chưa có ảnh hoặc tài liệu" />,
                },
              ]}
            />
          </>
        ) : null}
      </Drawer>

      <Modal title="Tạo work order" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => createForm.submit()} confirmLoading={create.isPending} width={760} destroyOnHidden>
        <Form form={createForm} layout="vertical" onFinish={(values) => create.mutate(values)} requiredMark={false}>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}><Select showSearch optionFilterProp="label" options={customers?.content.map((customer) => ({ value: customer.id, label: `${customer.code} · ${customer.name}` }))} /></Form.Item>
            <Form.Item label="Thiết bị" name="assetId"><Select allowClear showSearch optionFilterProp="label" options={assets?.content.map((asset) => ({ value: asset.id, label: `${asset.serialNumber} · ${asset.customerName}` }))} /></Form.Item>
          </div>
          <Form.Item label="Nội dung công việc" name="summary" rules={[{ required: true, message: 'Nhập nội dung công việc' }]}><Input /></Form.Item>
          <Form.Item label="Mô tả" name="description"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={priorityOptions} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Phân công và xếp lịch" open={scheduleOpen} onCancel={() => setScheduleOpen(false)} onOk={() => scheduleForm.submit()} confirmLoading={schedule.isPending} width={620} destroyOnHidden>
        <Form form={scheduleForm} layout="vertical" onFinish={(values) => schedule.mutate(values)} requiredMark={false}>
          <Form.Item label="Kỹ thuật viên" name="technicianId" rules={[{ required: true, message: 'Chọn kỹ thuật viên' }]}><Select showSearch optionFilterProp="label" options={technicians?.map((technician) => ({ value: technician.id, label: `${technician.name} · ${technician.skills ?? ''}` }))} /></Form.Item>
          <Form.Item label="Thời gian thực hiện" name="period" rules={[{ required: true, message: 'Chọn thời gian thực hiện' }]}><RangePicker showTime format="DD/MM/YYYY HH:mm" style={{ width: '100%' }} disabledDate={(date) => date.isBefore(dayjs().startOf('day'))} /></Form.Item>
          <Typography.Text type="secondary">Hệ thống khoá bản ghi kỹ thuật viên và kiểm tra lịch chồng lấn trong cùng transaction.</Typography.Text>
        </Form>
      </Modal>

      <Modal title="Hoàn thành công việc" open={completeOpen} onCancel={() => setCompleteOpen(false)} onOk={() => completeForm.submit()} confirmLoading={complete.isPending} width={680} destroyOnHidden>
        <Form form={completeForm} layout="vertical" onFinish={(values) => complete.mutate(values)} requiredMark={false}>
          <Form.Item label="Chẩn đoán / nguyên nhân" name="diagnosis" rules={[{ required: true, message: 'Nhập chẩn đoán' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Giải pháp đã thực hiện" name="resolution" rules={[{ required: true, message: 'Nhập giải pháp' }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item label="Ghi chú bàn giao" name="note"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Ghi nhận phụ tùng sử dụng" open={consumeOpen} onCancel={() => setConsumeOpen(false)} onOk={() => consumeForm.submit()} confirmLoading={consume.isPending} width={620} destroyOnHidden>
        <Form form={consumeForm} layout="vertical" onFinish={(values) => consume.mutate(values)} requiredMark={false}>
          <Form.Item label="Phụ tùng" name="sparePartId" rules={[{ required: true, message: 'Chọn phụ tùng' }]}>
            <Select showSearch optionFilterProp="label" options={parts?.content.map((part) => ({ value: part.id, label: `${part.sku} · ${part.name} · Tồn ${formatNumber(part.stockQuantity)} ${part.unit}` }))} />
          </Form.Item>
          <Form.Item label="Số lượng" name="quantity" rules={[{ required: true, message: 'Nhập số lượng' }]}><InputNumber min={0.001} precision={3} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Ghi chú" name="note"><Input placeholder="Ví dụ: Thay tụ máy nén" /></Form.Item>
          {consumeForm.getFieldValue('sparePartId') && (() => {
            const part = parts?.content.find((item) => item.id === consumeForm.getFieldValue('sparePartId'))
            return part ? <Tag color="blue">Đơn giá tham khảo: {formatCurrency(part.unitPrice)}</Tag> : null
          })()}
        </Form>
      </Modal>
    </div>
  )
}
