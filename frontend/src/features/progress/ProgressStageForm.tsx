import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { ProgressStagePayload } from "../../types/progress";
import "./ProgressForms.css";

export function ProgressStageForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (payload: ProgressStagePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [stageName, setStageName] = useState("");
  const [percentComplete, setPercentComplete] = useState("0");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!stageName.trim()) {
      setError("Please enter a stage name.");
      return;
    }
    const parsedPercent = Number(percentComplete);
    if (Number.isNaN(parsedPercent) || parsedPercent < 0 || parsedPercent > 100) {
      setError("Progress must be between 0 and 100.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({ stageName: stageName.trim(), percentComplete: parsedPercent });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-progress-form" onSubmit={handleSubmit}>
      {error && <div className="bf-progress-form__error">{error}</div>}

      <Input
        label="Stage Name"
        required
        placeholder="Foundation, Structure, Brickwork…"
        value={stageName}
        onChange={(e) => setStageName(e.target.value)}
      />
      <Input
        label="Progress (%)"
        type="number"
        min="0"
        max="100"
        value={percentComplete}
        onChange={(e) => setPercentComplete(e.target.value)}
      />

      <div className="bf-progress-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Add Stage
        </Button>
      </div>
    </form>
  );
}
