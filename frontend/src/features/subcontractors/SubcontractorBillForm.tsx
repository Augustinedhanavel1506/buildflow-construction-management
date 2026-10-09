import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { SubcontractorBillPayload } from "../../types/subcontractor";
import "./WorkOrderForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function SubcontractorBillForm({
  nextBillNumber,
  onSubmit,
  onCancel,
}: {
  nextBillNumber: string;
  onSubmit: (payload: SubcontractorBillPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [billNumber, setBillNumber] = useState(nextBillNumber);
  const [billDate, setBillDate] = useState(today());
  const [workDoneValue, setWorkDoneValue] = useState("");
  const [tdsPercent, setTdsPercent] = useState("2");
  const [retentionPercent, setRetentionPercent] = useState("5");
  const [otherDeductions, setOtherDeductions] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const parsedWorkDone = Number(workDoneValue || 0);
  const parsedTds = Number(tdsPercent || 0);
  const parsedRetention = Number(retentionPercent || 0);
  const parsedOther = Number(otherDeductions || 0);
  const tdsPreview = (parsedWorkDone * parsedTds) / 100;
  const retentionPreview = (parsedWorkDone * parsedRetention) / 100;
  const netPreview = parsedWorkDone - tdsPreview - retentionPreview - parsedOther;

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

    setIsSubmitting(true);
    try {
      await onSubmit({
        billNumber: billNumber.trim(),
        billDate,
        workDoneValue: parsedWorkDone,
        tdsPercent: parsedTds,
        retentionPercent: parsedRetention,
        otherDeductions: otherDeductions ? parsedOther : undefined,
        notes: notes.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-sub-bill-form" onSubmit={handleSubmit}>
      {error && <div className="bf-sub-bill-form__error">{error}</div>}

      <div className="bf-sub-bill-form__row">
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

      <div className="bf-sub-bill-form__row">
        <Input
          label="TDS (%)"
          type="number"
          min="0"
          max="100"
          step="0.01"
          value={tdsPercent}
          onChange={(e) => setTdsPercent(e.target.value)}
        />
        <Input
          label="Retention (%)"
          type="number"
          min="0"
          max="100"
          step="0.01"
          value={retentionPercent}
          onChange={(e) => setRetentionPercent(e.target.value)}
        />
      </div>

      <Input
        label="Other Deductions (₹)"
        type="number"
        min="0"
        step="0.01"
        value={otherDeductions}
        onChange={(e) => setOtherDeductions(e.target.value)}
      />

      <div className="bf-sub-bill-form__preview">
        <span>
          TDS: <strong>₹{tdsPreview.toLocaleString("en-IN")}</strong>
        </span>
        <span>
          Retention: <strong>₹{retentionPreview.toLocaleString("en-IN")}</strong>
        </span>
        <span>
          Net Payable: <strong>₹{netPreview.toLocaleString("en-IN")}</strong>
        </span>
      </div>

      <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-sub-bill-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Create Bill
        </Button>
      </div>
    </form>
  );
}
