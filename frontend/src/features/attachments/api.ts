import { http } from '../../api/http'
import type { AttachmentItem } from '../../types'

export const attachmentsApi = {
  list: (referenceType: string, referenceId: string) =>
    http.get<AttachmentItem[]>('/attachments', { params: { referenceType, referenceId } }).then((response) => response.data),
  upload: (referenceType: string, referenceId: string, file: File) => {
    const form = new FormData()
    form.append('referenceType', referenceType)
    form.append('referenceId', referenceId)
    form.append('file', file)
    return http.post<AttachmentItem>('/attachments', form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((response) => response.data)
  },
  download: (id: string) => http.get<Blob>(`/attachments/${id}/download`, { responseType: 'blob' }).then((response) => response.data),
}
