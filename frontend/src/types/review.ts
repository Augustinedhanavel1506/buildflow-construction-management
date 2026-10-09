import type { BoqItem } from "./boq";
import type { HouseRequirement } from "./estimation";
import type { PlansResponse } from "./plan";

export type ReviewStatus = "REQUESTED" | "COMPLETED" | "CHANGES_REQUESTED" | "CANCELLED";

export interface ReviewDecision {
  boqItemId: number;
  itemName: string;
  originalQuantity: number;
  correctedQuantity: number | null;
  comment: string | null;
}

export interface Review {
  id: number;
  projectId: number;
  projectName: string;
  projectLocation: string | null;
  engineerId: number;
  engineerName: string;
  requestedByName: string;
  status: ReviewStatus;
  message: string | null;
  outcomeNote: string | null;
  createdAt: string;
  completedAt: string | null;
  decisions: ReviewDecision[];
  items: BoqItem[];
  plan: HouseRequirement | null;
  floorPlans: PlansResponse | null;
}

export interface ReviewSubmitPayload {
  outcome: "COMPLETE" | "REQUEST_CHANGES";
  overallNote?: string;
  approveRemaining?: boolean;
  lines?: { boqItemId: number; quantity?: number; comment?: string }[];
}

export interface Engineer {
  id: number;
  fullName: string;
  email: string;
  registrationNo: string | null;
  active: boolean;
}

export interface EngineerPayload {
  fullName: string;
  email: string;
  password: string;
  registrationNo?: string;
}
