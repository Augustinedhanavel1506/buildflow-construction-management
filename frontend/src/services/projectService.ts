import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { Page, Project, ProjectPayload, ProjectStatus } from "../types/project";

export const projectService = {
  async list(page = 0, size = 20): Promise<Page<Project>> {
    const { data } = await apiClient.get<ApiResponse<Page<Project>>>("/api/projects", {
      params: { page, size },
    });
    return data.data;
  },

  async get(id: number): Promise<Project> {
    const { data } = await apiClient.get<ApiResponse<Project>>(`/api/projects/${id}`);
    return data.data;
  },

  async create(payload: ProjectPayload): Promise<Project> {
    const { data } = await apiClient.post<ApiResponse<Project>>("/api/projects", payload);
    return data.data;
  },

  async update(id: number, payload: ProjectPayload): Promise<Project> {
    const { data } = await apiClient.put<ApiResponse<Project>>(`/api/projects/${id}`, payload);
    return data.data;
  },

  async updateStatus(id: number, status: ProjectStatus): Promise<Project> {
    const { data } = await apiClient.patch<ApiResponse<Project>>(
      `/api/projects/${id}/status`,
      null,
      { params: { status } },
    );
    return data.data;
  },

  async archive(id: number): Promise<void> {
    await apiClient.delete(`/api/projects/${id}`);
  },
};
