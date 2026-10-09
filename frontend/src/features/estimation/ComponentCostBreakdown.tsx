import type { BoqItem } from "../../types/boq";
import { formatCurrency } from "../../utils/format";
import { componentIcon, componentLabel } from "./labels";
import "./ComponentCostBreakdown.css";

interface ComponentTotal {
  component: string | null;
  amount: number;
}

function groupByComponent(items: BoqItem[]): ComponentTotal[] {
  const totals = new Map<string | null, number>();
  for (const item of items) {
    const key = item.component;
    totals.set(key, (totals.get(key) ?? 0) + item.estimatedAmount);
  }
  return [...totals.entries()]
    .map(([component, amount]) => ({ component, amount }))
    .sort((a, b) => b.amount - a.amount);
}

export function ComponentCostBreakdown({ items }: { items: BoqItem[] }) {
  const totals = groupByComponent(items);
  const maxAmount = Math.max(...totals.map((t) => t.amount), 1);
  const grandTotal = totals.reduce((sum, t) => sum + t.amount, 0);

  return (
    <div className="bf-cost-breakdown">
      {totals.map((total) => {
        const widthPercent = Math.max((total.amount / maxAmount) * 100, 3);
        const sharePercent = grandTotal > 0 ? Math.round((total.amount / grandTotal) * 100) : 0;
        return (
          <div className="bf-cost-breakdown__row" key={total.component ?? "other"}>
            <div className="bf-cost-breakdown__label">
              <span className="bf-cost-breakdown__icon" aria-hidden="true">
                {componentIcon(total.component)}
              </span>
              <span>{componentLabel(total.component)}</span>
            </div>
            <div className="bf-cost-breakdown__track">
              <div className="bf-cost-breakdown__fill" style={{ width: `${widthPercent}%` }} />
            </div>
            <div className="bf-cost-breakdown__value">
              <span className="bf-cost-breakdown__amount">{formatCurrency(total.amount)}</span>
              <span className="bf-cost-breakdown__share">{sharePercent}%</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
