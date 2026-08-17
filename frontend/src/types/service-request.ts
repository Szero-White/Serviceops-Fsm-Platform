import type { Priority } from './common'

export type ServiceRequestStatus = 'OPEN' | 'CONVERTED' | 'CANCELLED'
export type RequestChannel = string

export interface ServiceRequest {
  id: string
  customerId: string
  customerName: string
  assetId?: string
  assetLabel?: string
  title: string
  description: string
  priority: Priority
  channel: RequestChannel
  status: ServiceRequestStatus
  createdBy: string
  createdAt: string
}

export interface ServiceRequestDraftSuggestion {
  title: string
  description: string
  priority: Priority
  channel: RequestChannel
  confidence: number
  reason: string
  provider: 'local' | 'gemini' | string
}

export interface ServiceChannel {
  id: string
  code: string
  name: string
  description?: string
  color: string
  sortOrder: number
  active: boolean
  systemDefined: boolean
  createdAt: string
  updatedAt: string
}
