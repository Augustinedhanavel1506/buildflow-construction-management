import type { Layout, PlanBeam, PlanColumn, PlanMetrics, PlanOpening, PlanRoom, PlanSide, RoomType } from "../../../types/plan";

import { footprint, furnitureLabel } from "../furniture";

export const GRID_FT = 0.5;
export const MIN_SIDE_FT = 2.5;
// Mirrors the server: touching rooms are fine, overlapping by more than this is not.
const TOLERANCE_FT = 0.05;

export const ROOM_META: Record<RoomType, { label: string; color: string; width: number; depth: number }> = {
  BEDROOM: { label: "Bedroom", color: "#5b8def", width: 12, depth: 12 },
  BATHROOM: { label: "Bathroom", color: "#2fb5b2", width: 6, depth: 8 },
  KITCHEN: { label: "Kitchen", color: "#f2a65a", width: 10, depth: 10 },
  HALL: { label: "Living hall", color: "#6fbf73", width: 14, depth: 12 },
  DINING: { label: "Dining", color: "#d9bd5a", width: 10, depth: 10 },
  POOJA: { label: "Pooja room", color: "#d98ccf", width: 5, depth: 5 },
  BALCONY: { label: "Balcony", color: "#8fcf98", width: 10, depth: 4 },
  STAIRCASE: { label: "Staircase", color: "#9aa3b5", width: 7, depth: 10 },
  PARKING: { label: "Parking", color: "#8a99b8", width: 10, depth: 15 },
  UTILITY: { label: "Utility", color: "#c4a48b", width: 6, depth: 6 },
  STUDY: { label: "Study", color: "#8f7be0", width: 8, depth: 10 },
  OTHER: { label: "Other room", color: "#adb4c0", width: 8, depth: 8 },
};

export const ROOM_TYPES = Object.keys(ROOM_META) as RoomType[];

export function snap(value: number): number {
  return Math.round(value / GRID_FT) * GRID_FT;
}

export function newId(prefix: string): string {
  return `${prefix}${crypto.randomUUID().slice(0, 8)}`;
}

export function roomLabel(room: PlanRoom): string {
  return room.name?.trim() || ROOM_META[room.type].label;
}

export function sideLength(room: PlanRoom, side: PlanSide): number {
  return side === "NORTH" || side === "SOUTH" ? room.widthFt : room.depthFt;
}

export function overlaps(a: PlanRoom, b: PlanRoom): boolean {
  const overlapX = Math.min(a.x + a.widthFt, b.x + b.widthFt) - Math.max(a.x, b.x);
  const overlapY = Math.min(a.y + a.depthFt, b.y + b.depthFt) - Math.max(a.y, b.y);
  return overlapX > TOLERANCE_FT && overlapY > TOLERANCE_FT;
}

export interface LayoutProblems {
  messages: string[];
  roomIds: Set<string>;
}

// Same rules the server enforces, so problems show while drawing instead of on save.
export function findProblems(layout: Layout, plotWidth: number, plotDepth: number): LayoutProblems {
  const messages: string[] = [];
  const roomIds = new Set<string>();
  for (const room of layout.rooms) {
    if (room.widthFt < MIN_SIDE_FT || room.depthFt < MIN_SIDE_FT) {
      messages.push(`${roomLabel(room)} is too small (each side needs at least ${MIN_SIDE_FT} ft).`);
      roomIds.add(room.id);
    }
    if (room.x < 0 || room.y < 0 || room.x + room.widthFt > plotWidth + TOLERANCE_FT || room.y + room.depthFt > plotDepth + TOLERANCE_FT) {
      messages.push(`${roomLabel(room)} extends beyond the plot.`);
      roomIds.add(room.id);
    }
  }
  for (let i = 0; i < layout.rooms.length; i++) {
    for (let j = i + 1; j < layout.rooms.length; j++) {
      if (overlaps(layout.rooms[i], layout.rooms[j])) {
        messages.push(`${roomLabel(layout.rooms[i])} overlaps ${roomLabel(layout.rooms[j])}.`);
        roomIds.add(layout.rooms[i].id);
        roomIds.add(layout.rooms[j].id);
      }
    }
  }
  for (const opening of layout.openings) {
    const room = layout.rooms.find((r) => r.id === opening.roomId);
    if (room && opening.offsetFt + opening.widthFt > sideLength(room, opening.side) + TOLERANCE_FT) {
      messages.push(`A ${opening.kind.toLowerCase()} in ${roomLabel(room)} does not fit on its ${opening.side.toLowerCase()} wall.`);
      roomIds.add(room.id);
    }
  }
  for (const item of layout.furniture) {
    const fp = footprint(item);
    if (item.x - fp.w / 2 < -TOLERANCE_FT || item.y - fp.d / 2 < -TOLERANCE_FT || item.x + fp.w / 2 > plotWidth + TOLERANCE_FT || item.y + fp.d / 2 > plotDepth + TOLERANCE_FT) {
      messages.push(`${furnitureLabel(item.kind)} extends beyond the plot.`);
    }
  }
  return { messages, roomIds };
}

