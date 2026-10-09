import { useEffect, useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { rateMasterService } from "../../services/rateMasterService";
import { getErrorMessage } from "../../utils/apiError";
import type { BoqItem, BoqItemPayload } from "../../types/boq";
import type { RateMasterItem } from "../../types/rateMaster";
import { BOQ_CATEGORIES, categoryLabel } from "./categoryLabel";
import "./BoqItemForm.css";

interface BoqItemFormProps {
  initialValue?: BoqItem;
  onSubmit: (payload: BoqItemPayload) => Promise<void>;
  onCancel: () => void;
}

export function BoqItemForm({ initialValue, onSubmit, onCancel }: BoqItemFormProps) {
  const [itemName, setItemName] = useState(initialValue?.itemName ?? "");
  const [category, setCategory] = useState(initialValue?.category ?? "MATERIAL");
  const [unit, setUnit] = useState(initialValue?.unit ?? "");
  const [quantity, setQuantity] = useState(initialValue?.quantity?.toString() ?? "");
  const [rate, setRate] = useState(initialValue?.rate?.toString() ?? "");
  const [actualAmount, setActualAmount] = useState(initialValue?.actualAmount?.toString() ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [rateMasterItems, setRateMasterItems] = useState<RateMasterItem[]>([]);

  useEffect(() => {
    if (initialValue) return;
    rateMasterService
      .list()
      .then(setRateMasterItems)
      .catch(() => setRateMasterItems([]));
  }, [initialValue]);

  const estimatedPreview = Number(quantity || 0) * Number(rate || 0);

  function handleRateMasterSelect(id: string) {
    const selected = rateMasterItems.find((item) => item.id === Number(id));
    if (!selected) return;
    setItemName(selected.itemName);
    setCategory(selected.category);
    setUnit(selected.unit);
    setRate(selected.standardRate.toString());
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedQuantity = Number(quantity);
    const parsedRate = Number(rate);

    if (!itemName.trim() || !unit.trim()) {
      setError("Please fill in the item name and unit.");
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
      await onSubmit({
        itemName: itemName.trim(),
        category,
        unit: unit.trim(),
        quantity: parsedQuantity,
        rate: parsedRate,
        actualAmount: actualAmount ? Number(actualAmount) : undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-boq-form" onSubmit={handleSubmit}>
      {error && <div className="bf-boq-form__error">{error}</div>}

      {rateMasterItems.length > 0 && (
        <Select label="Load from Rate Master (optional)" defaultValue="" onChange={(e) => handleRateMasterSelect(e.target.value)}>
          <option value="">— Select a saved rate —</option>
          {rateMasterItems.map((item) => (
            <option key={item.id} value={item.id}>
              {item.itemName} ({item.unit}) — ₹{item.standardRate.toLocaleString("en-IN")}
            </option>
          ))}
        </Select>
      )}

      <Input label="Item Name" required value={itemName} onChange={(e) => setItemName(e.target.value)} />

      <div className="bf-boq-form__row">
        <Select label="Category" value={category} onChange={(e) => setCategory(e.target.value as typeof category)}>
          {BOQ_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {categoryLabel(c)}
            </option>
          ))}
        </Select>
        <Input label="Unit" required placeholder="Bag, Kg, Nos…" value={unit} onChange={(e) => setUnit(e.target.value)} />
      </div>

      <div className="bf-boq-form__row">
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

      <div className="bf-boq-form__estimate">
        Estimated Amount: <strong>₹{estimatedPreview.toLocaleString("en-IN")}</strong>
      </div>

      <Input
        label="Actual Amount (₹) — optional"
        type="number"
        min="0"
        step="0.01"
        value={actualAmount}
        onChange={(e) => setActualAmount(e.target.value)}
      />

      <div className="bf-boq-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Add Item"}
        </Button>
      </div>
    </form>
  );
}
