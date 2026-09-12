export type AiResponseSource = 'GEMINI' | 'LOCAL'

export interface AiHelpResponse {
  answer: string
  steps: string[]
  relatedRoute: string
  actionLabel: string
  source: AiResponseSource
}
