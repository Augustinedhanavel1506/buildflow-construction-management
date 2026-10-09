import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { RaBill } from "../../types/billing";
import "./BillingForms.css";

export function CertifyBillForm({
  bill,
  onSubmit,
  onCancel,
}: {
  bill: RaBill;
  onSubmit: (certifiedAmount: number) => Promise<void>;
  onCancel: () => void;
}) {
  const [certifiedAmount, setCertifiedAmount] = useState(bill.workDoneValue.toString());
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsed = Number(certifiedAmount);
    if (!certifiedAmount || Number.isNaN(parsed) || parsed <= 0) {
      setError("Please enter a valid certified amount.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(parsed);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-certify-form" onSubmit={handleSubmit}>
      {error && <div className="bf-certify-form__error">{error}</div>}

      <p>
        Contractor claimed <strong>₹{bill.workDoneValue.toLocaleString("en-IN")}</strong>. Enter the amount the
        client has certified for this bill — retention and net payable will be recalculated from it.
      </p>

      <Input
        label="Certified Amount (₹)"
        type="number"
        min="0"
        step="0.01"
        required
        value={certifiedAmount}
        onChange={(e) => setCertifiedAmount(e.target.value)}
      />

      <div className="bf-certify-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Certify Bill
        </Button>
      </div>
    </form>
  );
}
