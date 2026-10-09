import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Skeleton } from "../../components/ui/Skeleton";
import { estimationService } from "../../services/estimationService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { ConstructionGrade, HouseRequirement } from "../../types/estimation";
import type { WhatIfRequest, WhatIfResult } from "../../types/whatIf";
import { CONSTRUCTION_GRADES, GRADE_LABEL, componentIcon, componentLabel, floorLabel } from "./labels";
import "./WhatIfPage.css";

interface FloorDraft {
  level: number;
  removed: boolean;
  area: string;
  bedrooms: number;
  bathrooms: number;
  kitchen: boolean;
  hall: boolean;
  balcony: boolean;
  pooja: boolean;
}

interface AddedFloor {
  key: string;
  area: string;
  bedrooms: number;
  bathrooms: number;
}

function toDrafts(requirement: HouseRequirement): FloorDraft[] {
  return requirement.floors.map((f) => ({
    level: f.floorLevel,
    removed: false,
    area: f.floorAreaSqft.toString(),
    bedrooms: f.bedroomCount,
    bathrooms: f.bathroomCount,
    kitchen: f.hasKitchen,
    hall: f.hasHall,
    balcony: f.hasBalcony,
    pooja: f.hasPoojaRoom,
  }));
}

function Stepper({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <div className="bf-whatif__stepper">
      <span>{label}</span>
      <button type="button" aria-label={`Decrease ${label}`} onClick={() => onChange(Math.max(0, value - 1))}>−</button>
      <strong>{value}</strong>
      <button type="button" aria-label={`Increase ${label}`} onClick={() => onChange(value + 1)}>+</button>
    </div>
  );
}

function signed(value: number): string {
  return `${value > 0 ? "+" : value < 0 ? "−" : ""}${formatCurrency(Math.abs(value))}`;
}

