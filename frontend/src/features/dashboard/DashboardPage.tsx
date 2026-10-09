import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { Skeleton } from "../../components/ui/Skeleton";
import { StatCard } from "../../components/ui/StatCard";
import { useAuth } from "../auth/AuthContext";
import { dashboardService } from "../../services/dashboardService";
import { projectService } from "../../services/projectService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import { expenseCategoryLabel } from "../expenses/categoryLabel";
import { statusTone } from "../projects/statusTone";
import type { DashboardSummary } from "../../types/dashboard";
import type { ExpenseCategory } from "../../types/expense";
import type { Project } from "../../types/project";
import "./DashboardPage.css";

export function DashboardPage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([dashboardService.getSummary(), projectService.list(0, 5)])
      .then(([summaryData, projectsPage]) => {
        if (cancelled) return;
        setSummary(summaryData);
        setProjects(projectsPage.content);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err));
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const isLoading = summary === null || projects === null;
  const maxCategoryAmount = summary
    ? Math.max(1, ...summary.costBreakdown.map((entry) => entry.amount))
    : 1;

  return (
    <div className="bf-dashboard">
      <div>
        <h1>Good morning, {user?.fullName.split(" ")[0]} 👋</h1>
        <p className="bf-dashboard__subtitle">Here's what's happening with your projects today.</p>
      </div>

      {error && <div className="bf-dashboard__error">{error}</div>}

      <div className="bf-dashboard__stats">
        {isLoading ? (
          <>
            <Skeleton height={92} />
            <Skeleton height={92} />
            <Skeleton height={92} />
            <Skeleton height={92} />
          </>
        ) : (
          <>
            <StatCard label="Total Projects" value={summary.projectCounts.totalProjects} to="/projects" />
            <StatCard label="In Progress" value={summary.projectCounts.activeProjects} to="/projects?filter=active" />
            <StatCard label="Delayed" value={summary.projectCounts.delayedProjects} to="/projects?filter=delayed" />
            <StatCard label="Completed" value={summary.projectCounts.completedProjects} to="/projects?filter=completed" />
          </>
        )}
      </div>

      {!isLoading && summary.projectCounts.delayedProjects > 0 && (
        <div className="bf-dashboard__alert">
          ⚠ {summary.projectCounts.delayedProjects} project{summary.projectCounts.delayedProjects > 1 ? "s" : ""}{" "}
          past their expected end date
        </div>
      )}

      {!isLoading && summary.billingSummary.totalOutstanding > 0 && (
        <div className="bf-dashboard__alert">
          ⚠ {formatCurrency(summary.billingSummary.totalOutstanding)} outstanding from clients across certified bills
        </div>
      )}

      {!isLoading && summary.estimatedMaterialVarianceValue < 0 && (
        <div className="bf-dashboard__alert">
          ⚠ {formatCurrency(Math.abs(summary.estimatedMaterialVarianceValue))} in estimated material shortage across
          physical stock counts — check the Reconciliation tab under Materials
        </div>
      )}

      <div>
        <h3 className="bf-dashboard__subsection-title">Cash Flow</h3>
        <div className="bf-dashboard__stats">
          {isLoading ? (
            <>
              <Skeleton height={92} />
              <Skeleton height={92} />
              <Skeleton height={92} />
              <Skeleton height={92} />
            </>
          ) : (
            <>
              <StatCard label="Total Billed" value={formatCurrency(summary.billingSummary.totalBilled)} />
              <StatCard label="Total Received" value={formatCurrency(summary.billingSummary.totalReceived)} />
              <StatCard
                label="Outstanding"
                value={
                  <span className={summary.billingSummary.totalOutstanding > 0 ? "bf-dashboard__negative" : undefined}>
                    {formatCurrency(summary.billingSummary.totalOutstanding)}
                  </span>
                }
              />
              <StatCard label="Retention Held" value={formatCurrency(summary.billingSummary.totalRetentionHeld)} />
            </>
          )}
        </div>
      </div>

      <div className="bf-dashboard__progress-row">
        <Card>
          <div className="bf-dashboard__progress-header">
            <span>Budget Utilization</span>
            <span>{isLoading ? "—" : `${summary.financialSummary.budgetUtilizationPercent}%`}</span>
          </div>
          {isLoading ? <Skeleton height={8} /> : <ProgressBar value={summary.financialSummary.budgetUtilizationPercent} />}
        </Card>
        <Card>
          <div className="bf-dashboard__progress-header">
            <span>Overall Project Progress</span>
            <span>{isLoading ? "—" : `${summary.overallProgress}%`}</span>
          </div>
          {isLoading ? <Skeleton height={8} /> : <ProgressBar value={summary.overallProgress} />}
        </Card>
      </div>

      <div>
        <h3 className="bf-dashboard__subsection-title">Project Costs</h3>
        <div className="bf-dashboard__stats">
          {isLoading ? (
            <>
              <Skeleton height={92} />
              <Skeleton height={92} />
              <Skeleton height={92} />
            </>
          ) : (
            <>
              <StatCard label="Contract Value" value={formatCurrency(summary.financialSummary.totalContractValue)} />
              <StatCard label="Estimated Cost" value={formatCurrency(summary.financialSummary.totalEstimatedCost)} />
              <StatCard label="Actual Cost" value={formatCurrency(summary.financialSummary.totalActualCost)} />
            </>
          )}
        </div>
      </div>

      <div className="bf-dashboard__panels">
        <Card>
          <h3>Cost Breakdown</h3>
          {isLoading ? (
            <Skeleton height={140} />
          ) : summary.costBreakdown.length === 0 ? (
            <EmptyState title="No expenses recorded yet." description="Cost breakdown appears once expenses are logged." />
          ) : (
            <div className="bf-dashboard__breakdown">
              {summary.costBreakdown.map((entry) => (
                <div key={entry.category} className="bf-dashboard__breakdown-row">
                  <span className="bf-dashboard__breakdown-label">
                    {expenseCategoryLabel(entry.category as ExpenseCategory)}
                  </span>
                  <div className="bf-dashboard__breakdown-bar">
                    <div
                      className="bf-dashboard__breakdown-bar-fill"
                      style={{ width: `${(entry.amount / maxCategoryAmount) * 100}%` }}
                    />
                  </div>
                  <span className="bf-dashboard__breakdown-value">{formatCurrency(entry.amount)}</span>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card>
          <h3>Recent Activity</h3>
          {isLoading ? (
            <Skeleton height={140} />
          ) : summary.recentActivity.length === 0 ? (
            <EmptyState title="No recent activity." description="Activity from expenses and daily reports will appear here." />
          ) : (
            <ul className="bf-dashboard__activity">
              {summary.recentActivity.map((item, index) => (
                <li key={index}>
                  <span className="bf-dashboard__activity-desc">{item.description}</span>
                  <span className="bf-dashboard__activity-meta">
                    {item.projectName} · {formatDate(item.occurredAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card>
        <div className="bf-dashboard__section-header">
          <h3>Recent Projects</h3>
          <Link to="/projects">View all</Link>
        </div>

        {projects === null ? (
          <Skeleton height={160} />
        ) : projects.length === 0 ? (
          <EmptyState
            title="No projects yet."
            description="Create your first project to start tracking construction costs and progress."
            action={
              <Link to="/projects" className="bf-dashboard__cta">
                + Create Project
              </Link>
            }
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Client</th>
                <th>Contract Value</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {projects.map((project) => (
                <tr key={project.id}>
                  <td>
                    <Link to={`/projects/${project.id}`}>{project.name}</Link>
                  </td>
                  <td>{project.clientName ?? "—"}</td>
                  <td>{formatCurrency(project.contractValue)}</td>
                  <td>
                    <Badge tone={statusTone(project.status)}>{project.status.replace("_", " ")}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
