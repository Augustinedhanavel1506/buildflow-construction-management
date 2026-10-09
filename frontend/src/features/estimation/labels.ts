import type { ConstructionGrade, RoofType, StructureType, WallMaterial } from "../../types/estimation";

export const CONSTRUCTION_GRADES: ConstructionGrade[] = ["ECONOMY", "STANDARD", "PREMIUM"];

export const GRADE_LABEL: Record<ConstructionGrade, string> = {
  ECONOMY: "Economy",
  STANDARD: "Standard",
  PREMIUM: "Premium",
};

export const GRADE_DESCRIPTION: Record<ConstructionGrade, string> = {
  ECONOMY: "Basic materials and finishes, tightest budget.",
  STANDARD: "Good-quality, commonly used materials and finishes.",
  PREMIUM: "Higher-grade flooring, fittings, and finishes.",
};

export const GRADE_TONE: Record<ConstructionGrade, "neutral" | "info" | "success"> = {
  ECONOMY: "neutral",
  STANDARD: "info",
  PREMIUM: "success",
};

export const STRUCTURE_TYPES: StructureType[] = ["RCC_FRAMED", "LOAD_BEARING", "STEEL_STRUCTURE"];

export const STRUCTURE_LABEL: Record<StructureType, string> = {
  RCC_FRAMED: "RCC Framed",
  LOAD_BEARING: "Load Bearing",
  STEEL_STRUCTURE: "Steel Structure",
};

export const WALL_MATERIALS: WallMaterial[] = ["RED_BRICK", "AAC_BLOCK", "SOLID_BLOCK"];

export const WALL_MATERIAL_LABEL: Record<WallMaterial, string> = {
  RED_BRICK: "Red Clay Brick",
  AAC_BLOCK: "AAC Block",
  SOLID_BLOCK: "Solid Concrete Block",
};

export const ROOF_TYPES: RoofType[] = ["RCC_SLAB", "OTHER"];

export const ROOF_TYPE_LABEL: Record<RoofType, string> = {
  RCC_SLAB: "RCC Slab",
  OTHER: "Other",
};

const COMPONENT_META: Record<string, { label: string; icon: string }> = {
  FOUNDATION: { label: "Foundation", icon: "\u{1FAA8}" },
  RCC_FRAMING: { label: "RCC Framing", icon: "\u{1F3D7}\u{FE0F}" },
  MASONRY: { label: "Masonry (Walls)", icon: "\u{1F9F1}" },
  PLASTERING: { label: "Plastering", icon: "\u{1FAA3}" },
  FLOORING: { label: "Flooring", icon: "\u{1F9E9}" },
  WATERPROOFING: { label: "Waterproofing", icon: "\u{1F4A7}" },
  PAINTING: { label: "Painting", icon: "\u{1F3A8}" },
  DOORS_WINDOWS: { label: "Doors & Windows", icon: "\u{1FAA8}" },
  ELECTRICAL: { label: "Electrical", icon: "\u{26A1}" },
  PLUMBING: { label: "Plumbing", icon: "\u{1F6B0}" },
  STAIRCASE: { label: "Staircase", icon: "\u{1FA9C}" },
  MISC: { label: "Miscellaneous", icon: "\u{1F4E6}" },
};

export function componentLabel(component: string | null): string {
  if (!component) return "Other";
  return COMPONENT_META[component]?.label ?? component;
}

export function componentIcon(component: string | null): string {
  if (!component) return "\u{1F4E6}";
  return COMPONENT_META[component]?.icon ?? "\u{1F4E6}";
}

export function floorLabel(floorLevel: number): string {
  if (floorLevel === 0) return "Ground Floor";
  const ordinals = ["First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh"];
  const name = ordinals[floorLevel - 1] ?? `${floorLevel}th`;
  return `${name} Floor`;
}

export function estimateSourceLabel(source: string): string {
  switch (source) {
    case "SYSTEM_PRELIMINARY":
      return "Preliminary";
    case "ENGINEER_VALIDATED":
      return "Engineer-validated";
    case "MANUAL":
      return "Manual";
    default:
      return source;
  }
}

export function estimateSourceTone(source: string): "neutral" | "info" | "success" {
  switch (source) {
    case "ENGINEER_VALIDATED":
      return "success";
    case "SYSTEM_PRELIMINARY":
      return "info";
    default:
      return "neutral";
  }
}
