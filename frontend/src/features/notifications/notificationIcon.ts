import type { NotificationType } from "../../types/notification";

export function notificationIcon(type: NotificationType): string {
  switch (type) {
    case "MATERIAL_REQUEST":
    case "MATERIAL_REQUEST_DECISION":
      return "📦";
    case "DAILY_REPORT":
      return "📝";
    case "VARIATION_ORDER":
      return "📐";
    case "RA_BILL":
      return "💰";
    case "BUDGET_VARIANCE":
      return "⚠️";
    case "STOCK_VARIANCE":
      return "📉";
    default:
      return "🔔";
  }
}
