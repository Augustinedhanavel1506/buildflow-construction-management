import { useCallback, useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { boqService } from "../../services/boqService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { BoqItem, BoqItemPayload } from "../../types/boq";
import { BoqItemForm } from "./BoqItemForm";
import { categoryLabel } from "./categoryLabel";
import "./BoqPanel.css";

export function BoqPanel({ projectId }: { projectId: number }) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [items, setItems] = useState<BoqItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<BoqItem | null>(null);
  const [deletingItem, setDeletingItem] = useState<BoqItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const loadItems = useCallback(async () => {
    try {
      const data = await boqService.list(projectId);
      setItems(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  async function handleCreate(payload: BoqItemPayload) {
    await boqService.create(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "BOQ item added successfully.");
    await loadItems();
  }

  async function handleUpdate(payload: BoqItemPayload) {
    if (!editingItem) return;
    await boqService.update(editingItem.id, payload);
    setEditingItem(null);
    showToast("success", "BOQ item updated successfully.");
    await loadItems();
  }

  async function handleDelete() {
    if (!deletingItem) return;
    setIsDeleting(true);
    try {
      await boqService.remove(deletingItem.id);
      showToast("success", "BOQ item deleted successfully.");
      await loadItems();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsDeleting(false);
      setDeletingItem(null);
    }
  }

  if (error) {
    return <div className="bf-boq-panel__error">{error}</div>;
  }

  if (items === null) {
    return <Skeleton height={240} />;
  }

  const totalEstimated = items.reduce((sum, item) => sum + Number(item.estimatedAmount), 0);
  const totalActual = items.reduce((sum, item) => sum + Number(item.actualAmount), 0);
  const totalVariance = totalActual - totalEstimated;

  return (
    <div className="bf-boq-panel">
      <div className="bf-boq-panel__header">
        <h3>BOQ & Estimates</h3>
        {canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Item</Button>}
      </div>

      <Card>
        {items.length === 0 ? (
          <EmptyState
            title="No BOQ items yet."
            description="Add materials, labour, and other line items to build this project's estimate."
            action={canManage && <Button onClick={() => setIsFormOpen(true)}>+ Add Item</Button>}
          />
        ) : (
          <>
            <table className="bf-table">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Category</th>
                  <th>Unit</th>
                  <th>Quantity</th>
                  <th>Rate</th>
                  <th>Estimated</th>
                  <th>Actual</th>
                  <th>Variance</th>
                  {canManage && <th></th>}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.itemName}</td>
                    <td>{categoryLabel(item.category)}</td>
                    <td>{item.unit}</td>
                    <td>{item.quantity}</td>
                    <td>{formatCurrency(item.rate)}</td>
                    <td>{formatCurrency(item.estimatedAmount)}</td>
                    <td>{formatCurrency(item.actualAmount)}</td>
                    <td className={item.variance > 0 ? "bf-boq-panel__variance-over" : "bf-boq-panel__variance-under"}>
                      {item.variance > 0 ? "+" : ""}
                      {formatCurrency(item.variance)}
                    </td>
                    {canManage && (
                      <td className="bf-boq-panel__row-actions">
                        <button onClick={() => setEditingItem(item)}>Edit</button>
                        <button onClick={() => setDeletingItem(item)}>Delete</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={5}>
                    <strong>Total</strong>
                  </td>
                  <td>
                    <strong>{formatCurrency(totalEstimated)}</strong>
                  </td>
                  <td>
                    <strong>{formatCurrency(totalActual)}</strong>
                  </td>
                  <td className={totalVariance > 0 ? "bf-boq-panel__variance-over" : "bf-boq-panel__variance-under"}>
                    <strong>
                      {totalVariance > 0 ? "+" : ""}
                      {formatCurrency(totalVariance)}
                    </strong>
                  </td>
                  {canManage && <td></td>}
                </tr>
              </tfoot>
            </table>
          </>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="Add BOQ Item" onClose={() => setIsFormOpen(false)}>
          <BoqItemForm onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {editingItem && (
        <Modal title="Edit BOQ Item" onClose={() => setEditingItem(null)}>
          <BoqItemForm initialValue={editingItem} onSubmit={handleUpdate} onCancel={() => setEditingItem(null)} />
        </Modal>
      )}

      {deletingItem && (
        <ConfirmDialog
          title="Delete BOQ Item?"
          description={`"${deletingItem.itemName}" will be permanently removed from this project's estimate.`}
          confirmLabel="Delete"
          isSubmitting={isDeleting}
          onConfirm={handleDelete}
          onCancel={() => setDeletingItem(null)}
        />
      )}
    </div>
  );
}
