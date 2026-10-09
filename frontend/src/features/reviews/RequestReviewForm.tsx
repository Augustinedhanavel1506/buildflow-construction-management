import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { reviewService, teamService } from "../../services/reviewService";
import { getErrorMessage } from "../../utils/apiError";
import type { Engineer, Review } from "../../types/review";

export function RequestReviewForm({
  projectId,
  onCreated,
  onCancel,
}: {
  projectId: number;
  onCreated: (review: Review) => void;
  onCancel: () => void;
}) {
  const [engineers, setEngineers] = useState<Engineer[] | null>(null);
  const [engineerId, setEngineerId] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    teamService
      .listEngineers()
      .then((list) => {
        const active = list.filter((e) => e.active);
        setEngineers(active);
        if (active.length === 1) setEngineerId(String(active[0].id));
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!engineerId) {
      setError("Choose an engineer.");
      return;
    }
    setIsSubmitting(true);
    try {
      onCreated(
        await reviewService.create({ projectId, engineerId: Number(engineerId), message: message.trim() || undefined }),
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "var(--space-4)" }}>
      {error && (
        <div style={{ background: "var(--danger-soft)", color: "var(--danger)", padding: "var(--space-3)", borderRadius: "var(--radius-md)", fontSize: 13 }}>
          {error}
        </div>
      )}
      {engineers !== null && engineers.length === 0 ? (
        <p style={{ fontSize: 13, color: "var(--text-secondary)" }}>
          You have no active engineers yet. An admin can add one under <Link to="/team">Team</Link>.
        </p>
      ) : (
        <>
          <Select label="Engineer" value={engineerId} onChange={(e) => setEngineerId(e.target.value)}>
            <option value="">Select an engineer…</option>
            {engineers?.map((e) => (
              <option key={e.id} value={e.id}>
                {e.fullName}
                {e.registrationNo ? ` (${e.registrationNo})` : ""}
              </option>
            ))}
          </Select>
          <Input label="Message (optional)" placeholder="e.g. please check the slab and foundation quantities" value={message} onChange={(e) => setMessage(e.target.value)} />
          <p style={{ fontSize: 12, color: "var(--text-muted)" }}>
            The engineer sees the plan and every line, can correct quantities, and validates them under their own name.
          </p>
        </>
      )}
      <div style={{ display: "flex", justifyContent: "flex-end", gap: "var(--space-3)" }}>
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting} disabled={!engineers || engineers.length === 0}>Send Request</Button>
      </div>
    </form>
  );
}
