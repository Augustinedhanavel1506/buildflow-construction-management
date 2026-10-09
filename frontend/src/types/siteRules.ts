export interface SiteRules {
  setbackFrontFt: number | null;
  setbackRearFt: number | null;
  setbackLeftFt: number | null;
  setbackRightFt: number | null;
  maxCoveragePercent: number | null;
  maxFar: number | null;
}

export type SiteStatus = "OK" | "EXCEEDS" | "NOT_SET";

export interface SiteMeasure {
  value: number;
  limit: number | null;
  status: SiteStatus;
}

export interface SiteCheck {
  rules: SiteRules;
  setbacks: {
    frontFt: number;
    rearFt: number;
    leftFt: number;
    rightFt: number;
    frontAssumed: boolean;
    rearAssumed: boolean;
    leftAssumed: boolean;
    rightAssumed: boolean;
  };
  buildableEnvelope: { x: number; y: number; widthFt: number; depthFt: number };
  plotAreaSqft: number;
  groundAreaSqft: number;
  totalAreaSqft: number;
  coverage: SiteMeasure;
  far: SiteMeasure;
  fullyDrawn: boolean;
  violations: { floorLevel: number; roomName: string; message: string }[];
  compliant: boolean;
  notes: string[];
}
