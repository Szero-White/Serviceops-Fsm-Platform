import { http } from '../../api/http'
import type { AiHelpResponse, ServiceRequestDraftSuggestion } from '../../types'

export const aiApi = {
  draftServiceRequest: (payload: { rawText: string; preferredChannel?: string }) =>
    http.post<ServiceRequestDraftSuggestion>('/ai/service-request-draft', payload).then((response) => response.data),
  help: (payload: { question: string; currentPath?: string }) =>
    http.post<AiHelpResponse>('/ai/help', payload).then((response) => response.data),
}
