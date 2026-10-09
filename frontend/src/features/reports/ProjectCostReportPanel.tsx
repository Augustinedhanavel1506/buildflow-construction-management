import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Select } from "../../components/ui/Select";
import { Skeleton } from "../../components/ui/Skeleton";
import { StatCard } from "../../components/ui/StatCard";
import { useToast } from "../../components/ui/ToastProvider";
import { projectService } from "../../services/projectService";
import { reportService } from "../../services/reportService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import { expenseCategoryLabel } from "../expenses/categoryLabel";
import type { ExpenseCategory } from "../../types/expense";
import type { Project } from "../../types/project";
import type { ProjectCostReport } from "../../types/report";

export function ProjectCostReportPanel() {
  const { showToast } = useToast();
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [report, setReport] = useState<ProjectCostReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isExportingBoq, setIsExportingBoq] = useState(false);
  const [isExportingExpenses, setIsExportingExpenses] = useState(false);

  useEffect(() => {
    projectService
      .list(0, 100)
      .then((page) => {
        setProjects(page.content);
        if (page.content.length > 0) {
          setSelectedProjectId(page.content[0].id.toString());
        }
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  const loadReport = useCallback(async () => {
    if (!selectedProjectId) return;
    try {
      const data = await reportService.getProjectCostSummary(Number(selectedProjectId));
      setReport(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [selectedProjectId]);

  useEffect(() => {
    setReport(null);
    loadReport();
  }, [loadReport]);

  async function handleExportBoq() {
    setIsExportingBoq(true);
    try {
      await reportService.downloadBoqVarianceCsv(Number(selectedProjectId));
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsExportingBoq(false);
    }
  }

  async function handleExportExpenses() {
    setIsExportingExpenses(true);
    try {
      await reportService.downloadExpensesCsv(Number(selectedProjectId));
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsExportingExpenses(false);
    }
  }

  if (error) {
    return <div className="bf-reports-page__error">{error}</div>;
  }

  return (
    <div className="bf-reports-page__section">
      <div className="bf-reports-page__section-header">
        <h3>Project Cost Report</h3>
        {projects && projects.length > 0 && (
          <Select
            label="Project"
            value={selectedProjectId}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            className="bf-reports-page__project-select"
          >
            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </Select>
        )}
      </div>

      {projects === null || (projects.length > 0 && report === null) ? (
        <Skeleton height={280} />
      ) : projects.length === 0 ? (
        <Card>
          <EmptyState title="No projects yet." description="Create a project to generate its cost report." />
        </Card>
      ) : (
        report && (
          <div className="bf-reports-page__project-report">
            <div className="bf-reports-page__stats">
              <StatCard
                label="Contract Value"
                value={formatCurrency(report.revisedContractValue)}
                note={
                  report.revisedContractValue !== report.contractValue
                    ? `Original: ${formatCurrency(report.contractValue)}`
                    : undefined
                }
              />
              <StatCard label="Estimated Cost" value={formatCurrency(report.estimatedCost)} />
              <StatCard label="Actual Cost" value={formatCurrency(report.actualCost)} />
              <StatCard label="Estimated Final Cost" value={formatCurrency(report.estimatedFinalCost)} />
            </div>

            <Card>
              <div className="bf-reports-page__margin">
                <span>Estimated Project Margin</span>
                <strong className={report.estimatedMargin >= 0 ? "bf-reports-page__positive" : "bf-reports-page__negative"}>
                  {formatCurrency(report.estimatedMargin)}
                </strong>
              </div>
            </Card>

            <Card>
              <h4>BOQ Variance</h4>
              <div className="bf-reports-page__boq-variance">
                <div>
                  <span>Estimated</span>
                  <strong>{formatCurrency(report.boqVariance.totalEstimated)}</strong>
                </div>
                <div>
                  <span>Actual</span>
                  <strong>{formatCurrency(report.boqVariance.totalActual)}</strong>
                </div>
                <div>
                  <span>Variance</span>
                  <strong className={report.boqVariance.totalVariance > 0 ? "bf-reports-page__negative" : "bf-reports-page__positive"}>
                    {report.boqVariance.totalVariance > 0 ? "+" : ""}
                    {formatCurrency(report.boqVariance.totalVariance)}
                  </strong>
                </div>
              </div>
              <Button variant="secondary" onClick={handleExportBoq} isLoading={isExportingBoq}>
                Export BOQ Variance CSV
              </Button>
            </Card>

            <Card>
              <h4>Expense Breakdown</h4>
              {report.expenseBreakdown.length === 0 ? (
                <EmptyState title="No expenses recorded yet." description="" />
              ) : (
                <table className="bf-table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      <th>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.expenseBreakdown.map((entry) => (
                      <tr key={entry.category}>
                        <td>{expenseCategoryLabel(entry.category as ExpenseCategory)}</td>
                        <td>{formatCurrency(entry.amount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Button variant="secondary" onClick={handleExportExpenses} isLoading={isExportingExpenses}>
                Export Expenses CSV
              </Button>
            </Card>
          </div>
        )
      )}
    </div>
  );
}
