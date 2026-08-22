export type AssetStatus = 'ACTIVE' | 'IN_SERVICE' | 'OUT_OF_SERVICE' | 'RETIRED'

export interface Asset {
  id: string
  customerId: string
  customerName: string
  category: string
  brand?: string
  model?: string
  serialNumber: string | null
  installedAt?: string
  warrantyUntil?: string
  underWarranty: boolean
  status: AssetStatus
  notes?: string
  createdAt: string
}

export interface AssetImportRowResult {
  rowNumber: number
  serialNumber: string
  customerCode: string
  valid: boolean
  message: string
}

export interface AssetImportResult {
  totalRows: number
  validRows: number
  errorRows: number
  importedRows: number
  committed: boolean
  rows: AssetImportRowResult[]
}
