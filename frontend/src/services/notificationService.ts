import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { Page } from "../types/project";
import type { AppNotification } from "../types/notification";

export const notificationService = {
  async list(page = 0, size = 20): Promise<Page<AppNotification>> {
    const { data } = await apiClient.get<ApiResponse<Page<AppNotification>>>("/api/notifications", {
      params: { page, size },
    });
    return data.data;
  },

  async getUnreadCount(): Promise<number> {
    const { data } = await apiClient.get<ApiResponse<number>>("/api/notifications/unread-count");
    return data.data;
  },

  async markAsRead(id: number): Promise<AppNotification> {
    const { data } = await apiClient.patch<ApiResponse<AppNotification>>(`/api/notifications/${id}/read`);
    return data.data;
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.patch("/api/notifications/read-all");
  },
};
