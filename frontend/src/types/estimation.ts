import type { BoqItem } from "./boq";

export type ConstructionGrade = "ECONOMY" | "STANDARD" | "PREMIUM";
export type StructureType = "RCC_FRAMED" | "LOAD_BEARING" | "STEEL_STRUCTURE";
export type WallMaterial = "RED_BRICK" | "AAC_BLOCK" | "SOLID_BLOCK";
export type RoofType = "RCC_SLAB" | "OTHER";
export type HouseRequirementStatus = "DRAFT" | "ESTIMATED";

export interface FloorRequirement {
  id: number;
  floorLevel: number;
  floorAreaSqft: number;
  bedroomCount: number;
  bathroomCount: number;
  hasKitchen: boolean;
  hasHall: boolean;
  hasBalcony: boolean;
  hasPoojaRoom: boolean;
  doorCount: number;
  windowCount: number;
}

export interface FloorRequirementPayload {
  floorLevel: number;
  floorAreaSqft: number;
  bedroomCount?: number;
  bathroomCount?: number;
  hasKitchen?: boolean;
  hasHall?: boolean;
  hasBalcony?: boolean;
  hasPoojaRoom?: boolean;
  doorCount?: number;
  windowCount?: number;
}

export interface HouseRequirement {
  id: number;
  projectId: number;
  label: string;
  location: string | null;
  district: string | null;
  plotWidthFt: number;
  plotLengthFt: number;
  constructionGrade: ConstructionGrade;
  structureType: StructureType;
  wallMaterial: WallMaterial;
  roofType: RoofType;
  status: HouseRequirementStatus;
  floors: FloorRequirement[];
  createdAt: string;
}

export interface HouseRequirementPayload {
  label: string;
  location?: string;
  district?: string;
  plotWidthFt: number;
  plotLengthFt: number;
  constructionGrade: ConstructionGrade;
  structureType?: StructureType;
  wallMaterial?: WallMaterial;
  roofType?: RoofType;
  floors: FloorRequirementPayload[];
}

export interface BoqGenerationResult {
  houseRequirementId: number;
  projectId: number;
  totalBuiltupAreaSqft: number;
  estimatedCost: number;
  estimatedCostLow: number;
  estimatedCostHigh: number;
  items: BoqItem[];
  unpricedItems: string[];
  quantityBasis: "DRAWN_PLAN" | "AREA_RULES";
  structuralBasis: "DRAWN_STRUCTURE" | "AREA_RULES";
}
