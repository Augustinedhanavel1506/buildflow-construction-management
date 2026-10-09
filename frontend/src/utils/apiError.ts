import { isAxiosError } from "axios";
import type { ApiResponse } from "../types/api";

const FALLBACK_MESSAGE = "Something went wrong. Please try again.";

export function getErrorMessage(error: unknown): string {
  if (isAxiosError<ApiResponse<unknown>>(error)) {
    const body = error.response?.data;
    if (body?.errors?.length) {
      return body.errors[0];
    }
    if (body?.message) {
      return body.message;
    }
    if (error.response?.status === 401) {
      return "Your session has expired. Please sign in again.";
    }
  }
  return FALLBACK_MESSAGE;
}
