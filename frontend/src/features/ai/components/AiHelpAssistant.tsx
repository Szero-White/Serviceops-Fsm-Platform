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
    'Với quyền Chủ sở hữu, tôi có thể quản lý những chức năng nào?',
    'Làm sao lọc tài khoản đang hoạt động và tạm ngưng?',
    'Tôi điều phối lại kỹ thuật viên hoặc lịch trước khi công việc bắt đầu như thế nào?',
    'Làm sao kiểm tra audit và các thông báo quan trọng?',
  ],
  DISPATCHER: [
    'Tôi mới làm Điều phối viên, trong vai trò này tôi được làm những gì?',
    'Tôi phân công và xếp lịch kỹ thuật viên ở đâu?',
    'Nếu kỹ thuật viên chưa bắt đầu nhưng không thể đáp ứng, tôi điều phối lại thế nào?',
    'Theo dõi phiếu đang xử lý như thế nào?',
  ],
  CUSTOMER_SERVICE: [
    'Tôi mới làm CSKH, trong vai trò này tôi được làm những gì?',
    'Tôi tiếp nhận yêu cầu và chuyển sang điều phối như thế nào?',
    'Khi kỹ thuật viên hoàn thành nhưng khách báo còn lỗi, tôi xử lý phản hồi thế nào?',
    'Khi nào nên tạo khách hàng và thiết bị trước?',
    'AI tiếp nhận trong form dùng ra sao?',
  ],
  TECHNICIAN: [
    'Tôi mới làm Kỹ thuật viên, trong vai trò này tôi được làm những gì?',
    'Tôi nên bắt đầu ca làm việc từ đâu?',
    'Tôi xem công việc được giao ở đâu?',
    'Cập nhật trạng thái và ghi chẩn đoán như thế nào?',
    'Tôi ghi phụ tùng đã dùng ở đâu?',
  ],
  WAREHOUSE_STAFF: [
    'Tôi mới làm kho, trong vai trò này tôi được làm những gì?',
    'Tôi nên bắt đầu từ đâu?',
    'Tôi kiểm kê tồn thực tế và xử lý chênh lệch như thế nào?',
    'Tôi chỉnh ngưỡng tồn tối thiểu ở đâu và khi nào có cảnh báo?',
    'Kiểm kê bị lệch tồn thì ai sẽ nhận thông báo?',
    'Tôi xem lịch sử nhập, sử dụng, hoàn trả và điều chỉnh kho ở đâu?',
    'Kỹ thuật viên không dùng hết phụ tùng thì tôi hoàn trả theo Work Order như thế nào?',
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

  const suggestions = useMemo(() => {
    const roleItems = roleSuggestions[user?.role ?? ''] ?? ['Tôi nên bắt đầu sử dụng hệ thống từ đâu?']
    return [...roleItems, 'Tôi bấm Lưu/Hoàn thành nhưng hệ thống chưa thực hiện, cần kiểm tra gì?', 'Tôi lỡ đánh dấu thông báo đã đọc, làm sao chuyển lại chưa đọc?']
  }, [user?.role])

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
          <span className="ai-help-launcher-subtitle">Hướng dẫn theo vai trò</span>
        </span>
      </button>

      <Drawer
        title="Trợ lý AI ServiceOps"
        open={open}
        onClose={() => setOpen(false)}
        width={440}
        className="ai-help-drawer"
      >
        <div className="ai-help-intro">
          <Typography.Text strong>Hỏi cách dùng ServiceOps theo vai trò của bạn</Typography.Text>
          <Typography.Text type="secondary">
            Trợ lý giải thích quy trình và chức năng theo vai trò của bạn. Trợ lý không tự đọc dữ liệu nghiệp vụ trong database và không tự thay đổi dữ liệu.
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
