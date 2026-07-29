import { http } from '../../api/http'
import type { ServiceRequestDraftSuggestion } from '../../types'

export const aiApi = {
  draftServiceRequest: (payload: { rawText: string; preferredChannel?: string }) =>
    http.post<ServiceRequestDraftSuggestion>('/ai/service-request-draft', payload).then((response) => response.data),
}
