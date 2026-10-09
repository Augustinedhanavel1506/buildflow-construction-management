import { useState } from "react";
import type { PlansResponse } from "../../../types/plan";
import { floorLabel } from "../labels";
import { PlanCanvas } from "./PlanCanvas";
import "./PlanViewer.css";

// Read-only drawing of each floor, e.g. for the engineer reviewing an estimate.
export function PlanViewer({ plans }: { plans: PlansResponse }) {
  const drawn = plans.floors.filter((f) => f.rooms.length > 0);
  const [level, setLevel] = useState(drawn[0]?.floorLevel ?? 0);
  const floor = drawn.find((f) => f.floorLevel === level) ?? drawn[0];
  if (!floor) return null;

  return (
    <div className="bf-plan-viewer">
      {drawn.length > 1 && (
        <div className="bf-plan-viewer__tabs">
          {drawn.map((f) => (
            <button
              key={f.floorLevel}
              className={`bf-plan-viewer__tab ${f.floorLevel === floor.floorLevel ? "bf-plan-viewer__tab--active" : ""}`}
              onClick={() => setLevel(f.floorLevel)}
            >
              {floorLabel(f.floorLevel)}
            </button>
          ))}
        </div>
      )}
      <PlanCanvas
        plotWidth={plans.plotWidthFt}
        plotDepth={plans.plotLengthFt}
        rooms={floor.rooms}
        openings={floor.openings}
        columns={floor.columns}
        beams={floor.beams}
        furniture={floor.furniture}
        readOnly
      />
      <p className="bf-plan-viewer__meta">
        {floorLabel(floor.floorLevel)}: {floor.rooms.length} rooms, {Math.round(floor.metrics.builtUpAreaSqft).toLocaleString("en-IN")} sq.ft
        drawn, {floor.metrics.doors} doors, {floor.metrics.windows} windows{floor.structure.columnCount > 0 && `, ${floor.structure.columnCount} columns, ${Math.round(floor.structure.beamLengthFt)} ft of beams (${floor.structure.concreteM3.toLocaleString("en-IN", { maximumFractionDigits: 1 })} m³ concrete, ${Math.round(floor.structure.steelKg).toLocaleString("en-IN")} kg steel)`}. Concept plan, not a sanctioned or structural drawing.
      </p>
    </div>
  );
}
