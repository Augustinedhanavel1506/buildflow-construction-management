export type NotificationType =
  | "MATERIAL_REQUEST"
  | "MATERIAL_REQUEST_DECISION"
  | "DAILY_REPORT"
  | "VARIATION_ORDER"
  | "RA_BILL"
  | "BUDGET_VARIANCE"
  | "STOCK_VARIANCE";

export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  entityType: string | null;
  entityId: number | null;
  projectId: number | null;
  read: boolean;
  createdAt: string;
}
