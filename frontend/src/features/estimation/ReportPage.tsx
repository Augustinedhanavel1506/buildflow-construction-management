import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Skeleton } from "../../components/ui/Skeleton";
import { boqService } from "../../services/boqService";
import { businessService } from "../../services/businessService";
import { estimationService } from "../../services/estimationService";
import { estimationRuleService } from "../../services/estimationRuleService";
import { projectService } from "../../services/projectService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { Business } from "../../types/business";
import type { BoqItem } from "../../types/boq";
import type { HouseRequirement } from "../../types/estimation";
import type { BoqGenerationRule } from "../../types/estimationRule";
import type { PlansResponse } from "../../types/plan";
import type { SiteCheck } from "../../types/siteRules";
import { SiteCheckSummary } from "./SiteCheckSummary";
import { ComponentCostBreakdown } from "./ComponentCostBreakdown";
import {
  GRADE_LABEL,
  ROOF_TYPE_LABEL,
  STRUCTURE_LABEL,
  WALL_MATERIAL_LABEL,
  componentLabel,
  estimateSourceLabel,
  floorLabel,
} from "./labels";
import { PlanCanvas } from "./plan/PlanCanvas";
import { DEFAULT_COLOURS, DEFAULT_ROOM_COLOURS, type HouseColours } from "./view3d/houseModel";
import "./ReportPage.css";

