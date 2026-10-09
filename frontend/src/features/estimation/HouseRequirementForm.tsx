import { useState, type FormEvent } from "react";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { getErrorMessage } from "../../utils/apiError";
import { FloorCard, type FloorFormValue } from "./FloorCard";
import {
  CONSTRUCTION_GRADES,
  GRADE_DESCRIPTION,
  GRADE_LABEL,
  ROOF_TYPES,
  ROOF_TYPE_LABEL,
  STRUCTURE_LABEL,
  STRUCTURE_TYPES,
  WALL_MATERIALS,
  WALL_MATERIAL_LABEL,
  floorLabel,
} from "./labels";
import type {
  ConstructionGrade,
  HouseRequirement,
  HouseRequirementPayload,
  RoofType,
  StructureType,
  WallMaterial,
} from "../../types/estimation";
import "./HouseRequirementForm.css";

function newFloor(): FloorFormValue {
  return {
    key: crypto.randomUUID(),
    floorAreaSqft: "",
    bedroomCount: "0",
    bathroomCount: "0",
    hasKitchen: false,
    hasHall: false,
    hasBalcony: false,
    hasPoojaRoom: false,
    doorCount: "0",
    windowCount: "0",
  };
}

const STEPS = [
  { number: 1, title: "Land & Grade" },
  { number: 2, title: "Floors & Rooms" },
  { number: 3, title: "Review" },
];

