export type MixGrade = "M15" | "M20" | "M25";
export type MemberType = "FOOTING" | "COLUMN" | "BEAM" | "SLAB" | "OTHER";
export type UnitType = "RED_BRICK" | "AAC_BLOCK" | "SOLID_BLOCK";

export interface ConcretePayload {
  grade: MixGrade;
  wastagePercent?: number;
  members: {
    name: string;
    type: MemberType;
    count: number;
    lengthM: number;
    widthM: number;
    depthM: number;
    steelPercent?: number;
  }[];
}

export interface ConcreteResult {
  grade: string;
  mixRatio: string;
  members: { name: string; type: string; count: number; volumeM3: number; steelPercent: number; steelKg: number }[];
  concreteM3: number;
  cementBags: number;
  sandM3: number;
  aggregateM3: number;
  steelKg: number;
  wastagePercent: number;
  assumptions: string[];
}

export interface MasonryPayload {
  unitType: UnitType;
  mortarSandParts?: number;
  wastagePercent?: number;
  walls: { name: string; lengthM: number; heightM: number; thicknessM: number; openingsSqm?: number }[];
}

export interface MasonryResult {
  unitType: string;
  mortarSandParts: number;
  walls: { name: string; grossAreaSqm: number; netAreaSqm: number; netVolumeM3: number; units: number }[];
  units: number;
  mortarDryM3: number;
  cementBags: number;
  sandM3: number;
  wastagePercent: number;
  assumptions: string[];
}

export interface SteelPayload {
  wastagePercent?: number;
  standardBarLengthM?: number;
  bars: { mark: string; diameterMm: number; lengthM: number; nos: number }[];
}

export interface SteelResult {
  bars: { mark: string; diameterMm: number; totalLengthM: number; weightKg: number }[];
  byDiameter: { diameterMm: number; totalLengthM: number; weightKg: number; standardBarsRequired: number }[];
  totalKg: number;
  totalKgWithWastage: number;
  wastagePercent: number;
  assumptions: string[];
}
