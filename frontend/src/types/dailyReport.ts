export interface DailyReport {
  id: number;
  projectId: number;
  reportDate: string;
  workersPresent: number | null;
  workCompleted: string | null;
  materialsUsed: string | null;
  issues: string | null;
  notes: string | null;
  submittedById: number;
  submittedByName: string;
}

export interface DailyReportPayload {
  reportDate: string;
  workersPresent?: number;
  workCompleted?: string;
  materialsUsed?: string;
  issues?: string;
  notes?: string;
}
