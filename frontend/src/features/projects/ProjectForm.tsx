import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import type { Project, ProjectPayload } from "../../types/project";
import "./ProjectForm.css";

interface ProjectFormProps {
  initialValue?: Project;
  onSubmit: (payload: ProjectPayload) => Promise<void>;
  onCancel: () => void;
}

export function ProjectForm({ initialValue, onSubmit, onCancel }: ProjectFormProps) {
  const [name, setName] = useState(initialValue?.name ?? "");
  const [clientName, setClientName] = useState(initialValue?.clientName ?? "");
  const [clientGstin, setClientGstin] = useState(initialValue?.clientGstin ?? "");
  const [clientAddress, setClientAddress] = useState(initialValue?.clientAddress ?? "");
  const [location, setLocation] = useState(initialValue?.location ?? "");
  const [contractValue, setContractValue] = useState(initialValue?.contractValue?.toString() ?? "");
  const [estimatedCost, setEstimatedCost] = useState(initialValue?.estimatedCost?.toString() ?? "");
  const [startDate, setStartDate] = useState(initialValue?.startDate ?? "");
  const [expectedEndDate, setExpectedEndDate] = useState(initialValue?.expectedEndDate ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedContractValue = Number(contractValue);
    if (!name.trim()) {
      setError("Please enter a valid project name.");
      return;
    }
    if (!contractValue || Number.isNaN(parsedContractValue) || parsedContractValue <= 0) {
      setError("Please enter a valid contract value.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        name: name.trim(),
        clientName: clientName.trim() || undefined,
        clientGstin: clientGstin.trim() || undefined,
        clientAddress: clientAddress.trim() || undefined,
        location: location.trim() || undefined,
        contractValue: parsedContractValue,
        estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
        startDate: startDate || undefined,
        expectedEndDate: expectedEndDate || undefined,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save project. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-project-form" onSubmit={handleSubmit}>
      {error && <div className="bf-project-form__error">{error}</div>}

      <Input label="Project Name" required value={name} onChange={(e) => setName(e.target.value)} />
      <Input label="Client Name" value={clientName} onChange={(e) => setClientName(e.target.value)} />

      <div className="bf-project-form__row">
        <Input label="Client GSTIN" value={clientGstin} onChange={(e) => setClientGstin(e.target.value)} />
        <Input label="Client Address" value={clientAddress} onChange={(e) => setClientAddress(e.target.value)} />
      </div>

      <Input label="Location" value={location} onChange={(e) => setLocation(e.target.value)} />

      <div className="bf-project-form__row">
        <Input
          label="Contract Value (₹)"
          type="number"
          min="0"
          required
          value={contractValue}
          onChange={(e) => setContractValue(e.target.value)}
        />
        <Input
          label="Estimated Cost (₹)"
          type="number"
          min="0"
          value={estimatedCost}
          onChange={(e) => setEstimatedCost(e.target.value)}
        />
      </div>

      <div className="bf-project-form__row">
        <Input
          label="Start Date"
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
        />
        <Input
          label="Expected End Date"
          type="date"
          value={expectedEndDate}
          onChange={(e) => setExpectedEndDate(e.target.value)}
        />
      </div>

      <div className="bf-project-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Create Project"}
        </Button>
      </div>
    </form>
  );
}
