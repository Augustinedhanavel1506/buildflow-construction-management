import type { BoqCategory } from "./boq";

export interface RateMasterItem {
  id: number;
  itemName: string;
  category: BoqCategory;
  unit: string;
  standardRate: number;
  minRate: number | null;
  maxRate: number | null;
  consumptionPerSqft: number | null;
  notes: string | null;
  active: boolean;
  district: string | null;
}

export interface RateMasterItemPayload {
  itemName: string;
  category: BoqCategory;
  unit: string;
  standardRate: number;
  minRate?: number;
  maxRate?: number;
  consumptionPerSqft?: number;
  notes?: string;
  active?: boolean;
  district?: string;
}
