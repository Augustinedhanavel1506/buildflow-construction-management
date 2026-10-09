import { useEffect, useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { businessService } from "../../services/businessService";
import { getErrorMessage } from "../../utils/apiError";
import type { Business } from "../../types/business";

export function BusinessProfileSection({ canEdit }: { canEdit: boolean }) {
  const { showToast } = useToast();
  const [business, setBusiness] = useState<Business | null>(null);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [gstin, setGstin] = useState("");
  const [stateName, setStateName] = useState("");
  const [materialRegion, setMaterialRegion] = useState("");
  const [defaultGstRate, setDefaultGstRate] = useState("18");
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    businessService
      .get()
      .then((data) => {
        setBusiness(data);
        setName(data.name);
        setPhone(data.phone ?? "");
        setAddress(data.address ?? "");
        setGstin(data.gstin ?? "");
        setStateName(data.stateName ?? "");
        setMaterialRegion(data.materialRegion ?? "");
        setDefaultGstRate(data.defaultGstRate.toString());
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError("Please enter a business name.");
      return;
    }

    setIsSaving(true);
    try {
      const updated = await businessService.update({
        name: name.trim(),
        phone: phone.trim() || undefined,
        address: address.trim() || undefined,
        gstin: gstin.trim() || undefined,
        stateName: stateName.trim() || undefined,
        materialRegion: materialRegion.trim() || undefined,
        defaultGstRate: defaultGstRate ? Number(defaultGstRate) : undefined,
      });
      setBusiness(updated);
      showToast("success", "Business profile updated successfully.");
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Card className="bf-settings-page__section">
      <h3>Business Profile</h3>
      <p>This GSTIN and address appear on every tax invoice you issue.</p>

      {error && <div className="bf-settings-page__error">{error}</div>}

      {business === null ? (
        <Skeleton height={160} />
      ) : (
        <form className="bf-settings-page__form" onSubmit={handleSubmit}>
          <Input label="Business Name" required disabled={!canEdit} value={name} onChange={(e) => setName(e.target.value)} />

          <div className="bf-settings-page__form-row">
            <Input label="Phone" disabled={!canEdit} value={phone} onChange={(e) => setPhone(e.target.value)} />
            <Input label="State" disabled={!canEdit} value={stateName} onChange={(e) => setStateName(e.target.value)} />
          </div>

          <Input label="Address" disabled={!canEdit} value={address} onChange={(e) => setAddress(e.target.value)} />

          <Input
            label="Material Sourcing Region"
            placeholder="e.g. Coimbatore, Tamil Nadu"
            disabled={!canEdit}
            value={materialRegion}
            onChange={(e) => setMaterialRegion(e.target.value)}
          />
          <p className="bf-settings-page__hint">
            Labels the Rate Master price index (e.g. "Live Building Material Rate Index — {materialRegion || "your region"}").
            Separate from the GST state above, since materials are often sourced locally.
          </p>

          <div className="bf-settings-page__form-row">
            <Input label="GSTIN" disabled={!canEdit} value={gstin} onChange={(e) => setGstin(e.target.value)} />
            <Input
              label="Default GST Rate (%)"
              type="number"
              min="0"
              max="100"
              step="0.01"
              disabled={!canEdit}
              value={defaultGstRate}
              onChange={(e) => setDefaultGstRate(e.target.value)}
            />
          </div>

          {canEdit && (
            <Button type="submit" isLoading={isSaving}>
              Save Business Profile
            </Button>
          )}
        </form>
      )}
    </Card>
  );
}