// First position (scanning the plot in 1 ft steps) where a room of this size fits without overlap.
export function findFreeSpot(rooms: PlanRoom[], width: number, depth: number, plotWidth: number, plotDepth: number) {
  for (let y = 0; y + depth <= plotDepth; y += 1) {
    for (let x = 0; x + width <= plotWidth; x += 1) {
      const candidate = { x, y, widthFt: width, depthFt: depth } as PlanRoom;
      if (!rooms.some((r) => overlaps(candidate, r))) {
        return { x, y };
      }
    }
  }
  return null;
}

export function computeMetrics(layout: Layout): PlanMetrics {
  let roomsArea = 0;
  let minX = Infinity;
  let minY = Infinity;
  let maxX = 0;
  let maxY = 0;
  for (const r of layout.rooms) {
    roomsArea += r.widthFt * r.depthFt;
    minX = Math.min(minX, r.x);
    minY = Math.min(minY, r.y);
    maxX = Math.max(maxX, r.x + r.widthFt);
    maxY = Math.max(maxY, r.y + r.depthFt);
  }
  const has = layout.rooms.length > 0;
  return {
    builtUpAreaSqft: roomsArea,
    roomsAreaSqft: roomsArea,
    boundingWidthFt: has ? maxX - minX : 0,
    boundingDepthFt: has ? maxY - minY : 0,
    bedrooms: layout.rooms.filter((r) => r.type === "BEDROOM").length,
    bathrooms: layout.rooms.filter((r) => r.type === "BATHROOM").length,
    doors: layout.openings.filter((o) => o.kind === "DOOR").length,
    windows: layout.openings.filter((o) => o.kind === "WINDOW").length,
    // Wall and plaster quantities are derived on the server when the plan is saved.
    externalWallFt: 0,
    internalWallFt: 0,
    wallAreaSqft: 0,
    plasterAreaSqft: 0,
  };
}

// Line segment of an opening in plan feet, for drawing and hit-testing.
export function openingSegment(room: PlanRoom, opening: PlanOpening) {
  const { x, y, widthFt, depthFt } = room;
  switch (opening.side) {
    case "NORTH":
      return { x1: x + opening.offsetFt, y1: y, x2: x + opening.offsetFt + opening.widthFt, y2: y };
    case "SOUTH":
      return { x1: x + opening.offsetFt, y1: y + depthFt, x2: x + opening.offsetFt + opening.widthFt, y2: y + depthFt };
    case "WEST":
      return { x1: x, y1: y + opening.offsetFt, x2: x, y2: y + opening.offsetFt + opening.widthFt };
    case "EAST":
      return { x1: x + widthFt, y1: y + opening.offsetFt, x2: x + widthFt, y2: y + opening.offsetFt + opening.widthFt };
  }
}

export function formatFt(value: number): string {
  return Number.isInteger(value) ? `${value}` : value.toFixed(1);
}

export const COLUMN_WIDTH_FT = 0.75;
export const COLUMN_DEPTH_FT = 1;

// Half-foot wall pieces used by the rooms, so beams can be limited to where a wall really runs.
function wallEdges(rooms: PlanRoom[]): Set<string> {
  const unit = (v: number) => Math.round(v / GRID_FT);
  const edges = new Set<string>();
  for (const r of rooms) {
    const x0 = unit(r.x);
    const y0 = unit(r.y);
    const x1 = x0 + unit(r.widthFt);
    const y1 = y0 + unit(r.depthFt);
    for (let x = x0; x < x1; x++) {
      edges.add(`H:${y0}:${x}`);
      edges.add(`H:${y1}:${x}`);
    }
    for (let y = y0; y < y1; y++) {
      edges.add(`V:${x0}:${y}`);
      edges.add(`V:${x1}:${y}`);
    }
  }
  return edges;
}

