import type { PlanColumn, PlanOpening, PlanRoom } from "../../../types/plan";

// What the drawing needs from a floor, so it can draw a saved floor or an unsaved one.
export interface PlanFloorLike {
  rooms: PlanRoom[];
  openings: PlanOpening[];
  columns: PlanColumn[];
}
