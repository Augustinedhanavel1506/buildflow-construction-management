import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { Material, MaterialPurchasePayload } from "../../types/material";
import "./MaterialForms.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function MaterialPurchaseForm({
  materials,
  onSubmit,
  onCancel,
}: {
  materials: Material[];
  onSubmit: (materialId: number, payload: MaterialPurchasePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [materialId, setMaterialId] = useState(materials[0]?.id.toString() ?? "");
  const [quantity, setQuantity] = useState("");
  const [rate, setRate] = useState("");
  const [supplierName, setSupplierName] = useState("");
  const [purchaseDate, setPurchaseDate] = useState(today());
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedQuantity = Number(quantity);
    const parsedRate = Number(rate);
    if (!materialId) {
      setError("Please select a material.");
      return;
    }
    if (!quantity || Number.isNaN(parsedQuantity) || parsedQuantity <= 0) {
      setError("Please enter a valid quantity.");
      return;
    }
    if (!rate || Number.isNaN(parsedRate) || parsedRate <= 0) {
      setError("Please enter a valid rate.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(Number(materialId), {
        quantity: parsedQuantity,
        rate: parsedRate,
        supplierName: supplierName.trim() || undefined,
        purchaseDate,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-purchase-form" onSubmit={handleSubmit}>
      {error && <div className="bf-purchase-form__error">{error}</div>}

      <Select label="Material" value={materialId} onChange={(e) => setMaterialId(e.target.value)}>
        {materials.map((material) => (
          <option key={material.id} value={material.id}>
            {material.name} ({material.unit})
          </option>
        ))}
      </Select>

      <div className="bf-purchase-form__row">
        <Input
          label="Quantity"
          type="number"
          min="0"
          step="0.01"
          required
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
        <Input
          label="Rate (₹)"
          type="number"
          min="0"
          step="0.01"
          required
          value={rate}
          onChange={(e) => setRate(e.target.value)}
        />
      </div>

      <Input label="Supplier" value={supplierName} onChange={(e) => setSupplierName(e.target.value)} />
      <Input
        label="Purchase Date"
        type="date"
        required
        value={purchaseDate}
        onChange={(e) => setPurchaseDate(e.target.value)}
      />

      <div className="bf-purchase-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Record Purchase
        </Button>
      </div>
    </form>
  );
}