// Beams between consecutive aligned columns, only along walls. Mirrors the server's derivation.
export function deriveBeams(columns: PlanColumn[], rooms: PlanRoom[]): PlanBeam[] {
  const unit = (v: number) => Math.round(v / GRID_FT);
  const walls = wallEdges(rooms);
  const beams: PlanBeam[] = [];
  for (const horizontal of [true, false]) {
    const lines = new Map<number, [number, number][]>();
    for (const c of columns) {
      const p: [number, number] = [unit(c.x), unit(c.y)];
      const key = horizontal ? p[1] : p[0];
      lines.set(key, [...(lines.get(key) ?? []), p]);
    }
    for (const line of lines.values()) {
      line.sort((a, b) => (horizontal ? a[0] - b[0] : a[1] - b[1]));
      for (let i = 0; i + 1 < line.length; i++) {
        const a = line[i];
        const b = line[i + 1];
        if (a[0] === b[0] && a[1] === b[1]) continue;
        let onWall = true;
        if (horizontal) {
          for (let x = Math.min(a[0], b[0]); x < Math.max(a[0], b[0]); x++) onWall &&= walls.has(`H:${a[1]}:${x}`);
        } else {
          for (let y = Math.min(a[1], b[1]); y < Math.max(a[1], b[1]); y++) onWall &&= walls.has(`V:${a[0]}:${y}`);
        }
        if (onWall) beams.push({ x1: a[0] * GRID_FT, y1: a[1] * GRID_FT, x2: b[0] * GRID_FT, y2: b[1] * GRID_FT });
      }
    }
  }
  return beams;
}

export interface SiteInput {
  plotWidth: number;
  plotDepth: number;
  front: number | null;
  rear: number | null;
  left: number | null;
  right: number | null;
  maxCoverage: number | null;
  maxFar: number | null;
}

export const DEFAULT_SETBACKS = { front: 5, rear: 3, left: 3, right: 3 };

export function effectiveSetbacks(site: SiteInput) {
  return {
    front: site.front ?? DEFAULT_SETBACKS.front,
    rear: site.rear ?? DEFAULT_SETBACKS.rear,
    left: site.left ?? DEFAULT_SETBACKS.left,
    right: site.right ?? DEFAULT_SETBACKS.right,
  };
}

export function envelopeFor(site: SiteInput) {
  const sb = effectiveSetbacks(site);
  return { x: sb.left, y: sb.front, widthFt: site.plotWidth - sb.left - sb.right, depthFt: site.plotDepth - sb.front - sb.rear };
}

// Mirrors the server's site check using what is on screen, saved or not. floors: checklist area per level.
export function liveSiteCheck(
  site: SiteInput,
  layouts: Record<number, Layout>,
  floors: { floorLevel: number; floorAreaSqft: number }[],
) {
  const env = envelopeFor(site);
  const tol = 0.05;
  const violations: { floorLevel: number; roomId: string; message: string }[] = [];
  let ground = 0;
  let total = 0;
  for (const floor of floors) {
    const layout = layouts[floor.floorLevel];
    let area: number;
    if (layout && layout.rooms.length > 0) {
      area = 0;
      for (const r of layout.rooms) {
        area += r.widthFt * r.depthFt;
        const sides: string[] = [];
        if (r.y < env.y - tol) sides.push("front setback");
        if (r.y + r.depthFt > env.y + env.depthFt + tol) sides.push("rear setback");
        if (r.x < env.x - tol) sides.push("left setback");
        if (r.x + r.widthFt > env.x + env.widthFt + tol) sides.push("right setback");
        if (sides.length) violations.push({ floorLevel: floor.floorLevel, roomId: r.id, message: `${roomLabel(r)} extends into the ${sides.join(" and the ")}` });
      }
    } else {
      area = floor.floorAreaSqft;
    }
    total += area;
    if (floor.floorLevel === 0) ground = area;
  }
  const plot = site.plotWidth * site.plotDepth;
  const coverage = plot > 0 ? (ground / plot) * 100 : 0;
  const far = plot > 0 ? total / plot : 0;
  const status = (value: number, limit: number | null) => (limit === null ? "NOT_SET" : value <= limit + 0.005 ? "OK" : "EXCEEDS");
  return {
    envelope: env,
    violations,
    coverage: { value: coverage, status: status(coverage, site.maxCoverage) as "OK" | "EXCEEDS" | "NOT_SET" },
    far: { value: far, status: status(far, site.maxFar) as "OK" | "EXCEEDS" | "NOT_SET" },
    noRoom: env.widthFt <= 0 || env.depthFt <= 0,
  };
}
