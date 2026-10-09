import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { businessService } from "../../services/businessService";
import { rateMasterService } from "../../services/rateMasterService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import { categoryLabel } from "../boq/categoryLabel";
import type { RateMasterItem, RateMasterItemPayload } from "../../types/rateMaster";
import { MaterialEstimatorPanel } from "./MaterialEstimatorPanel";
import { RateMasterItemForm } from "./RateMasterItemForm";
import "./RateMasterPage.css";

function formatRateCell(item: RateMasterItem): string {
  if (item.minRate !== null && item.maxRate !== null) {
    return `${formatCurrency(item.minRate)} – ${formatCurrency(item.maxRate)}`;
  }
  return formatCurrency(item.standardRate);
}

export function RateMasterPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [items, setItems] = useState<RateMasterItem[] | null>(null);
  const [region, setRegion] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  // "all" shows everything, "default" the rates with no district, otherwise one district's rates.
  const [districtFilter, setDistrictFilter] = useState("all");
  const [isCopying, setIsCopying] = useState(false);
  const [copyFrom, setCopyFrom] = useState("");
  const [copyTo, setCopyTo] = useState("");
  const [copyPercent, setCopyPercent] = useState("0");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<RateMasterItem | null>(null);

  const loadItems = useCallback(async () => {
    try {
      const data = await rateMasterService.list(true);
      setItems(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    loadItems();
    businessService.get().then((business) => setRegion(business.materialRegion)).catch(() => setRegion(null));
  }, [loadItems]);

  async function handleCreate(payload: RateMasterItemPayload) {
    await rateMasterService.create(payload);
    setIsFormOpen(false);
    showToast("success", "Rate master item added successfully.");
    await loadItems();
  }

  async function handleStarterRates() {
    try {
      const result = await rateMasterService.addStarterRates();
      showToast(
        "success",
        result.added === 0
          ? "You already have every starter item."
          : `${result.added} starter rates added. They are illustrative, so replace them with your local rates.`,
      );
      await loadItems();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    }
  }

  async function handleCopy() {
    if (!copyTo.trim()) {
      showToast("warning", "Enter the district to copy the rates into.");
      return;
    }
    try {
      const result = await rateMasterService.copyToDistrict({
        fromDistrict: copyFrom.trim() || undefined,
        toDistrict: copyTo.trim(),
        adjustPercent: Number(copyPercent) || 0,
      });
      setIsCopying(false);
      showToast("success", `${result.added} rates copied to ${copyTo.trim()}${result.skipped ? `; ${result.skipped} already there` : ""}. Edit them to the real local rates.`);
      setDistrictFilter(copyTo.trim());
      await loadItems();
    } catch (err) {
      showToast("error", getErrorMessage(err));
    }
  }

  async function handleUpdate(payload: RateMasterItemPayload) {
    if (!editingItem) return;
    await rateMasterService.update(editingItem.id, payload);
    setEditingItem(null);
    showToast("success", "Rate master item updated successfully.");
    await loadItems();
  }

  if (error) {
    return <div className="bf-rate-master-page__error">{error}</div>;
  }

  const districts = [...new Set((items ?? []).map((i) => i.district).filter(Boolean))].sort() as string[];
  const filteredItems =
    items?.filter(
      (item) =>
        item.itemName.toLowerCase().includes(search.toLowerCase()) &&
        (districtFilter === "all" ||
          (districtFilter === "default" ? item.district === null : item.district === districtFilter)),
    ) ?? [];

  return (
    <div className="bf-rate-master-page">
      <div className="bf-rate-master-page__header">
        <div>
          <h1>Rate Master</h1>
          <p>
            Live Building Material Rate Index{region ? ` — ${region}` : ""}. Set a min/max band per item instead of
            one fixed price, since raw material costs shift weekly with transport, brand, and bulk-order variables.
          </p>
        </div>
        {canManage && (
          <div style={{ display: "flex", gap: "var(--space-2)", flexWrap: "wrap" }}>
            <Button variant="secondary" onClick={handleStarterRates}>Load starter rates</Button>
            <Button onClick={() => setIsFormOpen(true)}>+ Add Rate</Button>
          </div>
        )}
      </div>

      {items && items.length > 0 && <MaterialEstimatorPanel items={items} />}

      {items && items.length > 0 && (
        <div style={{ display: "flex", gap: "var(--space-2)", flexWrap: "wrap", alignItems: "center" }}>
          <input
            className="bf-rate-master-page__search"
            placeholder="Search items…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select
            className="bf-rate-master-page__search"
            aria-label="District"
            value={districtFilter}
            onChange={(e) => setDistrictFilter(e.target.value)}
          >
            <option value="all">All districts</option>
            <option value="default">Default rates</option>
            {districts.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
          {canManage && <Button variant="secondary" onClick={() => setIsCopying(true)}>Copy to district</Button>}
        </div>
      )}

      <Card>
        {items === null ? (
          <Skeleton height={280} />
        ) : items.length === 0 ? (
          <EmptyState
            title="No rate master items yet."
            description="Build a reusable price list once, then pull it into every project's BOQ instead of retyping rates each time."
            action={canManage && (
              <div style={{ display: "flex", gap: "var(--space-2)", justifyContent: "center", flexWrap: "wrap" }}>
                <Button onClick={handleStarterRates}>Load starter rates</Button>
                <Button variant="secondary" onClick={() => setIsFormOpen(true)}>+ Add Rate</Button>
              </div>
            )}
          />
        ) : (
          <table className="bf-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>District</th>
                <th>Category</th>
                <th>Unit</th>
                <th>Market Price</th>
                <th>Notes</th>
                <th>Status</th>
                {canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.id}>
                  <td>{item.itemName}</td>
                  <td>{item.district ?? "Default"}</td>
                  <td>{categoryLabel(item.category)}</td>
                  <td>{item.unit}</td>
                  <td>{formatRateCell(item)}</td>
                  <td className="bf-rate-master-page__notes">{item.notes ?? "—"}</td>
                  <td>
                    <Badge tone={item.active ? "success" : "neutral"}>{item.active ? "Active" : "Inactive"}</Badge>
                  </td>
                  {canManage && (
                    <td className="bf-rate-master-page__row-actions">
                      <button onClick={() => setEditingItem(item)}>Edit</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {isFormOpen && (
        <Modal title="Add Rate Master Item" onClose={() => setIsFormOpen(false)}>
          <RateMasterItemForm
            districts={districts}
            defaultDistrict={districtFilter !== "all" && districtFilter !== "default" ? districtFilter : undefined}
            onSubmit={handleCreate}
            onCancel={() => setIsFormOpen(false)}
          />
        </Modal>
      )}

      {isCopying && (
        <Modal title="Copy rates to a district" onClose={() => setIsCopying(false)}>
          <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-4)" }}>
            <p style={{ fontSize: 13, color: "var(--text-secondary)" }}>
              Start a district from existing rates, raised or lowered by a percentage you choose, then edit the ones you
              know. Items the district already has are left alone.
            </p>
            <Input label="Copy from (blank = default rates)" list="bf-copy-districts" value={copyFrom} onChange={(e) => setCopyFrom(e.target.value)} />
            <Input label="Copy into district" list="bf-copy-districts" placeholder="e.g. Coimbatore" value={copyTo} onChange={(e) => setCopyTo(e.target.value)} />
            <datalist id="bf-copy-districts">
              {districts.map((d) => (
                <option key={d} value={d} />
              ))}
            </datalist>
            <Input label="Adjust by (%)" type="number" min="-50" max="100" step="1" value={copyPercent} onChange={(e) => setCopyPercent(e.target.value)} />
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "var(--space-3)" }}>
              <Button variant="secondary" onClick={() => setIsCopying(false)}>Cancel</Button>
              <Button onClick={handleCopy}>Copy rates</Button>
            </div>
          </div>
        </Modal>
      )}

      {editingItem && (
        <Modal title="Edit Rate Master Item" onClose={() => setEditingItem(null)}>
          <RateMasterItemForm initialValue={editingItem} districts={districts} onSubmit={handleUpdate} onCancel={() => setEditingItem(null)} />
        </Modal>
      )}
    </div>
  );
}
