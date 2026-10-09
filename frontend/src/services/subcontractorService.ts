import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type {
  Subcontractor,
  SubcontractorBill,
  SubcontractorBillPayload,
  SubcontractorPayload,
  SubcontractorPayment,
  SubcontractorPaymentPayload,
  WorkOrder,
  WorkOrderPayload,
} from "../types/subcontractor";

export const subcontractorService = {
  async list(includeInactive = false): Promise<Subcontractor[]> {
    const { data } = await apiClient.get<ApiResponse<Subcontractor[]>>("/api/subcontractors", {
      params: { includeInactive },
    });
    return data.data;
  },

  async create(payload: SubcontractorPayload): Promise<Subcontractor> {
    const { data } = await apiClient.post<ApiResponse<Subcontractor>>("/api/subcontractors", payload);
    return data.data;
  },

  async update(id: number, payload: SubcontractorPayload): Promise<Subcontractor> {
    const { data } = await apiClient.put<ApiResponse<Subcontractor>>(`/api/subcontractors/${id}`, payload);
    return data.data;
  },

  async listWorkOrders(projectId: number): Promise<WorkOrder[]> {
    const { data } = await apiClient.get<ApiResponse<WorkOrder[]>>(
      `/api/projects/${projectId}/subcontract-work-orders`,
    );
    return data.data;
  },

  async createWorkOrder(projectId: number, payload: WorkOrderPayload): Promise<WorkOrder> {
    const { data } = await apiClient.post<ApiResponse<WorkOrder>>(
      `/api/projects/${projectId}/subcontract-work-orders`,
      payload,
    );
    return data.data;
  },

  async listBills(workOrderId: number): Promise<SubcontractorBill[]> {
    const { data } = await apiClient.get<ApiResponse<SubcontractorBill[]>>(
      `/api/subcontract-work-orders/${workOrderId}/bills`,
    );
    return data.data;
  },

  async createBill(workOrderId: number, payload: SubcontractorBillPayload): Promise<SubcontractorBill> {
    const { data } = await apiClient.post<ApiResponse<SubcontractorBill>>(
      `/api/subcontract-work-orders/${workOrderId}/bills`,
      payload,
    );
    return data.data;
  },

  async approveBill(id: number): Promise<SubcontractorBill> {
    const { data } = await apiClient.patch<ApiResponse<SubcontractorBill>>(`/api/subcontractor-bills/${id}/approve`);
    return data.data;
  },

  async listPayments(billId: number): Promise<SubcontractorPayment[]> {
    const { data } = await apiClient.get<ApiResponse<SubcontractorPayment[]>>(
      `/api/subcontractor-bills/${billId}/payments`,
    );
    return data.data;
  },

  async createPayment(billId: number, payload: SubcontractorPaymentPayload): Promise<SubcontractorPayment> {
    const { data } = await apiClient.post<ApiResponse<SubcontractorPayment>>(
      `/api/subcontractor-bills/${billId}/payments`,
      payload,
    );
    return data.data;
  },
};
