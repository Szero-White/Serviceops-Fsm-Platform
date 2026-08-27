import { http } from '../../api/http'
import type { CompanyPaymentProfile, PageResponse, Payment, PaymentStatus, WorkOrder, WorkOrderBilling } from '../../types'

export const paymentsApi = {
  billing: (workOrderId: string) =>
    http.get<WorkOrderBilling>(`/work-orders/${workOrderId}/billing`).then((response) => response.data),
  updateBilling: (workOrderId: string, payload: { laborFee: number; incidentalFee: number; incidentalReason?: string }) =>
    http.put<WorkOrderBilling>(`/work-orders/${workOrderId}/billing`, payload).then((response) => response.data),
  customerAcceptance: (workOrderId: string, note?: string) =>
    http.post<WorkOrder>(`/work-orders/${workOrderId}/customer-acceptance`, { note }).then((response) => response.data),
  workOrderPayment: (workOrderId: string) =>
    http.get<Payment>(`/work-orders/${workOrderId}/payment`).then((response) => response.data),
  list: (params: { status?: PaymentStatus; search?: string; page?: number; size?: number }) =>
    http.get<PageResponse<Payment>>('/payments', { params }).then((response) => response.data),
  reportTransfer: (workOrderId: string, evidenceAttachmentId?: string) =>
    http.post<Payment>(`/work-orders/${workOrderId}/payment/report-transfer`, { evidenceAttachmentId }).then((response) => response.data),
  collectCash: (workOrderId: string) =>
    http.post<Payment>(`/work-orders/${workOrderId}/payment/collect-cash`).then((response) => response.data),
  settleTransfer: (paymentId: string) =>
    http.post<Payment>(`/payments/${paymentId}/settle-transfer`).then((response) => response.data),
  settleCash: (paymentId: string) =>
    http.post<Payment>(`/payments/${paymentId}/settle-cash`).then((response) => response.data),
  issueReceipt: (workOrderId: string) =>
    http.post<Blob>(`/work-orders/${workOrderId}/receipt`, undefined, { responseType: 'blob' }).then((response) => response.data),
  downloadReceipt: (workOrderId: string) =>
    http.get<Blob>(`/work-orders/${workOrderId}/receipt`, { responseType: 'blob' }).then((response) => response.data),
  companyProfile: () =>
    http.get<CompanyPaymentProfile | null>('/company-payment-profile').then((response) => response.data),
  updateCompanyProfile: (payload: { bankName: string; accountHolder: string; accountNumber: string; qrAttachmentId?: string }) =>
    http.put<CompanyPaymentProfile>('/company-payment-profile', payload).then((response) => response.data),
}
