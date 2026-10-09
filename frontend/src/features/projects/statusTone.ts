import type { ProjectStatus } from "../../types/project";

export function statusTone(status: ProjectStatus): "neutral" | "success" | "warning" | "danger" | "info" {
  switch (status) {
    case "ACTIVE":
      return "success";
    case "ON_HOLD":
      return "warning";
    case "CANCELLED":
      return "danger";
    case "COMPLETED":
      return "info";
    case "PLANNING":
    default:
      return "neutral";
  }
}
