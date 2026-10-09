export interface GstInvoice {
  id: number;
  raBillId: number;
  billNumber: string;
  projectId: number;
  projectName: string;
  invoiceNumber: string;
  invoiceDate: string;
  hsnSacCode: string;
  description: string;
  taxableValue: number;
  gstRate: number;
  interState: boolean;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalInvoiceValue: number;
  placeOfSupply: string | null;
  supplierName: string;
  supplierGstin: string | null;
  supplierAddress: string | null;
  clientName: string | null;
  clientGstin: string | null;
  clientAddress: string | null;
  createdByName: string;
}

export interface GstInvoicePayload {
  invoiceNumber: string;
  invoiceDate: string;
  hsnSacCode?: string;
  gstRate?: number;
  interState: boolean;
  placeOfSupply?: string;
  description?: string;
}
