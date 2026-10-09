export type RaBillStatus = "DRAFT" | "SUBMITTED" | "CERTIFIED" | "PAID";
export type PaymentMode = "BANK_TRANSFER" | "CHEQUE" | "CASH" | "UPI" | "OTHER";

export interface RaBill {
  id: number;
  projectId: number;
  billNumber: string;
  billDate: string;
  workDoneValue: number;
  retentionPercent: number;
  otherDeductions: number;
  certifiedAmount: number | null;
  retentionAmount: number;
  netPayableAmount: number;
  amountReceived: number;
  outstandingAmount: number;
  status: RaBillStatus;
  notes: string | null;
  createdByName: string;
}

export interface RaBillPayload {
  billNumber: string;
  billDate: string;
  workDoneValue: number;
  retentionPercent: number;
  otherDeductions?: number;
  notes?: string;
}

export interface BillPayment {
  id: number;
  raBillId: number;
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  referenceNumber: string | null;
  recordedByName: string;
}

export interface BillPaymentPayload {
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  referenceNumber?: string;
}

export interface BillingSummary {
  totalBilled: number;
  totalReceived: number;
  totalOutstanding: number;
  totalRetentionHeld: number;
}
