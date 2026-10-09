import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type {
  MaterialReconciliationReport,
  MaterialStockCount,
  MaterialStockCountPayload,
} from "../types/materialReconciliation";

export const materialReconciliationService = {
  async getReport(projectId: number): Promise<MaterialReconciliationReport> {
    const { data } = await apiClient.get<ApiResponse<MaterialReconciliationReport>>(
      `/api/projects/${projectId}/materials/reconciliation`,
    );
    return data.data;
  },

  async listStockCounts(materialId: number): Promise<MaterialStockCount[]> {
    const { data } = await apiClient.get<ApiResponse<MaterialStockCount[]>>(
      `/api/materials/${materialId}/stock-counts`,
    );
    return data.data;
  },

  async createStockCount(materialId: number, payload: MaterialStockCountPayload): Promise<MaterialStockCount> {
    const { data } = await apiClient.post<ApiResponse<MaterialStockCount>>(
      `/api/materials/${materialId}/stock-counts`,
      payload,
    );
    return data.data;
  },
};
