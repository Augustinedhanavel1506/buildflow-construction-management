import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { estimationService } from "../../services/estimationService";
import { getErrorMessage } from "../../utils/apiError";
import type { HouseRequirement, HouseRequirementPayload } from "../../types/estimation";
import { GRADE_LABEL, GRADE_TONE } from "./labels";
import { HouseRequirementForm } from "./HouseRequirementForm";
import "./EstimationListPage.css";

export function EstimationListPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [plans, setPlans] = useState<HouseRequirement[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const loadPlans = useCallback(async () => {
    try {
      const data = await estimationService.list();
      setPlans(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    loadPlans();
  }, [loadPlans]);

  async function handleCreate(payload: HouseRequirementPayload) {
    const created = await estimationService.create(payload);
    setIsCreating(false);
    showToast("success", "House plan saved. Generate a preliminary estimate whenever you're ready.");
    navigate(`/estimation/${created.id}`);
  }

  if (error) {
    return <div className="bf-estimation-list__error">{error}</div>;
  }

  return (
    <div className="bf-estimation-list">
      <div className="bf-estimation-list__hero">
        <div>
          <span className="bf-estimation-list__eyebrow">Pre-construction planning</span>
          <h1>Home Construction Estimator</h1>
          <p>
            Turn a plot and a room checklist into a preliminary bill of quantities and cost estimate — the first
            step before an engineer validates the design and construction begins.
          </p>
        </div>
        <Button onClick={() => setIsCreating(true)}>+ New House Plan</Button>
      </div>

      {plans === null ? (
        <Skeleton height={220} />
      ) : plans.length === 0 ? (
        <Card>
          <EmptyState
            title="No house plans yet."
            description="Enter a plot size and a room checklist to get an instant preliminary material estimate and cost range."
            action={<Button onClick={() => setIsCreating(true)}>+ New House Plan</Button>}
          />
        </Card>
      ) : (
        <div className="bf-estimation-list__grid">
          {plans.map((plan) => {
            const totalArea = plan.floors.reduce((sum, f) => sum + f.floorAreaSqft, 0);
            return (
              <Link to={`/estimation/${plan.id}`} className="bf-plan-card" key={plan.id}>
                <div className="bf-plan-card__header">
                  <span className="bf-plan-card__icon" aria-hidden="true">
                    🏠
                  </span>
                  <Badge tone={plan.status === "ESTIMATED" ? "success" : "neutral"}>
                    {plan.status === "ESTIMATED" ? "Estimated" : "Draft"}
                  </Badge>
                </div>
                <h3 className="bf-plan-card__title">{plan.label}</h3>
                {plan.location && <p className="bf-plan-card__location">{plan.location}</p>}
                <div className="bf-plan-card__stats">
                  <div>
                    <span className="bf-plan-card__stat-value">
                      {plan.plotWidthFt} × {plan.plotLengthFt}
                    </span>
                    <span className="bf-plan-card__stat-label">Plot (ft)</span>
                  </div>
                  <div>
                    <span className="bf-plan-card__stat-value">{totalArea.toLocaleString("en-IN")}</span>
                    <span className="bf-plan-card__stat-label">Built-up sq.ft</span>
                  </div>
                  <div>
                    <span className="bf-plan-card__stat-value">{plan.floors.length}</span>
                    <span className="bf-plan-card__stat-label">{plan.floors.length === 1 ? "Floor" : "Floors"}</span>
                  </div>
                </div>
                <Badge tone={GRADE_TONE[plan.constructionGrade]}>{GRADE_LABEL[plan.constructionGrade]}</Badge>
              </Link>
            );
          })}
        </div>
      )}

      {isCreating && (
        <div className="bf-estimation-list__form-overlay">
          <div className="bf-estimation-list__form-panel">
            <div className="bf-estimation-list__form-header">
              <h2>New House Plan</h2>
              <button className="bf-estimation-list__form-close" onClick={() => setIsCreating(false)} aria-label="Close">
                ×
              </button>
            </div>
            <HouseRequirementForm onSubmit={handleCreate} onCancel={() => setIsCreating(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
