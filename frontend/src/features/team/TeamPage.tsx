import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { teamService } from "../../services/reviewService";
import { getErrorMessage } from "../../utils/apiError";
import type { Engineer } from "../../types/review";
import { useAuth } from "../auth/AuthContext";
import "./TeamPage.css";

function EngineerForm({ onCreated, onCancel }: { onCreated: () => Promise<void>; onCancel: () => void }) {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [registrationNo, setRegistrationNo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!fullName.trim() || !email.trim()) {
      setError("Enter the engineer's name and email.");
      return;
    }
    if (password.length < 8) {
      setError("The password must be at least 8 characters.");
      return;
    }
    setIsSubmitting(true);
    try {
      await teamService.createEngineer({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        registrationNo: registrationNo.trim() || undefined,
      });
      await onCreated();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-team-form" onSubmit={handleSubmit}>
      {error && <div className="bf-team-form__error">{error}</div>}
      <Input label="Full name" required value={fullName} onChange={(e) => setFullName(e.target.value)} />
      <Input label="Email (used to sign in)" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
      <Input
        label="Registration number (optional)"
        placeholder="Shown beside their name on validated lines"
        value={registrationNo}
        onChange={(e) => setRegistrationNo(e.target.value)}
      />
      <Input label="Temporary password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
      <p className="bf-team-form__hint">
        Share these sign-in details with the engineer. They can only see review requests assigned to them, the
        calculators and their notifications.
      </p>
      <div className="bf-team-form__actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" isLoading={isSubmitting}>Create Account</Button>
      </div>
    </form>
  );
}

export function TeamPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [engineers, setEngineers] = useState<Engineer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const load = useCallback(async () => {
    try {
      setEngineers(await teamService.listEngineers());
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function toggleActive(engineer: Engineer) {
    try {
      await teamService.setActive(engineer.id, !engineer.active);
      showToast("success", engineer.active ? "Engineer deactivated." : "Engineer reactivated.");
      await load();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    }
  }

  if (error) {
    return <div className="bf-team-page__error">{error}</div>;
  }
  const isAdmin = user?.role === "ADMIN";

  return (
    <div className="bf-team-page">
      <div className="bf-team-page__header">
        <div>
          <h1>Team</h1>
          <p>
            Engineers who review and validate your estimates. They sign in with their own account and see only the
            reviews you assign to them.
          </p>
        </div>
        {isAdmin && <Button onClick={() => setIsCreating(true)}>+ Add Engineer</Button>}
      </div>

      <Card>
        {engineers === null ? (
          <Skeleton height={160} />
        ) : engineers.length === 0 ? (
          <EmptyState
            title="No engineers yet."
            description="Add an engineer to send a project's estimate for professional review and validation."
            action={isAdmin && <Button onClick={() => setIsCreating(true)}>+ Add Engineer</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Registration</th>
                <th>Status</th>
                {isAdmin && <th></th>}
              </tr>
            </thead>
            <tbody>
              {engineers.map((engineer) => (
                <tr key={engineer.id}>
                  <td data-label="Name">{engineer.fullName}</td>
                  <td data-label="Email">{engineer.email}</td>
                  <td data-label="Registration">{engineer.registrationNo ?? "—"}</td>
                  <td data-label="Status">
                    <Badge tone={engineer.active ? "success" : "neutral"}>{engineer.active ? "Active" : "Inactive"}</Badge>
                  </td>
                  {isAdmin && (
                    <td className="bf-team-page__actions">
                      <button onClick={() => toggleActive(engineer)}>{engineer.active ? "Deactivate" : "Reactivate"}</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isCreating && (
        <Modal title="Add Engineer" onClose={() => setIsCreating(false)}>
          <EngineerForm
            onCreated={async () => {
              setIsCreating(false);
              showToast("success", "Engineer account created.");
              await load();
            }}
            onCancel={() => setIsCreating(false)}
          />
        </Modal>
      )}
    </div>
  );
}
