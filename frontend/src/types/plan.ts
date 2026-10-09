export type RoomType =
  | "BEDROOM"
  | "BATHROOM"
  | "KITCHEN"
  | "HALL"
  | "DINING"
  | "POOJA"
  | "BALCONY"
  | "STAIRCASE"
  | "PARKING"
  | "UTILITY"
  | "STUDY"
  | "OTHER";

export type OpeningKind = "DOOR" | "WINDOW";
export type PlanSide = "NORTH" | "EAST" | "SOUTH" | "WEST";

export interface PlanRoom {
  id: string;
  type: RoomType;
  name: string | null;
  x: number;
  y: number;
  widthFt: number;
  depthFt: number;
}

export interface PlanOpening {
  id: string;
  roomId: string;
  kind: OpeningKind;
  side: PlanSide;
  offsetFt: number;
  widthFt: number;
}

export interface PlanColumn {
  id: string;
  x: number;
  y: number;
  widthFt: number;
  depthFt: number;
}

export type FurnitureKind =
  | "BED" | "SOFA" | "TABLE" | "CHAIR" | "WARDROBE" | "COUNTER" | "FRIDGE" | "TV_UNIT"
  | "TOILET" | "BASIN" | "CAR" | "RUG" | "PLANT" | "SHRINE" | "DESK"
  | "TREE" | "SHRUB" | "FLOWER_BED" | "BENCH";

// x and y are the piece's centre, in feet from the plot's top-left corner. Layout only: furniture is never estimated.
export interface PlanFurniture {
  id: string;
  kind: FurnitureKind;
  x: number;
  y: number;
  widthFt: number;
  depthFt: number;
  heightFt: number;
  rotationDeg: number;
  colour: string | null;
}

export interface PlanBeam {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

export interface StructureMetrics {
  columnCount: number;
  beamLengthFt: number;
  concreteM3: number;
  steelKg: number;
}

export interface PlanMetrics {
  builtUpAreaSqft: number;
  roomsAreaSqft: number;
  boundingWidthFt: number;
  boundingDepthFt: number;
  bedrooms: number;
  bathrooms: number;
  doors: number;
  windows: number;
  externalWallFt: number;
  internalWallFt: number;
  wallAreaSqft: number;
  plasterAreaSqft: number;
}

export interface FloorPlan {
  floorLevel: number;
  saved: boolean;
  rooms: PlanRoom[];
  openings: PlanOpening[];
  columns: PlanColumn[];
  beams: PlanBeam[];
  furniture: PlanFurniture[];
  metrics: PlanMetrics;
  structure: StructureMetrics;
}

export interface PlansResponse {
  plotWidthFt: number;
  plotLengthFt: number;
  floors: FloorPlan[];
}

export interface Layout {
  rooms: PlanRoom[];
  openings: PlanOpening[];
  columns: PlanColumn[];
  furniture: PlanFurniture[];
}
