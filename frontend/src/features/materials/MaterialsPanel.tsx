import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { submitOrQueue } from "../offline/submitOrQueue";
import type { MaterialUsageQueuePayload } from "../../services/syncManager";
import { materialReconciliationService } from "../../services/materialReconciliationService";
import { materialService } from "../../services/materialService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency, formatDate } from "../../utils/format";
import type {
  Material,
  MaterialPayload,
  MaterialPurchase,
  MaterialPurchasePayload,
  MaterialRequisition,
  MaterialRequisitionPayload,
  MaterialUsage,
  MaterialUsagePayload,
} from "../../types/material";
import type { MaterialReconciliationReport, MaterialStockCountPayload } from "../../types/materialReconciliation";
import { MaterialForm } from "./MaterialForm";
import { MaterialPurchaseForm } from "./MaterialPurchaseForm";
import { MaterialRequisitionForm } from "./MaterialRequisitionForm";
import { MaterialUsageForm } from "./MaterialUsageForm";
import { StockCountForm } from "./StockCountForm";
import { requisitionStatusTone, stockStatusTone } from "./statusTone";
import "./MaterialsPanel.css";

type MaterialSubTab = "inventory" | "purchases" | "usage" | "requests" | "reconciliation";

export function MaterialsPanel({ projectId }: { projectId: number }) {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [subTab, setSubTab] = useState<MaterialSubTab>("inventory");
  const [materials, setMaterials] = useState<Material[] | null>(null);
  const [purchases, setPurchases] = useState<MaterialPurchase[] | null>(null);
  const [usage, setUsage] = useState<MaterialUsage[] | null>(null);
  const [requisitions, setRequisitions] = useState<MaterialRequisition[] | null>(null);
  const [reconciliation, setReconciliation] = useState<MaterialReconciliationReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [isMaterialFormOpen, setIsMaterialFormOpen] = useState(false);
  const [isPurchaseFormOpen, setIsPurchaseFormOpen] = useState(false);
  const [isUsageFormOpen, setIsUsageFormOpen] = useState(false);
  const [isRequisitionFormOpen, setIsRequisitionFormOpen] = useState(false);
  const [isStockCountFormOpen, setIsStockCountFormOpen] = useState(false);

  const loadAll = useCallback(async () => {
    try {
      const [materialsData, purchasesData, usageData, requisitionsData, reconciliationData] = await Promise.all([
        materialService.list(projectId),
        materialService.listPurchases(projectId),
        materialService.listUsage(projectId),
        materialService.listRequisitions(projectId),
        materialReconciliationService.getReport(projectId),
      ]);
      setMaterials(materialsData);
      setPurchases(purchasesData);
      setUsage(usageData);
      setRequisitions(requisitionsData);
      setReconciliation(reconciliationData);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [projectId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  async function handleCreateMaterial(payload: MaterialPayload) {
    await materialService.create(projectId, payload);
    setIsMaterialFormOpen(false);
    showToast("success", "Material added successfully.");
    await loadAll();
  }

  async function handleCreatePurchase(materialId: number, payload: MaterialPurchasePayload) {
    await materialService.createPurchase(materialId, payload);
    setIsPurchaseFormOpen(false);
    showToast("success", "Purchase recorded successfully.");
    await loadAll();
  }

  async function handleCreateUsage(materialId: number, payload: MaterialUsagePayload) {
    const materialName = materials?.find((m) => m.id === materialId)?.name ?? "material";
    const queuePayload: MaterialUsageQueuePayload = { materialId, usage: payload };
    const { queued } = await submitOrQueue(
      "MATERIAL_USAGE",
      `${payload.quantity} used of ${materialName}`,
      projectId,
      queuePayload,
      () => materialService.createUsage(materialId, payload),
    );
    setIsUsageFormOpen(false);
    showToast(
      "success",
      queued
        ? "You're offline — this usage will be sent automatically once you're back online."
        : "Usage recorded successfully.",
    );
    if (!queued) {
      await loadAll();
    }
  }

  async function handleCreateRequisition(payload: MaterialRequisitionPayload) {
    await materialService.createRequisition(projectId, payload);
    setIsRequisitionFormOpen(false);
    showToast("success", "Material request submitted successfully.");
    await loadAll();
  }

  async function handleApprove(id: number) {
    try {
      await materialService.approveRequisition(id);
      showToast("success", "Request approved.");
      await loadAll();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    }
  }

  async function handleReject(id: number) {
    try {
      await materialService.rejectRequisition(id);
      showToast("success", "Request rejected.");
      await loadAll();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    }
  }

  async function handleCreateStockCount(materialId: number, payload: MaterialStockCountPayload) {
    await materialReconciliationService.createStockCount(materialId, payload);
    setIsStockCountFormOpen(false);
    showToast("success", "Stock count recorded successfully.");
    await loadAll();
  }

  if (error) {
    return <div className="bf-materials-panel__error">{error}</div>;
  }

  if (materials === null || purchases === null || usage === null || requisitions === null || reconciliation === null) {
    return <Skeleton height={280} />;
  }

  return (
    <div className="bf-materials-panel">
      <div className="bf-materials-panel__subtabs">
        <button className={subTab === "inventory" ? "bf-materials-panel__subtab--active" : ""} onClick={() => setSubTab("inventory")}>
          Inventory
        </button>
        <button className={subTab === "purchases" ? "bf-materials-panel__subtab--active" : ""} onClick={() => setSubTab("purchases")}>
          Purchases
        </button>
        <button className={subTab === "usage" ? "bf-materials-panel__subtab--active" : ""} onClick={() => setSubTab("usage")}>
          Usage
        </button>
        <button className={subTab === "requests" ? "bf-materials-panel__subtab--active" : ""} onClick={() => setSubTab("requests")}>
          Requests
        </button>
        <button
          className={subTab === "reconciliation" ? "bf-materials-panel__subtab--active" : ""}
          onClick={() => setSubTab("reconciliation")}
        >
          Reconciliation
        </button>
      </div>

      {subTab === "inventory" && (
        <div className="bf-materials-panel__section">
          <div className="bf-materials-panel__section-header">
            <h3>Material Inventory</h3>
            {canManage && <Button onClick={() => setIsMaterialFormOpen(true)}>+ Add Material</Button>}
          </div>
          <Card>
            {materials.length === 0 ? (
              <EmptyState
                title="No materials tracked yet."
                description="Add materials to start tracking stock, purchases, and usage for this project."
                action={canManage && <Button onClick={() => setIsMaterialFormOpen(true)}>+ Add Material</Button>}
              />
            ) : (
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Material</th>
                    <th>Available</th>
                    <th>Minimum</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {materials.map((material) => (
                    <tr key={material.id}>
                      <td>{material.name}</td>
                      <td>
                        {material.currentStock} {material.unit}
                      </td>
                      <td>
                        {material.minimumStock} {material.unit}
                      </td>
                      <td>
                        <Badge tone={stockStatusTone(material.status)}>{material.status}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
      )}

      {subTab === "purchases" && (
        <div className="bf-materials-panel__section">
          <div className="bf-materials-panel__section-header">
            <h3>Purchases</h3>
            {canManage && materials.length > 0 && (
              <Button onClick={() => setIsPurchaseFormOpen(true)}>+ Record Purchase</Button>
            )}
          </div>
          <Card>
            {purchases.length === 0 ? (
              <EmptyState
                title="No purchases recorded yet."
                description={
                  materials.length === 0
                    ? "Add a material first, then record purchases against it."
                    : "Record material purchases to keep stock and costs up to date."
                }
                action={
                  canManage &&
                  materials.length > 0 && <Button onClick={() => setIsPurchaseFormOpen(true)}>+ Record Purchase</Button>
                }
              />
            ) : (
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Material</th>
                    <th>Quantity</th>
                    <th>Rate</th>
                    <th>Amount</th>
                    <th>Supplier</th>
                    <th>Recorded By</th>
                  </tr>
                </thead>
                <tbody>
                  {purchases.map((purchase) => (
                    <tr key={purchase.id}>
                      <td>{formatDate(purchase.purchaseDate)}</td>
                      <td>{purchase.materialName}</td>
                      <td>{purchase.quantity}</td>
                      <td>{formatCurrency(purchase.rate)}</td>
                      <td>{formatCurrency(purchase.amount)}</td>
                      <td>{purchase.supplierName ?? "—"}</td>
                      <td>{purchase.createdByName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
      )}

      {subTab === "usage" && (
        <div className="bf-materials-panel__section">
          <div className="bf-materials-panel__section-header">
            <h3>Usage</h3>
            {materials.length > 0 && <Button onClick={() => setIsUsageFormOpen(true)}>+ Record Usage</Button>}
          </div>
          <Card>
            {usage.length === 0 ? (
              <EmptyState
                title="No usage recorded yet."
                description={
                  materials.length === 0
                    ? "Add a material first, then log site usage against it."
                    : "Log material usage as work happens on site."
                }
                action={materials.length > 0 && <Button onClick={() => setIsUsageFormOpen(true)}>+ Record Usage</Button>}
              />
            ) : (
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Material</th>
                    <th>Quantity Used</th>
                    <th>Notes</th>
                    <th>Recorded By</th>
                  </tr>
                </thead>
                <tbody>
                  {usage.map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDate(entry.usageDate)}</td>
                      <td>{entry.materialName}</td>
                      <td>{entry.quantity}</td>
                      <td>{entry.notes ?? "—"}</td>
                      <td>{entry.createdByName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
      )}

      {subTab === "requests" && (
        <div className="bf-materials-panel__section">
          <div className="bf-materials-panel__section-header">
            <h3>Material Requests</h3>
            {materials.length > 0 && (
              <Button onClick={() => setIsRequisitionFormOpen(true)}>+ New Request</Button>
            )}
          </div>
          <Card>
            {requisitions.length === 0 ? (
              <EmptyState
                title="No material requests yet."
                description={
                  materials.length === 0
                    ? "Add a material first, then request more when stock runs low."
                    : "Request more material when stock on site runs low."
                }
                action={
                  materials.length > 0 && <Button onClick={() => setIsRequisitionFormOpen(true)}>+ New Request</Button>
                }
              />
            ) : (
              <table className="bf-table">
                <thead>
                  <tr>
                    <th>Material</th>
                    <th>Quantity</th>
                    <th>Required Date</th>
                    <th>Reason</th>
                    <th>Requested By</th>
                    <th>Status</th>
                    {canManage && <th></th>}
                  </tr>
                </thead>
                <tbody>
                  {requisitions.map((req) => (
                    <tr key={req.id}>
                      <td>{req.materialName}</td>
                      <td>{req.quantity}</td>
                      <td>{formatDate(req.requiredDate)}</td>
                      <td>{req.reason ?? "—"}</td>
                      <td>{req.requestedByName}</td>
                      <td>
                        <Badge tone={requisitionStatusTone(req.status)}>{req.status}</Badge>
                      </td>
                      {canManage && (
                        <td className="bf-materials-panel__row-actions">
                          {req.status === "PENDING" && (
                            <>
                              <button onClick={() => handleApprove(req.id)}>Approve</button>
                              <button onClick={() => handleReject(req.id)}>Reject</button>
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
        </div>
      )}

      {subTab === "reconciliation" && (
        <div className="bf-materials-panel__section">
          <div className="bf-materials-panel__section-header">
            <div>
              <h3>Stock Reconciliation</h3>
              <p className="bf-materials-panel__reconciliation-subtitle">
                Compares system-tracked stock against physical site counts to surface shrinkage, theft, or wastage.
              </p>
            </div>
            {materials.length > 0 && <Button onClick={() => setIsStockCountFormOpen(true)}>+ Record Stock Count</Button>}
          </div>

          {reconciliation.materials.length === 0 ? (
            <Card>
              <EmptyState
                title="No materials to reconcile yet."
                description="Add a material first, then record a physical stock count to compare against the system."
              />
            </Card>
          ) : (
            <>
              <div
                className={`bf-materials-panel__variance-total ${
                  reconciliation.totalEstimatedVarianceValue < 0
                    ? "bf-materials-panel__variance-total--negative"
                    : "bf-materials-panel__variance-total--positive"
                }`}
              >
                Estimated value of all discrepancies to date:{" "}
                <strong>{formatCurrency(reconciliation.totalEstimatedVarianceValue)}</strong>
              </div>

              <Card>
                <table className="bf-table">
                  <thead>
                    <tr>
                      <th>Material</th>
                      <th>Current Stock</th>
                      <th>Total Purchased</th>
                      <th>Total Used</th>
                      <th>Avg. Rate</th>
                      <th>Last Count</th>
                      <th>Cumulative Variance</th>
                      <th>Estimated Value</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reconciliation.materials.map((row) => (
                      <tr key={row.materialId}>
                        <td>{row.materialName}</td>
                        <td>
                          {row.currentStock} {row.unit}
                        </td>
                        <td>
                          {row.totalPurchased} {row.unit}
                        </td>
                        <td>
                          {row.totalUsed} {row.unit}
                        </td>
                        <td>{formatCurrency(row.averageRate)}</td>
                        <td>{row.lastCountDate ? formatDate(row.lastCountDate) : "Never counted"}</td>
                        <td
                          className={
                            row.cumulativeVarianceQty < 0
                              ? "bf-materials-panel__variance-negative"
                              : row.cumulativeVarianceQty > 0
                                ? "bf-materials-panel__variance-positive"
                                : undefined
                          }
                        >
                          {row.cumulativeVarianceQty > 0 ? "+" : ""}
                          {row.cumulativeVarianceQty} {row.unit}
                        </td>
                        <td
                          className={
                            row.estimatedVarianceValue < 0
                              ? "bf-materials-panel__variance-negative"
                              : row.estimatedVarianceValue > 0
                                ? "bf-materials-panel__variance-positive"
                                : undefined
                          }
                        >
                          {formatCurrency(row.estimatedVarianceValue)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            </>
          )}
        </div>
      )}

      {isMaterialFormOpen && (
        <Modal title="Add Material" onClose={() => setIsMaterialFormOpen(false)}>
          <MaterialForm onSubmit={handleCreateMaterial} onCancel={() => setIsMaterialFormOpen(false)} />
        </Modal>
      )}

      {isPurchaseFormOpen && (
        <Modal title="Record Purchase" onClose={() => setIsPurchaseFormOpen(false)}>
          <MaterialPurchaseForm
            materials={materials}
            onSubmit={handleCreatePurchase}
            onCancel={() => setIsPurchaseFormOpen(false)}
          />
        </Modal>
      )}

      {isUsageFormOpen && (
        <Modal title="Record Usage" onClose={() => setIsUsageFormOpen(false)}>
          <MaterialUsageForm
            materials={materials}
            onSubmit={handleCreateUsage}
            onCancel={() => setIsUsageFormOpen(false)}
          />
        </Modal>
      )}

      {isRequisitionFormOpen && (
        <Modal title="New Material Request" onClose={() => setIsRequisitionFormOpen(false)}>
          <MaterialRequisitionForm
            materials={materials}
            onSubmit={handleCreateRequisition}
            onCancel={() => setIsRequisitionFormOpen(false)}
          />
        </Modal>
      )}

      {isStockCountFormOpen && (
        <Modal title="Record Stock Count" onClose={() => setIsStockCountFormOpen(false)}>
          <StockCountForm
            materials={materials}
            onSubmit={handleCreateStockCount}
            onCancel={() => setIsStockCountFormOpen(false)}
          />
        </Modal>
      )}
    </div>
  );
}