export function WhatIfPage() {
  const { id } = useParams<{ id: string }>();
  const requirementId = Number(id);

  const [requirement, setRequirement] = useState<HouseRequirement | null>(null);
  const [grade, setGrade] = useState<ConstructionGrade>("STANDARD");
  const [drafts, setDrafts] = useState<FloorDraft[]>([]);
  const [added, setAdded] = useState<AddedFloor[]>([]);
  const [result, setResult] = useState<WhatIfResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const latest = useRef(0);

  useEffect(() => {
    estimationService
      .get(requirementId)
      .then((req) => {
        setRequirement(req);
        setGrade(req.constructionGrade);
        setDrafts(toDrafts(req));
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, [requirementId]);

  // Only what differs from the plan as it stands is sent, so "no changes" is recognised as such.
  const request = useMemo<WhatIfRequest | null>(() => {
    if (!requirement) return null;
    const body: WhatIfRequest = {};
    if (grade !== requirement.constructionGrade) body.constructionGrade = grade;

    const changes = [];
    const removed: number[] = [];
    for (const draft of drafts) {
      const original = requirement.floors.find((f) => f.floorLevel === draft.level)!;
      if (draft.removed) {
        removed.push(draft.level);
        continue;
      }
      const change: NonNullable<WhatIfRequest["floors"]>[number] = { floorLevel: draft.level };
      const area = Number(draft.area);
      if (area > 0 && area !== original.floorAreaSqft) change.floorAreaSqft = area;
      if (draft.bedrooms !== original.bedroomCount) change.bedroomCount = draft.bedrooms;
      if (draft.bathrooms !== original.bathroomCount) change.bathroomCount = draft.bathrooms;
      if (draft.kitchen !== original.hasKitchen) change.hasKitchen = draft.kitchen;
      if (draft.hall !== original.hasHall) change.hasHall = draft.hall;
      if (draft.balcony !== original.hasBalcony) change.hasBalcony = draft.balcony;
      if (draft.pooja !== original.hasPoojaRoom) change.hasPoojaRoom = draft.pooja;
      if (Object.keys(change).length > 1) changes.push(change);
    }
    if (changes.length) body.floors = changes;
    if (removed.length) body.removeFloorLevels = removed;
    const extra = added.filter((a) => Number(a.area) > 0).map((a) => ({
      floorAreaSqft: Number(a.area),
      bedroomCount: a.bedrooms,
      bathroomCount: a.bathrooms,
    }));
    if (extra.length) body.addFloors = extra;
    return Object.keys(body).length > 0 ? body : null;
  }, [requirement, grade, drafts, added]);

  const requestKey = JSON.stringify(request);
  useEffect(() => {
    if (!request) {
      setResult(null);
      setError(null);
      return;
    }
    const ticket = ++latest.current;
    const timer = setTimeout(async () => {
      setIsLoading(true);
      try {
        const response = await estimationService.whatIf(requirementId, request);
        if (ticket === latest.current) {
          setResult(response);
          setError(null);
        }
      } catch (err) {
        if (ticket === latest.current) setError(getErrorMessage(err));
      } finally {
        if (ticket === latest.current) setIsLoading(false);
      }
    }, 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey, requirementId]);

  function patchDraft(level: number, patch: Partial<FloorDraft>) {
    setDrafts((current) => current.map((d) => (d.level === level ? { ...d, ...patch } : d)));
  }

  function reset() {
    if (!requirement) return;
    setGrade(requirement.constructionGrade);
    setDrafts(toDrafts(requirement));
    setAdded([]);
  }

  if (!requirement) {
    return error ? <div className="bf-whatif__error">{error}</div> : <Skeleton height={420} />;
  }

  const top = [...drafts].filter((d) => !d.removed).sort((a, b) => b.level - a.level)[0];
  const hasBalcony = drafts.some((d) => !d.removed && d.balcony);
  const ground = drafts.find((d) => !d.removed) ?? drafts[0];
  const maxComponent = Math.max(...(result?.components.map((c) => Math.abs(c.difference)) ?? [1]), 1);

  return (
    <div className="bf-whatif">
      <Link to={`/estimation/${requirementId}`} className="bf-whatif__back">← Back to estimate</Link>
      <div className="bf-whatif__header">
        <div>
          <h1>What if…</h1>
          <p>
            Try a change and see what it does to the cost. Nothing is saved: this compares a copy of {requirement.label}
            {" "}with the plan as it stands.
          </p>
        </div>
        <Button variant="secondary" onClick={reset} disabled={!request}>Reset</Button>
      </div>

      <div className="bf-whatif__quick">
        <span>Quick tries:</span>
        {hasBalcony && (
          <button onClick={() => setDrafts((c) => c.map((d) => ({ ...d, balcony: false })))}>Skip the balcony</button>
        )}
        {ground && <button onClick={() => patchDraft(ground.level, { bedrooms: ground.bedrooms + 1 })}>One more bedroom</button>}
        {grade !== "PREMIUM" && <button onClick={() => setGrade("PREMIUM")}>Premium finish</button>}
        {grade !== "ECONOMY" && <button onClick={() => setGrade("ECONOMY")}>Economy finish</button>}
        {top && drafts.filter((d) => !d.removed).length > 1 && (
          <button onClick={() => patchDraft(top.level, { removed: true })}>Drop {floorLabel(top.level).toLowerCase()}</button>
        )}
        <button onClick={() => setAdded((c) => [...c, { key: crypto.randomUUID(), area: "600", bedrooms: 1, bathrooms: 1 }])}>
          Add a floor
        </button>
      </div>

      <div className="bf-whatif__body">
        <div className="bf-whatif__controls">
          <Card>
            <h3>Finish level</h3>
            <div className="bf-whatif__grades">
              {CONSTRUCTION_GRADES.map((g) => (
                <button key={g} className={grade === g ? "bf-whatif__grade bf-whatif__grade--active" : "bf-whatif__grade"} onClick={() => setGrade(g)}>
                  {GRADE_LABEL[g]}
                  {g === requirement.constructionGrade && <small> current</small>}
                </button>
              ))}
            </div>
          </Card>

          {drafts.map((draft) => (
            <Card key={draft.level} className={draft.removed ? "bf-whatif__floor bf-whatif__floor--removed" : "bf-whatif__floor"}>
              <div className="bf-whatif__floor-head">
                <h3>{floorLabel(draft.level)}</h3>
                <label className="bf-whatif__check">
                  <input
                    type="checkbox"
                    checked={draft.removed}
                    disabled={drafts.filter((d) => !d.removed).length === 1 && !draft.removed}
                    onChange={(e) => patchDraft(draft.level, { removed: e.target.checked })}
                  />
                  Remove floor
                </label>
              </div>
              {!draft.removed && (
                <>
                  <label className="bf-whatif__field">
                    Area (sq.ft)
                    <input type="number" min="0" step="10" value={draft.area} onChange={(e) => patchDraft(draft.level, { area: e.target.value })} />
                  </label>
                  <div className="bf-whatif__steppers">
                    <Stepper label="Bedrooms" value={draft.bedrooms} onChange={(v) => patchDraft(draft.level, { bedrooms: v })} />
                    <Stepper label="Bathrooms" value={draft.bathrooms} onChange={(v) => patchDraft(draft.level, { bathrooms: v })} />
                  </div>
                  <div className="bf-whatif__toggles">
                    {([["kitchen", "Kitchen"], ["hall", "Hall"], ["balcony", "Balcony"], ["pooja", "Pooja room"]] as const).map(([field, label]) => (
                      <label key={field} className="bf-whatif__check">
                        <input type="checkbox" checked={draft[field]} onChange={(e) => patchDraft(draft.level, { [field]: e.target.checked })} />
                        {label}
                      </label>
                    ))}
                  </div>
                </>
              )}
            </Card>
          ))}

          {added.map((floor, index) => (
            <Card key={floor.key} className="bf-whatif__floor">
              <div className="bf-whatif__floor-head">
                <h3>New floor {index + 1}</h3>
                <button className="bf-whatif__remove" onClick={() => setAdded((c) => c.filter((x) => x.key !== floor.key))}>Remove</button>
              </div>
              <label className="bf-whatif__field">
                Area (sq.ft)
                <input type="number" min="0" step="10" value={floor.area} onChange={(e) => setAdded((c) => c.map((x) => (x.key === floor.key ? { ...x, area: e.target.value } : x)))} />
              </label>
              <div className="bf-whatif__steppers">
                <Stepper label="Bedrooms" value={floor.bedrooms} onChange={(v) => setAdded((c) => c.map((x) => (x.key === floor.key ? { ...x, bedrooms: v } : x)))} />
                <Stepper label="Bathrooms" value={floor.bathrooms} onChange={(v) => setAdded((c) => c.map((x) => (x.key === floor.key ? { ...x, bathrooms: v } : x)))} />
              </div>
            </Card>
          ))}
        </div>

        <div className="bf-whatif__results">
          <Card>
            {error && <div className="bf-whatif__error">{error}</div>}
            {!request && !error && (
              <p className="bf-whatif__placeholder">Change something on the left, or tap a quick try, to see the difference.</p>
            )}
            {request && !result && !error && <Skeleton height={140} />}
            {result && (
              <div className={isLoading ? "bf-whatif__loading" : undefined}>
                <span className="bf-whatif__label">Cost difference</span>
                <div className={`bf-whatif__delta ${result.difference > 0 ? "bf-whatif__delta--up" : result.difference < 0 ? "bf-whatif__delta--down" : ""}`}>
                  {signed(result.difference)}
                </div>
                {result.differencePercent !== null && (
                  <span className="bf-whatif__percent">
                    {result.differencePercent > 0 ? "+" : ""}{result.differencePercent}% against the current plan
                  </span>
                )}
                <div className="bf-whatif__compare">
                  <div>
                    <span>Current</span>
                    <strong>{formatCurrency(result.baseline.total)}</strong>
                    <small>{result.baseline.builtUpAreaSqft.toLocaleString("en-IN")} sq.ft</small>
                  </div>
                  <div>
                    <span>With your change</span>
                    <strong>{formatCurrency(result.scenario.total)}</strong>
                    <small>{result.scenario.builtUpAreaSqft.toLocaleString("en-IN")} sq.ft</small>
                  </div>
                </div>
              </div>
            )}
          </Card>

          {result && result.components.some((c) => c.difference !== 0) && (
            <Card>
              <h3>Where it changes</h3>
              <div className="bf-whatif__components">
                {result.components.filter((c) => c.difference !== 0).map((c) => (
                  <div key={c.component} className="bf-whatif__component">
                    <span>{componentIcon(c.component)} {componentLabel(c.component)}</span>
                    <div className="bf-whatif__bar">
                      <div
                        className={c.difference > 0 ? "bf-whatif__bar-fill bf-whatif__bar-fill--up" : "bf-whatif__bar-fill bf-whatif__bar-fill--down"}
                        style={{ width: `${Math.max((Math.abs(c.difference) / maxComponent) * 100, 4)}%` }}
                      />
                    </div>
                    <strong>{signed(c.difference)}</strong>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {result && result.items.length > 0 && (
            <Card>
              <h3>Biggest material changes</h3>
              <table className="bf-table bf-whatif__table">
                <thead>
                  <tr><th>Item</th><th>Quantity</th><th>Cost change</th></tr>
                </thead>
                <tbody>
                  {result.items.slice(0, 8).map((item) => (
                    <tr key={`${item.itemName}-${item.unit}`}>
                      <td>{item.itemName}</td>
                      <td>
                        {item.baselineQuantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} → {item.scenarioQuantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {item.unit}
                      </td>
                      <td>{signed(item.difference)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          )}

          {result && result.unpricedItems.length > 0 && (
            <p className="bf-whatif__note">
              Not priced (no rate yet), so left out of both sides: {result.unpricedItems.join(", ")}.
            </p>
          )}
          <p className="bf-whatif__note">
            Both sides use the built-up-area rules, so the difference is only your change. It ignores a drawn floor plan
            and anything you added by hand.
          </p>
        </div>
      </div>
    </div>
  );
}
