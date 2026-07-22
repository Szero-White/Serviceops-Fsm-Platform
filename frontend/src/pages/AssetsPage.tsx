import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, DatePicker, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { apiErrorMessage } from '../api/http'
import { assetsApi, customersApi } from '../api/services'
import { PageHeader } from '../components/PageHeader'
import { StatusTag } from '../components/StatusTag'
import type { Asset } from '../types'
import { EMPTY_VALUE, formatDate } from '../utils/format'

const assetStatusOptions = [
  { value: 'ACTIVE', label: 'Hoạt động' },
  { value: 'IN_SERVICE', label: 'Đang sửa chữa' },
  { value: 'OUT_OF_SERVICE', label: 'Tạm ngưng' },
  { value: 'RETIRED', label: 'Thanh lý' },
]

export function AssetsPage() {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Asset>()
  const [form] = Form.useForm()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['assets', search], queryFn: () => assetsApi.list(search) })
  const { data: customers } = useQuery({ queryKey: ['customers', 'all'], queryFn: () => customersApi.list('', 0, 200) })

  const save = useMutation({
    mutationFn: (values: Record<string, unknown>) => {
      const payload = {
        ...values,
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

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Installed base"
        title="Thiết bị khách hàng"
        description="Theo dõi serial, bảo hành, vòng đời và tình trạng phục vụ của từng tài sản."
        actions={<Button type="primary" icon={<PlusOutlined />} onClick={showCreate}>Thêm thiết bị</Button>}
        meta={<Tag color="blue">{data?.totalElements ?? 0} thiết bị</Tag>}
      />

      <div className="table-toolbar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="Tìm serial, hãng, model hoặc khách hàng" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content ?? []}
        className="content-table"
        scroll={{ x: 1080 }}
        pagination={{ pageSize: 12, showSizeChanger: false }}
        locale={{ emptyText: <Empty description="Chưa có thiết bị phù hợp" /> }}
        columns={[
          {
            title: 'Thiết bị',
            width: 300,
            render: (_, record) => (
              <div className="table-primary-cell">
                <Typography.Text strong>{[record.brand, record.model].filter(Boolean).join(' ') || record.category}</Typography.Text>
                <Typography.Text type="secondary" code>{record.serialNumber}</Typography.Text>
              </div>
            ),
          },
          { title: 'Khách hàng', dataIndex: 'customerName', width: 230, ellipsis: true },
          { title: 'Loại', dataIndex: 'category', width: 150 },
          {
            title: 'Bảo hành',
            width: 180,
            render: (_, record) => (
              <div className="table-secondary-stack">
                <span>{formatDate(record.warrantyUntil)}</span>
                <Tag color={record.underWarranty ? 'green' : 'red'}>{record.underWarranty ? 'Còn bảo hành' : 'Hết bảo hành'}</Tag>
              </div>
            ),
          },
          { title: 'Trạng thái', dataIndex: 'status', width: 150, render: (value) => <StatusTag status={value} /> },
          { title: 'Ngày lắp', dataIndex: 'installedAt', width: 130, render: formatDate },
          { title: 'Ghi chú', dataIndex: 'notes', ellipsis: true, render: (value) => value || EMPTY_VALUE },
          {
            title: '',
            width: 92,
            fixed: 'right',
            render: (_, record) => (
              <Space size={4}>
                <Button aria-label="Sửa thiết bị" type="text" icon={<EditOutlined />} onClick={() => showEdit(record)} />
                <Popconfirm
                  title="Xoá thiết bị này?"
                  description="Chỉ xoá được khi thiết bị chưa được dùng trong yêu cầu dịch vụ hoặc work order."
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
            <Form.Item label="Serial number" name="serialNumber" rules={[{ required: true, message: 'Nhập serial' }]}><Input /></Form.Item>
            <Form.Item label="Hãng" name="brand"><Input placeholder="Daikin" /></Form.Item>
            <Form.Item label="Model" name="model"><Input /></Form.Item>
            <Form.Item label="Ngày lắp đặt" name="installedAt"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
            <Form.Item label="Bảo hành đến" name="warrantyUntil"><DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" /></Form.Item>
          </div>
          <Form.Item label="Trạng thái" name="status"><Select options={assetStatusOptions} /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
