import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { VariationOrder, VariationOrderPayload } from "../types/variation";

export const variationService = {
  async list(projectId: number): Promise<VariationOrder[]> {
    const { data } = await apiClient.get<ApiResponse<VariationOrder[]>>(
      `/api/projects/${projectId}/variations`,
    );
    return data.data;
  },

  async create(projectId: number, payload: VariationOrderPayload): Promise<VariationOrder> {
    const { data } = await apiClient.post<ApiResponse<VariationOrder>>(
      `/api/projects/${projectId}/variations`,
      payload,
    );
    return data.data;
  },

  async approve(id: number): Promise<VariationOrder> {
    const { data } = await apiClient.patch<ApiResponse<VariationOrder>>(`/api/variations/${id}/approve`);
    return data.data;
  },

  async reject(id: number): Promise<VariationOrder> {
    const { data } = await apiClient.patch<ApiResponse<VariationOrder>>(`/api/variations/${id}/reject`);
    return data.data;
  },
};
