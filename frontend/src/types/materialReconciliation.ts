export interface MaterialStockCount {
  id: number;
  materialId: number;
  materialName: string;
  countDate: string;
  systemStock: number;
  countedStock: number;
  variance: number;
  notes: string | null;
  countedByName: string;
}

export interface MaterialStockCountPayload {
  countDate: string;
  countedStock: number;
  notes?: string;
}

export interface MaterialReconciliationRow {
  materialId: number;
  materialName: string;
  unit: string;
  currentStock: number;
  totalPurchased: number;
  totalUsed: number;
  averageRate: number;
  lastCountDate: string | null;
  lastCountVariance: number | null;
  cumulativeVarianceQty: number;
  estimatedVarianceValue: number;
}

export interface MaterialReconciliationReport {
  materials: MaterialReconciliationRow[];
  totalEstimatedVarianceValue: number;
}
