import { useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { calculatorService } from "../../services/reviewService";
import { getErrorMessage } from "../../utils/apiError";
import type {
  ConcreteResult,
  MasonryResult,
  MemberType,
  MixGrade,
  SteelResult,
  UnitType,
} from "../../types/calculator";
import "./CalculatorsPage.css";

type Tab = "concrete" | "masonry" | "steel";

const n = (v: number) => v.toLocaleString("en-IN", { maximumFractionDigits: 3 });

function Assumptions({ items }: { items: string[] }) {
  return (
    <details className="bf-calc__assumptions">
      <summary>Assumptions used</summary>
      <ul>
        {items.map((a) => (
          <li key={a}>{a}</li>
        ))}
      </ul>
    </details>
  );
}

function Stat({ label, value, unit }: { label: string; value: string; unit: string }) {
  return (
    <div className="bf-calc__stat">
      <span className="bf-calc__stat-label">{label}</span>
      <span className="bf-calc__stat-value">
        {value} <small>{unit}</small>
      </span>
    </div>
  );
}

function NumberCell({ label, value, onChange, step = "0.01" }: { label: string; value: string; onChange: (v: string) => void; step?: string }) {
  return (
    <input type="number" min="0" step={step} placeholder={label} aria-label={label} value={value} onChange={(e) => onChange(e.target.value)} />
  );
}

interface MemberRow { key: string; name: string; type: MemberType; count: string; l: string; w: string; d: string; steel: string }

function ConcreteCalculator() {
  const [grade, setGrade] = useState<MixGrade>("M20");
  const [wastage, setWastage] = useState("5");
  const [rows, setRows] = useState<MemberRow[]>([{ key: crypto.randomUUID(), name: "Footings", type: "FOOTING", count: "4", l: "1.5", w: "1.5", d: "0.4", steel: "" }]);
  const [result, setResult] = useState<ConcreteResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function patch(key: string, p: Partial<MemberRow>) {
    setRows((cur) => cur.map((r) => (r.key === key ? { ...r, ...p } : r)));
  }

  async function calculate() {
    setError(null);
    for (const r of rows) {
      if (!r.name.trim() || !(Number(r.count) >= 1) || !(Number(r.l) > 0) || !(Number(r.w) > 0) || !(Number(r.d) > 0)) {
        setError("Every member needs a name, a count of at least 1, and length, width and depth above zero.");
        return;
      }
    }
    setBusy(true);
    try {
      setResult(
        await calculatorService.concrete({
          grade,
          wastagePercent: Number(wastage) || 0,
          members: rows.map((r) => ({
            name: r.name.trim(),
            type: r.type,
            count: Number(r.count),
            lengthM: Number(r.l),
            widthM: Number(r.w),
            depthM: Number(r.d),
            steelPercent: r.steel ? Number(r.steel) : undefined,
          })),
        }),
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="bf-calc__panel">
      <div className="bf-calc__controls">
        <label>Mix grade
          <select value={grade} onChange={(e) => setGrade(e.target.value as MixGrade)}>
            <option value="M15">M15 (1:2:4)</option>
            <option value="M20">M20 (1:1.5:3)</option>
            <option value="M25">M25 (1:1:2)</option>
          </select>
        </label>
        <label>Wastage %<NumberCell label="Wastage %" value={wastage} onChange={setWastage} /></label>
      </div>
      <div className="bf-calc__rows">
        <div className="bf-calc__row bf-calc__row--head concrete">
          <span>Member</span><span>Type</span><span>Nos</span><span>L (m)</span><span>W (m)</span><span>D (m)</span><span>Steel %</span><span></span>
        </div>
        {rows.map((r) => (
          <div className="bf-calc__row concrete" key={r.key}>
            <input aria-label="Member name" value={r.name} onChange={(e) => patch(r.key, { name: e.target.value })} />
            <select aria-label="Member type" value={r.type} onChange={(e) => patch(r.key, { type: e.target.value as MemberType })}>
              {(["FOOTING", "COLUMN", "BEAM", "SLAB", "OTHER"] as MemberType[]).map((t) => (
                <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>
              ))}
            </select>
            <NumberCell label="Count" step="1" value={r.count} onChange={(v) => patch(r.key, { count: v })} />
            <NumberCell label="Length" value={r.l} onChange={(v) => patch(r.key, { l: v })} />
            <NumberCell label="Width" value={r.w} onChange={(v) => patch(r.key, { w: v })} />
            <NumberCell label="Depth" value={r.d} onChange={(v) => patch(r.key, { d: v })} />
            <NumberCell label="Auto" value={r.steel} onChange={(v) => patch(r.key, { steel: v })} />
            <button type="button" aria-label="Remove member" onClick={() => setRows((c) => (c.length > 1 ? c.filter((x) => x.key !== r.key) : c))}>×</button>
          </div>
        ))}
      </div>
      <div className="bf-calc__buttons">
        <button type="button" className="bf-calc__add" onClick={() => setRows((c) => [...c, { key: crypto.randomUUID(), name: "", type: "COLUMN", count: "1", l: "", w: "", d: "", steel: "" }])}>+ Add member</button>
        <Button onClick={calculate} isLoading={busy}>Calculate</Button>
      </div>
      {error && <div className="bf-calc__error">{error}</div>}
      {result && (
        <div className="bf-calc__result">
          <div className="bf-calc__stats">
            <Stat label="Concrete" value={n(result.concreteM3)} unit="m³" />
            <Stat label="Cement" value={n(result.cementBags)} unit="bags" />
            <Stat label="Sand" value={n(result.sandM3)} unit="m³" />
            <Stat label="Aggregate" value={n(result.aggregateM3)} unit="m³" />
            <Stat label="Steel" value={n(result.steelKg)} unit="kg" />
          </div>
          <table className="bf-table bf-calc__table">
            <thead><tr><th>Member</th><th>Nos</th><th>Volume (m³)</th><th>Steel %</th><th>Steel (kg)</th></tr></thead>
            <tbody>
              {result.members.map((m) => (
                <tr key={m.name}><td>{m.name}</td><td>{m.count}</td><td>{n(m.volumeM3)}</td><td>{m.steelPercent}</td><td>{n(m.steelKg)}</td></tr>
              ))}
            </tbody>
          </table>
          <Assumptions items={result.assumptions} />
        </div>
      )}
    </div>
  );
}

interface WallRow { key: string; name: string; l: string; h: string; t: string; open: string }

function MasonryCalculator() {
  const [unit, setUnit] = useState<UnitType>("RED_BRICK");
  const [mortar, setMortar] = useState("6");
  const [wastage, setWastage] = useState("5");
  const [rows, setRows] = useState<WallRow[]>([{ key: crypto.randomUUID(), name: "Front wall", l: "10", h: "3", t: "0.2", open: "3" }]);
  const [result, setResult] = useState<MasonryResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function patch(key: string, p: Partial<WallRow>) {
    setRows((cur) => cur.map((r) => (r.key === key ? { ...r, ...p } : r)));
  }

  async function calculate() {
    setError(null);
    for (const r of rows) {
      if (!r.name.trim() || !(Number(r.l) > 0) || !(Number(r.h) > 0) || !(Number(r.t) > 0)) {
        setError("Every wall needs a name and length, height and thickness above zero.");
        return;
      }
    }
    setBusy(true);
    try {
      setResult(
        await calculatorService.masonry({
          unitType: unit,
          mortarSandParts: Number(mortar) || 6,
          wastagePercent: Number(wastage) || 0,
          walls: rows.map((r) => ({
            name: r.name.trim(),
            lengthM: Number(r.l),
            heightM: Number(r.h),
            thicknessM: Number(r.t),
            openingsSqm: r.open ? Number(r.open) : undefined,
          })),
        }),
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="bf-calc__panel">
      <div className="bf-calc__controls">
        <label>Unit
          <select value={unit} onChange={(e) => setUnit(e.target.value as UnitType)}>
            <option value="RED_BRICK">Red brick</option>
            <option value="AAC_BLOCK">AAC block</option>
            <option value="SOLID_BLOCK">Solid concrete block</option>
          </select>
        </label>
        <label>Mortar 1 : <NumberCell label="Mortar sand parts" step="1" value={mortar} onChange={setMortar} /></label>
        <label>Wastage %<NumberCell label="Wastage %" value={wastage} onChange={setWastage} /></label>
      </div>
      <div className="bf-calc__rows">
        <div className="bf-calc__row bf-calc__row--head masonry">
          <span>Wall</span><span>Length (m)</span><span>Height (m)</span><span>Thickness (m)</span><span>Openings (m²)</span><span></span>
        </div>
        {rows.map((r) => (
          <div className="bf-calc__row masonry" key={r.key}>
            <input aria-label="Wall name" value={r.name} onChange={(e) => patch(r.key, { name: e.target.value })} />
            <NumberCell label="Length" value={r.l} onChange={(v) => patch(r.key, { l: v })} />
            <NumberCell label="Height" value={r.h} onChange={(v) => patch(r.key, { h: v })} />
            <NumberCell label="Thickness" value={r.t} onChange={(v) => patch(r.key, { t: v })} />
            <NumberCell label="Openings" value={r.open} onChange={(v) => patch(r.key, { open: v })} />
            <button type="button" aria-label="Remove wall" onClick={() => setRows((c) => (c.length > 1 ? c.filter((x) => x.key !== r.key) : c))}>×</button>
          </div>
        ))}
      </div>
      <div className="bf-calc__buttons">
        <button type="button" className="bf-calc__add" onClick={() => setRows((c) => [...c, { key: crypto.randomUUID(), name: "", l: "", h: "", t: "0.2", open: "" }])}>+ Add wall</button>
        <Button onClick={calculate} isLoading={busy}>Calculate</Button>
      </div>
      {error && <div className="bf-calc__error">{error}</div>}
      {result && (
        <div className="bf-calc__result">
          <div className="bf-calc__stats">
            <Stat label="Units" value={n(result.units)} unit="nos" />
            <Stat label="Mortar (dry)" value={n(result.mortarDryM3)} unit="m³" />
            <Stat label="Cement" value={n(result.cementBags)} unit="bags" />
            <Stat label="Sand" value={n(result.sandM3)} unit="m³" />
          </div>
          <table className="bf-table bf-calc__table">
            <thead><tr><th>Wall</th><th>Gross (m²)</th><th>Net (m²)</th><th>Volume (m³)</th><th>Units</th></tr></thead>
            <tbody>
              {result.walls.map((w) => (
                <tr key={w.name}><td>{w.name}</td><td>{n(w.grossAreaSqm)}</td><td>{n(w.netAreaSqm)}</td><td>{n(w.netVolumeM3)}</td><td>{n(w.units)}</td></tr>
              ))}
            </tbody>
          </table>
          <Assumptions items={result.assumptions} />
        </div>
      )}
    </div>
  );
}

interface BarRow { key: string; mark: string; dia: string; len: string; nos: string }

function SteelCalculator() {
  const [wastage, setWastage] = useState("3");
  const [stdLength, setStdLength] = useState("12");
  const [rows, setRows] = useState<BarRow[]>([{ key: crypto.randomUUID(), mark: "B1", dia: "12", len: "6", nos: "10" }]);
  const [result, setResult] = useState<SteelResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function patch(key: string, p: Partial<BarRow>) {
    setRows((cur) => cur.map((r) => (r.key === key ? { ...r, ...p } : r)));
  }

  async function calculate() {
    setError(null);
    for (const r of rows) {
      if (!r.mark.trim() || !(Number(r.dia) >= 6 && Number(r.dia) <= 40) || !(Number(r.len) > 0) || !(Number(r.nos) >= 1)) {
        setError("Every bar needs a mark, a diameter of 6 to 40 mm, a cut length above zero and at least 1 bar.");
        return;
      }
    }
    setBusy(true);
    try {
      setResult(
        await calculatorService.steel({
          wastagePercent: Number(wastage) || 0,
          standardBarLengthM: Number(stdLength) || 12,
          bars: rows.map((r) => ({ mark: r.mark.trim(), diameterMm: Number(r.dia), lengthM: Number(r.len), nos: Number(r.nos) })),
        }),
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="bf-calc__panel">
      <div className="bf-calc__controls">
        <label>Wastage %<NumberCell label="Wastage %" value={wastage} onChange={setWastage} /></label>
        <label>Standard bar length (m)<NumberCell label="Standard bar length" value={stdLength} onChange={setStdLength} /></label>
      </div>
      <div className="bf-calc__rows">
        <div className="bf-calc__row bf-calc__row--head steel">
          <span>Mark</span><span>Dia (mm)</span><span>Cut length (m)</span><span>Nos</span><span></span>
        </div>
        {rows.map((r) => (
          <div className="bf-calc__row steel" key={r.key}>
            <input aria-label="Bar mark" value={r.mark} onChange={(e) => patch(r.key, { mark: e.target.value })} />
            <NumberCell label="Diameter" step="1" value={r.dia} onChange={(v) => patch(r.key, { dia: v })} />
            <NumberCell label="Cut length" value={r.len} onChange={(v) => patch(r.key, { len: v })} />
            <NumberCell label="Nos" step="1" value={r.nos} onChange={(v) => patch(r.key, { nos: v })} />
            <button type="button" aria-label="Remove bar" onClick={() => setRows((c) => (c.length > 1 ? c.filter((x) => x.key !== r.key) : c))}>×</button>
          </div>
        ))}
      </div>
      <div className="bf-calc__buttons">
        <button type="button" className="bf-calc__add" onClick={() => setRows((c) => [...c, { key: crypto.randomUUID(), mark: "", dia: "12", len: "", nos: "1" }])}>+ Add bar</button>
        <Button onClick={calculate} isLoading={busy}>Calculate</Button>
      </div>
      {error && <div className="bf-calc__error">{error}</div>}
      {result && (
        <div className="bf-calc__result">
          <div className="bf-calc__stats">
            <Stat label="Total weight" value={n(result.totalKg)} unit="kg" />
            <Stat label={`With ${result.wastagePercent}% wastage`} value={n(result.totalKgWithWastage)} unit="kg" />
          </div>
          <table className="bf-table bf-calc__table">
            <thead><tr><th>Diameter</th><th>Total length (m)</th><th>Weight (kg)</th><th>Standard bars</th></tr></thead>
            <tbody>
              {result.byDiameter.map((d) => (
                <tr key={d.diameterMm}><td>{d.diameterMm} mm</td><td>{n(d.totalLengthM)}</td><td>{n(d.weightKg)}</td><td>{d.standardBarsRequired}</td></tr>
              ))}
            </tbody>
          </table>
          <Assumptions items={result.assumptions} />
        </div>
      )}
    </div>
  );
}

export function CalculatorsPage() {
  const [tab, setTab] = useState<Tab>("concrete");
  return (
    <div className="bf-calc">
      <div>
        <h1>Calculators</h1>
        <p className="bf-calc__intro">
          Working quantities for members and walls. Each result lists the assumptions it used so the numbers can be
          checked. These are estimating quantities, not structural design.
        </p>
      </div>
      <div className="bf-calc__tabs">
        {([["concrete", "Concrete & steel"], ["masonry", "Masonry"], ["steel", "Bar weights"]] as [Tab, string][]).map(([key, label]) => (
          <button key={key} className={`bf-calc__tab ${tab === key ? "bf-calc__tab--active" : ""}`} onClick={() => setTab(key)}>{label}</button>
        ))}
      </div>
      <Card>
        {tab === "concrete" && <ConcreteCalculator />}
        {tab === "masonry" && <MasonryCalculator />}
        {tab === "steel" && <SteelCalculator />}
      </Card>
    </div>
  );
}
