import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Button } from "../../../components/ui/Button";
import { Skeleton } from "../../../components/ui/Skeleton";
import { businessService } from "../../../services/businessService";
import { estimationService } from "../../../services/estimationService";
import { getErrorMessage } from "../../../utils/apiError";
import type { Business } from "../../../types/business";
import type { HouseRequirement } from "../../../types/estimation";
import type { PlansResponse } from "../../../types/plan";
import { floorLabel } from "../labels";
import { PlanSheetDrawing } from "./PlanSheetDrawing";
import { ELEVATION_TITLES, ElevationDrawing, elevationFrame, type ElevationStyle, type ElevationView } from "./ElevationDrawing";
import { chooseScaleIn } from "./sheetGeometry";
import { roomLabel } from "./planUtils";
import "./PlanSheetPage.css";

// A printable drawing sheet: one page per floor with a title block, the plan, and a room schedule. "Print / Save as
// PDF" uses the browser print dialog, so nothing is uploaded anywhere.
export function PlanSheetPage() {
  const { id } = useParams<{ id: string }>();
  const requirementId = Number(id);
  const [requirement, setRequirement] = useState<HouseRequirement | null>(null);
  const [plans, setPlans] = useState<PlansResponse | null>(null);
  const [business, setBusiness] = useState<Business | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [look, setLook] = useState<Partial<ElevationStyle>>({});

  useEffect(() => {
    Promise.all([
      estimationService.get(requirementId),
      estimationService.getPlans(requirementId),
      businessService.get().catch(() => null),
      estimationService.getHouseStyle(requirementId).catch(() => ({})),
    ])
      .then(([req, planData, biz, style]) => {
        setLook(style as Partial<ElevationStyle>);
        setRequirement(req);
        setPlans(planData);
        setBusiness(biz);
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, [requirementId]);

  if (error) return <div className="bf-sheet__error">{error}</div>;
  if (!requirement || !plans) return <Skeleton height={520} />;

  const drawn = plans.floors.filter((f) => f.rooms.length > 0);
  const today = new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });

  return (
    <div className="bf-sheet">
      <div className="bf-sheet__controls">
        <Link to={`/estimation/${requirementId}/plan`} className="bf-sheet__back">← Back to plan</Link>
        <Button onClick={() => window.print()} disabled={drawn.length === 0}>Print / Save as PDF</Button>
      </div>
      {drawn.length === 0 && <p>Draw and save a floor plan first; the drawing sheet is built from it.</p>}

      {drawn.map((floor, index) => {
        const area = floor.rooms.reduce((sum, r) => sum + r.widthFt * r.depthFt, 0);
        return (
          <section key={floor.floorLevel} className="bf-sheet__page">
            <header className="bf-sheet__title">
              <div>
                <h1>{floorLabel(floor.floorLevel)} plan</h1>
                <p>{requirement.label}</p>
              </div>
              <dl>
                <div><dt>Plot</dt><dd>{plans.plotWidthFt} × {plans.plotLengthFt} ft</dd></div>
                <div><dt>Floor area</dt><dd>{Math.round(area).toLocaleString("en-IN")} sq.ft</dd></div>
                <div><dt>Sheet</dt><dd>{index + 1} of {drawn.length + 1}</dd></div>
                <div><dt>Date</dt><dd>{today}</dd></div>
                <div><dt>Prepared by</dt><dd>{business?.name ?? "—"}</dd></div>
              </dl>
            </header>

            <div className="bf-sheet__body">
              <figure className="bf-sheet__drawing">
                <PlanSheetDrawing floor={floor} />
                <figcaption>North (road side) at the top. Plot {plans.plotWidthFt} × {plans.plotLengthFt} ft.</figcaption>
              </figure>

              <table className="bf-sheet__schedule">
                <thead>
                  <tr><th>Room</th><th>Size (ft)</th><th>Area (sq.ft)</th><th>Doors</th><th>Windows</th></tr>
                </thead>
                <tbody>
                  {floor.rooms.map((r) => (
                    <tr key={r.id}>
                      <td>{roomLabel(r)}</td>
                      <td>{r.widthFt} × {r.depthFt}</td>
                      <td>{Math.round(r.widthFt * r.depthFt)}</td>
                      <td>{floor.openings.filter((o) => o.roomId === r.id && o.kind === "DOOR").length}</td>
                      <td>{floor.openings.filter((o) => o.roomId === r.id && o.kind === "WINDOW").length}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr><td colSpan={2}>Total</td><td>{Math.round(area)}</td><td>{floor.metrics.doors}</td><td>{floor.metrics.windows}</td></tr>
                </tfoot>
              </table>
            </div>

            <footer className="bf-sheet__note">
              Concept plan for discussion. Not a sanctioned, structural or construction drawing; to be reviewed and approved
              by a licensed architect or engineer.
            </footer>
          </section>
        );
      })}

      {drawn.length > 0 && (() => {
        const style: ElevationStyle = {
          exterior: look.exterior ?? "#efe6d6",
          roof: look.roof ?? "#a0522d",
          roofStyle: look.roofStyle ?? "SLOPED",
        };
        const views: ElevationView[] = ["FRONT", "REAR", "LEFT", "RIGHT"];
        // One scale for all four views, so they can be compared side by side.
        const scale = Math.max(...views.map((v) => {
          const fr = elevationFrame(drawn, v, style);
          return chooseScaleIn(fr.width + 15, fr.height + 11, 128, 52);
        }));
        return (
          <section className="bf-sheet__page">
            <header className="bf-sheet__title">
              <div>
                <h1>Elevations</h1>
                <p>{requirement.label}</p>
              </div>
              <dl>
                <div><dt>Scale</dt><dd>1:{scale}</dd></div>
                <div><dt>Storeys</dt><dd>{drawn.length}</dd></div>
                <div><dt>Sheet</dt><dd>{drawn.length + 1} of {drawn.length + 1}</dd></div>
                <div><dt>Date</dt><dd>{today}</dd></div>
                <div><dt>Prepared by</dt><dd>{business?.name ?? "—"}</dd></div>
              </dl>
            </header>
            <div className="bf-sheet__elevations">
              {views.map((v) => (
                <figure key={v} className="bf-sheet__elevation">
                  <ElevationDrawing floors={drawn} view={v} style={style} scale={scale} />
                  <figcaption>{ELEVATION_TITLES[v]}</figcaption>
                </figure>
              ))}
            </div>
            <footer className="bf-sheet__note">
              Concept elevations drawn from the floor plans at a storey height of 10 ft. Colours are those chosen in the 3D home
              view. Not a sanctioned or construction drawing; to be reviewed by a licensed architect or engineer.
            </footer>
          </section>
        );
      })()}
    </div>
  );
}
