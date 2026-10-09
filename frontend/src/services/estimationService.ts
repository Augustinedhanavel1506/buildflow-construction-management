import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { BoqGenerationResult, HouseRequirement, HouseRequirementPayload } from "../types/estimation";
import type { FloorPlan, Layout, PlanColumn, PlansResponse } from "../types/plan";
import type { SiteCheck, SiteRules } from "../types/siteRules";
import type { WhatIfRequest, WhatIfResult } from "../types/whatIf";

export const estimationService = {
  async list(): Promise<HouseRequirement[]> {
    const { data } = await apiClient.get<ApiResponse<HouseRequirement[]>>("/api/house-requirements");
    return data.data;
  },

  async get(id: number): Promise<HouseRequirement> {
    const { data } = await apiClient.get<ApiResponse<HouseRequirement>>(`/api/house-requirements/${id}`);
    return data.data;
  },

  async create(payload: HouseRequirementPayload): Promise<HouseRequirement> {
    const { data } = await apiClient.post<ApiResponse<HouseRequirement>>("/api/house-requirements", payload);
    return data.data;
  },

  async update(id: number, payload: HouseRequirementPayload): Promise<HouseRequirement> {
    const { data } = await apiClient.put<ApiResponse<HouseRequirement>>(`/api/house-requirements/${id}`, payload);
    return data.data;
  },

  async getPlans(id: number): Promise<PlansResponse> {
    const { data } = await apiClient.get<ApiResponse<PlansResponse>>(`/api/house-requirements/${id}/plans`);
    return data.data;
  },

  async savePlan(id: number, floorLevel: number, layout: Layout): Promise<FloorPlan> {
    const { data } = await apiClient.put<ApiResponse<FloorPlan>>(`/api/house-requirements/${id}/plans/${floorLevel}`, layout);
    return data.data;
  },

  async suggestPlans(id: number): Promise<PlansResponse> {
    const { data } = await apiClient.post<ApiResponse<PlansResponse>>(`/api/house-requirements/${id}/plans/suggest`);
    return data.data;
  },

  async suggestColumns(id: number, layout: Layout, style: "grid" | "junctions" = "grid"): Promise<PlanColumn[]> {
    const { data } = await apiClient.post<ApiResponse<PlanColumn[]>>(
      `/api/house-requirements/${id}/plans/columns/suggest?style=${style}`, layout);
    return data.data;
  },

  async getHouseStyle(id: number): Promise<Record<string, unknown>> {
    const { data } = await apiClient.get<ApiResponse<Record<string, unknown>>>(`/api/house-requirements/${id}/house-style`);
    return data.data;
  },

  async saveHouseStyle(id: number, style: object): Promise<void> {
    await apiClient.put(`/api/house-requirements/${id}/house-style`, style);
  },

  async downloadDxf(id: number): Promise<void> {
    const { data } = await apiClient.get<Blob>(`/api/house-requirements/${id}/plans/dxf`, { responseType: "blob" });
    const url = URL.createObjectURL(data);
    const link = document.createElement("a");
    link.href = url;
    link.download = `house-plan-${id}.dxf`;
    link.click();
    URL.revokeObjectURL(url);
  },

  async applyPlans(id: number): Promise<HouseRequirement> {
    const { data } = await apiClient.post<ApiResponse<HouseRequirement>>(`/api/house-requirements/${id}/plans/apply`);
    return data.data;
  },

  async whatIf(id: number, payload: WhatIfRequest): Promise<WhatIfResult> {
    const { data } = await apiClient.post<ApiResponse<WhatIfResult>>(`/api/house-requirements/${id}/what-if`, payload);
    return data.data;
  },

  async getSiteCheck(id: number): Promise<SiteCheck> {
    const { data } = await apiClient.get<ApiResponse<SiteCheck>>(`/api/house-requirements/${id}/site-rules`);
    return data.data;
  },

  async saveSiteRules(id: number, rules: SiteRules): Promise<SiteCheck> {
    const { data } = await apiClient.put<ApiResponse<SiteCheck>>(`/api/house-requirements/${id}/site-rules`, rules);
    return data.data;
  },

  async generateBoq(id: number): Promise<BoqGenerationResult> {
    const { data } = await apiClient.post<ApiResponse<BoqGenerationResult>>(
      `/api/house-requirements/${id}/generate-boq`,
    );
    return data.data;
  },
};
