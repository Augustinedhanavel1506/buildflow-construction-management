import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { reportService } from "../../services/reportService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import { statusTone } from "../projects/statusTone";
import type { ProfitabilityReport } from "../../types/report";
import type { ProjectStatus } from "../../types/project";

export function ProfitabilityReportPanel() {
  const { showToast } = useToast();
  const [report, setReport] = useState<ProfitabilityReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await reportService.getProfitability();
      setReport(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function handleExport() {
    setIsExporting(true);
    try {
      await reportService.downloadProfitabilityCsv();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsExporting(false);
    }
  }

  if (error) {
    return <div className="bf-reports-page__error">{error}</div>;
  }

  return (
    <div className="bf-reports-page__section">
      <div className="bf-reports-page__section-header">
        <h3>Profitability Report</h3>
        {report && report.projects.length > 0 && (
          <Button variant="secondary" onClick={handleExport} isLoading={isExporting}>
            Export CSV
          </Button>
        )}
      </div>

      <Card>
        {report === null ? (
          <Skeleton height={200} />
        ) : report.projects.length === 0 ? (
          <EmptyState title="No projects yet." description="Profitability is calculated once you have active projects." />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Status</th>
                <th>Contract Value</th>
                <th>Estimated Final Cost</th>
                <th>Estimated Margin</th>
                <th>Margin %</th>
              </tr>
            </thead>
            <tbody>
              {report.projects.map((row) => (
                <tr key={row.projectId}>
                  <td>
                    <Link to={`/projects/${row.projectId}`}>{row.projectName}</Link>
                  </td>
                  <td>
                    <Badge tone={statusTone(row.status as ProjectStatus)}>{row.status.replace("_", " ")}</Badge>
                  </td>
                  <td>
                    {formatCurrency(row.revisedContractValue)}
                    {row.revisedContractValue !== row.contractValue && (
                      <div className="bf-reports-page__original-value">Original: {formatCurrency(row.contractValue)}</div>
                    )}
                  </td>
                  <td>{formatCurrency(row.estimatedFinalCost)}</td>
                  <td className={row.estimatedMargin >= 0 ? "bf-reports-page__positive" : "bf-reports-page__negative"}>
                    {formatCurrency(row.estimatedMargin)}
                  </td>
                  <td>{row.marginPercent}%</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td>
                  <strong>Total</strong>
                </td>
                <td></td>
                <td>
                  <strong>{formatCurrency(report.totalContractValue)}</strong>
                </td>
                <td>
                  <strong>{formatCurrency(report.totalEstimatedFinalCost)}</strong>
                </td>
                <td>
                  <strong>{formatCurrency(report.totalEstimatedMargin)}</strong>
                </td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        )}
      </Card>
    </div>
  );
}
