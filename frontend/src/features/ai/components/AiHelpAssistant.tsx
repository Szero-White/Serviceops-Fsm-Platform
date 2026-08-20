import { BulbOutlined, SendOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { App, Button, Drawer, Empty, Input, Space, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { useAuth } from '../../auth/AuthContext'
import { MetaBadge } from '../../../components/PresentationBadge'
import { aiApi } from '../api'
import type { AiHelpResponse } from '../../../types'

type ChatMessage =
  | { id: string; role: 'user'; content: string }
  | { id: string; role: 'assistant'; content: AiHelpResponse }

const roleSuggestions: Record<string, string[]> = {
  OWNER: [
    'Tôi cần tạo tài khoản nhân viên mới như thế nào?',
    'Làm sao kiểm tra ai đã sửa dữ liệu trong hệ thống?',
    'Nên cấu hình kênh tiếp nhận ở đâu?',
  ],
  DISPATCHER: [
    'Làm sao chuyển yêu cầu thành phiếu công việc?',
    'Tôi phân công và xếp lịch kỹ thuật viên ở đâu?',
    'Theo dõi phiếu đang xử lý như thế nào?',
  ],
  CUSTOMER_SERVICE: [
    'Tôi tiếp nhận yêu cầu dịch vụ mới như thế nào?',
    'Khi nào nên tạo khách hàng và thiết bị trước?',
    'AI tiếp nhận trong form dùng ra sao?',
  ],
  TECHNICIAN: [
    'Tôi xem lịch làm việc của mình ở đâu?',
    'Tôi xem công việc được giao ở đâu?',
    'Cập nhật trạng thái và ghi chẩn đoán như thế nào?',
    'Tôi ghi phụ tùng đã dùng ở đâu?',
  ],
  WAREHOUSE_STAFF: [
    'Tôi nhập kho phụ tùng như thế nào?',
    'Làm sao biết phụ tùng sắp hết tồn?',
    'Khi kỹ thuật viên dùng phụ tùng thì theo dõi ở đâu?',
  ],
}

export function AiHelpAssistant() {
  const [open, setOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const { message } = App.useApp()
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const suggestions = useMemo(
    () => roleSuggestions[user?.role ?? ''] ?? ['Tôi nên bắt đầu sử dụng hệ thống từ đâu?'],
    [user?.role],
  )

  const help = useMutation({
    mutationFn: aiApi.help,
    onSuccess: (response, variables) => {
      setMessages((items) => [
        ...items,
        { id: `${Date.now()}-user`, role: 'user', content: variables.question },
        { id: `${Date.now()}-assistant`, role: 'assistant', content: response },
      ])
      setQuestion('')
    },
    onError: (error) => message.error(apiErrorMessage(error)),
  })

  const ask = (value = question) => {
    const normalized = value.trim()
    if (!normalized) {
      message.warning('Nhập câu hỏi về cách sử dụng hệ thống')
      return
    }
    help.mutate({ question: normalized, currentPath: location.pathname })
  }

  const openRoute = (route: string) => {
    setOpen(false)
    navigate(route)
  }

  return (
    <>
      <button
        type="button"
        className="ai-help-launcher"
        onClick={() => setOpen(true)}
        aria-label="Mở trợ lý hướng dẫn AI"
      >
        <span className="ai-help-mascot" aria-hidden="true">
          <span className="ai-help-mascot-antenna" />
          <span className="ai-help-mascot-face">
            <span className="ai-help-mascot-eye" />
            <span className="ai-help-mascot-eye" />
            <span className="ai-help-mascot-smile" />
          </span>
        </span>
        <span className="ai-help-launcher-copy">
          <span className="ai-help-launcher-title">Trợ lý AI</span>
          <span className="ai-help-launcher-subtitle">Hướng dẫn thao tác</span>
        </span>
      </button>

      <Drawer
        title="Trợ lý hướng dẫn"
        open={open}
        onClose={() => setOpen(false)}
        width={440}
        className="ai-help-drawer"
      >
        <div className="ai-help-intro">
          <Typography.Text strong>Hỏi cách dùng ServiceOps theo vai trò của bạn</Typography.Text>
          <Typography.Text type="secondary">
            Trợ lý này hướng dẫn quy trình thao tác, quyền hạn và trang cần mở. Dữ liệu chỉ thay đổi khi bạn tự bấm lưu/xác nhận.
          </Typography.Text>
        </div>

        <div className="ai-help-suggestions">
          {suggestions.map((item) => (
            <Button key={item} size="small" onClick={() => ask(item)}>
              {item}
            </Button>
          ))}
        </div>

        <div className="ai-help-thread">
          {messages.length ? messages.map((item) => (
            <div key={item.id} className={`ai-help-message ai-help-message-${item.role}`}>
              {item.role === 'user' ? (
                <Typography.Text>{item.content}</Typography.Text>
              ) : (
                <AssistantAnswer response={item.content} onOpenRoute={openRoute} />
              )}
            </div>
          )) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có câu hỏi hướng dẫn" />
          )}
        </div>

        <div className="ai-help-composer">
          <Input.TextArea
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Ví dụ: Tôi là nhân viên mới, làm sao tiếp nhận yêu cầu dịch vụ?"
            rows={3}
            maxLength={1000}
            onPressEnter={(event) => {
              if (!event.shiftKey) {
                event.preventDefault()
                ask()
              }
            }}
          />
          <Button type="primary" icon={<SendOutlined />} loading={help.isPending} onClick={() => ask()}>
            Hỏi trợ lý
          </Button>
        </div>
      </Drawer>
    </>
  )
}

function AssistantAnswer({ response, onOpenRoute }: { response: AiHelpResponse; onOpenRoute: (route: string) => void }) {
  return (
    <Space direction="vertical" size={10}>
      <Space size={8} wrap>
        <Typography.Text strong>Hướng dẫn</Typography.Text>
        <MetaBadge tone="info">{response.provider === 'gemini' ? 'Gemini' : 'Nội bộ'}</MetaBadge>
      </Space>
      <Typography.Text>{response.answer}</Typography.Text>
      <ol className="ai-help-steps">
        {response.steps.map((step) => <li key={step}>{step}</li>)}
      </ol>
      {response.relatedRoute && (
        <Button icon={<BulbOutlined />} onClick={() => onOpenRoute(response.relatedRoute)}>
          {response.actionLabel || 'Mở trang liên quan'}
        </Button>
      )}
    </Space>
  )
}
