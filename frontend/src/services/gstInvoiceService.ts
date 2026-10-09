import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { GstInvoice, GstInvoicePayload } from "../types/gstInvoice";

export const gstInvoiceService = {
  async getForBill(raBillId: number): Promise<GstInvoice | null> {
    try {
      const { data } = await apiClient.get<ApiResponse<GstInvoice>>(`/api/billing/ra-bills/${raBillId}/gst-invoice`);
      return data.data;
    } catch (err: any) {
      if (err?.response?.status === 404) return null;
      throw err;
    }
  },

  async create(raBillId: number, payload: GstInvoicePayload): Promise<GstInvoice> {
    const { data } = await apiClient.post<ApiResponse<GstInvoice>>(
      `/api/billing/ra-bills/${raBillId}/gst-invoice`,
      payload,
    );
    return data.data;
  },
};
