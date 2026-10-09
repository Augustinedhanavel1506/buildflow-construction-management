import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { Dealer, DealerPayload } from "../../types/dealer";
import "./DealerForm.css";

interface RateRow {
  key: string;
  itemName: string;
  unit: string;
  rate: string;
}

export function DealerForm({
  initialValue,
  itemSuggestions,
  onSubmit,
  onCancel,
}: {
  initialValue?: Dealer;
  itemSuggestions: { itemName: string; unit: string }[];
  onSubmit: (payload: DealerPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [name, setName] = useState(initialValue?.name ?? "");
  const [contactPerson, setContactPerson] = useState(initialValue?.contactPerson ?? "");
  const [phone, setPhone] = useState(initialValue?.phone ?? "");
  const [email, setEmail] = useState(initialValue?.email ?? "");
  const [district, setDistrict] = useState(initialValue?.district ?? "");
  const [deliveryRadiusKm, setDeliveryRadiusKm] = useState(initialValue?.deliveryRadiusKm?.toString() ?? "");
  const [gstin, setGstin] = useState(initialValue?.gstin ?? "");
  const [notes, setNotes] = useState(initialValue?.notes ?? "");
  const [active, setActive] = useState(initialValue?.active ?? true);
  const [rates, setRates] = useState<RateRow[]>(
    (initialValue?.rates ?? []).map((r) => ({
      key: crypto.randomUUID(),
      itemName: r.itemName,
      unit: r.unit,
      rate: r.rate.toString(),
    })),
  );
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateRate(key: string, patch: Partial<RateRow>) {
    setRates((current) =>
      current.map((row) => {
        if (row.key !== key) return row;
        const next = { ...row, ...patch };
        // Picking a known item name fills its unit, since quotes only prefill when units match.
        if (patch.itemName !== undefined && !row.unit) {
          const match = itemSuggestions.find((s) => s.itemName.toLowerCase() === patch.itemName!.trim().toLowerCase());
          if (match) next.unit = match.unit;
        }
        return next;
      }),
    );
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!name.trim()) {
      setError("Enter the dealer's name.");
      return;
    }
    const filled = rates.filter((r) => r.itemName.trim() || r.unit.trim() || r.rate);
    for (const row of filled) {
      if (!row.itemName.trim() || !row.unit.trim() || !(Number(row.rate) > 0)) {
        setError("Each rate needs an item name, a unit and a rate above zero.");
        return;
      }
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        name: name.trim(),
        contactPerson: contactPerson.trim() || undefined,
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        district: district.trim() || undefined,
        deliveryRadiusKm: deliveryRadiusKm ? Number(deliveryRadiusKm) : undefined,
        gstin: gstin.trim() || undefined,
        notes: notes.trim() || undefined,
        active,
        rates: filled.map((r) => ({ itemName: r.itemName.trim(), unit: r.unit.trim(), rate: Number(r.rate) })),
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-dealer-form" onSubmit={handleSubmit}>
      {error && <div className="bf-dealer-form__error">{error}</div>}

      <Input label="Dealer name" required value={name} onChange={(e) => setName(e.target.value)} />
      <div className="bf-dealer-form__row">
        <Input label="Contact person" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
        <Input label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
      </div>
      <div className="bf-dealer-form__row">
        <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Input label="District" placeholder="e.g. Madurai" value={district} onChange={(e) => setDistrict(e.target.value)} />
      </div>
      <div className="bf-dealer-form__row">
        <Input label="Delivery radius (km)" type="number" min="0" value={deliveryRadiusKm} onChange={(e) => setDeliveryRadiusKm(e.target.value)} />
        <Input label="GSTIN" value={gstin} onChange={(e) => setGstin(e.target.value)} />
      </div>
      <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <div className="bf-dealer-form__rates">
        <h4>Rate card</h4>
        <p className="bf-dealer-form__hint">
          Item names should match your Rate Master exactly. They are used to prefill quote requests, marked as
          indicative until the dealer confirms.
        </p>
        <datalist id="bf-dealer-item-names">
          {itemSuggestions.map((s) => (
            <option key={s.itemName} value={s.itemName} />
          ))}
        </datalist>
        {rates.map((row) => (
          <div className="bf-dealer-form__rate-row" key={row.key}>
            <input
              list="bf-dealer-item-names"
              placeholder="Item"
              aria-label="Item"
              value={row.itemName}
              onChange={(e) => updateRate(row.key, { itemName: e.target.value })}
            />
            <input placeholder="Unit" aria-label="Unit" value={row.unit} onChange={(e) => updateRate(row.key, { unit: e.target.value })} />
            <input
              placeholder="Rate (₹)"
              aria-label="Rate"
              type="number"
              min="0"
              step="0.01"
              value={row.rate}
              onChange={(e) => updateRate(row.key, { rate: e.target.value })}
            />
            <button type="button" aria-label="Remove rate" onClick={() => setRates((c) => c.filter((r) => r.key !== row.key))}>
              ×
            </button>
          </div>
        ))}
        <button
          type="button"
          className="bf-dealer-form__add"
          onClick={() => setRates((c) => [...c, { key: crypto.randomUUID(), itemName: "", unit: "", rate: "" }])}
        >
          + Add rate
        </button>
      </div>

      {initialValue && (
        <label className="bf-dealer-form__checkbox">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Active (can be selected for quote requests)
        </label>
      )}

      <div className="bf-dealer-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting}>{initialValue ? "Save Changes" : "Add Dealer"}</Button>
      </div>
    </form>
  );
}
