import { ProfitabilityReportPanel } from "./ProfitabilityReportPanel";
import { ProjectCostReportPanel } from "./ProjectCostReportPanel";
import { TallyExportPanel } from "./TallyExportPanel";
import "./ReportsPage.css";

export function ReportsPage() {
  return (
    <div className="bf-reports-page">
      <div>
        <h1>Reports</h1>
        <p>Understand project cost, variance, and profitability across your business.</p>
      </div>

      <ProfitabilityReportPanel />
      <ProjectCostReportPanel />
      <TallyExportPanel />
    </div>
  );
}
