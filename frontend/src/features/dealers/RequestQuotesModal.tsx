import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { dealerService, quoteRequestService } from "../../services/dealerService";
import { getErrorMessage } from "../../utils/apiError";
import type { Dealer, QuoteRequest } from "../../types/dealer";
import "./RequestQuotesModal.css";

export function RequestQuotesModal({
  projectId,
  defaultTitle,
  onCreated,
  onCancel,
}: {
  projectId: number;
  defaultTitle: string;
  onCreated: (request: QuoteRequest) => void;
  onCancel: () => void;
}) {
  const [dealers, setDealers] = useState<Dealer[] | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [title, setTitle] = useState(defaultTitle);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    dealerService.list().then(setDealers).catch((err) => setError(getErrorMessage(err)));
  }, []);

  function toggle(id: number) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!title.trim()) {
      setError("Give the request a title.");
      return;
    }
    if (selected.size === 0) {
      setError("Select at least one dealer.");
      return;
    }
    setIsSubmitting(true);
    try {
      onCreated(await quoteRequestService.create({ projectId, title: title.trim(), dealerIds: [...selected] }));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-request-quotes" onSubmit={handleSubmit}>
      {error && <div className="bf-request-quotes__error">{error}</div>}
      <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} />

      <div>
        <div className="bf-request-quotes__label">Dealers to ask</div>
        {dealers === null ? (
          <p className="bf-request-quotes__hint">Loading dealers…</p>
        ) : dealers.length === 0 ? (
          <p className="bf-request-quotes__hint">
            You have no dealers yet. <Link to="/dealers">Add dealers</Link> first.
          </p>
        ) : (
          <div className="bf-request-quotes__list">
            {dealers.map((dealer) => (
              <label key={dealer.id} className="bf-request-quotes__dealer">
                <input type="checkbox" checked={selected.has(dealer.id)} onChange={() => toggle(dealer.id)} />
                <span>
                  <strong>{dealer.name}</strong>
                  <span className="bf-request-quotes__meta">
                    {[dealer.district, `${dealer.rates.length} rates on file`].filter(Boolean).join(" · ")}
                  </span>
                </span>
              </label>
            ))}
          </div>
        )}
      </div>

      <p className="bf-request-quotes__hint">
        Every material in this project's BOQ is requested with its combined quantity. Rates from each dealer's rate card
        are prefilled as indicative; record the real quotes as dealers reply.
      </p>

      <div className="bf-request-quotes__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting} disabled={!dealers || dealers.length === 0}>
          Create Request
        </Button>
      </div>
    </form>
  );
}
