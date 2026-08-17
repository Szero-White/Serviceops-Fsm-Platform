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

/**
 * Formats operational quantities for Vietnamese users without the ambiguous
 * period thousands separator. Grouped values use a narrow non-breaking space
 * (100 002), while decimal values keep the Vietnamese comma (12,5).
 */
export function formatQuantity(value?: number, maximumFractionDigits = 3) {
  const formatter = new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits,
    useGrouping: true,
  })

  return formatter
    .formatToParts(value ?? 0)
    .map((part) => part.type === 'group' ? '\u202f' : part.value)
    .join('')
}

export type NumericInputFormatterInfo = {
  userTyping: boolean
  input: string
}

/**
 * Keeps up to three-decimal business precision without padding whole values
 * with trailing zeroes when InputNumber commits on blur/Enter.
 *
 * Examples: 2.000 -> 2, 2.500 -> 2.5, 0.125 -> 0.125.
 */
export function formatCompactDecimalInput(
  value: string | number | undefined,
  info: NumericInputFormatterInfo,
) {
  if (info.userTyping) return info.input
  if (value === undefined || value === null || value === '') return ''

  return String(value).replace(/(\.\d*?[1-9])0+$|\.0+$/, '$1')
}

export function formatQuantityWithUnit(value?: number, unit?: string, maximumFractionDigits = 3) {
  const quantity = formatQuantity(value, maximumFractionDigits)
  const normalizedUnit = unit?.trim()
  return normalizedUnit ? `${quantity} ${normalizedUnit}` : quantity
}
