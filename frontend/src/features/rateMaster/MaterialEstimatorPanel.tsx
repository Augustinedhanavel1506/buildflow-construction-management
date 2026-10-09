import { useMemo, useState } from "react";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { formatCurrency } from "../../utils/format";
import type { RateMasterItem } from "../../types/rateMaster";
import "./MaterialEstimatorPanel.css";

function formatQuantity(value: number): string {
  return value.toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

function formatRateBand(low: number, high: number): string {
  return low === high ? formatCurrency(low) : `${formatCurrency(low)} – ${formatCurrency(high)}`;
}

export function MaterialEstimatorPanel({ items }: { items: RateMasterItem[] }) {
  const [areaInput, setAreaInput] = useState("1000");

  const estimable = useMemo(
    () =>
      items.filter(
        (item) => item.category === "MATERIAL" && item.active && item.consumptionPerSqft !== null && item.consumptionPerSqft > 0,
      ),
    [items],
  );

  const area = Number(areaInput);
  const isValidArea = areaInput !== "" && !Number.isNaN(area) && area > 0;

  const rows = useMemo(() => {
    if (!isValidArea) return [];
    return estimable.map((item) => {
      const quantity = area * (item.consumptionPerSqft as number);
      const low = quantity * (item.minRate ?? item.standardRate);
      const high = quantity * (item.maxRate ?? item.standardRate);
      return { item, quantity, low, high };
    });
  }, [estimable, area, isValidArea]);

  const totalLow = rows.reduce((sum, r) => sum + r.low, 0);
  const totalHigh = rows.reduce((sum, r) => sum + r.high, 0);

  return (
    <Card className="bf-material-estimator">
      <h3>Material Quantity Estimator</h3>
      <p>
        Rough thumb-rule estimate, not a structural takeoff — enter a built-up area to see approximate
        material quantities and cost range, using the consumption coefficients set on your Rate Master items.
      </p>

      <Input
        label="Built-up Area (sqft)"
        type="number"
        min="0"
        value={areaInput}
        onChange={(e) => setAreaInput(e.target.value)}
      />

      {estimable.length === 0 ? (
        <EmptyState
          title="No materials configured for estimation."
          description='Edit a MATERIAL item in Rate Master and set "Consumption per sqft" to include it here (e.g. 0.4 bags of cement per sqft).'
        />
      ) : !isValidArea ? (
        <EmptyState title="Enter a built-up area to see the estimate." description="" />
      ) : (
        <>
          <table className="bf-table">
            <thead>
              <tr>
                <th>Material</th>
                <th>Estimated Quantity</th>
                <th>Rate Band</th>
                <th>Estimated Cost</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ item, quantity, low, high }) => (
                <tr key={item.id}>
                  <td>{item.itemName}</td>
                  <td>
                    {formatQuantity(quantity)} {item.unit}
                  </td>
                  <td>{formatRateBand(item.minRate ?? item.standardRate, item.maxRate ?? item.standardRate)}</td>
                  <td>{formatRateBand(low, high)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bf-material-estimator__total-row">
                <td colSpan={3}>Estimated Total</td>
                <td>{formatRateBand(totalLow, totalHigh)}</td>
              </tr>
            </tfoot>
          </table>
          <p className="bf-material-estimator__disclaimer">
            Ballpark planning estimate only. Always confirm quantities against an actual structural BOQ
            before quoting a client.
          </p>
        </>
      )}
    </Card>
  );
}
