import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { PaymentMode } from "../../types/billing";
import type { SubcontractorPaymentPayload } from "../../types/subcontractor";
import "./WorkOrderForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

const PAYMENT_MODES: PaymentMode[] = ["BANK_TRANSFER", "CHEQUE", "UPI", "CASH", "OTHER"];

export function SubcontractorPaymentForm({
  maxAmount,
  onSubmit,
  onCancel,
}: {
  maxAmount: number;
  onSubmit: (payload: SubcontractorPaymentPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [amount, setAmount] = useState(maxAmount > 0 ? maxAmount.toString() : "");
  const [paymentDate, setPaymentDate] = useState(today());
  const [mode, setMode] = useState<PaymentMode>("BANK_TRANSFER");
  const [referenceNumber, setReferenceNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsed = Number(amount);
    if (!amount || Number.isNaN(parsed) || parsed <= 0) {
      setError("Please enter a valid amount.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({ amount: parsed, paymentDate, mode, referenceNumber: referenceNumber.trim() || undefined });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-sub-payment-form" onSubmit={handleSubmit}>
      {error && <div className="bf-sub-payment-form__error">{error}</div>}

      <Input
        label={`Amount (₹) — outstanding: ₹${maxAmount.toLocaleString("en-IN")}`}
        type="number"
        min="0"
        step="0.01"
        required
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
      />

      <div className="bf-sub-payment-form__row">
        <Input label="Payment Date" type="date" required value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} />
        <Select label="Mode" value={mode} onChange={(e) => setMode(e.target.value as PaymentMode)}>
          {PAYMENT_MODES.map((m) => (
            <option key={m} value={m}>
              {m.replace("_", " ")}
            </option>
          ))}
        </Select>
      </div>

      <Input label="Reference Number" value={referenceNumber} onChange={(e) => setReferenceNumber(e.target.value)} />

      <div className="bf-sub-payment-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Record Payment
        </Button>
      </div>
    </form>
  );
}
