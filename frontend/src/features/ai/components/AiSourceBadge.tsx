import { MetaBadge } from '../../../components/PresentationBadge'
import type { AiResponseSource } from '../../../types'

const SOURCE_LABELS: Record<AiResponseSource, string> = {
  GEMINI: 'Gemini',
  LOCAL: 'Nội bộ',
}

export function AiSourceBadge({ source }: { source?: AiResponseSource }) {
  if (!source) {
    return <MetaBadge tone="info">Sẵn sàng</MetaBadge>
  }

  return (
    <MetaBadge tone={source === 'GEMINI' ? 'success' : 'neutral'}>
      {SOURCE_LABELS[source]}
    </MetaBadge>
  )
}
