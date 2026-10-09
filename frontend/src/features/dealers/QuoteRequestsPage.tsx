import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { quoteRequestService } from "../../services/dealerService";
import { getErrorMessage } from "../../utils/apiError";
import { formatDate } from "../../utils/format";
import type { QuoteRequestSummary } from "../../types/dealer";

export const QUOTE_STATUS_TONE = { OPEN: "info", AWARDED: "success", CLOSED: "neutral" } as const;

export function QuoteRequestsPage() {
  const [requests, setRequests] = useState<QuoteRequestSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    quoteRequestService.list().then(setRequests).catch((err) => setError(getErrorMessage(err)));
  }, []);

  if (error) {
    return <div style={{ color: "var(--danger)" }}>{error}</div>;
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-5)" }}>
      <div>
        <h1>Quote Requests</h1>
        <p style={{ color: "var(--text-secondary)", marginTop: "var(--space-1)", maxWidth: 560 }}>
          Compare dealer quotes for a project's materials. Start one from a project's estimate with "Request Dealer
          Quotes".
        </p>
      </div>
      <Card>
        {requests === null ? (
          <Skeleton height={200} />
        ) : requests.length === 0 ? (
          <EmptyState
            title="No quote requests yet."
            description="Generate an estimate in the Home Estimator, then request quotes from your dealers."
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Request</th>
                <th>Project</th>
                <th>Materials</th>
                <th>Quotes received</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((r) => (
                <tr key={r.id}>
                  <td data-label="Request">
                    <Link to={`/quotes/${r.id}`}>{r.title}</Link>
                  </td>
                  <td data-label="Project">{r.projectName}</td>
                  <td data-label="Materials">{r.itemCount}</td>
                  <td data-label="Quotes received">
                    {r.receivedCount} of {r.dealerCount}
                  </td>
                  <td data-label="Status">
                    <Badge tone={QUOTE_STATUS_TONE[r.status]}>{r.status.charAt(0) + r.status.slice(1).toLowerCase()}</Badge>
                  </td>
                  <td data-label="Created">{formatDate(r.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
