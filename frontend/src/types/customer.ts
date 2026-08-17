export interface Customer {
  id: string
  code: string
  name: string
  phone?: string
  email?: string
  address?: string
  notes?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CustomerImportRowResult {
  rowNumber: number
  code: string
  name: string
  valid: boolean
  message: string
}

export interface CustomerImportResult {
  totalRows: number
  validRows: number
  errorRows: number
  importedRows: number
  committed: boolean
  rows: CustomerImportRowResult[]
}
