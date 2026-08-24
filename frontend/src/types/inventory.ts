import type { UserRole } from './common'

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

export type InventoryTransactionType = 'IMPORT' | 'CONSUME' | 'RETURN' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT'

export interface InventoryTransaction {
  id: string
  type: InventoryTransactionType
  sparePartId: string
  sparePartSku: string
  sparePartName: string
  unit: string
  quantity: number
  balanceAfter: number
  workOrderId?: string
  workOrderCode?: string
  workOrderSummary?: string
  note?: string
  createdBy: string
  actorDisplayName?: string
  actorRole?: UserRole | 'SYSTEM'
  createdAt: string
}

export interface StocktakeResult {
  sparePart: SparePart
  systemQuantity: number
  actualQuantity: number
  difference: number
  adjustmentType?: InventoryTransactionType
}

export interface ReturnablePart {
  workOrderId: string
  workOrderCode: string
  sparePartId: string
  sparePartSku: string
  sparePartName: string
  unit: string
  returnableQuantity: number
}

export interface SparePartImportRowResult { rowNumber: number; sku: string; name: string; valid: boolean; message: string }
export interface SparePartImportResult { totalRows: number; validRows: number; errorRows: number; importedRows: number; committed: boolean; rows: SparePartImportRowResult[] }
