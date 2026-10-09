import type { VariationStatus } from "../../types/variation";

export function variationStatusTone(status: VariationStatus): "neutral" | "success" | "danger" {
  switch (status) {
    case "APPROVED":
      return "success";
    case "REJECTED":
      return "danger";
    case "PENDING":
    default:
      return "neutral";
  }
}
