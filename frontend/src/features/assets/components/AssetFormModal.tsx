import { DatePicker, Form, Input, Modal, Select } from 'antd'
import type { FormInstance } from 'antd'
import { QueryErrorAlert } from '../../../components/QueryErrorAlert'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
import type { Asset } from '../../../types'

const assetStatusOptions = [
  { value: 'ACTIVE', label: 'Hoạt động' },
  { value: 'IN_SERVICE', label: 'Đang sửa chữa' },
  { value: 'OUT_OF_SERVICE', label: 'Tạm ngưng' },
  { value: 'RETIRED', label: 'Thanh lý' },
]

type CustomerOption = { value: string; label: string }

type AssetFormModalProps = {
  open: boolean
  editing?: Asset
  form: FormInstance
  saving: boolean
  customerOptions: CustomerOption[]
  customersLoading: boolean
  customersError: unknown
  onRetryCustomers: () => void
  onCustomerSearch: (value: string) => void
  onCancel: () => void
  onSubmit: (values: Record<string, unknown>) => void
}

export function AssetFormModal({
  open,
  editing,
  form,
  saving,
  customerOptions,
  customersLoading,
  customersError,
  onRetryCustomers,
  onCustomerSearch,
  onCancel,
  onSubmit,
}: AssetFormModalProps) {
  const onFinishFailed = useFormValidationFeedback()

  return (
    <Modal
      title={editing ? 'Cập nhật thiết bị' : 'Thêm thiết bị'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={saving}
      okText={editing ? 'Lưu thay đổi' : 'Thêm thiết bị'}
      width={720}
      destroyOnHidden
    >
      {customersError ? (
        <QueryErrorAlert title="Chưa tải được danh sách khách hàng" error={customersError} onRetry={onRetryCustomers} />
      ) : null}
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={onFinishFailed} scrollToFirstError requiredMark>
        <Form.Item label="Khách hàng" name="customerId" rules={[{ required: true, message: 'Chọn khách hàng' }]}>
          <Select
            showSearch
            filterOption={false}
            loading={customersLoading}
            placeholder="Tìm theo mã hoặc tên khách hàng"
            onSearch={onCustomerSearch}
            options={customerOptions}
          />
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
  )
}
