import type { WorkOrderStatus } from './work-order'

export interface BillingItem {
  sparePartId: string
  sparePartSku: string
  sparePartName: string
  unit: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface WorkOrderBilling {
  workOrderId: string
  workOrderCode: string
  frozen: boolean
  items: BillingItem[]
  partsTotal: number
  laborFee: number
  incidentalFee: number
  incidentalReason?: string
  totalAmount: number
  acceptedByDisplayName?: string
  acceptedAt?: string
}

export type PaymentMethod = 'BANK_TRANSFER' | 'CASH'
export type PaymentStatus = 'UNPAID' | 'TRANSFER_PENDING_VERIFICATION' | 'CASH_PENDING_HANDOVER' | 'SETTLED'

export interface Payment {
  id: string
  workOrderId: string
  workOrderCode: string
  workOrderSummary: string
  workOrderStatus: WorkOrderStatus
  customerName?: string
  technicianName?: string
  amount: number
  method?: PaymentMethod
  status: PaymentStatus
  transferEvidenceAttachmentId?: string
  transferReportedAt?: string
  cashCollectedAt?: string
  collectedByDisplayName?: string
  settledAt?: string
  settledByDisplayName?: string
  updatedAt: string
}

export interface CompanyPaymentProfile {
  id: string
  tenantId: string
  bankName: string
  accountHolder: string
  accountNumber: string
  qrAttachmentId?: string
  updatedByDisplayName: string
  updatedAt: string
}
