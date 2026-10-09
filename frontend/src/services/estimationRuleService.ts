import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { BoqGenerationRule, BoqGenerationRulePayload } from "../types/estimationRule";

export const estimationRuleService = {
  async list(): Promise<BoqGenerationRule[]> {
    const { data } = await apiClient.get<ApiResponse<BoqGenerationRule[]>>("/api/boq-generation-rules");
    return data.data;
  },

  async update(id: number, payload: BoqGenerationRulePayload): Promise<BoqGenerationRule> {
    const { data } = await apiClient.put<ApiResponse<BoqGenerationRule>>(`/api/boq-generation-rules/${id}`, payload);
    return data.data;
  },

  async revert(id: number): Promise<void> {
    await apiClient.delete(`/api/boq-generation-rules/${id}`);
  },
};
