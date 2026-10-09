import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { variationService } from "../../services/variationService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type { VariationOrder, VariationOrderPayload } from "../../types/variation";
import { VariationOrderForm } from "./VariationOrderForm";
import { variationStatusTone } from "./statusTone";
import "./VariationsPanel.css";

interface VariationsPanelProps {
  projectId: number;
  onVariationsChanged?: () => void;
}

export function VariationsPanel({ projectId, onVariationsChanged }: VariationsPanelProps) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [variations, setVariations] = useState<VariationOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [decidingId, setDecidingId] = useState<number | null>(null);

  const loadVariations = useCallback(async () => {
    try {
      const data = await variationService.list(projectId);
      setVariations(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadVariations();
  }, [loadVariations]);

  async function handleCreate(payload: VariationOrderPayload) {
    await variationService.create(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "Variation order submitted successfully.");
    await loadVariations();
  }

  async function handleApprove(id: number) {
    setDecidingId(id);
    try {
      await variationService.approve(id);
      showToast("success", "Variation order approved.");
      await loadVariations();
      onVariationsChanged?.();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setDecidingId(null);
    }
  }

  async function handleReject(id: number) {
    setDecidingId(id);
    try {
      await variationService.reject(id);
      showToast("success", "Variation order rejected.");
      await loadVariations();
      onVariationsChanged?.();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setDecidingId(null);
    }
  }

  if (error) {
    return <div className="bf-variations-panel__error">{error}</div>;
  }

  if (variations === null) {
    return <Skeleton height={240} />;
  }

  const netApproved = variations
    .filter((v) => v.status === "APPROVED")
    .reduce((sum, v) => sum + (v.type === "ADDITION" ? v.amount : -v.amount), 0);

  return (
    <div className="bf-variations-panel">
      <div className="bf-variations-panel__header">
        <div>
          <h3>Change Orders / Variations</h3>
          <p>
            Net approved impact on contract value:{" "}
            <strong className={netApproved >= 0 ? "bf-variations-panel__positive" : "bf-variations-panel__negative"}>
              {netApproved >= 0 ? "+" : ""}
              {formatCurrency(netApproved)}
            </strong>
          </p>
        </div>
        {canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Variation</Button>}
      </div>

      <Card>
        {variations.length === 0 ? (
          <EmptyState
            title="No variation orders yet."
            description="Record client-requested scope changes here to keep the contract value accurate."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ New Variation</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Requested By</th>
                <th>Date</th>
                <th>Status</th>
                {canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {variations.map((variation) => (
                <tr key={variation.id}>
                  <td>{variation.title}</td>
                  <td>{variation.type === "ADDITION" ? "Addition" : "Omission"}</td>
                  <td className={variation.type === "ADDITION" ? "bf-variations-panel__positive" : "bf-variations-panel__negative"}>
                    {variation.type === "ADDITION" ? "+" : "-"}
                    {formatCurrency(variation.amount)}
                  </td>
                  <td>{variation.requestedByName}</td>
                  <td>{formatDate(variation.createdAt)}</td>
                  <td>
                    <Badge tone={variationStatusTone(variation.status)}>{variation.status}</Badge>
                  </td>
                  {canManage && (
                    <td className="bf-variations-panel__row-actions">
                      {variation.status === "PENDING" && (
                        <>
                          <button onClick={() => handleApprove(variation.id)} disabled={decidingId === variation.id}>
                            Approve
                          </button>
                          <button onClick={() => handleReject(variation.id)} disabled={decidingId === variation.id}>
                            Reject
                          </button>
                        </>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="New Variation Order" onClose={() => setIsFormOpen(false)}>
          <VariationOrderForm onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
