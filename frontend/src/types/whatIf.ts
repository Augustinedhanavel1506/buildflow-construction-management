import type { ConstructionGrade } from "./estimation";

export interface FloorChange {
  floorLevel: number;
  floorAreaSqft?: number;
  bedroomCount?: number;
  bathroomCount?: number;
  hasKitchen?: boolean;
  hasHall?: boolean;
  hasBalcony?: boolean;
  hasPoojaRoom?: boolean;
}

export interface NewFloor {
  floorAreaSqft: number;
  bedroomCount?: number;
  bathroomCount?: number;
  hasKitchen?: boolean;
  hasHall?: boolean;
  hasBalcony?: boolean;
}

export interface WhatIfRequest {
  constructionGrade?: ConstructionGrade;
  floors?: FloorChange[];
  removeFloorLevels?: number[];
  addFloors?: NewFloor[];
}

export interface WhatIfSummary {
  total: number;
  builtUpAreaSqft: number;
}

export interface WhatIfResult {
  baseline: WhatIfSummary;
  scenario: WhatIfSummary;
  difference: number;
  differencePercent: number | null;
  components: { component: string; baseline: number; scenario: number; difference: number }[];
  items: {
    itemName: string;
    unit: string;
    baselineQuantity: number;
    scenarioQuantity: number;
    baselineAmount: number;
    scenarioAmount: number;
    difference: number;
  }[];
  unpricedItems: string[];
}
