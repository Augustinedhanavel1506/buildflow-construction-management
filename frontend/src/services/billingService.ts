import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { BillingSummary, BillPayment, BillPaymentPayload, RaBill, RaBillPayload } from "../types/billing";

export const billingService = {
  async listBills(projectId: number): Promise<RaBill[]> {
    const { data } = await apiClient.get<ApiResponse<RaBill[]>>(`/api/projects/${projectId}/billing/ra-bills`);
    return data.data;
  },

  async createBill(projectId: number, payload: RaBillPayload): Promise<RaBill> {
    const { data } = await apiClient.post<ApiResponse<RaBill>>(
      `/api/projects/${projectId}/billing/ra-bills`,
      payload,
    );
    return data.data;
  },

  async updateBill(id: number, payload: RaBillPayload): Promise<RaBill> {
    const { data } = await apiClient.put<ApiResponse<RaBill>>(`/api/billing/ra-bills/${id}`, payload);
    return data.data;
  },

  async submitBill(id: number): Promise<RaBill> {
    const { data } = await apiClient.patch<ApiResponse<RaBill>>(`/api/billing/ra-bills/${id}/submit`);
    return data.data;
  },

  async certifyBill(id: number, certifiedAmount: number): Promise<RaBill> {
    const { data } = await apiClient.patch<ApiResponse<RaBill>>(`/api/billing/ra-bills/${id}/certify`, {
      certifiedAmount,
    });
    return data.data;
  },

  async getSummary(projectId: number): Promise<BillingSummary> {
    const { data } = await apiClient.get<ApiResponse<BillingSummary>>(`/api/projects/${projectId}/billing/summary`);
    return data.data;
  },

  async listPayments(raBillId: number): Promise<BillPayment[]> {
    const { data } = await apiClient.get<ApiResponse<BillPayment[]>>(`/api/billing/ra-bills/${raBillId}/payments`);
    return data.data;
  },

  async createPayment(raBillId: number, payload: BillPaymentPayload): Promise<BillPayment> {
    const { data } = await apiClient.post<ApiResponse<BillPayment>>(
      `/api/billing/ra-bills/${raBillId}/payments`,
      payload,
    );
    return data.data;
  },
};
