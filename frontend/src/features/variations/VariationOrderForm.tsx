import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { VariationOrderPayload, VariationType } from "../../types/variation";
import "./VariationOrderForm.css";

export function VariationOrderForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (payload: VariationOrderPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState<VariationType>("ADDITION");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!title.trim()) {
      setError("Please enter a title for this variation.");
      return;
    }
    const parsedAmount = Number(amount);
    if (!amount || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("Please enter a valid amount.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({ title: title.trim(), description: description.trim() || undefined, type, amount: parsedAmount });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-variation-form" onSubmit={handleSubmit}>
      {error && <div className="bf-variation-form__error">{error}</div>}

      <Input label="Title" required placeholder="Extra bathroom, drop garden wall…" value={title} onChange={(e) => setTitle(e.target.value)} />

      <div className="bf-variation-form__row">
        <Select label="Type" value={type} onChange={(e) => setType(e.target.value as VariationType)}>
          <option value="ADDITION">Addition (increases contract value)</option>
          <option value="OMISSION">Omission (decreases contract value)</option>
        </Select>
        <Input
          label="Amount (₹)"
          type="number"
          min="0"
          step="0.01"
          required
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
      </div>

      <Input label="Description" value={description} onChange={(e) => setDescription(e.target.value)} />

      <div className="bf-variation-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Submit Variation
        </Button>
      </div>
    </form>
  );
}
