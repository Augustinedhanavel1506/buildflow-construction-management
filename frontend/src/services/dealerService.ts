import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type {
  Dealer,
  DealerPayload,
  QuoteRequest,
  QuoteRequestSummary,
  QuoteUpdatePayload,
} from "../types/dealer";

export const dealerService = {
  async list(includeInactive = false): Promise<Dealer[]> {
    const { data } = await apiClient.get<ApiResponse<Dealer[]>>("/api/dealers", { params: { includeInactive } });
    return data.data;
  },

  async create(payload: DealerPayload): Promise<Dealer> {
    const { data } = await apiClient.post<ApiResponse<Dealer>>("/api/dealers", payload);
    return data.data;
  },

  async update(id: number, payload: DealerPayload): Promise<Dealer> {
    const { data } = await apiClient.put<ApiResponse<Dealer>>(`/api/dealers/${id}`, payload);
    return data.data;
  },
};

export const quoteRequestService = {
  async list(): Promise<QuoteRequestSummary[]> {
    const { data } = await apiClient.get<ApiResponse<QuoteRequestSummary[]>>("/api/quote-requests");
    return data.data;
  },

  async get(id: number): Promise<QuoteRequest> {
    const { data } = await apiClient.get<ApiResponse<QuoteRequest>>(`/api/quote-requests/${id}`);
    return data.data;
  },

  async create(payload: { projectId: number; title: string; dealerIds: number[] }): Promise<QuoteRequest> {
    const { data } = await apiClient.post<ApiResponse<QuoteRequest>>("/api/quote-requests", payload);
    return data.data;
  },

  async recordQuote(id: number, quoteId: number, payload: QuoteUpdatePayload): Promise<QuoteRequest> {
    const { data } = await apiClient.put<ApiResponse<QuoteRequest>>(`/api/quote-requests/${id}/quotes/${quoteId}`, payload);
    return data.data;
  },

  async award(id: number, quoteId: number): Promise<QuoteRequest> {
    const { data } = await apiClient.post<ApiResponse<QuoteRequest>>(`/api/quote-requests/${id}/award/${quoteId}`);
    return data.data;
  },

  async close(id: number): Promise<QuoteRequest> {
    const { data } = await apiClient.post<ApiResponse<QuoteRequest>>(`/api/quote-requests/${id}/close`);
    return data.data;
  },
};
