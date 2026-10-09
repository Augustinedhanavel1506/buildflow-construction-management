import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { Business, BusinessPayload } from "../types/business";

export const businessService = {
  async get(): Promise<Business> {
    const { data } = await apiClient.get<ApiResponse<Business>>("/api/business");
    return data.data;
  },

  async update(payload: BusinessPayload): Promise<Business> {
    const { data } = await apiClient.put<ApiResponse<Business>>("/api/business", payload);
    return data.data;
  },
};
