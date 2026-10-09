import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { RateMasterItem, RateMasterItemPayload } from "../types/rateMaster";

export const rateMasterService = {
  async list(includeInactive = false): Promise<RateMasterItem[]> {
    const { data } = await apiClient.get<ApiResponse<RateMasterItem[]>>("/api/rate-master", {
      params: { includeInactive },
    });
    return data.data;
  },

  async create(payload: RateMasterItemPayload): Promise<RateMasterItem> {
    const { data } = await apiClient.post<ApiResponse<RateMasterItem>>("/api/rate-master", payload);
    return data.data;
  },

  async addStarterRates(): Promise<{ added: number; alreadyPresent: number }> {
    const { data } = await apiClient.post<ApiResponse<{ added: number; alreadyPresent: number }>>("/api/rate-master/starter");
    return data.data;
  },

  async copyToDistrict(payload: { fromDistrict?: string; toDistrict: string; adjustPercent?: number }): Promise<{ added: number; skipped: number }> {
    const { data } = await apiClient.post<ApiResponse<{ added: number; skipped: number }>>("/api/rate-master/copy-district", payload);
    return data.data;
  },

  async update(id: number, payload: RateMasterItemPayload): Promise<RateMasterItem> {
    const { data } = await apiClient.put<ApiResponse<RateMasterItem>>(`/api/rate-master/${id}`, payload);
    return data.data;
  },
};
