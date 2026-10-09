import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { DailyReport, DailyReportPayload } from "../types/dailyReport";

export const dailyReportService = {
  async list(projectId: number): Promise<DailyReport[]> {
    const { data } = await apiClient.get<ApiResponse<DailyReport[]>>(
      `/api/projects/${projectId}/daily-reports`,
    );
    return data.data;
  },

  async create(projectId: number, payload: DailyReportPayload): Promise<DailyReport> {
    const { data } = await apiClient.post<ApiResponse<DailyReport>>(
      `/api/projects/${projectId}/daily-reports`,
      payload,
    );
    return data.data;
  },

  async update(id: number, payload: DailyReportPayload): Promise<DailyReport> {
    const { data } = await apiClient.put<ApiResponse<DailyReport>>(`/api/daily-reports/${id}`, payload);
    return data.data;
  },
};
