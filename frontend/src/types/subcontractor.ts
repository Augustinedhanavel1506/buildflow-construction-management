import type { PaymentMode } from "./billing";

export interface Subcontractor {
  id: number;
  name: string;
  tradeType: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  gstNumber: string | null;
  panNumber: string | null;
  active: boolean;
}

export interface SubcontractorPayload {
  name: string;
  tradeType: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  gstNumber?: string;
  panNumber?: string;
  active?: boolean;
}

export type ContractType = "LUMP_SUM" | "ITEM_RATE";
export type WorkOrderStatus = "ACTIVE" | "COMPLETED" | "TERMINATED";

export interface WorkOrder {
  id: number;
  projectId: number;
  subcontractorId: number;
  subcontractorName: string;
  tradeType: string;
  title: string;
  scopeDescription: string | null;
  contractType: ContractType;
  contractValue: number;
  status: WorkOrderStatus;
  startDate: string | null;
  endDate: string | null;
  totalBilled: number;
  totalPaid: number;
}

export interface WorkOrderPayload {
  subcontractorId: number;
  title: string;
  scopeDescription?: string;
  contractType: ContractType;
  contractValue: number;
  startDate?: string;
  endDate?: string;
}

export type SubcontractorBillStatus = "DRAFT" | "APPROVED" | "PAID";

export interface SubcontractorBill {
  id: number;
  workOrderId: number;
  billNumber: string;
  billDate: string;
  workDoneValue: number;
  tdsPercent: number;
  retentionPercent: number;
  otherDeductions: number;
  tdsAmount: number;
  retentionAmount: number;
  netPayableAmount: number;
  amountPaid: number;
  outstandingAmount: number;
  status: SubcontractorBillStatus;
  notes: string | null;
  createdByName: string;
}

export interface SubcontractorBillPayload {
  billNumber: string;
  billDate: string;
  workDoneValue: number;
  tdsPercent: number;
  retentionPercent: number;
  otherDeductions?: number;
  notes?: string;
}

export interface SubcontractorPayment {
  id: number;
  subcontractorBillId: number;
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  referenceNumber: string | null;
  recordedByName: string;
}

export interface SubcontractorPaymentPayload {
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  referenceNumber?: string;
}
