import type { ExpenseCategory } from "../../types/expense";

export const EXPENSE_CATEGORIES: ExpenseCategory[] = [
  "MATERIAL",
  "LABOUR",
  "TRANSPORT",
  "EQUIPMENT",
  "SUBCONTRACTOR",
  "OTHER",
];

export function expenseCategoryLabel(category: ExpenseCategory): string {
  return category.charAt(0) + category.slice(1).toLowerCase();
}
