export type BoqCategory = "MATERIAL" | "LABOUR" | "EQUIPMENT" | "TRANSPORT" | "OTHER";

export type EstimateSource = "SYSTEM_PRELIMINARY" | "ENGINEER_VALIDATED" | "MANUAL";

export interface BoqItem {
  id: number;
  projectId: number;
  itemName: string;
  category: BoqCategory;
  unit: string;
  quantity: number;
  quantityLow: number | null;
  quantityHigh: number | null;
  rate: number;
  estimatedAmount: number;
  actualAmount: number;
  variance: number;
  estimateSource: EstimateSource;
  sourceRuleCode: string | null;
  component: string | null;
  validatedBy: string | null;
  validatedAt: string | null;
  validationNote: string | null;
}

export interface BoqValidationPayload {
  engineerName: string;
  quantity?: number;
  note?: string;
}

export interface BoqItemPayload {
  itemName: string;
  category: BoqCategory;
  unit: string;
  quantity: number;
  rate: number;
  actualAmount?: number;
}
