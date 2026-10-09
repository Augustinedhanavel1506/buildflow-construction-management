export type MaterialStockStatus = "NORMAL" | "LOW" | "CRITICAL";

export interface Material {
  id: number;
  projectId: number;
  name: string;
  unit: string;
  currentStock: number;
  minimumStock: number;
  status: MaterialStockStatus;
}

export interface MaterialPayload {
  name: string;
  unit: string;
  minimumStock: number;
  openingStock?: number;
}

export interface MaterialPurchase {
  id: number;
  materialId: number;
  materialName: string;
  quantity: number;
  rate: number;
  amount: number;
  supplierName: string | null;
  purchaseDate: string;
  createdByName: string;
}

export interface MaterialPurchasePayload {
  quantity: number;
  rate: number;
  supplierName?: string;
  purchaseDate: string;
}

export interface MaterialUsage {
  id: number;
  materialId: number;
  materialName: string;
  quantity: number;
  usageDate: string;
  notes: string | null;
  createdByName: string;
}

export interface MaterialUsagePayload {
  quantity: number;
  usageDate: string;
  notes?: string;
}

export type MaterialRequisitionStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface MaterialRequisition {
  id: number;
  projectId: number;
  materialId: number;
  materialName: string;
  quantity: number;
  requiredDate: string | null;
  reason: string | null;
  status: MaterialRequisitionStatus;
  requestedByName: string;
  decidedByName: string | null;
}

export interface MaterialRequisitionPayload {
  materialId: number;
  quantity: number;
  requiredDate?: string;
  reason?: string;
}
