import { useCallback, useEffect, useState } from "react";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { dealerService } from "../../services/dealerService";
import { rateMasterService } from "../../services/rateMasterService";
import { getErrorMessage } from "../../utils/apiError";
import { formatCurrency } from "../../utils/format";
import type { Dealer, DealerPayload } from "../../types/dealer";
import { useAuth } from "../auth/AuthContext";
import { DealerForm } from "./DealerForm";
import "./DealersPage.css";

export function DealersPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManage = user?.role === "ADMIN" || user?.role === "PROJECT_MANAGER";

  const [dealers, setDealers] = useState<Dealer[] | null>(null);
  const [suggestions, setSuggestions] = useState<{ itemName: string; unit: string }[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  // "new" opens the add form, a Dealer opens the edit form, null is closed.
  const [formTarget, setFormTarget] = useState<Dealer | "new" | null>(null);

  const load = useCallback(async () => {
    try {
      setDealers(await dealerService.list(true));
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
    rateMasterService
      .list()
      .then((items) => setSuggestions(items.map((i) => ({ itemName: i.itemName, unit: i.unit }))))
      .catch(() => setSuggestions([]));
  }, [load]);

  async function handleSave(payload: DealerPayload) {
    if (formTarget === "new") {
      await dealerService.create(payload);
      showToast("success", "Dealer added successfully.");
    } else if (formTarget) {
      await dealerService.update(formTarget.id, payload);
      showToast("success", "Dealer updated successfully.");
    }
    setFormTarget(null);
    await load();
  }

  if (error) {
    return <div className="bf-dealers-page__error">{error}</div>;
  }

  const term = search.trim().toLowerCase();
  const filtered =
    dealers?.filter(
      (d) =>
        !term ||
        d.name.toLowerCase().includes(term) ||
        (d.district ?? "").toLowerCase().includes(term) ||
        d.rates.some((r) => r.itemName.toLowerCase().includes(term)),
    ) ?? [];

  return (
    <div className="bf-dealers-page">
      <div className="bf-dealers-page__header">
        <div>
          <h1>Dealers</h1>
          <p>
            Your local material suppliers and their rate cards. Pick dealers from here when requesting quotes for a
            project's materials.
          </p>
        </div>
        {canManage && <Button onClick={() => setFormTarget("new")}>+ Add Dealer</Button>}
      </div>

      {dealers && dealers.length > 0 && (
        <input
          className="bf-dealers-page__search"
          placeholder="Search by name, district or material…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      )}

      {dealers === null ? (
        <Skeleton height={240} />
      ) : dealers.length === 0 ? (
        <Card>
          <EmptyState
            title="No dealers yet."
            description="Add the cement, steel, sand and brick suppliers you buy from, with their rates, to compare quotes in one place."
            action={canManage && <Button onClick={() => setFormTarget("new")}>+ Add Dealer</Button>}
          />
        </Card>
      ) : (
        <div className="bf-dealers-page__grid">
          {filtered.map((dealer) => (
            <Card key={dealer.id} className="bf-dealer-card">
              <div className="bf-dealer-card__header">
                <h3>{dealer.name}</h3>
                {!dealer.active && <Badge>Inactive</Badge>}
              </div>
              <p className="bf-dealer-card__meta">
                {[dealer.district, dealer.deliveryRadiusKm ? `delivers ${dealer.deliveryRadiusKm} km` : null]
                  .filter(Boolean)
                  .join(" · ") || "No location set"}
              </p>
              {(dealer.contactPerson || dealer.phone) && (
                <p className="bf-dealer-card__contact">
                  {[dealer.contactPerson, dealer.phone].filter(Boolean).join(" · ")}
                </p>
              )}
              {dealer.rates.length > 0 ? (
                <ul className="bf-dealer-card__rates">
                  {dealer.rates.slice(0, 5).map((rate) => (
                    <li key={rate.id}>
                      <span>{rate.itemName}</span>
                      <span>
                        {formatCurrency(rate.rate)} / {rate.unit}
                      </span>
                    </li>
                  ))}
                  {dealer.rates.length > 5 && <li className="bf-dealer-card__more">+{dealer.rates.length - 5} more</li>}
                </ul>
              ) : (
                <p className="bf-dealer-card__meta">No rate card yet.</p>
              )}
              {canManage && (
                <button className="bf-dealer-card__edit" onClick={() => setFormTarget(dealer)}>
                  Edit
                </button>
              )}
            </Card>
          ))}
        </div>
      )}

      {formTarget && (
        <div className="bf-dealers-page__overlay">
          <div className="bf-dealers-page__panel">
            <div className="bf-dealers-page__panel-header">
              <h2>{formTarget === "new" ? "Add Dealer" : `Edit ${formTarget.name}`}</h2>
              <button onClick={() => setFormTarget(null)} aria-label="Close">×</button>
            </div>
            <div className="bf-dealers-page__panel-body">
              <DealerForm
                initialValue={formTarget === "new" ? undefined : formTarget}
                itemSuggestions={suggestions}
                onSubmit={handleSave}
                onCancel={() => setFormTarget(null)}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
