import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { Modal } from "../../components/ui/Modal";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { projectService } from "../../services/projectService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { Project, ProjectPayload } from "../../types/project";
import { BillingPanel } from "../billing/BillingPanel";
import { BoqPanel } from "../boq/BoqPanel";
import { DailyReportsPanel } from "../dailyReports/DailyReportsPanel";
import { ExpensePanel } from "../expenses/ExpensePanel";
import { LabourPanel } from "../labour/LabourPanel";
import { MaterialsPanel } from "../materials/MaterialsPanel";
import { ProgressPanel } from "../progress/ProgressPanel";
import { WorkOrdersPanel } from "../subcontractors/WorkOrdersPanel";
import { VariationsPanel } from "../variations/VariationsPanel";
import { ProjectForm } from "./ProjectForm";
import { statusTone } from "./statusTone";
import "./ProjectDetailPage.css";

type ProjectTab =
  | "overview"
  | "boq"
  | "materials"
  | "labour"
  | "expenses"
  | "dailyReports"
  | "progress"
  | "variations"
  | "billing"
  | "subcontractors";

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showToast } = useToast();

  const [project, setProject] = useState<Project | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isArchiveOpen, setIsArchiveOpen] = useState(false);
  const [isArchiving, setIsArchiving] = useState(false);
  const [activeTab, setActiveTab] = useState<ProjectTab>("overview");

  const canManageProjects = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const loadProject = useCallback(async () => {
    try {
      const data = await projectService.get(projectId);
      setProject(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadProject();
  }, [loadProject]);

  async function handleUpdate(payload: ProjectPayload) {
    await projectService.update(projectId, payload);
    setIsEditOpen(false);
    showToast("success", "Project updated successfully.");
    await loadProject();
  }

  async function handleArchive() {
    setIsArchiving(true);
    try {
      await projectService.archive(projectId);
      showToast("success", "Project archived successfully.");
      navigate("/projects");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsArchiving(false);
      setIsArchiveOpen(false);
    }
  }

  if (error) {
    return <div className="bf-project-detail__error">{error}</div>;
  }

  if (!project) {
    return <Skeleton height={320} />;
  }

  // Estimated Final Cost = Actual Cost + Remaining Estimated Cost, where the remaining
  // estimate is whatever part of the original budget hasn't been spent yet (floored at 0).
  const estimatedFinalCost = Math.max(Number(project.actualCost), Number(project.estimatedCost));
  const estimatedMargin = Number(project.revisedContractValue) - estimatedFinalCost;
  const hasApprovedVariations = Number(project.revisedContractValue) !== Number(project.contractValue);

  return (
    <div className="bf-project-detail">
      <div className="bf-project-detail__header">
        <div>
          <Link to="/projects" className="bf-project-detail__back">
            ← Projects
          </Link>
          <h1>{project.name}</h1>
          <p>
            {project.clientName && <span>Client: {project.clientName}</span>}
            {project.location && <span> · Location: {project.location}</span>}
          </p>
        </div>
        <div className="bf-project-detail__header-actions">
          <Badge tone={statusTone(project.status)}>{project.status.replace("_", " ")}</Badge>
          {canManageProjects && (
            <>
              <Button variant="secondary" onClick={() => setIsEditOpen(true)}>
                Edit
              </Button>
              <Button variant="danger" onClick={() => setIsArchiveOpen(true)}>
                Archive
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="bf-project-detail__tabs">
        <button
          className={activeTab === "overview" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("overview")}
        >
          Overview
        </button>
        <button
          className={activeTab === "boq" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("boq")}
        >
          BOQ & Estimates
        </button>
        <button
          className={activeTab === "materials" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("materials")}
        >
          Materials
        </button>
        <button
          className={activeTab === "labour" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("labour")}
        >
          Labour
        </button>
        <button
          className={activeTab === "expenses" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("expenses")}
        >
          Expenses
        </button>
        <button
          className={activeTab === "dailyReports" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("dailyReports")}
        >
          Daily Reports
        </button>
        <button
          className={activeTab === "progress" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("progress")}
        >
          Progress
        </button>
        <button
          className={activeTab === "variations" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("variations")}
        >
          Variations
        </button>
        <button
          className={activeTab === "billing" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("billing")}
        >
          Billing
        </button>
        <button
          className={activeTab === "subcontractors" ? "bf-project-detail__tab--active" : ""}
          onClick={() => setActiveTab("subcontractors")}
        >
          Subcontractors
        </button>
      </div>

      {activeTab === "overview" && (
        <>
          <Card>
            <div className="bf-project-detail__progress-header">
              <span>Project Progress</span>
              <span className="bf-project-detail__stat-value">{project.overallProgress}%</span>
            </div>
            <ProgressBar value={project.overallProgress} />
          </Card>

          <div className="bf-project-detail__stats">
            <Card>
              <span className="bf-project-detail__stat-label">Contract Value</span>
              <span className="bf-project-detail__stat-value">{formatCurrency(project.revisedContractValue)}</span>
              {hasApprovedVariations && (
                <span className="bf-project-detail__stat-note">Original: {formatCurrency(project.contractValue)}</span>
              )}
            </Card>
            <Card>
              <span className="bf-project-detail__stat-label">Estimated Cost</span>
              <span className="bf-project-detail__stat-value">{formatCurrency(project.estimatedCost)}</span>
            </Card>
            <Card>
              <span className="bf-project-detail__stat-label">Actual Cost</span>
              <span className="bf-project-detail__stat-value">{formatCurrency(project.actualCost)}</span>
            </Card>
            <Card>
              <span className="bf-project-detail__stat-label">Estimated Final Cost</span>
              <span className="bf-project-detail__stat-value">{formatCurrency(estimatedFinalCost)}</span>
            </Card>
          </div>

          <Card>
            <div className="bf-project-detail__margin">
              <span>Estimated Project Margin</span>
              <span className={estimatedMargin >= 0 ? "bf-project-detail__margin-positive" : "bf-project-detail__margin-negative"}>
                {formatCurrency(estimatedMargin)}
              </span>
            </div>
          </Card>

          <Card>
            <h3>Timeline</h3>
            <div className="bf-project-detail__timeline">
              <div>
                <span className="bf-project-detail__stat-label">Start Date</span>
                <span>{formatDate(project.startDate)}</span>
              </div>
              <div>
                <span className="bf-project-detail__stat-label">Expected End Date</span>
                <span>{formatDate(project.expectedEndDate)}</span>
              </div>
            </div>
          </Card>
        </>
      )}

      {activeTab === "boq" && <BoqPanel projectId={project.id} />}

      {activeTab === "materials" && <MaterialsPanel projectId={project.id} />}

      {activeTab === "labour" && <LabourPanel projectId={project.id} />}

      {activeTab === "expenses" && (
        <ExpensePanel projectId={project.id} onExpensesChanged={loadProject} />
      )}

      {activeTab === "dailyReports" && <DailyReportsPanel projectId={project.id} />}

      {activeTab === "progress" && (
        <ProgressPanel projectId={project.id} onProgressChanged={loadProject} />
      )}

      {activeTab === "variations" && (
        <VariationsPanel projectId={project.id} onVariationsChanged={loadProject} />
      )}

      {activeTab === "billing" && <BillingPanel projectId={project.id} />}

      {activeTab === "subcontractors" && (
        <WorkOrdersPanel projectId={project.id} onCostChanged={loadProject} />
      )}

      {isEditOpen && (
        <Modal title="Edit Project" onClose={() => setIsEditOpen(false)}>
          <ProjectForm initialValue={project} onSubmit={handleUpdate} onCancel={() => setIsEditOpen(false)} />
        </Modal>
      )}

      {isArchiveOpen && (
        <ConfirmDialog
          title="Archive Project?"
          description="This project will be hidden from your active project list. This action can be reviewed by an administrator later."
          confirmLabel="Archive"
          isSubmitting={isArchiving}
          onConfirm={handleArchive}
          onCancel={() => setIsArchiveOpen(false)}
        />
      )}
    </div>
  );
}
