export type ProjectStatus = "PLANNING" | "ACTIVE" | "ON_HOLD" | "COMPLETED" | "CANCELLED";

export interface Project {
  id: number;
  name: string;
  clientName: string | null;
  clientGstin: string | null;
  clientAddress: string | null;
  location: string | null;
  contractValue: number;
  revisedContractValue: number;
  estimatedCost: number;
  actualCost: number;
  overallProgress: number;
  startDate: string | null;
  expectedEndDate: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectPayload {
  name: string;
  clientName?: string;
  clientGstin?: string;
  clientAddress?: string;
  location?: string;
  contractValue: number;
  estimatedCost?: number;
  startDate?: string;
  expectedEndDate?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
