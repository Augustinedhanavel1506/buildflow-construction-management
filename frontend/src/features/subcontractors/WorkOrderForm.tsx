import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type { ContractType, Subcontractor, WorkOrderPayload } from "../../types/subcontractor";
import "./WorkOrderForms.css";

export function WorkOrderForm({
  subcontractors,
  onSubmit,
  onCancel,
}: {
  subcontractors: Subcontractor[];
  onSubmit: (payload: WorkOrderPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [subcontractorId, setSubcontractorId] = useState(subcontractors[0]?.id.toString() ?? "");
  const [title, setTitle] = useState("");
  const [scopeDescription, setScopeDescription] = useState("");
  const [contractType, setContractType] = useState<ContractType>("LUMP_SUM");
  const [contractValue, setContractValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!subcontractorId) {
      setError("Please select a subcontractor.");
      return;
    }
    if (!title.trim()) {
      setError("Please enter a title for this work order.");
      return;
    }
    const parsedValue = Number(contractValue);
    if (!contractValue || Number.isNaN(parsedValue) || parsedValue <= 0) {
      setError("Please enter a valid contract value.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        subcontractorId: Number(subcontractorId),
        title: title.trim(),
        scopeDescription: scopeDescription.trim() || undefined,
        contractType,
        contractValue: parsedValue,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-work-order-form" onSubmit={handleSubmit}>
      {error && <div className="bf-work-order-form__error">{error}</div>}

      <Select label="Subcontractor" value={subcontractorId} onChange={(e) => setSubcontractorId(e.target.value)}>
        {subcontractors.map((sub) => (
          <option key={sub.id} value={sub.id}>
            {sub.name} ({sub.tradeType})
          </option>
        ))}
      </Select>

      <Input label="Title" required placeholder="Full electrical wiring, Bathroom tiling…" value={title} onChange={(e) => setTitle(e.target.value)} />

      <div className="bf-work-order-form__row">
        <Select label="Contract Type" value={contractType} onChange={(e) => setContractType(e.target.value as ContractType)}>
          <option value="LUMP_SUM">Lump Sum</option>
          <option value="ITEM_RATE">Item Rate</option>
        </Select>
        <Input
          label="Contract Value (₹)"
          type="number"
          min="0"
          step="0.01"
          required
          value={contractValue}
          onChange={(e) => setContractValue(e.target.value)}
        />
      </div>

      <div className="bf-work-order-form__row">
        <Input label="Start Date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        <Input label="End Date" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
      </div>

      <Input label="Scope Description" value={scopeDescription} onChange={(e) => setScopeDescription(e.target.value)} />

      <div className="bf-work-order-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Create Work Order
        </Button>
      </div>
    </form>
  );
}
