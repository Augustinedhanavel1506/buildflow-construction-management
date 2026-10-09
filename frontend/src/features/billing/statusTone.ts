import type { RaBillStatus } from "../../types/billing";

export function billStatusTone(status: RaBillStatus): "neutral" | "info" | "warning" | "success" {
  switch (status) {
    case "DRAFT":
      return "neutral";
    case "SUBMITTED":
      return "info";
    case "CERTIFIED":
      return "warning";
    case "PAID":
      return "success";
  }
}
