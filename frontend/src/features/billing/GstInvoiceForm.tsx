import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { GstInvoicePayload } from "../../types/gstInvoice";
import "./BillingForms.css";

export function GstInvoiceForm({
  suggestedInvoiceNumber,
  defaultGstRate,
  onSubmit,
  onCancel,
}: {
  suggestedInvoiceNumber: string;
  defaultGstRate: number;
  onSubmit: (payload: GstInvoicePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [invoiceNumber, setInvoiceNumber] = useState(suggestedInvoiceNumber);
  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().slice(0, 10));
  const [hsnSacCode, setHsnSacCode] = useState("9954");
  const [gstRate, setGstRate] = useState(defaultGstRate.toString());
  const [interState, setInterState] = useState("false");
  const [placeOfSupply, setPlaceOfSupply] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!invoiceNumber.trim()) {
      setError("Please enter an invoice number.");
      return;
    }
    if (!invoiceDate) {
      setError("Please select an invoice date.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        invoiceNumber: invoiceNumber.trim(),
        invoiceDate,
        hsnSacCode: hsnSacCode.trim() || undefined,
        gstRate: gstRate ? Number(gstRate) : undefined,
        interState: interState === "true",
        placeOfSupply: placeOfSupply.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-certify-form" onSubmit={handleSubmit}>
      {error && <div className="bf-certify-form__error">{error}</div>}

      <p>Issue a GST tax invoice against this bill. Once issued, it cannot be edited or reissued.</p>

      <div className="bf-project-form__row">
        <Input label="Invoice Number" required value={invoiceNumber} onChange={(e) => setInvoiceNumber(e.target.value)} />
        <Input
          label="Invoice Date"
          type="date"
          required
          value={invoiceDate}
          onChange={(e) => setInvoiceDate(e.target.value)}
        />
      </div>

      <div className="bf-project-form__row">
        <Input label="HSN/SAC Code" value={hsnSacCode} onChange={(e) => setHsnSacCode(e.target.value)} />
        <Input
          label="GST Rate (%)"
          type="number"
          min="0"
          max="100"
          step="0.01"
          value={gstRate}
          onChange={(e) => setGstRate(e.target.value)}
        />
      </div>

      <div className="bf-project-form__row">
        <Select label="Supply Type" value={interState} onChange={(e) => setInterState(e.target.value)}>
          <option value="false">Intra-state (CGST + SGST)</option>
          <option value="true">Inter-state (IGST)</option>
        </Select>
        <Input label="Place of Supply" value={placeOfSupply} onChange={(e) => setPlaceOfSupply(e.target.value)} />
      </div>

      <div className="bf-certify-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Issue Invoice
        </Button>
      </div>
    </form>
  );
}
