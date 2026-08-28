export type AttachmentPurpose = 'GENERAL' | 'WORK_EVIDENCE' | 'PAYMENT_EVIDENCE'

export interface AttachmentItem {
  id: string
  originalFilename: string
  contentType: string
  fileSize: number
  referenceType: string
  referenceId: string
  uploadedBy: string
  purpose: AttachmentPurpose
  locked: boolean
  manageable: boolean
  createdAt: string
}
