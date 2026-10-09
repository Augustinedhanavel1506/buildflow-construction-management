import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { reviewService } from "../../services/reviewService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { Review } from "../../types/review";
import { useAuth } from "../auth/AuthContext";
import { GRADE_LABEL, ROOF_TYPE_LABEL, STRUCTURE_LABEL, WALL_MATERIAL_LABEL, componentLabel, estimateSourceLabel, estimateSourceTone, floorLabel } from "../estimation/labels";
import { PlanViewer } from "../estimation/plan/PlanViewer";
import { REVIEW_LABEL, REVIEW_TONE } from "./ReviewsPage";
import "./ReviewDetailPage.css";

interface LineState {
  approve: boolean;
  quantity: string;
  comment: string;
}

export function ReviewDetailPage() {
  const { id } = useParams<{ id: string }>();
  const reviewId = Number(id);
  const { user } = useAuth();
  const { showToast } = useToast();

  const [review, setReview] = useState<Review | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lines, setLines] = useState<Record<number, LineState>>({});
  const [overallNote, setOverallNote] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const loaded = await reviewService.get(reviewId);
      setReview(loaded);
      setLines(
        Object.fromEntries(
          loaded.items
            .filter((i) => i.estimateSource === "SYSTEM_PRELIMINARY")
            .map((i) => [i.id, { approve: true, quantity: i.quantity.toString(), comment: "" }]),
        ),
      );
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [reviewId]);

  useEffect(() => {
    load();
  }, [load]);

  function updateLine(itemId: number, patch: Partial<LineState>) {
    setLines((current) => ({ ...current, [itemId]: { ...current[itemId], ...patch } }));
  }

  async function submit(outcome: "COMPLETE" | "REQUEST_CHANGES") {
    if (!review) return;
    setFormError(null);

    if (outcome === "REQUEST_CHANGES" && !overallNote.trim()) {
      setFormError("Say what needs to change before sending the estimate back.");
      return;
    }
    const chosen = review.items.filter((i) => lines[i.id]?.approve);
    if (outcome === "COMPLETE") {
      if (chosen.length === 0) {
        setFormError("Tick at least one line to validate.");
        return;
      }
      for (const item of chosen) {
        if (!(Number(lines[item.id].quantity) > 0)) {
          setFormError(`Enter a quantity above zero for ${item.itemName}.`);
          return;
        }
      }
    }

    setIsBusy(true);
    try {
      await reviewService.submit(reviewId, {
        outcome,
        overallNote: overallNote.trim() || undefined,
        lines:
          outcome === "COMPLETE"
            ? chosen.map((item) => ({
                boqItemId: item.id,
                quantity: Number(lines[item.id].quantity),
                comment: lines[item.id].comment.trim() || undefined,
              }))
            : undefined,
      });
      showToast("success", outcome === "COMPLETE" ? "Validation recorded." : "Estimate sent back.");
      await load();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  async function handleCancel() {
    setIsBusy(true);
    try {
      await reviewService.cancel(reviewId);
      showToast("success", "Review cancelled.");
      await load();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  if (error) {
    return <div className="bf-review-detail__error">{error}</div>;
  }
  if (!review) {
    return <Skeleton height={360} />;
  }

  const isEngineer = user?.role === "ENGINEER";
  const canReview = isEngineer && review.status === "REQUESTED";
  const canCancel = !isEngineer && review.status === "REQUESTED" && (user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER");
  const total = review.items.reduce((sum, i) => sum + i.estimatedAmount, 0);
  const plan = review.plan;

  return (
    <div className="bf-review-detail">
      <Link to="/reviews" className="bf-review-detail__back">← All reviews</Link>

      <div className="bf-review-detail__header">
        <div>
          <div className="bf-review-detail__title-row">
            <h1>{review.projectName}</h1>
            <Badge tone={REVIEW_TONE[review.status]}>{REVIEW_LABEL[review.status]}</Badge>
          </div>
          <p className="bf-review-detail__sub">
            {[review.projectLocation, `Requested by ${review.requestedByName}`, `Engineer: ${review.engineerName}`, formatDate(review.createdAt)]
              .filter(Boolean)
              .join(" · ")}
          </p>
        </div>
        {canCancel && (
          <Button variant="secondary" onClick={handleCancel} isLoading={isBusy}>Cancel request</Button>
        )}
      </div>

      {review.message && (
        <Card className="bf-review-detail__note">
          <strong>Message from {review.requestedByName}:</strong> {review.message}
        </Card>
      )}
      {review.outcomeNote && (
        <Card className="bf-review-detail__note">
          <strong>Engineer's note:</strong> {review.outcomeNote}
        </Card>
      )}

      {plan && (
        <Card>
          <h3 className="bf-review-detail__section">Plan being reviewed</h3>
          <div className="bf-review-detail__plan-grid">
            <div><span>Plot</span>{plan.plotWidthFt} × {plan.plotLengthFt} ft</div>
            <div><span>Grade</span>{GRADE_LABEL[plan.constructionGrade]}</div>
            <div><span>Structure</span>{STRUCTURE_LABEL[plan.structureType]}</div>
            <div><span>Walls</span>{WALL_MATERIAL_LABEL[plan.wallMaterial]}</div>
            <div><span>Roof</span>{ROOF_TYPE_LABEL[plan.roofType]}</div>
            <div><span>Built-up</span>{plan.floors.reduce((s, f) => s + f.floorAreaSqft, 0).toLocaleString("en-IN")} sq.ft</div>
          </div>
          <ul className="bf-review-detail__floors">
            {plan.floors.map((f) => (
              <li key={f.id}>
                <strong>{floorLabel(f.floorLevel)}</strong> {f.floorAreaSqft.toLocaleString("en-IN")} sq.ft · {f.bedroomCount} BR · {f.bathroomCount} bath · {f.doorCount} doors · {f.windowCount} windows
              </li>
            ))}
          </ul>
        </Card>
      )}

      {review.floorPlans && review.floorPlans.floors.some((f) => f.rooms.length > 0) && (
        <Card>
          <h3 className="bf-review-detail__section">Drawn floor plan</h3>
          <PlanViewer plans={review.floorPlans} />
        </Card>
      )}

      <Card>
        <h3 className="bf-review-detail__section">Bill of quantities ({formatCurrency(total)})</h3>
        <div className="bf-review-detail__table-wrap">
          <table className="bf-table bf-review-detail__table">
            <thead>
              <tr>
                {canReview && <th>Validate</th>}
                <th>Item</th>
                <th>Component</th>
                <th>Estimated</th>
                {canReview && <th>Your quantity</th>}
                {canReview && <th>Comment</th>}
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {review.items.map((item) => {
                const line = lines[item.id];
                return (
                  <tr key={item.id}>
                    {canReview && (
                      <td>
                        {line ? (
                          <input type="checkbox" checked={line.approve} aria-label={`Validate ${item.itemName}`} onChange={(e) => updateLine(item.id, { approve: e.target.checked })} />
                        ) : null}
                      </td>
                    )}
                    <td>{item.itemName}</td>
                    <td>{componentLabel(item.component)}</td>
                    <td>
                      {item.quantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {item.unit}
                      {item.quantityLow !== null && item.quantityHigh !== null && (
                        <div className="bf-review-detail__range">
                          {item.quantityLow.toLocaleString("en-IN", { maximumFractionDigits: 2 })}–{item.quantityHigh.toLocaleString("en-IN", { maximumFractionDigits: 2 })}
                        </div>
                      )}
                    </td>
                    {canReview && (
                      <td>
                        {line && (
                          <input className="bf-review-detail__qty" type="number" min="0" step="0.001" aria-label={`Quantity for ${item.itemName}`} value={line.quantity} onChange={(e) => updateLine(item.id, { quantity: e.target.value })} />
                        )}
                      </td>
                    )}
                    {canReview && (
                      <td>
                        {line && (
                          <input className="bf-review-detail__comment" placeholder="Optional" aria-label={`Comment for ${item.itemName}`} value={line.comment} onChange={(e) => updateLine(item.id, { comment: e.target.value })} />
                        )}
                      </td>
                    )}
                    <td>
                      <Badge tone={estimateSourceTone(item.estimateSource)}>{estimateSourceLabel(item.estimateSource)}</Badge>
                      {item.validationNote && <div className="bf-review-detail__range">{item.validationNote}</div>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {review.decisions.length > 0 && (
        <Card>
          <h3 className="bf-review-detail__section">Corrections made</h3>
          <ul className="bf-review-detail__floors">
            {review.decisions.map((d) => (
              <li key={d.boqItemId}>
                <strong>{d.itemName}</strong>{" "}
                {d.correctedQuantity !== null
                  ? `${d.originalQuantity} → ${d.correctedQuantity}`
                  : "approved as estimated"}
                {d.comment ? ` — ${d.comment}` : ""}
              </li>
            ))}
          </ul>
        </Card>
      )}

      {canReview && (
        <Card className="bf-review-detail__submit">
          <h3 className="bf-review-detail__section">Your review</h3>
          {formError && <div className="bf-review-detail__error">{formError}</div>}
          <label className="bf-review-detail__field">
            Overall note
            <textarea rows={3} value={overallNote} onChange={(e) => setOverallNote(e.target.value)} placeholder="Assumptions, drawings referred to, or what must change" />
          </label>
          <p className="bf-review-detail__hint">
            Ticked lines are recorded as validated under your name{user?.fullName ? ` (${user.fullName})` : ""}, with any
            quantity you changed. Unticked lines stay preliminary.
          </p>
          <div className="bf-review-detail__actions">
            <Button variant="secondary" onClick={() => submit("REQUEST_CHANGES")} isLoading={isBusy}>Send back with comments</Button>
            <Button onClick={() => submit("COMPLETE")} isLoading={isBusy}>Validate ticked lines</Button>
          </div>
        </Card>
      )}
    </div>
  );
}
