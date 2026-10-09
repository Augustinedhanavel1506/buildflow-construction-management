import type { MaterialRequisitionStatus, MaterialStockStatus } from "../../types/material";

export function stockStatusTone(status: MaterialStockStatus): "success" | "warning" | "danger" {
  switch (status) {
    case "NORMAL":
      return "success";
    case "LOW":
      return "warning";
    case "CRITICAL":
      return "danger";
  }
}

export function requisitionStatusTone(status: MaterialRequisitionStatus): "neutral" | "success" | "danger" {
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
