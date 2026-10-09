import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { Engineer, EngineerPayload, Review, ReviewSubmitPayload } from "../types/review";
import type {
  ConcretePayload,
  ConcreteResult,
  MasonryPayload,
  MasonryResult,
  SteelPayload,
  SteelResult,
} from "../types/calculator";

export const reviewService = {
  async list(): Promise<Review[]> {
    const { data } = await apiClient.get<ApiResponse<Review[]>>("/api/review-requests");
    return data.data;
  },

  async get(id: number): Promise<Review> {
    const { data } = await apiClient.get<ApiResponse<Review>>(`/api/review-requests/${id}`);
    return data.data;
  },

  async create(payload: { projectId: number; engineerId: number; message?: string }): Promise<Review> {
    const { data } = await apiClient.post<ApiResponse<Review>>("/api/review-requests", payload);
    return data.data;
  },

  async submit(id: number, payload: ReviewSubmitPayload): Promise<Review> {
    const { data } = await apiClient.post<ApiResponse<Review>>(`/api/review-requests/${id}/submit`, payload);
    return data.data;
  },

  async cancel(id: number): Promise<Review> {
    const { data } = await apiClient.post<ApiResponse<Review>>(`/api/review-requests/${id}/cancel`);
    return data.data;
  },
};

export const teamService = {
  async listEngineers(): Promise<Engineer[]> {
    const { data } = await apiClient.get<ApiResponse<Engineer[]>>("/api/team/engineers");
    return data.data;
  },

  async createEngineer(payload: EngineerPayload): Promise<Engineer> {
    const { data } = await apiClient.post<ApiResponse<Engineer>>("/api/team/engineers", payload);
    return data.data;
  },

  async setActive(id: number, active: boolean): Promise<Engineer> {
    const { data } = await apiClient.put<ApiResponse<Engineer>>(`/api/team/engineers/${id}/active`, null, {
      params: { active },
    });
    return data.data;
  },
};

export const calculatorService = {
  async concrete(payload: ConcretePayload): Promise<ConcreteResult> {
    const { data } = await apiClient.post<ApiResponse<ConcreteResult>>("/api/calculators/concrete", payload);
    return data.data;
  },

  async masonry(payload: MasonryPayload): Promise<MasonryResult> {
    const { data } = await apiClient.post<ApiResponse<MasonryResult>>("/api/calculators/masonry", payload);
    return data.data;
  },

  async steel(payload: SteelPayload): Promise<SteelResult> {
    const { data } = await apiClient.post<ApiResponse<SteelResult>>("/api/calculators/steel", payload);
    return data.data;
  },
};