export function ReportPage() {
  const { id } = useParams<{ id: string }>();
  const requirementId = Number(id);

  const [requirement, setRequirement] = useState<HouseRequirement | null>(null);
  const [items, setItems] = useState<BoqItem[] | null>(null);
  const [plans, setPlans] = useState<PlansResponse | null>(null);
  const [business, setBusiness] = useState<Business | null>(null);
  const [rules, setRules] = useState<BoqGenerationRule[]>([]);
  const [siteCheck, setSiteCheck] = useState<SiteCheck | null>(null);
  const [look, setLook] = useState<Partial<HouseColours> | null>(null);
  const [estimatedCost, setEstimatedCost] = useState(0);
  const [preparedFor, setPreparedFor] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const req = await estimationService.get(requirementId);
        setRequirement(req);
        const [project, boq, planData, biz] = await Promise.all([
          projectService.get(req.projectId),
          boqService.list(req.projectId),
          estimationService.getPlans(requirementId),
          businessService.get().catch(() => null),
        ]);
        setEstimatedCost(project.estimatedCost);
        setItems(boq);
        setPlans(planData);
        setBusiness(biz);
        estimationRuleService.list().then(setRules).catch(() => setRules([]));
        estimationService.getHouseStyle(requirementId).then((s) => setLook(Object.keys(s).length > 0 ? (s as Partial<HouseColours>) : null)).catch(() => setLook(null));
        estimationService.getSiteCheck(requirementId).then(setSiteCheck).catch(() => setSiteCheck(null));
      } catch (err) {
        setError(getErrorMessage(err));
      }
    })();
  }, [requirementId]);

  const summary = useMemo(() => {
    if (!items) return null;
    const ruleByCode = new Map<string, BoqGenerationRule>();
    for (const r of rules) {
      ruleByCode.set(r.ruleCode, r);
      ruleByCode.set(r.baseRuleCode, r);
    }
    const validated = items.filter((i) => i.estimateSource === "ENGINEER_VALIDATED");
    const engineers = [...new Set(validated.map((i) => i.validatedBy).filter(Boolean))] as string[];
    const generated = items.filter((i) => i.sourceRuleCode);
    const unverified = generated.filter((i) => {
      const rule = ruleByCode.get(i.sourceRuleCode!);
      return rule ? !rule.verified : false;
    });
    const total = items.reduce((sum, i) => sum + i.estimatedAmount, 0);
    return { validated, engineers, unverified, generated, total };
  }, [items, rules]);

  if (error) {
    return <div className="bf-report__error">{error}</div>;
  }
  if (!requirement || !items || !plans || !summary) {
    return <Skeleton height={500} />;
  }
  if (items.length === 0) {
    return (
      <div className="bf-report__empty">
        <p>There is no estimate to report on yet.</p>
        <Link to={`/estimation/${requirementId}`}>Generate the estimate first</Link>
      </div>
    );
  }

  const totalArea = requirement.floors.reduce((s, f) => s + f.floorAreaSqft, 0);
  const costPerSqft = totalArea > 0 ? Math.round(estimatedCost / totalArea) : 0;
  const drawnFloors = plans.floors.filter((f) => f.rooms.length > 0);
  const allValidated = summary.validated.length === items.length;

  return (
    <div className="bf-report">
      <div className="bf-report__controls">
        <Link to={`/estimation/${requirementId}`} className="bf-report__back">← Back to estimate</Link>
        <label>
          Prepared for
          <input value={preparedFor} onChange={(e) => setPreparedFor(e.target.value)} placeholder="Client name (optional)" />
        </label>
        <Button onClick={() => window.print()}>Print / Save as PDF</Button>
      </div>

      <article className="bf-report__sheet">
        <header className="bf-report__head">
          <div>
            <div className="bf-report__business">{business?.name ?? "Construction estimate"}</div>
            <div className="bf-report__contact">
              {[business?.address, business?.phone, business?.gstin ? `GSTIN ${business.gstin}` : null].filter(Boolean).join(" · ")}
            </div>
          </div>
          <div className="bf-report__meta">
            <div>{formatDate(new Date().toISOString())}</div>
            {preparedFor && <div>Prepared for <strong>{preparedFor}</strong></div>}
          </div>
        </header>

        <h1 className="bf-report__title">Preliminary construction estimate</h1>
        <p className="bf-report__subtitle">
          {requirement.label}
          {requirement.location ? ` · ${requirement.location}` : ""}
        </p>

        <section className="bf-report__tiles">
          <div className="bf-report__tile bf-report__tile--hero">
            <span>Estimated cost</span>
            <strong>{formatCurrency(estimatedCost)}</strong>
            <small>{costPerSqft ? `about ${formatCurrency(costPerSqft)} per sq.ft` : ""}</small>
          </div>
          <div className="bf-report__tile"><span>Built-up area</span><strong>{totalArea.toLocaleString("en-IN")} sq.ft</strong><small>{requirement.floors.length} {requirement.floors.length === 1 ? "floor" : "floors"}</small></div>
          <div className="bf-report__tile"><span>Plot</span><strong>{requirement.plotWidthFt} × {requirement.plotLengthFt} ft</strong><small>{(requirement.plotWidthFt * requirement.plotLengthFt).toLocaleString("en-IN")} sq.ft</small></div>
          <div className="bf-report__tile"><span>Finish</span><strong>{GRADE_LABEL[requirement.constructionGrade]}</strong><small>{STRUCTURE_LABEL[requirement.structureType]}</small></div>
        </section>

        <section className="bf-report__section">
          <h2>Specification</h2>
          <dl className="bf-report__spec">
            <div><dt>Structure</dt><dd>{STRUCTURE_LABEL[requirement.structureType]}</dd></div>
            <div><dt>Walls</dt><dd>{WALL_MATERIAL_LABEL[requirement.wallMaterial]}</dd></div>
            <div><dt>Roof</dt><dd>{ROOF_TYPE_LABEL[requirement.roofType]}</dd></div>
          </dl>
          <ul className="bf-report__floors">
            {requirement.floors.map((f) => {
              const rooms = [f.hasKitchen && "Kitchen", f.hasHall && "Hall", f.hasBalcony && "Balcony", f.hasPoojaRoom && "Pooja room"].filter(Boolean);
              return (
                <li key={f.id}>
                  <strong>{floorLabel(f.floorLevel)}</strong> {f.floorAreaSqft.toLocaleString("en-IN")} sq.ft · {f.bedroomCount} {f.bedroomCount === 1 ? "bedroom" : "bedrooms"} · {f.bathroomCount} {f.bathroomCount === 1 ? "bathroom" : "bathrooms"}
                  {rooms.length ? ` · ${rooms.join(", ")}` : ""}
                </li>
              );
            })}
          </ul>
        </section>

        {drawnFloors.length > 0 && (
          <section className="bf-report__section">
            <h2>Floor plans</h2>
            <p className="bf-report__note">Concept plans for discussion; not sanctioned or structural drawings.</p>
            <div className="bf-report__plans">
              {drawnFloors.map((f) => (
                <figure key={f.floorLevel} className="bf-report__plan">
                  <PlanCanvas plotWidth={plans.plotWidthFt} plotDepth={plans.plotLengthFt} rooms={f.rooms} openings={f.openings} columns={f.columns} beams={f.beams} furniture={f.furniture} envelope={siteCheck?.buildableEnvelope} readOnly />
                  <figcaption>{floorLabel(f.floorLevel)} · {Math.round(f.metrics.builtUpAreaSqft).toLocaleString("en-IN")} sq.ft</figcaption>
                </figure>
              ))}
            </div>
          </section>
        )}

        {look && drawnFloors.length > 0 && plans && (
          <section className="bf-report__section">
            <h2>Colours and finish</h2>
            <p className="bf-report__note">Chosen in the 3D home view; indicative shades, final paint and roofing to be confirmed.</p>
            <ul className="bf-report__swatches">
              <li><span style={{ background: look.exterior ?? DEFAULT_COLOURS.exterior }} />Outside walls</li>
              <li><span style={{ background: look.roof ?? DEFAULT_COLOURS.roof }} />Roof ({(look.roofStyle ?? DEFAULT_COLOURS.roofStyle) === "FLAT" ? "flat" : "sloped"})</li>
              {drawnFloors.flatMap((f) => f.rooms).map((r) => (
                <li key={r.id}><span style={{ background: look.rooms?.[r.id] ?? DEFAULT_ROOM_COLOURS[r.type] ?? "#ececec" }} />{r.name || r.type} (inside)</li>
              ))}
            </ul>
          </section>
        )}

        {siteCheck && (
          <section className="bf-report__section">
            <h2>Site check</h2>
            <SiteCheckSummary check={siteCheck} />
          </section>
        )}

        <section className="bf-report__section">
          <h2>Where the money goes</h2>
          <ComponentCostBreakdown items={items} />
        </section>

        <section className="bf-report__section">
          <h2>Bill of quantities</h2>
          <table className="bf-report__table">
            <thead>
              <tr><th>Item</th><th>Part of building</th><th className="num">Quantity</th><th className="num">Rate</th><th className="num">Amount</th><th>Status</th></tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{item.itemName}</td>
                  <td>{componentLabel(item.component)}</td>
                  <td className="num">{item.quantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {item.unit}</td>
                  <td className="num">{formatCurrency(item.rate)}</td>
                  <td className="num">{formatCurrency(item.estimatedAmount)}</td>
                  <td>{estimateSourceLabel(item.estimateSource)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr><td colSpan={4}>Total</td><td className="num">{formatCurrency(summary.total)}</td><td></td></tr>
            </tfoot>
          </table>
        </section>

        <section className="bf-report__section bf-report__status">
          <h2>Review status</h2>
          {summary.validated.length > 0 ? (
            <p>
              {allValidated ? "All" : `${summary.validated.length} of ${items.length}`} quantity lines have been reviewed and
              validated by {summary.engineers.join("; ")}.
              {!allValidated && " The remaining lines are preliminary."}
            </p>
          ) : (
            <p>These quantities are preliminary. They have not yet been reviewed by an engineer.</p>
          )}
          {summary.unverified.length > 0 && (
            <p className="bf-report__warning">
              {summary.unverified.length} of {summary.generated.length} estimated lines use placeholder coefficients that have
              not been checked against a published schedule or completed projects, so treat their quantities as indicative.
            </p>
          )}
        </section>

        <section className="bf-report__section">
          <h2>Notes</h2>
          <ul className="bf-report__notes">
            <li>Costs use the business's current material and labour rates and exclude land, approvals, taxes and fittings not listed.</li>
            <li>Quantities are estimates from standard coefficients. Actual quantities depend on the final structural design, soil conditions and site practice.</li>
            <li>This is not a substitute for an engineer's design, a quantity survey or a contractor's quotation.</li>
          </ul>
        </section>

        <footer className="bf-report__signatures">
          <div><span /> Prepared by</div>
          <div><span /> Reviewed by (engineer, registration no.)</div>
          <div><span /> Accepted by</div>
        </footer>
      </article>
    </div>
  );
}
