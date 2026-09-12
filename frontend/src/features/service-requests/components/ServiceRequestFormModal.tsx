import { BulbOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { LIST_PAGE_SIZE } from '../../../constants/pagination'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
import type { ServiceRequest, ServiceRequestDraftSuggestion } from '../../../types'
import { aiApi } from '../../ai/api'
import { AiSourceBadge } from '../../ai/components/AiSourceBadge'
import { assetsApi } from '../../assets/api'
import { customersApi } from '../../customers/api'
import { serviceChannelsApi } from '../../service-channels/api'
import { serviceRequestsApi } from '../api'

const PRIORITY_OPTIONS = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
]

interface ServiceRequestFormModalProps {
  open: boolean
  editing?: ServiceRequest
  onClose: () => void
  onSaved: (request: ServiceRequest, wasEditing: boolean) => void
}

export function ServiceRequestFormModal({ open, editing, onClose, onSaved }: ServiceRequestFormModalProps) {
  const { message } = App.useApp()
  const [form] = Form.useForm()
  const handleFormValidationFailed = useFormValidationFeedback()
  const [customerSearchInput, setCustomerSearchInput] = useState('')
  const [assetSearchInput, setAssetSearchInput] = useState('')
  const [lastAiDraft, setLastAiDraft] = useState<ServiceRequestDraftSuggestion>()
  const customerSearch = useDebouncedValue(customerSearchInput.trim())
  const assetSearch = useDebouncedValue(assetSearchInput.trim())
  const watchedCustomerId = Form.useWatch('customerId', form)
  const watchedTitle = Form.useWatch('title', form)
  const watchedDescription = Form.useWatch('description', form)

  const customersQuery = useQuery({
    queryKey: ['customers', 'active-options', customerSearch],
    queryFn: () => customersApi.list(customerSearch, 0, LIST_PAGE_SIZE, true),
    enabled: open,
    placeholderData: keepPreviousData,
  })
  const assetsQuery = useQuery({
    queryKey: ['assets', 'service-request-customer', watchedCustomerId, assetSearch],
    queryFn: () => assetsApi.list(assetSearch, 0, LIST_PAGE_SIZE, watchedCustomerId),
    enabled: open && Boolean(watchedCustomerId),
  })
  const channelsQuery = useQuery({
    queryKey: ['service-channels'],
    queryFn: () => serviceChannelsApi.list(false),
    enabled: open,
  })

  const channels = channelsQuery.data ?? []
  const channelOptions = useMemo(
    () => channels.filter((channel) => channel.active).map((channel) => ({ value: channel.code, label: channel.name })),
    [channels],
  )
  const customerOptions = useMemo(() => {
    const options = (customersQuery.data?.content ?? []).map((customer) => ({
      value: customer.id,
      label: `${customer.code} · ${customer.name}`,
    }))
    if (editing && !options.some((option) => option.value === editing.customerId)) {
      return [{ value: editing.customerId, label: `${editing.customerName} · Khách hàng hiện tại` }, ...options]
    }
    return options
  }, [customersQuery.data, editing])
  const assetOptions = useMemo(() => {
    const options = (assetsQuery.data?.content ?? []).map((asset) => ({
      value: asset.id,
      label: `${asset.serialNumber ?? 'Chưa xác định serial'} · ${[asset.brand, asset.model].filter(Boolean).join(' ') || asset.category}`,
    }))
    if (editing?.assetId && editing.customerId === watchedCustomerId && !options.some((option) => option.value === editing.assetId)) {
      return [{ value: editing.assetId, label: `${editing.assetLabel ?? 'Thiết bị hiện tại'} · Thiết bị hiện tại` }, ...options]
    }
    return options
  }, [assetsQuery.data, editing, watchedCustomerId])

  useEffect(() => {
    if (!open) return
    setCustomerSearchInput('')
    setAssetSearchInput('')
    setLastAiDraft(undefined)
    form.resetFields()
    if (editing) {
      form.setFieldsValue({
        customerId: editing.customerId,
        assetId: editing.assetId,
        priority: editing.priority,
        channel: editing.channel,
        title: editing.title,
        description: editing.description,
      })
    } else {
      form.setFieldsValue({ priority: 'NORMAL' })
    }
  }, [editing, form, open])

  useEffect(() => {
    if (open && !editing && !form.getFieldValue('channel') && channelOptions[0]?.value) {
      form.setFieldValue('channel', channelOptions[0].value)
    }
  }, [channelOptions, editing, form, open])

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => editing
      ? serviceRequestsApi.update(editing.id, values)
      : serviceRequestsApi.create(values),
    onSuccess: (savedRequest) => onSaved(savedRequest, Boolean(editing)),
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const aiDraft = useMutation({
    mutationFn: aiApi.draftServiceRequest,
    onSuccess: (draft) => {
      setLastAiDraft(draft)
      form.setFieldsValue({ title: draft.title, description: draft.description })
      message.success('AI đã gợi ý nội dung tiếp nhận')
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const hasTitleInput = Boolean(`${watchedTitle ?? ''}`.trim())
  const hasDescriptionInput = Boolean(`${watchedDescription ?? ''}`.trim())
  const hasDraftInput = hasTitleInput || hasDescriptionInput
  const aiAssistDescription = hasTitleInput && hasDescriptionInput
    ? 'AI sẽ chuẩn hóa tiêu đề và mô tả. Mức độ ưu tiên và kênh tiếp nhận giữ nguyên theo lựa chọn của bạn.'
    : hasTitleInput
      ? 'Bạn đã nhập tiêu đề. Bấm AI gợi ý để hệ thống viết mô tả chi tiết; ưu tiên và kênh tiếp nhận không thay đổi.'
      : hasDescriptionInput
        ? 'Bạn đã nhập mô tả. Bấm AI gợi ý để hệ thống rút gọn tiêu đề; ưu tiên và kênh tiếp nhận không thay đổi.'
        : 'Nhập ít nhất một ô: Tiêu đề hoặc Mô tả chi tiết. Ô còn lại sẽ được AI tạo gợi ý.'

  const suggestWithAi = () => {
    const values = form.getFieldsValue(['title', 'description'])
    const rawText = [values.title, values.description].filter(Boolean).join('\n\n').trim()
    if (!rawText) {
      message.warning('Nhập nội dung khách báo trước khi dùng AI gợi ý')
      return
    }
    aiDraft.mutate({ rawText })
  }

  const handleCustomerChange = (customerId: string) => {
    setAssetSearchInput('')
    form.setFieldsValue({ customerId, assetId: undefined })
  }

  return (
    <Modal
      title={editing ? 'Cập nhật yêu cầu dịch vụ' : 'Tiếp nhận yêu cầu dịch vụ'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={save.isPending}
      okText={editing ? 'Lưu thay đổi' : 'Tiếp nhận yêu cầu'}
      width={760}
      destroyOnHidden
    >
      {customersQuery.isError ? <QueryErrorAlert title="Chưa tải được danh sách khách hàng" error={customersQuery.error} onRetry={() => customersQuery.refetch()} /> : null}
      {channelsQuery.isError ? <QueryErrorAlert title="Chưa tải được kênh tiếp nhận" error={channelsQuery.error} onRetry={() => channelsQuery.refetch()} /> : null}
      {watchedCustomerId && assetsQuery.isError ? <QueryErrorAlert title="Chưa tải được thiết bị của khách hàng" error={assetsQuery.error} onRetry={() => assetsQuery.refetch()} /> : null}

      <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} onFinishFailed={handleFormValidationFailed} scrollToFirstError requiredMark>
        <div className="form-grid two-cols">
          <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
            <Select showSearch filterOption={false} loading={customersQuery.isFetching} placeholder="Tìm theo mã hoặc tên khách hàng" options={customerOptions} onSearch={setCustomerSearchInput} onChange={handleCustomerChange} />
          </Form.Item>
          <Form.Item label="Thiết bị (không bắt buộc)" name="assetId">
            <Select
              allowClear
              showSearch
              filterOption={false}
              disabled={!watchedCustomerId}
              loading={assetsQuery.isFetching}
              placeholder={watchedCustomerId ? 'Tìm serial, hãng hoặc model' : 'Chọn khách hàng trước'}
              notFoundContent={watchedCustomerId && !assetsQuery.isFetching ? 'Không tìm thấy thiết bị phù hợp' : undefined}
              onSearch={setAssetSearchInput}
              options={assetOptions}
            />
          </Form.Item>
          <Form.Item label="Mức độ ưu tiên" name="priority" rules={[{ required: true, message: 'Chọn mức ưu tiên' }]}><Select options={PRIORITY_OPTIONS} /></Form.Item>
          <Form.Item label="Kênh tiếp nhận" name="channel" rules={[{ required: true, message: 'Chọn kênh tiếp nhận' }]}><Select options={channelOptions} placeholder="Chọn kênh tiếp nhận" /></Form.Item>
        </div>

        <div className="form-assist-row">
          <div>
            <Space size={8} wrap>
              <Typography.Text strong>AI tiếp nhận</Typography.Text>
              <AiSourceBadge source={lastAiDraft?.source} />
            </Space>
            <Typography.Text type="secondary">{aiAssistDescription}</Typography.Text>
          </div>
          <Button icon={<BulbOutlined />} loading={aiDraft.isPending} disabled={!hasDraftInput || aiDraft.isPending} onClick={suggestWithAi}>AI gợi ý</Button>
        </div>

        <Form.Item label="Tiêu đề" name="title" rules={[{ required: true, message: 'Nhập tiêu đề yêu cầu' }]}><Input disabled={aiDraft.isPending} placeholder="Ví dụ: Máy lạnh không đủ lạnh" /></Form.Item>
        <Form.Item label="Mô tả chi tiết" name="description" rules={[{ required: true, message: 'Nhập mô tả chi tiết' }]}><Input.TextArea disabled={aiDraft.isPending} rows={5} placeholder="Triệu chứng, thời điểm xảy ra, yêu cầu của khách hàng..." /></Form.Item>
      </Form>
    </Modal>
  )
}
