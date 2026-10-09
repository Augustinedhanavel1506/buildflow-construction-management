import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { reviewService } from "../../services/reviewService";
import { getErrorMessage } from "../../utils/apiError";
import { formatDate } from "../../utils/format";
import type { Review, ReviewStatus } from "../../types/review";
import { useAuth } from "../auth/AuthContext";

export const REVIEW_TONE: Record<ReviewStatus, "warning" | "success" | "danger" | "neutral"> = {
  REQUESTED: "warning",
  COMPLETED: "success",
  CHANGES_REQUESTED: "danger",
  CANCELLED: "neutral",
};

export const REVIEW_LABEL: Record<ReviewStatus, string> = {
  REQUESTED: "Awaiting review",
  COMPLETED: "Validated",
  CHANGES_REQUESTED: "Changes requested",
  CANCELLED: "Cancelled",
};

export function ReviewsPage() {
  const { user } = useAuth();
  const isEngineer = user?.role === "ENGINEER";
  const [reviews, setReviews] = useState<Review[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    reviewService.list().then(setReviews).catch((err) => setError(getErrorMessage(err)));
  }, []);

  if (error) {
    return <div style={{ color: "var(--danger)" }}>{error}</div>;
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-5)" }}>
      <div>
        <h1>{isEngineer ? "Review Queue" : "Engineer Reviews"}</h1>
        <p style={{ color: "var(--text-secondary)", marginTop: "var(--space-1)", maxWidth: 600 }}>
          {isEngineer
            ? "Estimates assigned to you. Open one to check the plan and quantities, correct what needs changing, and validate."
            : "Preliminary estimates sent to engineers for validation. Request one from a project's estimate page."}
        </p>
      </div>
      <Card>
        {reviews === null ? (
          <Skeleton height={180} />
        ) : reviews.length === 0 ? (
          <EmptyState
            title={isEngineer ? "Nothing assigned to you yet." : "No review requests yet."}
            description={isEngineer ? undefined : "Generate an estimate in the Home Estimator, then use Request Engineer Review."}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>{isEngineer ? "Requested by" : "Engineer"}</th>
                <th>Status</th>
                <th>Requested</th>
                <th>Completed</th>
              </tr>
            </thead>
            <tbody>
              {reviews.map((r) => (
                <tr key={r.id}>
                  <td data-label="Project">
                    <Link to={`/reviews/${r.id}`}>{r.projectName}</Link>
                  </td>
                  <td data-label="Engineer">{isEngineer ? r.requestedByName : r.engineerName}</td>
                  <td data-label="Status">
                    <Badge tone={REVIEW_TONE[r.status]}>{REVIEW_LABEL[r.status]}</Badge>
                  </td>
                  <td data-label="Requested">{formatDate(r.createdAt)}</td>
                  <td data-label="Completed">{formatDate(r.completedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
