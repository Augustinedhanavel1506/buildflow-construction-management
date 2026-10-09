import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type {
  AttendanceEntry,
  AttendanceSubmitPayload,
  LabourCostSummary,
  Worker,
  WorkerPayload,
} from "../types/labour";

export const labourService = {
  async listWorkers(projectId: number): Promise<Worker[]> {
    const { data } = await apiClient.get<ApiResponse<Worker[]>>(`/api/projects/${projectId}/labour/workers`);
    return data.data;
  },

  async createWorker(projectId: number, payload: WorkerPayload): Promise<Worker> {
    const { data } = await apiClient.post<ApiResponse<Worker>>(
      `/api/projects/${projectId}/labour/workers`,
      payload,
    );
    return data.data;
  },

  async updateWorker(id: number, payload: WorkerPayload): Promise<Worker> {
    const { data } = await apiClient.put<ApiResponse<Worker>>(`/api/labour/workers/${id}`, payload);
    return data.data;
  },

  async getAttendance(projectId: number, date: string): Promise<AttendanceEntry[]> {
    const { data } = await apiClient.get<ApiResponse<AttendanceEntry[]>>(
      `/api/projects/${projectId}/labour/attendance`,
      { params: { date } },
    );
    return data.data;
  },

  async submitAttendance(projectId: number, payload: AttendanceSubmitPayload): Promise<AttendanceEntry[]> {
    const { data } = await apiClient.post<ApiResponse<AttendanceEntry[]>>(
      `/api/projects/${projectId}/labour/attendance`,
      payload,
    );
    return data.data;
  },

  async getCostSummary(projectId: number): Promise<LabourCostSummary> {
    const { data } = await apiClient.get<ApiResponse<LabourCostSummary>>(
      `/api/projects/${projectId}/labour/cost`,
    );
    return data.data;
  },
};
