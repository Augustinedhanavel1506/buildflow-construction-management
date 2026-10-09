export type VariationType = "ADDITION" | "OMISSION";
export type VariationStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface VariationOrder {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  type: VariationType;
  amount: number;
  status: VariationStatus;
  requestedByName: string;
  decidedByName: string | null;
  createdAt: string;
}

export interface VariationOrderPayload {
  title: string;
  description?: string;
  type: VariationType;
  amount: number;
}
