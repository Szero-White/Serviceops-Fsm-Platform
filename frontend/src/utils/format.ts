import dayjs from 'dayjs'

export const EMPTY_VALUE = '—'

export function formatDateTime(value?: string) {
  return value ? dayjs(value).format('DD/MM/YYYY HH:mm') : EMPTY_VALUE
}

export function formatDate(value?: string) {
  return value ? dayjs(value).format('DD/MM/YYYY') : EMPTY_VALUE
}

export function formatCurrency(value?: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value ?? 0)
}

export function formatNumber(value?: number, maximumFractionDigits = 3) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits }).format(value ?? 0)
}
