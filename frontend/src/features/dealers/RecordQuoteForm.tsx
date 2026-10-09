import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { DealerQuote, DealerQuoteStatus, QuoteUpdatePayload } from "../../types/dealer";
import "./RecordQuoteForm.css";

export function RecordQuoteForm({
  quote,
  onSubmit,
  onCancel,
}: {
  quote: DealerQuote;
  onSubmit: (payload: QuoteUpdatePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [status, setStatus] = useState<DealerQuoteStatus>(quote.status === "PENDING" ? "RECEIVED" : quote.status);
  const [deliveryCharge, setDeliveryCharge] = useState(quote.deliveryCharge ? quote.deliveryCharge.toString() : "");
  const [loadingCharge, setLoadingCharge] = useState(quote.loadingCharge ? quote.loadingCharge.toString() : "");
  const [notes, setNotes] = useState(quote.notes ?? "");
  const [rates, setRates] = useState<Record<string, string>>(
    Object.fromEntries(quote.lines.map((l) => [l.itemName, l.unitRate?.toString() ?? ""])),
  );
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    for (const line of quote.lines) {
      const value = rates[line.itemName];
      if (value && !(Number(value) > 0)) {
        setError(`Enter a valid rate for ${line.itemName}, or leave it blank if the dealer did not price it.`);
        return;
      }
    }
    if (Number(deliveryCharge || 0) < 0 || Number(loadingCharge || 0) < 0) {
      setError("Charges cannot be negative.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        status,
        deliveryCharge: deliveryCharge ? Number(deliveryCharge) : 0,
        loadingCharge: loadingCharge ? Number(loadingCharge) : 0,
        notes: notes.trim() || undefined,
        lines: quote.lines.map((l) => ({
          itemName: l.itemName,
          unitRate: rates[l.itemName] ? Number(rates[l.itemName]) : undefined,
        })),
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-record-quote" onSubmit={handleSubmit}>
      {error && <div className="bf-record-quote__error">{error}</div>}
      <p className="bf-record-quote__hint">
        Enter the rates {quote.dealerName} actually quoted. Values prefilled from their rate card are only indicative
        until you save them here.
      </p>

      <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value as DealerQuoteStatus)}>
        <option value="RECEIVED">Quote received</option>
        <option value="PENDING">Still waiting</option>
        <option value="DECLINED">Declined to quote</option>
      </Select>

      <div className="bf-record-quote__lines">
        {quote.lines.map((line) => (
          <div className="bf-record-quote__line" key={line.itemName}>
            <div>
              <div className="bf-record-quote__item">{line.itemName}</div>
              <div className="bf-record-quote__qty">
                {line.quantity.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {line.unit}
              </div>
            </div>
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="₹ / unit"
              aria-label={`Rate for ${line.itemName}`}
              value={rates[line.itemName] ?? ""}
              onChange={(e) => setRates((current) => ({ ...current, [line.itemName]: e.target.value }))}
            />
          </div>
        ))}
      </div>

      <div className="bf-record-quote__row">
        <Input label="Delivery charge (₹)" type="number" min="0" step="0.01" value={deliveryCharge} onChange={(e) => setDeliveryCharge(e.target.value)} />
        <Input label="Loading / unloading (₹)" type="number" min="0" step="0.01" value={loadingCharge} onChange={(e) => setLoadingCharge(e.target.value)} />
      </div>
      <Input label="Notes" placeholder="e.g. free delivery above 100 bags" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-record-quote__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting}>Save Quote</Button>
      </div>
    </form>
  );
}