export function HouseRequirementForm({
  initialValue,
  onSubmit,
  onCancel,
}: {
  initialValue?: HouseRequirement;
  onSubmit: (payload: HouseRequirementPayload) => Promise<void>;
  onCancel: () => void;
}) {
  const [step, setStep] = useState(1);
  const [maxStepReached, setMaxStepReached] = useState(1);

  const [label, setLabel] = useState(initialValue?.label ?? "");
  const [location, setLocation] = useState(initialValue?.location ?? "");
  const [district, setDistrict] = useState(initialValue?.district ?? "");
  const [plotWidthFt, setPlotWidthFt] = useState(initialValue?.plotWidthFt?.toString() ?? "");
  const [plotLengthFt, setPlotLengthFt] = useState(initialValue?.plotLengthFt?.toString() ?? "");
  const [constructionGrade, setConstructionGrade] = useState<ConstructionGrade>(
    initialValue?.constructionGrade ?? "STANDARD",
  );
  const [structureType, setStructureType] = useState<StructureType>(initialValue?.structureType ?? "RCC_FRAMED");
  const [wallMaterial, setWallMaterial] = useState<WallMaterial>(initialValue?.wallMaterial ?? "RED_BRICK");
  const [roofType, setRoofType] = useState<RoofType>(initialValue?.roofType ?? "RCC_SLAB");

  const [floors, setFloors] = useState<FloorFormValue[]>(
    initialValue && initialValue.floors.length > 0
      ? initialValue.floors.map((f) => ({
          key: crypto.randomUUID(),
          floorAreaSqft: f.floorAreaSqft.toString(),
          bedroomCount: f.bedroomCount.toString(),
          bathroomCount: f.bathroomCount.toString(),
          hasKitchen: f.hasKitchen,
          hasHall: f.hasHall,
          hasBalcony: f.hasBalcony,
          hasPoojaRoom: f.hasPoojaRoom,
          doorCount: f.doorCount.toString(),
          windowCount: f.windowCount.toString(),
        }))
      : [newFloor()],
  );

  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const plotArea = Number(plotWidthFt) > 0 && Number(plotLengthFt) > 0 ? Number(plotWidthFt) * Number(plotLengthFt) : null;
  const totalBuiltupArea = floors.reduce((sum, f) => sum + (Number(f.floorAreaSqft) || 0), 0);

  function updateFloor(key: string, patch: Partial<FloorFormValue>) {
    setFloors((current) => current.map((f) => (f.key === key ? { ...f, ...patch } : f)));
  }

  function addFloor() {
    setFloors((current) => [...current, newFloor()]);
  }

  function removeFloor(key: string) {
    setFloors((current) => (current.length > 1 ? current.filter((f) => f.key !== key) : current));
  }

  function goToStep(target: number) {
    if (target <= maxStepReached) {
      setStep(target);
    }
  }

  function validateStep1(): string | null {
    if (!label.trim()) return "Give this plan a name, e.g. “30x50 plot – Madurai”.";
    const width = Number(plotWidthFt);
    const length = Number(plotLengthFt);
    if (!plotWidthFt || Number.isNaN(width) || width <= 0) return "Enter a valid plot width.";
    if (!plotLengthFt || Number.isNaN(length) || length <= 0) return "Enter a valid plot length.";
    return null;
  }

  function validateStep2(): string | null {
    if (floors.length === 0) return "Add at least one floor.";
    for (const floor of floors) {
      const area = Number(floor.floorAreaSqft);
      if (!floor.floorAreaSqft || Number.isNaN(area) || area <= 0) {
        return `Enter a valid floor area for ${floorLabel(floors.indexOf(floor))}.`;
      }
    }
    return null;
  }

  function handleNext() {
    const validationError = step === 1 ? validateStep1() : validateStep2();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    const next = step + 1;
    setStep(next);
    setMaxStepReached((current) => Math.max(current, next));
  }

  function handleBack() {
    setError(null);
    setStep((current) => Math.max(1, current - 1));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const step1Error = validateStep1();
    const step2Error = validateStep2();
    if (step1Error || step2Error) {
      setError(step1Error ?? step2Error);
      setStep(step1Error ? 1 : 2);
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await onSubmit({
        label: label.trim(),
        location: location.trim() || undefined,
        district: district.trim() || undefined,
        plotWidthFt: Number(plotWidthFt),
        plotLengthFt: Number(plotLengthFt),
        constructionGrade,
        structureType,
        wallMaterial,
        roofType,
        floors: floors.map((f, index) => ({
          floorLevel: index,
          floorAreaSqft: Number(f.floorAreaSqft),
          bedroomCount: Number(f.bedroomCount) || 0,
          bathroomCount: Number(f.bathroomCount) || 0,
          hasKitchen: f.hasKitchen,
          hasHall: f.hasHall,
          hasBalcony: f.hasBalcony,
          hasPoojaRoom: f.hasPoojaRoom,
          doorCount: Number(f.doorCount) || 0,
          windowCount: Number(f.windowCount) || 0,
        })),
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="bf-hr-form" onSubmit={handleSubmit}>
      <div className="bf-hr-form__steps">
        {STEPS.map((s, index) => (
          <div className="bf-hr-form__step-wrap" key={s.number}>
            <button
              type="button"
              className={[
                "bf-hr-form__step",
                step === s.number ? "bf-hr-form__step--active" : "",
                maxStepReached >= s.number ? "bf-hr-form__step--reachable" : "",
              ]
                .filter(Boolean)
                .join(" ")}
              onClick={() => goToStep(s.number)}
              disabled={s.number > maxStepReached}
            >
              <span className="bf-hr-form__step-number">
                {maxStepReached > s.number ? "✓" : s.number}
              </span>
              {s.title}
            </button>
            {index < STEPS.length - 1 && <span className="bf-hr-form__step-connector" />}
          </div>
        ))}
      </div>

      {error && <div className="bf-hr-form__error">{error}</div>}

      {step === 1 && (
        <div className="bf-hr-form__panel">
          <Input
            label="Plan name"
            required
            placeholder="e.g. 30x50 plot – Madurai"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <Input
            label="Location (optional)"
            placeholder="e.g. Madurai, Tamil Nadu"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />

          <Input
            label="District (optional)"
            placeholder="Uses that district's rates from Rate Master"
            value={district}
            onChange={(e) => setDistrict(e.target.value)}
          />

          <div className="bf-hr-form__row">
            <Input
              label="Plot width (ft)"
              type="number"
              min="0"
              step="0.1"
              required
              value={plotWidthFt}
              onChange={(e) => setPlotWidthFt(e.target.value)}
            />
            <Input
              label="Plot length (ft)"
              type="number"
              min="0"
              step="0.1"
              required
              value={plotLengthFt}
              onChange={(e) => setPlotLengthFt(e.target.value)}
            />
          </div>
          {plotArea && <p className="bf-hr-form__hint">Plot area: {plotArea.toLocaleString("en-IN")} sq.ft</p>}

          <div className="bf-hr-form__grade-grid">
            {CONSTRUCTION_GRADES.map((grade) => (
              <button
                type="button"
                key={grade}
                className={`bf-hr-form__grade-card ${constructionGrade === grade ? "bf-hr-form__grade-card--active" : ""}`}
                onClick={() => setConstructionGrade(grade)}
              >
                <span className="bf-hr-form__grade-name">{GRADE_LABEL[grade]}</span>
                <span className="bf-hr-form__grade-desc">{GRADE_DESCRIPTION[grade]}</span>
              </button>
            ))}
          </div>

          <details className="bf-hr-form__advanced">
            <summary>Construction specification (optional)</summary>
            <div className="bf-hr-form__row bf-hr-form__row--triple">
              <Select
                label="Structure type"
                value={structureType}
                onChange={(e) => setStructureType(e.target.value as StructureType)}
              >
                {STRUCTURE_TYPES.map((s) => (
                  <option key={s} value={s}>
                    {STRUCTURE_LABEL[s]}
                  </option>
                ))}
              </Select>
              <Select
                label="Wall material"
                value={wallMaterial}
                onChange={(e) => setWallMaterial(e.target.value as WallMaterial)}
              >
                {WALL_MATERIALS.map((w) => (
                  <option key={w} value={w}>
                    {WALL_MATERIAL_LABEL[w]}
                  </option>
                ))}
              </Select>
              <Select label="Roof type" value={roofType} onChange={(e) => setRoofType(e.target.value as RoofType)}>
                {ROOF_TYPES.map((r) => (
                  <option key={r} value={r}>
                    {ROOF_TYPE_LABEL[r]}
                  </option>
                ))}
              </Select>
            </div>
          </details>
        </div>
      )}

      {step === 2 && (
        <div className="bf-hr-form__panel">
          {floors.map((floor, index) => (
            <FloorCard
              key={floor.key}
              floorLevel={index}
              value={floor}
              onChange={(patch) => updateFloor(floor.key, patch)}
              onRemove={() => removeFloor(floor.key)}
              canRemove={floors.length > 1}
            />
          ))}

          <button type="button" className="bf-hr-form__add-floor" onClick={addFloor}>
            + Add Floor
          </button>

          <div className="bf-hr-form__total-area">
            Total built-up area: <strong>{totalBuiltupArea.toLocaleString("en-IN")} sq.ft</strong> across{" "}
            {floors.length} {floors.length === 1 ? "floor" : "floors"}
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="bf-hr-form__panel">
          <div className="bf-hr-form__review-card">
            <h4>{label || "Untitled plan"}</h4>
            {location && <p className="bf-hr-form__review-location">{location}</p>}
            <div className="bf-hr-form__review-grid">
              <div>
                <span className="bf-hr-form__review-label">Plot size</span>
                <span>
                  {plotWidthFt} × {plotLengthFt} ft ({plotArea?.toLocaleString("en-IN")} sq.ft)
                </span>
              </div>
              <div>
                <span className="bf-hr-form__review-label">Built-up area</span>
                <span>{totalBuiltupArea.toLocaleString("en-IN")} sq.ft</span>
              </div>
              <div>
                <span className="bf-hr-form__review-label">Grade</span>
                <span>{GRADE_LABEL[constructionGrade]}</span>
              </div>
              <div>
                <span className="bf-hr-form__review-label">Specification</span>
                <span>
                  {STRUCTURE_LABEL[structureType]} · {WALL_MATERIAL_LABEL[wallMaterial]} ·{" "}
                  {ROOF_TYPE_LABEL[roofType]}
                </span>
              </div>
            </div>
          </div>

          <div className="bf-hr-form__review-floors">
            {floors.map((floor, index) => {
              const rooms = [
                floor.hasKitchen && "Kitchen",
                floor.hasHall && "Hall",
                floor.hasBalcony && "Balcony",
                floor.hasPoojaRoom && "Pooja Room",
              ].filter(Boolean) as string[];
              return (
                <div className="bf-hr-form__review-floor" key={floor.key}>
                  <span className="bf-hr-form__review-floor-name">{floorLabel(index)}</span>
                  <span className="bf-hr-form__review-floor-detail">
                    {floor.floorAreaSqft || 0} sq.ft · {floor.bedroomCount || 0} BR ·{" "}
                    {floor.bathroomCount || 0} bath
                    {rooms.length > 0 ? ` · ${rooms.join(", ")}` : ""}
                  </span>
                </div>
              );
            })}
          </div>

          <p className="bf-hr-form__disclaimer">
            This creates a preliminary plan. You will generate the material estimate on the next screen, and it can
            always be edited and regenerated later.
          </p>
        </div>
      )}

      <div className="bf-hr-form__actions">
        <Button type="button" variant="secondary" onClick={step === 1 ? onCancel : handleBack}>
          {step === 1 ? "Cancel" : "Back"}
        </Button>
        {step < 3 ? (
          <Button key="continue" type="button" onClick={handleNext}>
            Continue
          </Button>
        ) : (
          <Button key="submit" type="submit" isLoading={isSubmitting}>
            {initialValue ? "Save Changes" : "Create Plan"}
          </Button>
        )}
      </div>
    </form>
  );
}
