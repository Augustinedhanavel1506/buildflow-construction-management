import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { Material, MaterialUsagePayload } from "../../types/material";
import "./MaterialForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function MaterialUsageForm({
  materials,
  onSubmit,
  onCancel,
}: {
  materials: Material[];
  onSubmit: (materialId: number, payload: MaterialUsagePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [materialId, setMaterialId] = useState(materials[0]?.id.toString() ?? "");
  const [quantity, setQuantity] = useState("");
  const [usageDate, setUsageDate] = useState(today());
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedQuantity = Number(quantity);
    if (!materialId) {
      setError("Please select a material.");
      return;
    }
    if (!quantity || Number.isNaN(parsedQuantity) || parsedQuantity <= 0) {
      setError("Please enter a valid quantity.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(Number(materialId), { quantity: parsedQuantity, usageDate, notes: notes.trim() || undefined });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-usage-form" onSubmit={handleSubmit}>
      {error && <div className="bf-usage-form__error">{error}</div>}

      <Select label="Material" value={materialId} onChange={(e) => setMaterialId(e.target.value)}>
        {materials.map((material) => (
          <option key={material.id} value={material.id}>
            {material.name} ({material.unit}) — {material.currentStock} available
          </option>
        ))}
      </Select>

      <Input
        label="Quantity Used"
        type="number"
        min="0"
        step="0.01"
        required
        value={quantity}
        onChange={(e) => setQuantity(e.target.value)}
      />
      <Input label="Date" type="date" required value={usageDate} onChange={(e) => setUsageDate(e.target.value)} />
      <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-usage-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Record Usage
        </Button>
      </div>
    </form>
  );
}
