import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import type { DailyReport, DailyReportPayload } from "../../types/dailyReport";
import "./DailyReportForm.css";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function DailyReportForm({
  initialValue,
  onSubmit,
  onCancel,
}: {
  initialValue?: DailyReport;
  onSubmit: (payload: DailyReportPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [reportDate, setReportDate] = useState(initialValue?.reportDate ?? today());
  const [workersPresent, setWorkersPresent] = useState(initialValue?.workersPresent?.toString() ?? "");
  const [workCompleted, setWorkCompleted] = useState(initialValue?.workCompleted ?? "");
  const [materialsUsed, setMaterialsUsed] = useState(initialValue?.materialsUsed ?? "");
  const [issues, setIssues] = useState(initialValue?.issues ?? "");
  const [notes, setNotes] = useState(initialValue?.notes ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!reportDate) {
      setError("Please select a date.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        reportDate,
        workersPresent: workersPresent ? Number(workersPresent) : undefined,
        workCompleted: workCompleted.trim() || undefined,
        materialsUsed: materialsUsed.trim() || undefined,
        issues: issues.trim() || undefined,
        notes: notes.trim() || undefined,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-daily-report-form" onSubmit={handleSubmit}>
      {error && <div className="bf-daily-report-form__error">{error}</div>}

      <div className="bf-daily-report-form__row">
        <Input label="Date" type="date" required value={reportDate} onChange={(e) => setReportDate(e.target.value)} />
        <Input
          label="Workers Present"
          type="number"
          min="0"
          value={workersPresent}
          onChange={(e) => setWorkersPresent(e.target.value)}
        />
      </div>

      <label className="bf-daily-report-form__label">
        Work Completed
        <textarea value={workCompleted} onChange={(e) => setWorkCompleted(e.target.value)} rows={3} />
      </label>

      <label className="bf-daily-report-form__label">
        Materials Used
        <textarea value={materialsUsed} onChange={(e) => setMaterialsUsed(e.target.value)} rows={2} />
      </label>

      <label className="bf-daily-report-form__label">
        Issues
        <textarea value={issues} onChange={(e) => setIssues(e.target.value)} rows={2} />
      </label>

      <label className="bf-daily-report-form__label">
        Notes
        <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
      </label>

      <div className="bf-daily-report-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          {initialValue ? "Save Changes" : "Submit Report"}
        </Button>
      </div>
    </form>
  );
}
