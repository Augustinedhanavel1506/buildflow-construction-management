import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { Material } from "../../types/material";
import type { MaterialStockCountPayload } from "../../types/materialReconciliation";
import "./MaterialForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function StockCountForm({
  materials,
  onSubmit,
  onCancel,
}: {
  materials: Material[];
  onSubmit: (materialId: number, payload: MaterialStockCountPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [materialId, setMaterialId] = useState(materials[0]?.id.toString() ?? "");
  const [countDate, setCountDate] = useState(today());
  const [countedStock, setCountedStock] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedMaterial = materials.find((m) => m.id === Number(materialId));

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedCounted = Number(countedStock);
    if (!materialId) {
      setError("Please select a material.");
      return;
    }
    if (!countedStock || Number.isNaN(parsedCounted) || parsedCounted < 0) {
      setError("Please enter a valid counted quantity.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(Number(materialId), { countDate, countedStock: parsedCounted, notes: notes.trim() || undefined });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-material-form" onSubmit={handleSubmit}>
      {error && <div className="bf-material-form__error">{error}</div>}

      <Select label="Material" value={materialId} onChange={(e) => setMaterialId(e.target.value)}>
        {materials.map((material) => (
          <option key={material.id} value={material.id}>
            {material.name} ({material.unit}) — system says {material.currentStock}
          </option>
        ))}
      </Select>

      {selectedMaterial && (
        <p className="bf-material-form__hint">
          System expects <strong>{selectedMaterial.currentStock} {selectedMaterial.unit}</strong> on site. Enter what
          you physically counted.
        </p>
      )}

      <div className="bf-material-form__row">
        <Input label="Count Date" type="date" required value={countDate} onChange={(e) => setCountDate(e.target.value)} />
        <Input
          label="Counted Stock"
          type="number"
          min="0"
          step="0.01"
          required
          value={countedStock}
          onChange={(e) => setCountedStock(e.target.value)}
        />
      </div>

      <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-material-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Record Count
        </Button>
      </div>
    </form>
  );
}
