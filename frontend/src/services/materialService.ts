import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type {
  Material,
  MaterialPayload,
  MaterialPurchase,
  MaterialPurchasePayload,
  MaterialRequisition,
  MaterialRequisitionPayload,
  MaterialUsage,
  MaterialUsagePayload,
} from "../types/material";

export const materialService = {
  async list(projectId: number): Promise<Material[]> {
    const { data } = await apiClient.get<ApiResponse<Material[]>>(`/api/projects/${projectId}/materials`);
    return data.data;
  },

  async create(projectId: number, payload: MaterialPayload): Promise<Material> {
    const { data } = await apiClient.post<ApiResponse<Material>>(
      `/api/projects/${projectId}/materials`,
      payload,
    );
    return data.data;
  },

  async update(id: number, payload: MaterialPayload): Promise<Material> {
    const { data } = await apiClient.put<ApiResponse<Material>>(`/api/materials/${id}`, payload);
    return data.data;
  },

  async listPurchases(projectId: number): Promise<MaterialPurchase[]> {
    const { data } = await apiClient.get<ApiResponse<MaterialPurchase[]>>(
      `/api/projects/${projectId}/materials/purchases`,
    );
    return data.data;
  },

  async createPurchase(materialId: number, payload: MaterialPurchasePayload): Promise<MaterialPurchase> {
    const { data } = await apiClient.post<ApiResponse<MaterialPurchase>>(
      `/api/materials/${materialId}/purchases`,
      payload,
    );
    return data.data;
  },

  async listUsage(projectId: number): Promise<MaterialUsage[]> {
    const { data } = await apiClient.get<ApiResponse<MaterialUsage[]>>(
      `/api/projects/${projectId}/materials/usage`,
    );
    return data.data;
  },

  async createUsage(materialId: number, payload: MaterialUsagePayload): Promise<MaterialUsage> {
    const { data } = await apiClient.post<ApiResponse<MaterialUsage>>(
      `/api/materials/${materialId}/usage`,
      payload,
    );
    return data.data;
  },

  async listRequisitions(projectId: number): Promise<MaterialRequisition[]> {
    const { data } = await apiClient.get<ApiResponse<MaterialRequisition[]>>(
      `/api/projects/${projectId}/materials/requests`,
    );
    return data.data;
  },

  async createRequisition(
    projectId: number,
    payload: MaterialRequisitionPayload,
  ): Promise<MaterialRequisition> {
    const { data } = await apiClient.post<ApiResponse<MaterialRequisition>>(
      `/api/projects/${projectId}/materials/requests`,
      payload,
    );
    return data.data;
  },

  async approveRequisition(id: number): Promise<MaterialRequisition> {
    const { data } = await apiClient.patch<ApiResponse<MaterialRequisition>>(
      `/api/materials/requests/${id}/approve`,
    );
    return data.data;
  },

  async rejectRequisition(id: number): Promise<MaterialRequisition> {
    const { data } = await apiClient.patch<ApiResponse<MaterialRequisition>>(
      `/api/materials/requests/${id}/reject`,
    );
    return data.data;
  },
};
