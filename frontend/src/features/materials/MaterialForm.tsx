import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { MaterialPayload } from "../../types/material";
import "./MaterialForms.css";

export function MaterialForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (payload: MaterialPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [name, setName] = useState("");
  const [unit, setUnit] = useState("");
  const [minimumStock, setMinimumStock] = useState("");
  const [openingStock, setOpeningStock] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!name.trim() || !unit.trim()) {
      setError("Please fill in the material name and unit.");
      return;
    }
    const parsedMinimum = Number(minimumStock);
    if (!minimumStock || Number.isNaN(parsedMinimum) || parsedMinimum < 0) {
      setError("Please enter a valid minimum stock level.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        name: name.trim(),
        unit: unit.trim(),
        minimumStock: parsedMinimum,
        openingStock: openingStock ? Number(openingStock) : undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-material-form" onSubmit={handleSubmit}>
      {error && <div className="bf-material-form__error">{error}</div>}

      <Input label="Material Name" required value={name} onChange={(e) => setName(e.target.value)} />
      <Input label="Unit" required placeholder="Bag, Kg, Nos…" value={unit} onChange={(e) => setUnit(e.target.value)} />

      <div className="bf-material-form__row">
        <Input
          label="Minimum Stock"
          type="number"
          min="0"
          step="0.01"
          required
          value={minimumStock}
          onChange={(e) => setMinimumStock(e.target.value)}
        />
        <Input
          label="Opening Stock"
          type="number"
          min="0"
          step="0.01"
          value={openingStock}
          onChange={(e) => setOpeningStock(e.target.value)}
        />
      </div>

      <div className="bf-material-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Add Material
        </Button>
      </div>
    </form>
  );
}
