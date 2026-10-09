import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import type {
  BoqGenerationRule,
  BoqGenerationRulePayload,
  ConfidenceLevel,
  RuleSourceType,
} from "../../types/estimationRule";
import "./EstimationRuleForm.css";

export const SOURCE_TYPE_LABEL: Record<RuleSourceType, string> = {
  GOVT_SOR: "Government Schedule of Rates",
  GOVT_DSR: "Government DSR / consumption coefficients",
  COMPLETED_PROJECT_AVERAGE: "Average of completed projects",
  VENDOR_QUOTE: "Vendor quote",
  PLACEHOLDER: "Placeholder (not sourced)",
};

const CONFIDENCE_LEVELS: ConfidenceLevel[] = ["HIGH", "MEDIUM", "LOW"];

export function EstimationRuleForm({
  rule,
  onSubmit,
  onCancel,
}: {
  rule: BoqGenerationRule;
  onSubmit: (payload: BoqGenerationRulePayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [coefficient, setCoefficient] = useState(rule.coefficient.toString());
  const [wastagePercent, setWastagePercent] = useState(rule.wastagePercent.toString());
  const [minCoefficient, setMinCoefficient] = useState(rule.minCoefficient?.toString() ?? "");
  const [maxCoefficient, setMaxCoefficient] = useState(rule.maxCoefficient?.toString() ?? "");
  const [sourceType, setSourceType] = useState<RuleSourceType>(rule.sourceType);
  const [sourceReference, setSourceReference] = useState(rule.sourceReference);
  const [confidenceLevel, setConfidenceLevel] = useState<ConfidenceLevel>(rule.confidenceLevel);
  const [verified, setVerified] = useState(rule.verified);
  const [sampleSize, setSampleSize] = useState(rule.sampleSize?.toString() ?? "");
  const [notes, setNotes] = useState(rule.notes ?? "");
  const [active, setActive] = useState(rule.active);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const parsedCoefficient = Number(coefficient);
    if (!coefficient || Number.isNaN(parsedCoefficient) || parsedCoefficient < 0) {
      setError("Enter a valid coefficient.");
      return;
    }
    const parsedWastage = Number(wastagePercent || "0");
    if (Number.isNaN(parsedWastage) || parsedWastage < 0 || parsedWastage > 100) {
      setError("Wastage must be between 0 and 100%.");
      return;
    }
    const parsedMin = minCoefficient ? Number(minCoefficient) : undefined;
    const parsedMax = maxCoefficient ? Number(maxCoefficient) : undefined;
    if (parsedMin !== undefined && parsedMin > parsedCoefficient) {
      setError("Minimum coefficient cannot exceed the coefficient.");
      return;
    }
    if (parsedMax !== undefined && parsedMax < parsedCoefficient) {
      setError("Maximum coefficient cannot be below the coefficient.");
      return;
    }
    if (!sourceReference.trim()) {
      setError("Say where this number came from, e.g. \"TN PWD SOR 2025-26, Table 4\".");
      return;
    }
    if (verified && sourceType === "PLACEHOLDER") {
      setError("A placeholder cannot be marked verified. Choose its real source first.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        coefficient: parsedCoefficient,
        wastagePercent: parsedWastage,
        minCoefficient: parsedMin,
        maxCoefficient: parsedMax,
        sourceType,
        sourceReference: sourceReference.trim(),
        confidenceLevel,
        verified,
        sampleSize: sampleSize ? Number(sampleSize) : undefined,
        notes: notes.trim() || undefined,
        active,
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-rule-form" onSubmit={handleSubmit}>
      {error && <div className="bf-rule-form__error">{error}</div>}

      <p className="bf-rule-form__context">
        <strong>{rule.itemName}</strong> · {rule.constructionGrade.toLowerCase()} grade · {rule.unit} per {rule.basis.toLowerCase().replaceAll("_", " ")}
      </p>
      {rule.scope === "PLATFORM" && (
        <p className="bf-rule-form__hint">
          This is a shared platform rule. Saving creates your own override that applies only to your business.
        </p>
      )}

      <div className="bf-rule-form__row">
        <Input label="Coefficient" type="number" min="0" step="0.0001" required value={coefficient} onChange={(e) => setCoefficient(e.target.value)} />
        <Input label="Wastage (%)" type="number" min="0" max="100" step="0.01" value={wastagePercent} onChange={(e) => setWastagePercent(e.target.value)} />
      </div>
      <div className="bf-rule-form__row">
        <Input label="Min coefficient (optional)" type="number" min="0" step="0.0001" value={minCoefficient} onChange={(e) => setMinCoefficient(e.target.value)} />
        <Input label="Max coefficient (optional)" type="number" min="0" step="0.0001" value={maxCoefficient} onChange={(e) => setMaxCoefficient(e.target.value)} />
      </div>

      <div className="bf-rule-form__row">
        <Select label="Source" value={sourceType} onChange={(e) => setSourceType(e.target.value as RuleSourceType)}>
          {(Object.keys(SOURCE_TYPE_LABEL) as RuleSourceType[]).map((s) => (
            <option key={s} value={s}>{SOURCE_TYPE_LABEL[s]}</option>
          ))}
        </Select>
        <Select label="Confidence" value={confidenceLevel} onChange={(e) => setConfidenceLevel(e.target.value as ConfidenceLevel)}>
          {CONFIDENCE_LEVELS.map((c) => (
            <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
          ))}
        </Select>
      </div>

      <Input label="Source reference" required placeholder="e.g. TN PWD SOR 2025-26, Table 4" value={sourceReference} onChange={(e) => setSourceReference(e.target.value)} />
      <Input label="Sample size (completed projects, optional)" type="number" min="0" step="1" value={sampleSize} onChange={(e) => setSampleSize(e.target.value)} />
      <Input label="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} />

      <label className="bf-rule-form__checkbox">
        <input type="checkbox" checked={verified} onChange={(e) => setVerified(e.target.checked)} />
        Verified against its source
      </label>
      <label className="bf-rule-form__checkbox">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Active (used when generating estimates)
      </label>

      <div className="bf-rule-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting}>Save Rule</Button>
      </div>
    </form>
  );
}
