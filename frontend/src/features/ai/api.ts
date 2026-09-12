import { http } from '../../api/http'
import type { AiHelpResponse, ServiceRequestDraftSuggestion } from '../../types'

const AI_DRAFT_TIMEOUT_MS = 22_000
const AI_HELP_TIMEOUT_MS = 30_000

export const aiApi = {
  draftServiceRequest: (payload: { rawText: string }) =>
    http.post<ServiceRequestDraftSuggestion>('/ai/service-request-draft', payload, { timeout: AI_DRAFT_TIMEOUT_MS })
      .then((response) => response.data),
  help: (payload: { question: string; currentPath?: string }) =>
    http.post<AiHelpResponse>('/ai/help', payload, { timeout: AI_HELP_TIMEOUT_MS })
      .then((response) => response.data),
}
