import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { projectService } from "../../services/projectService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { Project, ProjectPayload } from "../../types/project";
import { ProjectForm } from "./ProjectForm";
import { statusTone } from "./statusTone";
import "./ProjectsListPage.css";

const OPEN_STATUSES = new Set(["PLANNING", "ACTIVE", "ON_HOLD"]);

const FILTER_LABELS: Record<string, string> = {
  active: "In Progress",
  delayed: "Delayed",
  completed: "Completed",
};

export function ProjectsListPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const canManageProjects = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";
  const filter = searchParams.get("filter");

  const visibleProjects = useMemo(() => {
    if (!projects || !filter) return projects;
    const today = new Date().toISOString().slice(0, 10);

    if (filter === "active") return projects.filter((p) => p.status === "ACTIVE");
    if (filter === "completed") return projects.filter((p) => p.status === "COMPLETED");
    if (filter === "delayed") {
      return projects.filter(
        (p) => OPEN_STATUSES.has(p.status) && p.expectedEndDate !== null && p.expectedEndDate < today,
      );
    }
    return projects;
  }, [projects, filter]);

  const loadProjects = useCallback(async () => {
    try {
      const page = await projectService.list(0, 50);
      setProjects(page.content);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  async function handleCreate(payload: ProjectPayload) {
    await projectService.create(payload);
    setIsCreateOpen(false);
    showToast("success", "Project created successfully.");
    await loadProjects();
  }

  return (
    <div className="bf-projects-page">
      <div className="bf-projects-page__header">
        <div>
          <h1>Projects</h1>
          <p>Track every project's budget, progress, and profitability.</p>
        </div>
        {canManageProjects && <Button onClick={() => setIsCreateOpen(true)}>+ New Project</Button>}
      </div>

      {filter && FILTER_LABELS[filter] && (
        <div className="bf-projects-page__filter-chip">
          Filtered: {FILTER_LABELS[filter]}
          <button onClick={() => setSearchParams({})}>Clear</button>
        </div>
      )}

      {error && <div className="bf-projects-page__error">{error}</div>}

      <Card>
        {projects === null ? (
          <Skeleton height={240} />
        ) : visibleProjects && visibleProjects.length === 0 ? (
          <EmptyState
            title={filter ? `No ${FILTER_LABELS[filter]?.toLowerCase() ?? ""} projects.` : "No projects yet."}
            description={
              filter
                ? "Try clearing the filter to see all projects."
                : "Create your first project to start tracking construction costs and progress."
            }
            action={
              canManageProjects && !filter && (
                <Button onClick={() => setIsCreateOpen(true)}>+ Create Project</Button>
              )
            }
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Project Name</th>
                <th>Client</th>
                <th>Location</th>
                <th>Progress</th>
                <th>Contract Value</th>
                <th>Actual Cost</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {visibleProjects?.map((project) => (
                <tr key={project.id}>
                  <td>
                    <Link to={`/projects/${project.id}`}>{project.name}</Link>
                  </td>
                  <td>{project.clientName ?? "—"}</td>
                  <td>{project.location ?? "—"}</td>
                  <td className="bf-projects-page__progress-cell">
                    <ProgressBar value={project.overallProgress} />
                    <span>{project.overallProgress}%</span>
                  </td>
                  <td>{formatCurrency(project.contractValue)}</td>
                  <td>{formatCurrency(project.actualCost)}</td>
                  <td>
                    <Badge tone={statusTone(project.status)}>{project.status.replace("_", " ")}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isCreateOpen && (
        <Modal title="New Project" onClose={() => setIsCreateOpen(false)}>
          <ProjectForm onSubmit={handleCreate} onCancel={() => setIsCreateOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
