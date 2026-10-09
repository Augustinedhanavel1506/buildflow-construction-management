import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { Subcontractor, SubcontractorPayload } from "../../types/subcontractor";
import "./SubcontractorForm.css";

export function SubcontractorForm({
  initialValue,
  onSubmit,
  onCancel,
}: {
  initialValue?: Subcontractor;
  onSubmit: (payload: SubcontractorPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [name, setName] = useState(initialValue?.name ?? "");
  const [tradeType, setTradeType] = useState(initialValue?.tradeType ?? "");
  const [contactPerson, setContactPerson] = useState(initialValue?.contactPerson ?? "");
  const [phone, setPhone] = useState(initialValue?.phone ?? "");
  const [email, setEmail] = useState(initialValue?.email ?? "");
  const [gstNumber, setGstNumber] = useState(initialValue?.gstNumber ?? "");
  const [panNumber, setPanNumber] = useState(initialValue?.panNumber ?? "");
  const [active, setActive] = useState(initialValue?.active ?? true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!name.trim() || !tradeType.trim()) {
      setError("Please fill in the name and trade type.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        name: name.trim(),
        tradeType: tradeType.trim(),
        contactPerson: contactPerson.trim() || undefined,
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        gstNumber: gstNumber.trim() || undefined,
        panNumber: panNumber.trim() || undefined,
        active,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-subcontractor-form" onSubmit={handleSubmit}>
      {error && <div className="bf-subcontractor-form__error">{error}</div>}

      <div className="bf-subcontractor-form__row">
        <Input label="Name" required value={name} onChange={(e) => setName(e.target.value)} />
        <Input
          label="Trade Type"
          required
          placeholder="Electrical, Plumbing, Tiling…"
          value={tradeType}
          onChange={(e) => setTradeType(e.target.value)}
        />
      </div>

      <div className="bf-subcontractor-form__row">
        <Input label="Contact Person" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
        <Input label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
      </div>

      <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />

      <div className="bf-subcontractor-form__row">
        <Input label="GST Number" value={gstNumber} onChange={(e) => setGstNumber(e.target.value)} />
        <Input label="PAN Number" value={panNumber} onChange={(e) => setPanNumber(e.target.value)} />
      </div>

      {initialValue && (
        <label className="bf-subcontractor-form__checkbox">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Active
        </label>
      )}

      <div className="bf-subcontractor-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Add Subcontractor"}
        </Button>
      </div>
    </form>
  );
}
