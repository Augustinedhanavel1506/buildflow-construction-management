import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { progressService } from "../../services/progressService";
import { getErrorMessage } from "../../utils/apiError";
import type { ProgressStage, ProgressStagePayload } from "../../types/progress";
import { ProgressStageForm } from "./ProgressStageForm";
import "./ProgressPanel.css";

interface ProgressPanelProps {
  projectId: number;
  onProgressChanged?: () => void;
}

export function ProgressPanel({ projectId, onProgressChanged }: ProgressPanelProps) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canAddStages = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [stages, setStages] = useState<ProgressStage[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [drafts, setDrafts] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  const loadStages = useCallback(async () => {
    try {
      const data = await progressService.list(projectId);
      setStages(data);
      setDrafts(Object.fromEntries(data.map((stage) => [stage.id, stage.percentComplete.toString()])));
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadStages();
  }, [loadStages]);

  async function handleCreateStage(payload: ProgressStagePayload) {
    await progressService.create(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "Progress stage added successfully.");
    await loadStages();
    onProgressChanged?.();
  }

  async function handleSavePercent(stageId: number) {
    const draftValue = Number(drafts[stageId]);
    if (Number.isNaN(draftValue) || draftValue < 0 || draftValue > 100) {
      showToast("error", "Progress must be between 0 and 100.");
      return;
    }

    setSavingId(stageId);
    try {
      await progressService.updatePercent(stageId, draftValue);
      showToast("success", "Progress updated successfully.");
      await loadStages();
      onProgressChanged?.();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setSavingId(null);
    }
  }

  if (error) {
    return <div className="bf-progress-panel__error">{error}</div>;
  }

  if (stages === null) {
    return <Skeleton height={240} />;
  }

  return (
    <div className="bf-progress-panel">
      <div className="bf-progress-panel__header">
        <h3>Project Progress</h3>
        {canAddStages && <Button onClick={() => setIsFormOpen(true)}>+ Add Stage</Button>}
      </div>

      {stages.length === 0 ? (
        <Card>
          <EmptyState
            title="No progress stages defined yet."
            description="Add construction phases like Foundation, Structure, and Brickwork to track progress."
            action={canAddStages && <Button onClick={() => setIsFormOpen(true)}>+ Add Stage</Button>}
          />
        </Card>
      ) : (
        <Card className="bf-progress-panel__list">
          {stages.map((stage) => (
            <div key={stage.id} className="bf-progress-panel__row">
              <div className="bf-progress-panel__row-top">
                <span className="bf-progress-panel__stage-name">{stage.stageName}</span>
                <span className="bf-progress-panel__stage-percent">{stage.percentComplete}%</span>
              </div>
              <ProgressBar value={stage.percentComplete} />
              <div className="bf-progress-panel__row-actions">
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={drafts[stage.id] ?? ""}
                  onChange={(e) => setDrafts((current) => ({ ...current, [stage.id]: e.target.value }))}
                />
                <Button
                  variant="secondary"
                  onClick={() => handleSavePercent(stage.id)}
                  isLoading={savingId === stage.id}
                >
                  Update
                </Button>
              </div>
            </div>
          ))}
        </Card>
      )}

      {isFormOpen && (
        <Modal title="Add Progress Stage" onClose={() => setIsFormOpen(false)}>
          <ProgressStageForm onSubmit={handleCreateStage} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
