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
import { formatCurrency } from "../../utils/format";
import type { Subcontractor, WorkOrder, WorkOrderPayload } from "../../types/subcontractor";
import { WorkOrderBillsModal } from "./WorkOrderBillsModal";
import { WorkOrderForm } from "./WorkOrderForm";
import { workOrderStatusTone } from "./statusTone";
import "./WorkOrdersPanel.css";

interface WorkOrdersPanelProps {
  projectId: number;
  onCostChanged?: () => void;
}

export function WorkOrdersPanel({ projectId, onCostChanged }: WorkOrdersPanelProps) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [workOrders, setWorkOrders] = useState<WorkOrder[] | null>(null);
  const [subcontractors, setSubcontractors] = useState<Subcontractor[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [viewingWorkOrder, setViewingWorkOrder] = useState<WorkOrder | null>(null);

  const loadAll = useCallback(async () => {
    try {
      const [workOrdersData, subcontractorsData] = await Promise.all([
        subcontractorService.listWorkOrders(projectId),
        subcontractorService.list(),
      ]);
      setWorkOrders(workOrdersData);
      setSubcontractors(subcontractorsData);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  async function handleCreate(payload: WorkOrderPayload) {
    await subcontractorService.createWorkOrder(projectId, payload);
    setIsFormOpen(false);
    showToast("success", "Work order created successfully.");
    await loadAll();
  }

  if (error) {
    return <div className="bf-work-orders-panel__error">{error}</div>;
  }

  if (workOrders === null) {
    return <Skeleton height={280} />;
  }

  return (
    <div className="bf-work-orders-panel">
      <div className="bf-work-orders-panel__header">
        <h3>Subcontractors</h3>
        {canManage && subcontractors.length > 0 && (
          <Button onClick={() => setIsFormOpen(true)}>+ New Work Order</Button>
        )}
      </div>

      <Card>
        {workOrders.length === 0 ? (
          <EmptyState
            title="No subcontract work orders yet."
            description={
              subcontractors.length === 0
                ? "Add a subcontractor from the Subcontractors page first, then assign work orders here."
                : "Assign scope of work to a subcontractor to start tracking their bills and payments."
            }
            action={canManage && subcontractors.length > 0 && <Button onClick={() => setIsFormOpen(true)}>+ New Work Order</Button>}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Subcontractor</th>
                <th>Title</th>
                <th>Contract Value</th>
                <th>Billed</th>
                <th>Paid</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {workOrders.map((wo) => (
                <tr key={wo.id}>
                  <td>
                    {wo.subcontractorName}
                    <div className="bf-work-orders-panel__trade">{wo.tradeType}</div>
                  </td>
                  <td>{wo.title}</td>
                  <td>{formatCurrency(wo.contractValue)}</td>
                  <td>{formatCurrency(wo.totalBilled)}</td>
                  <td>{formatCurrency(wo.totalPaid)}</td>
                  <td>
                    <Badge tone={workOrderStatusTone(wo.status)}>{wo.status}</Badge>
                  </td>
                  <td>
                    <button className="bf-work-orders-panel__bills-link" onClick={() => setViewingWorkOrder(wo)}>
                      Bills
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="New Work Order" onClose={() => setIsFormOpen(false)}>
          <WorkOrderForm subcontractors={subcontractors} onSubmit={handleCreate} onCancel={() => setIsFormOpen(false)} />
        </Modal>
      )}

      {viewingWorkOrder && (
        <WorkOrderBillsModal
          workOrder={viewingWorkOrder}
          canManage={canManage}
          onClose={() => setViewingWorkOrder(null)}
          onChanged={() => {
            loadAll();
            onCostChanged?.();
          }}
        />
      )}
    </div>
  );
}
