import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { WorkerPayload } from "../../types/labour";
import "./LabourForms.css";

export function WorkerForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (payload: WorkerPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [name, setName] = useState("");
  const [role, setRole] = useState("");
  const [dailyRate, setDailyRate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedRate = Number(dailyRate);
    if (!name.trim() || !role.trim()) {
      setError("Please fill in the worker's name and role.");
      return;
    }
    if (!dailyRate || Number.isNaN(parsedRate) || parsedRate <= 0) {
      setError("Please enter a valid daily rate.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({ name: name.trim(), role: role.trim(), dailyRate: parsedRate });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-worker-form" onSubmit={handleSubmit}>
      {error && <div className="bf-worker-form__error">{error}</div>}

      <Input label="Worker Name" required value={name} onChange={(e) => setName(e.target.value)} />
      <Input label="Role" required placeholder="Mason, Helper, Electrician…" value={role} onChange={(e) => setRole(e.target.value)} />
      <Input
        label="Daily Rate (₹)"
        type="number"
        min="0"
        step="0.01"
        required
        value={dailyRate}
        onChange={(e) => setDailyRate(e.target.value)}
      />

      <div className="bf-worker-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Add Worker
        </Button>
      </div>
    </form>
  );
}
