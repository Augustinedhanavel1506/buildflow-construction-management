import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { estimationRuleService } from "../../services/estimationRuleService";
import { getErrorMessage } from "../../utils/apiError";
import type { BoqGenerationRule, BoqGenerationRulePayload } from "../../types/estimationRule";
import { useAuth } from "../auth/AuthContext";
import { EstimationRuleForm, SOURCE_TYPE_LABEL } from "./EstimationRuleForm";
import { GRADE_LABEL, componentIcon, componentLabel } from "./labels";
import type { ConstructionGrade } from "../../types/estimation";
import "./EstimationRulesPage.css";

function formatCoefficient(rule: BoqGenerationRule): string {
  const base = `${rule.coefficient} ${rule.unit}`;
  if (rule.minCoefficient !== null && rule.maxCoefficient !== null) {
    return `${base} (${rule.minCoefficient}–${rule.maxCoefficient})`;
  }
  return base;
}

export function EstimationRulesPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canEdit = user?.role === "ADMIN";

  const [rules, setRules] = useState<BoqGenerationRule[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [gradeFilter, setGradeFilter] = useState<string>("STANDARD");
  const [editing, setEditing] = useState<BoqGenerationRule | null>(null);
  const [reverting, setReverting] = useState<BoqGenerationRule | null>(null);
  const [isReverting, setIsReverting] = useState(false);

  const load = useCallback(async () => {
    try {
      setRules(await estimationRuleService.list());
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function handleSave(payload: BoqGenerationRulePayload) {
    if (!editing) return;
    await estimationRuleService.update(editing.id, payload);
    setEditing(null);
    showToast("success", "Rule saved. New estimates will use it; existing BOQs are unchanged until regenerated.");
    await load();
  }

  async function handleRevert() {
    if (!reverting) return;
    setIsReverting(true);
    try {
      await estimationRuleService.revert(reverting.id);
      setReverting(null);
      showToast("success", "Override removed.");
      await load();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsReverting(false);
    }
  }

  if (error) {
    return <div className="bf-rules-page__error">{error}</div>;
  }

  const visible = rules?.filter((r) => r.constructionGrade === gradeFilter) ?? [];
  const unverifiedCount = rules?.filter((r) => !r.verified && r.active).length ?? 0;

  return (
    <div className="bf-rules-page">
      <div>
        <h1>Estimation Rules</h1>
        <p className="bf-rules-page__intro">
          The coefficients behind every preliminary estimate. Replace placeholder numbers with figures from a schedule of
          rates or your own completed projects, and record the source so each quantity stays defensible.
        </p>
      </div>

      {unverifiedCount > 0 && (
        <div className="bf-rules-page__warning">
          {unverifiedCount} active rule{unverifiedCount === 1 ? " is" : "s are"} not verified yet, so estimates built on
          them are only illustrative.
        </div>
      )}

      <div className="bf-rules-page__tabs">
        {(["ECONOMY", "STANDARD", "PREMIUM"] as ConstructionGrade[]).map((grade) => (
          <button
            key={grade}
            className={`bf-rules-page__tab ${gradeFilter === grade ? "bf-rules-page__tab--active" : ""}`}
            onClick={() => setGradeFilter(grade)}
          >
            {GRADE_LABEL[grade]}
          </button>
        ))}
      </div>

      <Card>
        {rules === null ? (
          <Skeleton height={280} />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Component</th>
                <th>Per</th>
                <th>Coefficient</th>
                <th>Wastage</th>
                <th>Source</th>
                <th>Status</th>
                {canEdit && <th></th>}
              </tr>
            </thead>
            <tbody>
              {visible.map((rule) => (
                <tr key={rule.id}>
                  <td data-label="Item">{rule.itemName}</td>
                  <td data-label="Component">
                    {componentIcon(rule.component)} {componentLabel(rule.component)}
                  </td>
                  <td data-label="Per">{rule.basis.toLowerCase().replaceAll("_", " ")}</td>
                  <td data-label="Coefficient">{formatCoefficient(rule)}</td>
                  <td data-label="Wastage">{rule.wastagePercent}%</td>
                  <td data-label="Source" className="bf-rules-page__source" title={rule.sourceReference}>
                    {SOURCE_TYPE_LABEL[rule.sourceType]}
                  </td>
                  <td data-label="Status">
                    <div className="bf-rules-page__badges">
                      <Badge tone={rule.verified ? "success" : "warning"}>{rule.verified ? "Verified" : "Unverified"}</Badge>
                      {rule.scope === "BUSINESS" && <Badge tone="info">Your override v{rule.version}</Badge>}
                      {!rule.active && <Badge>Inactive</Badge>}
                    </div>
                  </td>
                  {canEdit && (
                    <td className="bf-rules-page__actions">
                      <button onClick={() => setEditing(rule)}>Edit</button>
                      {rule.scope === "BUSINESS" && <button onClick={() => setReverting(rule)}>Revert</button>}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {editing && (
        <Modal title={`Edit rule — ${editing.itemName}`} onClose={() => setEditing(null)}>
          <EstimationRuleForm rule={editing} onSubmit={handleSave} onCancel={() => setEditing(null)} />
        </Modal>
      )}

      {reverting && (
        <ConfirmDialog
          title="Revert to platform rule?"
          description={`Your override for ${reverting.itemName} will be removed and the shared platform coefficient will apply again.`}
          confirmLabel="Revert"
          isSubmitting={isReverting}
          onConfirm={handleRevert}
          onCancel={() => setReverting(null)}
        />
      )}
    </div>
  );
}
