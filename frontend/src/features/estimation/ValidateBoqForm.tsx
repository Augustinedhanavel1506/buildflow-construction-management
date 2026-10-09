import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { BoqItem, BoqValidationPayload } from "../../types/boq";
import "./ValidateBoqForm.css";

// item === null validates every preliminary line as estimated; otherwise one line, with an
// optional corrected quantity.
export function ValidateBoqForm({
  item,
  pendingCount,
  onSubmit,
  onCancel,
}: {
  item: BoqItem | null;
  pendingCount: number;
  onSubmit: (payload: BoqValidationPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [engineerName, setEngineerName] = useState(localStorage.getItem("buildflow.lastEngineer") ?? "");
  const [quantity, setQuantity] = useState(item ? item.quantity.toString() : "");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!engineerName.trim()) {
      setError("Enter the engineer's name.");
      return;
    }
    const corrected = item && quantity && Number(quantity) !== item.quantity ? Number(quantity) : undefined;
    if (item && (!quantity || !(Number(quantity) > 0))) {
      setError("Enter a quantity above zero.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({ engineerName: engineerName.trim(), quantity: corrected, note: note.trim() || undefined });
      localStorage.setItem("buildflow.lastEngineer", engineerName.trim());
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-validate-form" onSubmit={handleSubmit}>
      {error && <div className="bf-validate-form__error">{error}</div>}
      <p className="bf-validate-form__hint">
        {item
          ? `Record that a qualified engineer has reviewed ${item.itemName}. Correct the quantity if their design differs from the estimate.`
          : `Record that a qualified engineer has reviewed and approved all ${pendingCount} preliminary lines exactly as estimated.`}
      </p>
      <Input label="Engineer name and registration" required placeholder="e.g. R. Kumar, B.E. (Civil), Reg. 1234" value={engineerName} onChange={(e) => setEngineerName(e.target.value)} />
      {item && (
        <Input
          label={`Quantity (${item.unit})`}
          type="number"
          min="0"
          step="0.001"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      )}
      <Input label="Note (optional)" placeholder="e.g. per structural drawing S-02" value={note} onChange={(e) => setNote(e.target.value)} />
      <div className="bf-validate-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting}>{item ? "Mark Validated" : "Validate All"}</Button>
      </div>
    </form>
  );
}
