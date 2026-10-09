export interface Worker {
  id: number;
  projectId: number;
  name: string;
  role: string;
  dailyRate: number;
  active: boolean;
}

export interface WorkerPayload {
  name: string;
  role: string;
  dailyRate: number;
  active?: boolean;
}

export interface AttendanceEntry {
  workerId: number;
  workerName: string;
  role: string;
  dailyRate: number;
  date: string;
  present: boolean;
}

export interface AttendanceSubmitPayload {
  date: string;
  entries: { workerId: number; present: boolean }[];
}

export interface LabourCostSummary {
  totalCost: number;
  byRole: { role: string; presentDays: number; cost: number }[];
}
