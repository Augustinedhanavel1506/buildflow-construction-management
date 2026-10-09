import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import { BOQ_CATEGORIES, categoryLabel } from "../boq/categoryLabel";
import type { BoqCategory } from "../../types/boq";
import type { RateMasterItem, RateMasterItemPayload } from "../../types/rateMaster";
import "./RateMasterItemForm.css";

export function RateMasterItemForm({
  initialValue,
  defaultDistrict,
  districts = [],
  onSubmit,
  onCancel,
}: {
  initialValue?: RateMasterItem;
  defaultDistrict?: string;
  districts?: string[];
  onSubmit: (payload: RateMasterItemPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [itemName, setItemName] = useState(initialValue?.itemName ?? "");
  const [category, setCategory] = useState<BoqCategory>(initialValue?.category ?? "MATERIAL");
  const [unit, setUnit] = useState(initialValue?.unit ?? "");
  const [standardRate, setStandardRate] = useState(initialValue?.standardRate?.toString() ?? "");
  const [minRate, setMinRate] = useState(initialValue?.minRate?.toString() ?? "");
  const [maxRate, setMaxRate] = useState(initialValue?.maxRate?.toString() ?? "");
  const [consumptionPerSqft, setConsumptionPerSqft] = useState(initialValue?.consumptionPerSqft?.toString() ?? "");
  const [notes, setNotes] = useState(initialValue?.notes ?? "");
  const [active, setActive] = useState(initialValue?.active ?? true);
  const [district, setDistrict] = useState(initialValue?.district ?? defaultDistrict ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!itemName.trim() || !unit.trim()) {
      setError("Please fill in the item name and unit.");
      return;
    }
    const parsedRate = Number(standardRate);
    if (!standardRate || Number.isNaN(parsedRate) || parsedRate <= 0) {
      setError("Please enter a valid standard rate.");
      return;
    }
    const parsedMin = minRate ? Number(minRate) : undefined;
    const parsedMax = maxRate ? Number(maxRate) : undefined;
    if (parsedMin !== undefined && parsedMax !== undefined && parsedMin > parsedMax) {
      setError("Minimum rate cannot be greater than maximum rate.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        itemName: itemName.trim(),
        category,
        unit: unit.trim(),
        standardRate: parsedRate,
        minRate: parsedMin,
        maxRate: parsedMax,
        consumptionPerSqft: consumptionPerSqft ? Number(consumptionPerSqft) : undefined,
        notes: notes.trim() || undefined,
        active,
        district: district.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-rate-master-form" onSubmit={handleSubmit}>
      {error && <div className="bf-rate-master-form__error">{error}</div>}

      <Input label="Item Name" required value={itemName} onChange={(e) => setItemName(e.target.value)} />

      <Input
        label="District (blank = your default rate)"
        list="bf-rate-districts"
        placeholder="e.g. Madurai"
        value={district}
        onChange={(e) => setDistrict(e.target.value)}
      />
      <datalist id="bf-rate-districts">
        {districts.map((d) => (
          <option key={d} value={d} />
        ))}
      </datalist>

      <div className="bf-rate-master-form__row">
        <Select label="Category" value={category} onChange={(e) => setCategory(e.target.value as BoqCategory)}>
          {BOQ_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {categoryLabel(c)}
            </option>
          ))}
        </Select>
        <Input label="Unit" required placeholder="Bag, Kg, Day…" value={unit} onChange={(e) => setUnit(e.target.value)} />
      </div>

      <Input
        label="Standard Rate (₹)"
        type="number"
        min="0"
        step="0.01"
        required
        value={standardRate}
        onChange={(e) => setStandardRate(e.target.value)}
      />

      <div className="bf-rate-master-form__row">
        <Input
          label="Min Rate (₹, optional)"
          type="number"
          min="0"
          step="0.01"
          placeholder="e.g. 370"
          value={minRate}
          onChange={(e) => setMinRate(e.target.value)}
        />
        <Input
          label="Max Rate (₹, optional)"
          type="number"
          min="0"
          step="0.01"
          placeholder="e.g. 411"
          value={maxRate}
          onChange={(e) => setMaxRate(e.target.value)}
        />
      </div>
      <p className="bf-rate-master-form__hint">
        Set a min/max to show a realistic market price band instead of one fixed number — raw material
        rates shift weekly with transport, brand, and bulk-order variables.
      </p>

      {category === "MATERIAL" && (
        <Input
          label={`Consumption per sqft (${unit || "unit"}, optional)`}
          type="number"
          min="0"
          step="0.0001"
          placeholder="e.g. 0.4"
          value={consumptionPerSqft}
          onChange={(e) => setConsumptionPerSqft(e.target.value)}
        />
      )}

      <Input
        label="Notes (optional)"
        placeholder="e.g. Rates drop ₹10-20/bag on bulk orders exceeding 50 bags."
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
      />

      {initialValue && (
        <label className="bf-rate-master-form__checkbox">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Active (available for use in new BOQ items)
        </label>
      )}

      <div className="bf-rate-master-form__actions">
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
