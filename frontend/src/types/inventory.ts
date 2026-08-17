export interface SparePart {
  id: string
  sku: string
  name: string
  unit: string
  stockQuantity: number
  reorderLevel: number
  unitPrice: number
  lowStock: boolean
  active: boolean
  updatedAt: string
}

export interface SparePartImportRowResult {
  rowNumber: number
  sku: string
  name: string
  valid: boolean
  message: string
}

export interface SparePartImportResult {
  totalRows: number
  validRows: number
  errorRows: number
  importedRows: number
  committed: boolean
  rows: SparePartImportRowResult[]
}
