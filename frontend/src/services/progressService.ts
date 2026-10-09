import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { ProgressStage, ProgressStagePayload } from "../types/progress";

export const progressService = {
  async list(projectId: number): Promise<ProgressStage[]> {
    const { data } = await apiClient.get<ApiResponse<ProgressStage[]>>(`/api/projects/${projectId}/progress`);
    return data.data;
  },

  async create(projectId: number, payload: ProgressStagePayload): Promise<ProgressStage> {
    const { data } = await apiClient.post<ApiResponse<ProgressStage>>(
      `/api/projects/${projectId}/progress`,
      payload,
    );
    return data.data;
  },

  async updatePercent(id: number, percentComplete: number): Promise<ProgressStage> {
    const { data } = await apiClient.patch<ApiResponse<ProgressStage>>(`/api/progress/${id}`, {
      percentComplete,
    });
    return data.data;
  },
};
