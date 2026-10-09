import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { AuthResponse, LoginPayload, RegisterPayload } from "../types/auth";

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>("/api/auth/login", payload);
    return data.data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>("/api/auth/register", payload);
    return data.data;
  },
};
