import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { Material, MaterialRequisitionPayload } from "../../types/material";
import "./MaterialForms.css";

export function MaterialRequisitionForm({
  materials,
  onSubmit,
  onCancel,
}: {
  materials: Material[];
  onSubmit: (payload: MaterialRequisitionPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [materialId, setMaterialId] = useState(materials[0]?.id.toString() ?? "");
  const [quantity, setQuantity] = useState("");
  const [requiredDate, setRequiredDate] = useState("");
  const [reason, setReason] = useState("");
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
      await onSubmit({
        materialId: Number(materialId),
        quantity: parsedQuantity,
        requiredDate: requiredDate || undefined,
        reason: reason.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-requisition-form" onSubmit={handleSubmit}>
      {error && <div className="bf-requisition-form__error">{error}</div>}

      <Select label="Material" value={materialId} onChange={(e) => setMaterialId(e.target.value)}>
        {materials.map((material) => (
          <option key={material.id} value={material.id}>
            {material.name} ({material.unit})
          </option>
        ))}
      </Select>

      <Input
        label="Quantity Needed"
        type="number"
        min="0"
        step="0.01"
        required
        value={quantity}
        onChange={(e) => setQuantity(e.target.value)}
      />
      <Input
        label="Required Date"
        type="date"
        value={requiredDate}
        onChange={(e) => setRequiredDate(e.target.value)}
      />
      <Input label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} />

      <div className="bf-requisition-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Submit Request
        </Button>
      </div>
    </form>
  );
}
