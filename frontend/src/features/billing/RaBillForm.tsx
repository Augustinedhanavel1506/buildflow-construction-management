import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { RaBill, RaBillPayload } from "../../types/billing";
import "./BillingForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function RaBillForm({
  initialValue,
  nextBillNumber,
  onSubmit,
  onCancel,
}: {
  initialValue?: RaBill;
  nextBillNumber: string;
  onSubmit: (payload: RaBillPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [billNumber, setBillNumber] = useState(initialValue?.billNumber ?? nextBillNumber);
  const [billDate, setBillDate] = useState(initialValue?.billDate ?? today());
  const [workDoneValue, setWorkDoneValue] = useState(initialValue?.workDoneValue?.toString() ?? "");
  const [retentionPercent, setRetentionPercent] = useState(initialValue?.retentionPercent?.toString() ?? "5");
  const [otherDeductions, setOtherDeductions] = useState(initialValue?.otherDeductions?.toString() ?? "");
  const [notes, setNotes] = useState(initialValue?.notes ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const parsedWorkDone = Number(workDoneValue || 0);
  const parsedRetentionPercent = Number(retentionPercent || 0);
  const parsedOtherDeductions = Number(otherDeductions || 0);
  const retentionPreview = (parsedWorkDone * parsedRetentionPercent) / 100;
  const netPreview = parsedWorkDone - retentionPreview - parsedOtherDeductions;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!billNumber.trim()) {
      setError("Please enter a bill number.");
      return;
    }
    if (!workDoneValue || Number.isNaN(parsedWorkDone) || parsedWorkDone <= 0) {
      setError("Please enter a valid work done value.");
      return;
    }
    if (Number.isNaN(parsedRetentionPercent) || parsedRetentionPercent < 0 || parsedRetentionPercent > 100) {
      setError("Retention percent must be between 0 and 100.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        billNumber: billNumber.trim(),
        billDate,
        workDoneValue: parsedWorkDone,
        retentionPercent: parsedRetentionPercent,
        otherDeductions: otherDeductions ? parsedOtherDeductions : undefined,
        notes: notes.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-billing-form" onSubmit={handleSubmit}>
      {error && <div className="bf-billing-form__error">{error}</div>}

      <div className="bf-billing-form__row">
        <Input label="Bill Number" required value={billNumber} onChange={(e) => setBillNumber(e.target.value)} />
        <Input label="Bill Date" type="date" required value={billDate} onChange={(e) => setBillDate(e.target.value)} />
      </div>

      <Input
        label="Work Done Value (₹)"
        type="number"
        min="0"
        step="0.01"
        required
        value={workDoneValue}
        onChange={(e) => setWorkDoneValue(e.target.value)}
      />

      <div className="bf-billing-form__row">
        <Input
          label="Retention (%)"
          type="number"
          min="0"
          max="100"
          step="0.01"
          required
          value={retentionPercent}
          onChange={(e) => setRetentionPercent(e.target.value)}
        />
        <Input
          label="Other Deductions (₹)"
          type="number"
          min="0"
          step="0.01"
          value={otherDeductions}
          onChange={(e) => setOtherDeductions(e.target.value)}
        />
      </div>

      <div className="bf-billing-form__preview">
        <span>
          Retention: <strong>₹{retentionPreview.toLocaleString("en-IN")}</strong>
        </span>
        <span>
          Net Payable: <strong>₹{netPreview.toLocaleString("en-IN")}</strong>
        </span>
      </div>

      <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-billing-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Create Bill"}
        </Button>
      </div>
    </form>
  );
}
