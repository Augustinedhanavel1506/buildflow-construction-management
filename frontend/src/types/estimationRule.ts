export type RuleSourceType = "GOVT_SOR" | "GOVT_DSR" | "COMPLETED_PROJECT_AVERAGE" | "VENDOR_QUOTE" | "PLACEHOLDER";
export type ConfidenceLevel = "HIGH" | "MEDIUM" | "LOW";

export interface BoqGenerationRule {
  id: number;
  ruleCode: string;
  baseRuleCode: string;
  component: string;
  constructionGrade: string;
  itemName: string;
  boqCategory: string;
  unit: string;
  basis: string;
  coefficient: number;
  wastagePercent: number;
  minCoefficient: number | null;
  maxCoefficient: number | null;
  sourceType: RuleSourceType;
  sourceReference: string;
  confidenceLevel: ConfidenceLevel;
  verified: boolean;
  sampleSize: number | null;
  version: number;
  active: boolean;
  notes: string | null;
  scope: "PLATFORM" | "BUSINESS";
}

export interface BoqGenerationRulePayload {
  coefficient: number;
  wastagePercent: number;
  minCoefficient?: number;
  maxCoefficient?: number;
  sourceType: RuleSourceType;
  sourceReference: string;
  confidenceLevel: ConfidenceLevel;
  verified: boolean;
  sampleSize?: number;
  notes?: string;
  active: boolean;
}
