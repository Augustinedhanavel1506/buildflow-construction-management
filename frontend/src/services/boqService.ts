import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { BoqItem, BoqItemPayload, BoqValidationPayload } from "../types/boq";

export const boqService = {
  async list(projectId: number): Promise<BoqItem[]> {
    const { data } = await apiClient.get<ApiResponse<BoqItem[]>>(`/api/projects/${projectId}/boq`);
    return data.data;
  },

  async create(projectId: number, payload: BoqItemPayload): Promise<BoqItem> {
    const { data } = await apiClient.post<ApiResponse<BoqItem>>(
      `/api/projects/${projectId}/boq/items`,
      payload,
    );
    return data.data;
  },

  async update(id: number, payload: BoqItemPayload): Promise<BoqItem> {
    const { data } = await apiClient.put<ApiResponse<BoqItem>>(`/api/boq/items/${id}`, payload);
    return data.data;
  },

  async validate(id: number, payload: BoqValidationPayload): Promise<BoqItem> {
    const { data } = await apiClient.post<ApiResponse<BoqItem>>(`/api/boq/items/${id}/validate`, payload);
    return data.data;
  },

  async validateAll(projectId: number, payload: BoqValidationPayload): Promise<BoqItem[]> {
    const { data } = await apiClient.post<ApiResponse<BoqItem[]>>(`/api/projects/${projectId}/boq/validate`, payload);
    return data.data;
  },

  async remove(id: number): Promise<void> {
    await apiClient.delete(`/api/boq/items/${id}`);
  },
};
