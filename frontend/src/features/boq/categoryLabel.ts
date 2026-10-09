import type { BoqCategory } from "../../types/boq";

export const BOQ_CATEGORIES: BoqCategory[] = ["MATERIAL", "LABOUR", "EQUIPMENT", "TRANSPORT", "OTHER"];

export function categoryLabel(category: BoqCategory): string {
  return category.charAt(0) + category.slice(1).toLowerCase();
}
