export type ExpenseCategory = "MATERIAL" | "LABOUR" | "TRANSPORT" | "EQUIPMENT" | "SUBCONTRACTOR" | "OTHER";

export interface Expense {
  id: number;
  projectId: number;
  category: ExpenseCategory;
  amount: number;
  date: string;
  supplierName: string | null;
  description: string | null;
  createdByName: string;
}

export interface ExpensePayload {
  category: ExpenseCategory;
  amount: number;
  date: string;
  supplierName?: string;
  description?: string;
}
