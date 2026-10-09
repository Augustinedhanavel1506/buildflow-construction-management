import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { subcontractorService } from "../../services/subcontractorService";
import { getErrorMessage } from "../../utils/apiError";
import type { Subcontractor, SubcontractorPayload } from "../../types/subcontractor";
import { SubcontractorForm } from "./SubcontractorForm";
import "./SubcontractorsPage.css";

export function SubcontractorsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [subcontractors, setSubcontractors] = useState<Subcontractor[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSubcontractor, setEditingSubcontractor] = useState<Subcontractor | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await subcontractorService.list(true);
      setSubcontractors(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function handleCreate(payload: SubcontractorPayload) {
    await subcontractorService.create(payload);
    setIsFormOpen(false);
    showToast("success", "Subcontractor added successfully.");
    await load();
  }

  async function handleUpdate(payload: SubcontractorPayload) {
    if (!editingSubcontractor) return;
    await subcontractorService.update(editingSubcontractor.id, payload);
    setEditingSubcontractor(null);
    showToast("success", "Subcontractor updated successfully.");
    await load();
  }

  if (error) {
    return <div className="bf-subcontractors-page__error">{error}</div>;
  }

  return (
    <div className="bf-subcontractors-page">
      <div className="bf-subcontractors-page__header">
        <div>
          <h1>Subcontractors</h1>
          <p>Your subcontractor directory — assign work orders to them from any project's Subcontractors tab.</p>
        </div>
        {canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Subcontractor</Button>}
      </div>

      <Card>
        {subcontractors === null ? (
          <Skeleton height={280} />
        ) : subcontractors.length === 0 ? (
          <EmptyState
            title="No subcontractors yet."
            description="Add electricians, plumbers, tilers, and other trades you regularly subcontract work to."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Subcontractor</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Trade</th>
                <th>Contact</th>
                <th>GST Number</th>
                <th>Status</th>
                {canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {subcontractors.map((sub) => (
                <tr key={sub.id}>
                  <td>{sub.name}</td>
                  <td>{sub.tradeType}</td>
                  <td>
                    {sub.contactPerson ?? "—"}
                    {sub.phone ? ` · ${sub.phone}` : ""}
                  </td>
                  <td>{sub.gstNumber ?? "—"}</td>
                  <td>
                    <Badge tone={sub.active ? "success" : "neutral"}>{sub.active ? "Active" : "Inactive"}</Badge>
                  </td>
                  {canManage && (
                    <td className="bf-subcontractors-page__row-actions">
                      <button onClick={() => setEditingSubcontractor(sub)}>Edit</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="Add Subcontractor" onClose={() => setIsFormOpen(false)}>
          <SubcontractorForm onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {editingSubcontractor && (
        <Modal title="Edit Subcontractor" onClose={() => setEditingSubcontractor(null)}>
          <SubcontractorForm
            initialValue={editingSubcontractor}
            onSubmit={handleUpdate}
            onCancel={() => setEditingSubcontractor(null)}
          />
        </Modal>
      )}
    </div>
  );
}
