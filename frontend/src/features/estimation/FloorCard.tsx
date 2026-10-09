import { Input } from "../../components/ui/Input";
import { floorLabel } from "./labels";
import "./FloorCard.css";

export interface FloorFormValue {
  key: string;
  floorAreaSqft: string;
  bedroomCount: string;
  bathroomCount: string;
  hasKitchen: boolean;
  hasHall: boolean;
  hasBalcony: boolean;
  hasPoojaRoom: boolean;
  doorCount: string;
  windowCount: string;
}

function Stepper({ label, value, onChange }: { label: string; value: string; onChange: (next: string) => void }) {
  const current = Number(value) || 0;
  return (
    <div className="bf-stepper">
      <span className="bf-stepper__label">{label}</span>
      <div className="bf-stepper__control">
        <button
          type="button"
          className="bf-stepper__button"
          onClick={() => onChange(String(Math.max(0, current - 1)))}
          aria-label={`Decrease ${label}`}
        >
          −
        </button>
        <span className="bf-stepper__value">{current}</span>
        <button
          type="button"
          className="bf-stepper__button"
          onClick={() => onChange(String(current + 1))}
          aria-label={`Increase ${label}`}
        >
          +
        </button>
      </div>
    </div>
  );
}

function RoomToggle({
  icon,
  label,
  active,
  onToggle,
}: {
  icon: string;
  label: string;
  active: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      className={`bf-room-toggle ${active ? "bf-room-toggle--active" : ""}`}
      onClick={onToggle}
      aria-pressed={active}
    >
      <span className="bf-room-toggle__icon" aria-hidden="true">
        {icon}
      </span>
      {label}
    </button>
  );
}

export function FloorCard({
  floorLevel,
  value,
  onChange,
  onRemove,
  canRemove,
}: {
  floorLevel: number;
  value: FloorFormValue;
  onChange: (patch: Partial<FloorFormValue>) => void;
  onRemove: () => void;
  canRemove: boolean;
}) {
  return (
    <div className="bf-floor-card">
      <div className="bf-floor-card__header">
        <div className="bf-floor-card__title">
          <span className="bf-floor-card__badge">{floorLevel + 1}</span>
          <h4>{floorLabel(floorLevel)}</h4>
        </div>
        {canRemove && (
          <button type="button" className="bf-floor-card__remove" onClick={onRemove}>
            Remove
          </button>
        )}
      </div>

      <Input
        label="Floor area (sq.ft)"
        type="number"
        min="0"
        step="1"
        required
        placeholder="e.g. 1050"
        value={value.floorAreaSqft}
        onChange={(e) => onChange({ floorAreaSqft: e.target.value })}
      />

      <div className="bf-floor-card__toggles">
        <RoomToggle
          icon="🍳"
          label="Kitchen"
          active={value.hasKitchen}
          onToggle={() => onChange({ hasKitchen: !value.hasKitchen })}
        />
        <RoomToggle
          icon="🛋️"
          label="Hall"
          active={value.hasHall}
          onToggle={() => onChange({ hasHall: !value.hasHall })}
        />
        <RoomToggle
          icon="🌿"
          label="Balcony"
          active={value.hasBalcony}
          onToggle={() => onChange({ hasBalcony: !value.hasBalcony })}
        />
        <RoomToggle
          icon="🛕"
          label="Pooja Room"
          active={value.hasPoojaRoom}
          onToggle={() => onChange({ hasPoojaRoom: !value.hasPoojaRoom })}
        />
      </div>

      <div className="bf-floor-card__steppers">
        <Stepper label="Bedrooms" value={value.bedroomCount} onChange={(v) => onChange({ bedroomCount: v })} />
        <Stepper label="Bathrooms" value={value.bathroomCount} onChange={(v) => onChange({ bathroomCount: v })} />
        <Stepper label="Doors" value={value.doorCount} onChange={(v) => onChange({ doorCount: v })} />
        <Stepper label="Windows" value={value.windowCount} onChange={(v) => onChange({ windowCount: v })} />
      </div>
    </div>
  );
}
