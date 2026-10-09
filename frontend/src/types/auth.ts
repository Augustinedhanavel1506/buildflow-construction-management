export type UserRole = "ADMIN" | "PROJECT_MANAGER" | "SITE_SUPERVISOR" | "ENGINEER";

export interface AuthUser {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  businessId: number;
  businessName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  businessName: string;
  fullName: string;
  email: string;
  password: string;
}
