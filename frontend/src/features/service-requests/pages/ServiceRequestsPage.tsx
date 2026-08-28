import { BulbOutlined, CloseCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined, SwapOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Tooltip, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { aiApi } from '../../ai/api'
import { assetsApi } from '../../assets/api'
import { customersApi } from '../../customers/api'
import { serviceChannelsApi } from '../../service-channels/api'
import { serviceRequestsApi } from '../api'
import { useAuth } from '../../auth/AuthContext'
import { PageHeader } from '../../../components/PageHeader'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { MetaBadge } from '../../../components/PresentationBadge'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { ChannelTag, PriorityTag, StatusTag } from '../../../components/StatusTag'
import type { ServiceRequest, ServiceRequestDraftSuggestion } from '../../../types'
import { EMPTY_VALUE, formatDateTime } from '../../../utils/format'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'

const priorityOptions = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

const requestStatusOptions = [
  { value: 'OPEN', label: 'Đang mở' },
  { value: 'CONVERTED', label: 'Đã chuyển điều phối' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

export function ServiceRequestsPage() {
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebouncedValue(searchInput.trim())
  const [customerOptionSearchInput, setCustomerOptionSearchInput] = useState('')
  const customerOptionSearch = useDebouncedValue(customerOptionSearchInput.trim())
  const [assetOptionSearchInput, setAssetOptionSearchInput] = useState('')
  const assetOptionSearch = useDebouncedValue(assetOptionSearchInput.trim())
  const [status, setStatus] = useState<string>('OPEN')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ServiceRequest>()
  const [lastAiDraft, setLastAiDraft] = useState<ServiceRequestDraftSuggestion>()
  const [form] = Form.useForm()
  const handleFormValidationFailed = useFormValidationFeedback()
  const watchedCustomerId = Form.useWatch('customerId', form)
  const watchedTitle = Form.useWatch('title', form)
  const watchedDescription = Form.useWatch('description', form)
  const { message, notification } = App.useApp()
  const { user } = useAuth()
  const canConvert = user ? ['OWNER', 'CUSTOMER_SERVICE'].includes(user.role) : false
  const queryClient = useQueryClient()

  const serviceRequestsQuery = useQuery({
    queryKey: ['service-requests', { search, status, page, size: LIST_PAGE_SIZE }],
    queryFn: () => serviceRequestsApi.list(search, status, page, LIST_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const { data, isLoading, isFetching } = serviceRequestsQuery

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
    enabled: open,
    placeholderData: keepPreviousData,
  })
  const customers = customersQuery.data
  const assetsQuery = useQuery({
    queryKey: ['assets', 'service-request-customer', watchedCustomerId, assetOptionSearch],
    queryFn: () => assetsApi.list(assetOptionSearch, 0, LIST_PAGE_SIZE, watchedCustomerId),
    enabled: open && Boolean(watchedCustomerId),
  })
  const assets = assetsQuery.data
  const assetsLoading = assetsQuery.isFetching
  const channelsQuery = useQuery({ queryKey: ['service-channels'], queryFn: () => serviceChannelsApi.list(false) })
  const channels = channelsQuery.data ?? []

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

  const assetOptions = useMemo(() => {
    const options = (assets?.content ?? []).map((asset) => ({
      value: asset.id,
      label: `${asset.serialNumber ?? 'Chưa xác định serial'} · ${[asset.brand, asset.model].filter(Boolean).join(' ') || asset.category}`,
    }))
    if (
      editing?.assetId
      && editing.customerId === watchedCustomerId
      && !options.some((option) => option.value === editing.assetId)
    ) {
      return [
        {
          value: editing.assetId,
          label: `${editing.assetLabel ?? 'Thiết bị hiện tại'} · Thiết bị hiện tại`,
        },
        ...options,
      ]
    }
    return options
  }, [assets, editing, watchedCustomerId])

  const channelOptions = useMemo(
    () => channels.filter((channel) => channel.active).map((channel) => ({ value: channel.code, label: channel.name })),
    [channels],
  )
  const channelMap = useMemo(() => new Map(channels.map((channel) => [channel.code, channel])), [channels])

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['service-requests'] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-board'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => editing ? serviceRequestsApi.update(editing.id, values) : serviceRequestsApi.create(values),
    onSuccess: (savedRequest) => {
      if (editing) {
        message.success('Đã cập nhật yêu cầu dịch vụ')
      } else {
        notification.success({
          message: 'Đã tiếp nhận yêu cầu dịch vụ',
          description: savedRequest.title,
        })
      }
      setOpen(false)
      setEditing(undefined)
      form.resetFields()
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => serviceRequestsApi.delete(id),
    onSuccess: () => {
      message.success('Đã xóa yêu cầu dịch vụ')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const convert = useMutation({
    mutationFn: serviceRequestsApi.convert,
    onSuccess: (workOrder) => {
      notification.success({
        message: `Đã chuyển sang điều phối · ${workOrder.code}`,
        description: `${workOrder.summary} đã được tạo thành phiếu công việc và đưa vào hàng chờ điều phối.`,
      })
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const cancel = useMutation({
    mutationFn: serviceRequestsApi.cancel,
    onSuccess: () => {
      message.success('Đã hủy yêu cầu')
      refresh()
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const aiDraft = useMutation({
    mutationFn: aiApi.draftServiceRequest,
    onSuccess: (draft) => {
      setLastAiDraft(draft)
      form.setFieldsValue({
        title: draft.title,
        description: draft.description,
        priority: draft.priority,
        channel: draft.channel,
      })
      message.success(draft.provider === 'local' ? 'Đã tạo gợi ý nội bộ' : 'AI đã gợi ý nội dung tiếp nhận')
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const hasDraftInput = Boolean(`${watchedTitle ?? ''}${watchedDescription ?? ''}`.trim())
  const hasTitleInput = Boolean(`${watchedTitle ?? ''}`.trim())
  const hasDescriptionInput = Boolean(`${watchedDescription ?? ''}`.trim())
  const aiAssistDescription = (() => {
    if (hasTitleInput && hasDescriptionInput) {
      return 'AI sẽ chuẩn hóa cả tiêu đề và mô tả, đồng thời gợi ý mức ưu tiên và kênh tiếp nhận.'
    }
    if (hasTitleInput) {
      return 'Bạn đã nhập tiêu đề. Bấm AI gợi ý để hệ thống viết mô tả chi tiết và gợi ý ưu tiên/kênh.'
    }
    if (hasDescriptionInput) {
      return 'Bạn đã nhập mô tả. Bấm AI gợi ý để hệ thống rút gọn tiêu đề và gợi ý ưu tiên/kênh.'
    }
    return 'Nhập ít nhất một ô: Tiêu đề hoặc Mô tả chi tiết. Ô còn lại sẽ được AI tạo gợi ý.'
  })()

  const suggestWithAi = () => {
    const values = form.getFieldsValue(['title', 'description', 'channel'])
    const rawText = [values.title, values.description].filter(Boolean).join('\n\n').trim()
    if (!rawText) {
      message.warning('Nhập nội dung khách báo trước khi dùng AI gợi ý')
      return
    }
    aiDraft.mutate({ rawText, preferredChannel: values.channel })
  }

  const handleCustomerChange = (customerId: string) => {
    setAssetOptionSearchInput('')
    form.setFieldsValue({ customerId, assetId: undefined })
  }

  const showCreate = () => {
    setEditing(undefined)
    setLastAiDraft(undefined)
    setCustomerOptionSearchInput('')
    setAssetOptionSearchInput('')
    form.resetFields()
    form.setFieldsValue({ priority: 'NORMAL', channel: channelOptions[0]?.value ?? 'PHONE' })
    setOpen(true)
  }

  const showEdit = (record: ServiceRequest) => {
    setEditing(record)
    setLastAiDraft(undefined)
    setCustomerOptionSearchInput('')
    setAssetOptionSearchInput('')
    form.setFieldsValue({
      customerId: record.customerId,
      assetId: record.assetId,
      priority: record.priority,
      channel: record.channel,
      title: record.title,
      description: record.description,
    })
    setOpen(true)
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Tiếp nhận dịch vụ"
        title="Yêu cầu dịch vụ"
        description="Tiếp nhận nhu cầu khách hàng, hoàn thiện thông tin và bàn giao yêu cầu đủ điều kiện sang bộ phận điều phối."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Tiếp nhận yêu cầu</Button>}
        meta={<><MetaBadge>{serviceRequestsQuery.isError ? 'Lỗi tải dữ liệu' : `${data?.totalElements ?? 0} yêu cầu`}</MetaBadge><MetaBadge tone={status ? 'info' : 'neutral'}>{status ? requestStatusOptions.find((option) => option.value === status)?.label : 'Tất cả trạng thái'}</MetaBadge></>}
      />

      <div className="table-toolbar toolbar-row">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm tiêu đề, mô tả, khách hàng hoặc serial" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
        <Select allowClear placeholder="Tất cả trạng thái" value={status} onChange={(value) => { setStatus(value); setPage(0) }} options={requestStatusOptions} />
      </div>

      {serviceRequestsQuery.isError && (
        <QueryErrorAlert
          title="Chưa tải được danh sách yêu cầu dịch vụ"
          error={serviceRequestsQuery.error}
          onRetry={() => serviceRequestsQuery.refetch()}
        />
      )}

      <Table
        rowKey="id"
        loading={isLoading || isFetching}
        dataSource={serviceRequestsQuery.isError ? [] : (data?.content ?? [])}
        className="content-table"
        scroll={{ x: 1180 }}
        pagination={{
          current: page + 1,
          pageSize: LIST_PAGE_SIZE,
          total: serviceRequestsQuery.isError ? 0 : (data?.totalElements ?? 0),
          showSizeChanger: false,
          showTotal: (total, range) => `${range[0]}–${range[1]} / ${total} yêu cầu`,
        }}
        onChange={(pagination) => setPage(Math.max((pagination.current ?? 1) - 1, 0))}
        locale={{ emptyText: <Empty description={serviceRequestsQuery.isError ? 'Không thể tải dữ liệu yêu cầu dịch vụ' : 'Chưa có yêu cầu phù hợp'} /> }}
        columns={[
          {
            title: 'Yêu cầu',
            width: 280,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{record.title}</Typography.Text>
                <Typography.Text type="secondary" ellipsis>{record.description}</Typography.Text>
              </div>
            ),
          },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 175, ellipsis: true },
          { title: 'Thiết bị', dataIndex: 'assetLabel', width: 180, ellipsis: true, render: (value) => value || EMPTY_VALUE },
          { title: 'Ưu tiên', dataIndex: 'priority', width: 100, render: (value) => <PriorityTag priority={value} /> },
          {
            title: 'Kênh',
            dataIndex: 'channel',
            width: 120,
            render: (value) => {
              const channel = channelMap.get(value)
              return <ChannelTag channel={value} label={channel?.name} color={channel?.color} />
            },
          },
          { title: 'Trạng thái', dataIndex: 'status', width: 130, render: (value) => <StatusTag status={value} /> },
          { title: 'Tiếp nhận', dataIndex: 'createdAt', width: 150, render: formatDateTime },
          {
            title: 'Thao tác',
            width: 128,
            render: (_, record) => {
              const isOpen = record.status === 'OPEN'
              const isConverted = record.status === 'CONVERTED'
              return (
                <Space size={4}>
                  <Tooltip title={isOpen ? 'Sửa yêu cầu' : 'Chỉ sửa được yêu cầu đang mở'}>
                    <Button aria-label="Sửa yêu cầu" type="text" disabled={!isOpen} icon={<EditOutlined />} onClick={() => showEdit(record)} />
                  </Tooltip>

                  {isOpen && (
                    <>
                      {canConvert && (
                        <Popconfirm
                          title="Chuyển yêu cầu sang điều phối?"
                          description="Yêu cầu sẽ được khóa và một phiếu công việc mới sẽ được tạo cho Dispatcher xử lý."
                          okText="Chuyển sang điều phối"
                          cancelText="Giữ lại"
                          okButtonProps={{ loading: convert.isPending }}
                          onConfirm={() => convert.mutate(record.id)}
                        >
                          <Button
                            aria-label="Chuyển sang điều phối"
                            title="Chuyển sang điều phối"
                            type="text"
                            icon={<SwapOutlined />}
                          />
                        </Popconfirm>
                      )}
                      <Popconfirm title="Hủy yêu cầu này?" okText="Hủy" cancelText="Giữ lại" onConfirm={() => cancel.mutate(record.id)}>
                        <Tooltip title="Hủy yêu cầu"><Button aria-label="Hủy yêu cầu" type="text" danger icon={<CloseCircleOutlined />} /></Tooltip>
                      </Popconfirm>
                    </>
                  )}

                  <Tooltip title={isConverted ? 'Không thể xóa yêu cầu đã tạo phiếu công việc' : 'Xóa yêu cầu'}>
                    <Popconfirm
                      disabled={isConverted}
                      title="Xóa yêu cầu này?"
                      description="Không thể xóa yêu cầu đã chuyển thành phiếu công việc."
                      okText="Xóa"
                      cancelText="Hủy"
                      okButtonProps={{ danger: true, loading: remove.isPending }}
                      onConfirm={() => remove.mutate(record.id)}
                    >
                      <Button aria-label="Xóa yêu cầu" type="text" danger disabled={isConverted} icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </Tooltip>
                </Space>
              )
            },
          },
        ]}
      />

      <Modal title={editing ? 'Cập nhật yêu cầu dịch vụ' : 'Tiếp nhận yêu cầu dịch vụ'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} okText={editing ? 'Lưu thay đổi' : 'Tiếp nhận yêu cầu'} width={760} destroyOnHidden>
        {customersQuery.isError ? (
          <QueryErrorAlert title="Chưa tải được danh sách khách hàng" error={customersQuery.error} onRetry={() => customersQuery.refetch()} />
        ) : null}
        {channelsQuery.isError ? (
          <QueryErrorAlert title="Chưa tải được kênh tiếp nhận" error={channelsQuery.error} onRetry={() => channelsQuery.refetch()} />
        ) : null}
        {watchedCustomerId && assetsQuery.isError ? (
          <QueryErrorAlert title="Chưa tải được thiết bị của khách hàng" error={assetsQuery.error} onRetry={() => assetsQuery.refetch()} />
        ) : null}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
          <div className="form-grid two-cols">
            <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
              <Select
                showSearch
                filterOption={false}
                loading={customersQuery.isFetching}
                placeholder="Tìm theo mã hoặc tên khách hàng"
                options={customerOptions}
                onSearch={setCustomerOptionSearchInput}
                onChange={handleCustomerChange}
              />
            </Form.Item>
            <Form.Item label="Thiết bị (không bắt buộc)" name="assetId">
              <Select
                allowClear
                showSearch
                filterOption={false}
                disabled={!watchedCustomerId}
                loading={assetsLoading}
                placeholder={watchedCustomerId ? 'Tìm serial, hãng hoặc model' : 'Chọn khách hàng trước'}
                notFoundContent={watchedCustomerId && !assetsLoading ? 'Không tìm thấy thiết bị phù hợp' : undefined}
                onSearch={setAssetOptionSearchInput}
                options={assetOptions}
              />
            </Form.Item>
            <Form.Item label="Mức độ ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={priorityOptions} /></Form.Item>
            <Form.Item label="Kênh tiếp nhận" name="channel" rules={[{ required: true, message: 'Chọn kênh tiếp nhận' }]}>
              <Select options={channelOptions} placeholder="Chọn kênh tiếp nhận" />
            </Form.Item>
          </div>
          <div className="form-assist-row">
            <div>
              <Space size={8} wrap>
                <Typography.Text strong>AI tiếp nhận</Typography.Text>
                <MetaBadge tone="info">{lastAiDraft?.provider === 'gemini' ? 'Gemini' : 'Sẵn sàng'}</MetaBadge>
              </Space>
              <Typography.Text type="secondary">{aiAssistDescription}</Typography.Text>
              {lastAiDraft && (
                <Typography.Text type="secondary" className="form-assist-note">
                  {lastAiDraft.reason} · Độ tin cậy {Math.round(lastAiDraft.confidence * 100)}%
                </Typography.Text>
              )}
            </div>
            <Button icon={<BulbOutlined />} loading={aiDraft.isPending} disabled={!hasDraftInput} onClick={suggestWithAi}>
              AI gợi ý
            </Button>
          </div>
          <Form.Item label="Tiêu đề" name="title" rules={[{ required: true, message: 'Nhập tiêu đề yêu cầu' }]}><Input placeholder="Ví dụ: Máy lạnh không đủ lạnh" /></Form.Item>
          <Form.Item label="Mô tả chi tiết" name="description" rules={[{ required: true, message: 'Nhập mô tả chi tiết' }]}><Input.TextArea rows={5} placeholder="Triệu chứng, thời điểm xảy ra, yêu cầu của khách hàng..." /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
