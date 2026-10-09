import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { Expense, ExpensePayload } from "../types/expense";

export const expenseService = {
  async list(projectId: number): Promise<Expense[]> {
    const { data } = await apiClient.get<ApiResponse<Expense[]>>(`/api/projects/${projectId}/expenses`);
    return data.data;
  },

  async create(projectId: number, payload: ExpensePayload): Promise<Expense> {
    const { data } = await apiClient.post<ApiResponse<Expense>>(
      `/api/projects/${projectId}/expenses`,
      payload,
    );
    return data.data;
  },

  async update(id: number, payload: ExpensePayload): Promise<Expense> {
    const { data } = await apiClient.put<ApiResponse<Expense>>(`/api/expenses/${id}`, payload);
    return data.data;
  },

  async remove(id: number): Promise<void> {
    await apiClient.delete(`/api/expenses/${id}`);
  },
};
