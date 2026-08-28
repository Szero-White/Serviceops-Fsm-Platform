export interface AiHelpResponse {
  answer: string
  steps: string[]
  relatedRoute: string
  actionLabel: string
  provider: 'local' | 'gemini' | string
}
