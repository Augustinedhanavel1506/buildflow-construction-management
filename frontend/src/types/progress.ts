export interface ProgressStage {
  id: number;
  projectId: number;
  stageName: string;
  percentComplete: number;
  sortOrder: number;
}

export interface ProgressStagePayload {
  stageName: string;
  percentComplete: number;
}
