export type SortDirection = 'asc' | 'desc'
export type TableSortState = {
  sortBy: string
  sortDir: SortDirection
}

type AntSorterLike = {
  field?: string | number
  columnKey?: string | number
  order?: 'ascend' | 'descend' | null
}

export function resolveTableSort(
  sorter: AntSorterLike | AntSorterLike[],
  fallback: TableSortState,
): TableSortState {
  const active = Array.isArray(sorter) ? sorter.find((item) => item.order) : sorter
  if (!active?.order) return fallback
  const sortBy = String(active.columnKey ?? active.field ?? fallback.sortBy)
  return { sortBy, sortDir: active.order === 'ascend' ? 'asc' : 'desc' }
}

export function serverSortable(_sort: TableSortState, field: string) {
  return {
    key: field,
    sorter: true as const,
  }
}

export function compareText(left?: string | null, right?: string | null) {
  return (left ?? '').localeCompare(right ?? '', 'vi', { sensitivity: 'base', numeric: true })
}

export function compareNumber(left?: number | null, right?: number | null) {
  return Number(left ?? 0) - Number(right ?? 0)
}

export function compareDate(left?: string | null, right?: string | null) {
  return new Date(left ?? 0).getTime() - new Date(right ?? 0).getTime()
}
