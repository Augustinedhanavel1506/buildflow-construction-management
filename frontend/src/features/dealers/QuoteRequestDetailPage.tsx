import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { quoteRequestService } from "../../services/dealerService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { DealerQuote, QuoteRequest, QuoteUpdatePayload } from "../../types/dealer";
import { useAuth } from "../auth/AuthContext";
import { RecordQuoteForm } from "./RecordQuoteForm";
import { QUOTE_STATUS_TONE } from "./QuoteRequestsPage";
import "./QuoteRequestDetailPage.css";

const QUOTE_TONE = { PENDING: "warning", RECEIVED: "success", DECLINED: "neutral" } as const;
const QUOTE_LABEL = { PENDING: "Waiting", RECEIVED: "Received", DECLINED: "Declined" } as const;

export function QuoteRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const requestId = Number(id);
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [request, setRequest] = useState<QuoteRequest | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [recording, setRecording] = useState<DealerQuote | null>(null);
  const [awarding, setAwarding] = useState<DealerQuote | null>(null);
  const [isBusy, setIsBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setRequest(await quoteRequestService.get(requestId));
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [requestId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleRecord(payload: QuoteUpdatePayload) {
    if (!recording) return;
    setRequest(await quoteRequestService.recordQuote(requestId, recording.id, payload));
    setRecording(null);
    showToast("success", "Quote saved.");
  }

  async function handleAward() {
    if (!awarding) return;
    setIsBusy(true);
    try {
      setRequest(await quoteRequestService.award(requestId, awarding.id));
      setAwarding(null);
      showToast("success", `Awarded to ${awarding.dealerName}.`);
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  async function handleClose() {
    setIsBusy(true);
    try {
      setRequest(await quoteRequestService.close(requestId));
      showToast("success", "Quote request closed.");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  if (error) {
    return <div className="bf-quote-detail__error">{error}</div>;
  }
  if (!request) {
    return <Skeleton height={360} />;
  }

  const isOpen = request.status === "OPEN";

  return (
    <div className="bf-quote-detail">
      <Link to="/quotes" className="bf-quote-detail__back">← All quote requests</Link>

      <div className="bf-quote-detail__header">
        <div>
          <div className="bf-quote-detail__title-row">
            <h1>{request.title}</h1>
            <Badge tone={QUOTE_STATUS_TONE[request.status]}>{request.status.charAt(0) + request.status.slice(1).toLowerCase()}</Badge>
          </div>
          <p className="bf-quote-detail__sub">
            Project: <Link to={`/projects/${request.projectId}`}>{request.projectName}</Link> · {request.items.length} materials
          </p>
        </div>
        {canManage && isOpen && (
          <Button variant="secondary" onClick={handleClose} isLoading={isBusy}>
            Close request
          </Button>
        )}
      </div>

      <Card>
        <h3 className="bf-quote-detail__section">Quote comparison</h3>
        <p className="bf-quote-detail__note">
          Totals include delivery and loading, since a lower rate can still cost more once freight is added. Rates marked
          indicative come from the dealer's rate card and are not a real quotation until you record them.
        </p>
        <div className="bf-quote-detail__table-wrap">
          <table className="bf-table bf-quote-detail__matrix">
            <thead>
              <tr>
                <th>Material</th>
                {request.quotes.map((quote) => (
                  <th key={quote.id}>
                    <div className="bf-quote-detail__dealer">{quote.dealerName}</div>
                    <div className="bf-quote-detail__dealer-badges">
                      <Badge tone={QUOTE_TONE[quote.status]}>{QUOTE_LABEL[quote.status]}</Badge>
                      {request.lowestCompleteQuoteId === quote.id && <Badge tone="success">Lowest</Badge>}
                      {request.awardedQuoteId === quote.id && <Badge tone="info">Awarded</Badge>}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {request.items.map((item, index) => (
                <tr key={item.itemName}>
                  <td>
                    <div className="bf-quote-detail__item">{item.itemName}</div>
                    <div className="bf-quote-detail__qty">
                      {item.quantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {item.unit}
                    </div>
                  </td>
                  {request.quotes.map((quote) => {
                    const line = quote.lines[index];
                    return (
                      <td key={quote.id}>
                        {line.unitRate === null ? (
                          <span className="bf-quote-detail__none">—</span>
                        ) : (
                          <>
                            <div>{formatCurrency(line.amount)}</div>
                            <div className="bf-quote-detail__qty">
                              @ {formatCurrency(line.unitRate)}
                              {line.source === "INDICATIVE" && <span className="bf-quote-detail__indicative"> indicative</span>}
                            </div>
                          </>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td>Materials</td>
                {request.quotes.map((quote) => (
                  <td key={quote.id}>
                    {formatCurrency(quote.materialTotal)}
                    {!quote.complete && (
                      <div className="bf-quote-detail__partial">
                        {quote.pricedItemCount} of {quote.itemCount} priced
                      </div>
                    )}
                  </td>
                ))}
              </tr>
              <tr>
                <td>Delivery + loading</td>
                {request.quotes.map((quote) => (
                  <td key={quote.id}>{formatCurrency(quote.deliveryCharge + quote.loadingCharge)}</td>
                ))}
              </tr>
              <tr className="bf-quote-detail__total">
                <td>Total</td>
                {request.quotes.map((quote) => (
                  <td key={quote.id}>{formatCurrency(quote.effectiveTotal)}</td>
                ))}
              </tr>
              {canManage && isOpen && (
                <tr>
                  <td></td>
                  {request.quotes.map((quote) => (
                    <td key={quote.id} className="bf-quote-detail__actions">
                      <button onClick={() => setRecording(quote)}>{quote.status === "RECEIVED" ? "Edit quote" : "Record quote"}</button>
                      {quote.status === "RECEIVED" && <button onClick={() => setAwarding(quote)}>Award</button>}
                    </td>
                  ))}
                </tr>
              )}
            </tfoot>
          </table>
        </div>
      </Card>

      {recording && (
        <Modal title={`Quote from ${recording.dealerName}`} onClose={() => setRecording(null)}>
          <RecordQuoteForm quote={recording} onSubmit={handleRecord} onCancel={() => setRecording(null)} />
        </Modal>
      )}

      {awarding && (
        <ConfirmDialog
          title="Award this quote?"
          description={`${awarding.dealerName} will be marked as the chosen supplier (total ${formatCurrency(awarding.effectiveTotal)}) and this request will be locked.`}
          confirmLabel="Award"
          isSubmitting={isBusy}
          onConfirm={handleAward}
          onCancel={() => setAwarding(null)}
        />
      )}
    </div>
  );
}
