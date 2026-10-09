import type { SubcontractorBillStatus, WorkOrderStatus } from "../../types/subcontractor";

export function subBillStatusTone(status: SubcontractorBillStatus): "neutral" | "warning" | "success" {
  switch (status) {
    case "DRAFT":
      return "neutral";
    case "APPROVED":
      return "warning";
    case "PAID":
      return "success";
  }
}

export function workOrderStatusTone(status: WorkOrderStatus): "success" | "neutral" | "danger" {
  switch (status) {
    case "ACTIVE":
      return "success";
    case "COMPLETED":
      return "neutral";
    case "TERMINATED":
      return "danger";
  }
}
