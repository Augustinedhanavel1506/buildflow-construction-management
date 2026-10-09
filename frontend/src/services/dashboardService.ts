import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { DashboardSummary } from "../types/dashboard";

export const dashboardService = {
  async getSummary(): Promise<DashboardSummary> {
    const { data } = await apiClient.get<ApiResponse<DashboardSummary>>("/api/dashboard/summary");
    return data.data;
  },
};
