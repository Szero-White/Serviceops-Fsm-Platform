import {
  CloudUploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FileImageOutlined,
  FilePdfOutlined,
} from '@ant-design/icons'
import { App, Button, Empty, Form, Input, List, Modal, Popconfirm, Spin } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { apiErrorMessage } from '../../../api/http'
import type { AttachmentItem } from '../../../types'
import { formatNumber } from '../../../utils/format'
import { attachmentsApi } from '../api'

function isImage(contentType: string) {
  return contentType.startsWith('image/')
}

function isPdf(contentType: string) {
  return contentType === 'application/pdf'
}

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

function isFormValidationError(error: unknown) {
  return typeof error === 'object' && error !== null && 'errorFields' in error
}

type AttachmentListProps = {
  attachments?: AttachmentItem[]
  onChanged?: () => void
}

export function AttachmentList({ attachments, onChanged }: AttachmentListProps) {
  const { message } = App.useApp()
  const [renameForm] = Form.useForm<{ originalFilename: string }>()
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewFile, setPreviewFile] = useState<AttachmentItem | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string>()
  const [previewLoading, setPreviewLoading] = useState(false)
  const [renameFile, setRenameFile] = useState<AttachmentItem | null>(null)
  const [renaming, setRenaming] = useState(false)
  const [deletingId, setDeletingId] = useState<string>()
  const previewRequestRef = useRef(0)
  const previewUrlRef = useRef<string | undefined>(undefined)

  useEffect(() => {
    return () => {
      if (previewUrlRef.current) {
        URL.revokeObjectURL(previewUrlRef.current)
      }
    }
  }, [])

  const revokePreviewUrl = () => {
    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current)
      previewUrlRef.current = undefined
    }
    setPreviewUrl(undefined)
  }

  const closePreview = () => {
    previewRequestRef.current += 1
    setPreviewOpen(false)
    setPreviewFile(null)
    setPreviewLoading(false)
    revokePreviewUrl()
  }

  const handlePreview = async (file: AttachmentItem) => {
    if (!isImage(file.contentType) && !isPdf(file.contentType)) return

    setPreviewFile(file)
    setPreviewOpen(true)
    setPreviewLoading(true)
    const requestId = previewRequestRef.current + 1
    previewRequestRef.current = requestId

    try {
      const blob = await attachmentsApi.download(file.id)
      if (requestId !== previewRequestRef.current) return
      const objectUrl = URL.createObjectURL(blob)
      revokePreviewUrl()
      previewUrlRef.current = objectUrl
      setPreviewUrl(objectUrl)
    } catch (error) {
      if (requestId !== previewRequestRef.current) return
      message.error(apiErrorMessage(error))
      closePreview()
    } finally {
      if (requestId === previewRequestRef.current) {
        setPreviewLoading(false)
      }
    }
  }

  const handleDownload = async (file: AttachmentItem) => {
    try {
      const blob = await attachmentsApi.download(file.id)
      downloadBlob(blob, file.originalFilename)
    } catch (error) {
      message.error(apiErrorMessage(error))
    }
  }

  const openRename = (file: AttachmentItem) => {
    setRenameFile(file)
    renameForm.setFieldsValue({ originalFilename: file.originalFilename })
  }

  const closeRename = () => {
    setRenameFile(null)
    renameForm.resetFields()
  }

  const handleRename = async () => {
    if (!renameFile) return
    try {
      const values = await renameForm.validateFields()
      setRenaming(true)
      await attachmentsApi.rename(renameFile.id, values.originalFilename)
      message.success('Đã đổi tên tệp đính kèm')
      closeRename()
      onChanged?.()
    } catch (error) {
      if (isFormValidationError(error)) return
      message.error(apiErrorMessage(error))
    } finally {
      setRenaming(false)
    }
  }

  const handleDelete = async (file: AttachmentItem) => {
    try {
      setDeletingId(file.id)
      await attachmentsApi.delete(file.id)
      if (previewFile?.id === file.id) {
        closePreview()
      }
      message.success('Đã xoá tệp đính kèm')
      onChanged?.()
    } catch (error) {
      message.error(apiErrorMessage(error))
    } finally {
      setDeletingId(undefined)
    }
  }

  if (!attachments?.length) {
    return <Empty description="Chưa có ảnh hoặc tài liệu" />
  }

  return (
    <>
      <List
        dataSource={attachments}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button
                key="preview"
                type="text"
                icon={<EyeOutlined />}
                onClick={() => handlePreview(item)}
                disabled={!isImage(item.contentType) && !isPdf(item.contentType)}
              >
                Xem
              </Button>,
              <Button key="download" type="text" icon={<DownloadOutlined />} onClick={() => handleDownload(item)}>
                Tải xuống
              </Button>,
              <Button key="rename" type="text" icon={<EditOutlined />} onClick={() => openRename(item)}>
                Đổi tên
              </Button>,
              <Popconfirm
                key="delete"
                title="Xoá tệp đính kèm này?"
                description="File sẽ bị xoá khỏi phiếu công việc và không còn tải xuống được."
                okText="Xoá"
                cancelText="Giữ lại"
                okButtonProps={{ danger: true, loading: deletingId === item.id }}
                onConfirm={() => handleDelete(item)}
              >
                <Button type="text" danger icon={<DeleteOutlined />} loading={deletingId === item.id}>
                  Xoá
                </Button>
              </Popconfirm>,
            ]}
          >
            <List.Item.Meta
              avatar={
                isImage(item.contentType) ? (
                  <FileImageOutlined style={{ fontSize: 24, color: '#3b82f6' }} />
                ) : isPdf(item.contentType) ? (
                  <FilePdfOutlined style={{ fontSize: 24, color: '#ef4444' }} />
                ) : (
                  <CloudUploadOutlined style={{ fontSize: 24, color: '#64748b' }} />
                )
              }
              title={item.originalFilename}
              description={`${item.contentType} · ${formatNumber(item.fileSize / 1024, 1)} KB · ${item.uploadedBy}`}
            />
          </List.Item>
        )}
      />

      <Modal
        title={previewFile?.originalFilename}
        open={previewOpen}
        onCancel={closePreview}
        footer={[
          <Button key="download" icon={<DownloadOutlined />} onClick={() => previewFile && handleDownload(previewFile)}>
            Tải xuống
          </Button>,
          <Button key="close" onClick={closePreview}>
            Đóng
          </Button>,
        ]}
        width={isPdf(previewFile?.contentType ?? '') ? 800 : 'auto'}
        centered
      >
        {previewFile && (
          <div style={{ textAlign: 'center', minHeight: 400 }}>
            {previewLoading ? (
              <Spin style={{ marginTop: 160 }} />
            ) : isImage(previewFile.contentType) && previewUrl ? (
              <img
                src={previewUrl}
                alt={previewFile.originalFilename}
                style={{ maxWidth: '100%', maxHeight: '70vh', borderRadius: 8 }}
              />
            ) : isPdf(previewFile.contentType) && previewUrl ? (
              <iframe
                src={previewUrl}
                style={{ width: '100%', height: '70vh', border: 'none', borderRadius: 8 }}
                title={previewFile.originalFilename}
              />
            ) : (
              <Empty description="Không thể xem loại file này" />
            )}
          </div>
        )}
      </Modal>

      <Modal
        title="Đổi tên tệp đính kèm"
        open={Boolean(renameFile)}
        onCancel={closeRename}
        onOk={handleRename}
        confirmLoading={renaming}
        okText="Lưu"
        cancelText="Huỷ"
        destroyOnHidden
      >
        <Form form={renameForm} layout="vertical" requiredMark={false}>
          <Form.Item
            label="Tên file hiển thị"
            name="originalFilename"
            rules={[
              { required: true, message: 'Nhập tên file' },
              { max: 255, message: 'Tên file không được vượt quá 255 ký tự' },
            ]}
          >
            <Input placeholder="Ví dụ: Bien-ban-nghiem-thu.pdf" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
