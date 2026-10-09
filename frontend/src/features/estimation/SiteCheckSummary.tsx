import { Badge } from "../../components/ui/Badge";
import type { SiteCheck } from "../../types/siteRules";
import { floorLabel } from "./labels";
import "./SiteCheckSummary.css";

function status(measure: SiteCheck["coverage"], suffix: string) {
  if (measure.status === "NOT_SET") return <Badge>Limit not set</Badge>;
  return (
    <Badge tone={measure.status === "OK" ? "success" : "danger"}>
      {measure.status === "OK" ? "Within" : "Over"} {measure.limit}
      {suffix}
    </Badge>
  );
}

// Compact result of the site check, shared by the estimate page and the client report.
export function SiteCheckSummary({ check }: { check: SiteCheck }) {
  const sb = check.setbacks;
  return (
    <div className="bf-site-summary">
      <div className="bf-site-summary__grid">
        <div>
          <span>Setbacks</span>
          <strong>
            F {sb.frontFt} · R {sb.rearFt} · L {sb.leftFt} · R {sb.rightFt} ft
          </strong>
          <small>{[sb.frontAssumed, sb.rearAssumed, sb.leftAssumed, sb.rightAssumed].some(Boolean) ? "some are planning defaults" : "as entered"}</small>
        </div>
        <div>
          <span>Ground coverage</span>
          <strong>{check.coverage.value.toFixed(1)}%</strong>
          {status(check.coverage, "%")}
        </div>
        <div>
          <span>Floor area ratio</span>
          <strong>{check.far.value.toFixed(2)}</strong>
          {status(check.far, "")}
        </div>
        <div>
          <span>Result</span>
          <Badge tone={check.compliant ? "success" : "danger"}>{check.compliant ? "No issues found" : "Needs attention"}</Badge>
        </div>
      </div>
      {check.violations.length > 0 && (
        <ul className="bf-site-summary__issues">
          {check.violations.map((v, i) => (
            <li key={i}>
              {floorLabel(v.floorLevel)}: {v.roomName} {v.message}
            </li>
          ))}
        </ul>
      )}
      <p className="bf-site-summary__note">
        {check.notes[check.notes.length - 1]}
        {!check.fullyDrawn && " Floors without a drawn plan are counted by their checklist area."}
      </p>
    </div>
  );
}
