import { Form, Input, Modal, Select, Switch } from 'antd'
import type { FormInstance } from 'antd'
import { useFormValidationFeedback } from '../../../hooks/useFormValidationFeedback'
import { USER_ROLE_LABELS } from '../../../constants/userRoles'
import type { UserAccount, UserRole } from '../../../types'

const roleOptions = Object.entries(USER_ROLE_LABELS).map(([value, label]) => ({ value, label }))

function usernameFromName(value: string) {
  const slug = value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^a-zA-Z0-9]+/g, '.')
    .replace(/^\.+|\.+$/g, '')
    .toLowerCase()

  return slug || `user.${Date.now().toString().slice(-5)}`
}

type UserFormModalProps = {
  open: boolean
  editing?: UserAccount
  currentUserId?: string
  selectedRole?: UserRole
  form: FormInstance
  saving: boolean
  onCancel: () => void
  onSubmit: (values: Record<string, unknown>) => void
}

export function UserFormModal({ open, editing, currentUserId, selectedRole, form, saving, onCancel, onSubmit }: UserFormModalProps) {
  const onFinishFailed = useFormValidationFeedback()

  return (
    <Modal
      title={editing ? 'Cập nhật người dùng' : selectedRole === 'TECHNICIAN' ? 'Thêm kỹ thuật viên' : 'Thêm người dùng'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      okText={editing ? 'Lưu thay đổi' : 'Tạo người dùng'}
      confirmLoading={saving}
      width={760}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={onSubmit} onFinishFailed={onFinishFailed} scrollToFirstError requiredMark>
        <div className="form-grid two-cols">
          <Form.Item label="Họ tên" name="displayName" rules={[{ required: true, message: 'Nhập họ tên người dùng' }]}>
            <Input
              placeholder="Ví dụ: Lê Thu Điều phối"
              onBlur={(event) => !editing && !form.getFieldValue('username') && form.setFieldValue('username', usernameFromName(event.target.value))}
            />
          </Form.Item>
          <Form.Item label={editing ? 'Tên đăng nhập (không thể thay đổi)' : 'Tên đăng nhập'} name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
            <Input placeholder="le.thu.dieu.phoi" disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item label={editing ? 'Vai trò (không thể thay đổi)' : 'Vai trò'} name="role" rules={[{ required: true, message: 'Chọn vai trò' }]}>
            <Select options={roleOptions} disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item label={editing ? 'Mật khẩu mới' : 'Mật khẩu'} name="password" rules={editing ? [] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
            <Input.Password placeholder={editing ? 'Bỏ trống nếu không đổi' : 'Tối thiểu 8 ký tự'} />
          </Form.Item>
          {selectedRole === 'TECHNICIAN' && (
            <>
              <Form.Item label="Điện thoại kỹ thuật viên" name="phone"><Input placeholder="0909123456" /></Form.Item>
              <Form.Item label="Kỹ năng kỹ thuật viên" name="skills"><Input placeholder="Máy lạnh, tủ lạnh, điện dân dụng..." /></Form.Item>
            </>
          )}
        </div>
        <Form.Item name="active" valuePropName="checked">
          <Switch disabled={editing?.id === currentUserId} checkedChildren="Hoạt động" unCheckedChildren="Tạm ngưng" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
