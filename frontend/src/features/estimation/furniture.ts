import type { FurnitureKind, PlanFurniture, PlanOpening, PlanRoom, PlanSide } from "../../types/plan";

export interface FurnitureSpec {
  kind: FurnitureKind;
  label: string;
  widthFt: number;
  depthFt: number;
  heightFt: number;
  colour: string;
  // Garden pieces: placed on the plot outside the house.
  outdoor?: boolean;
}

// Sizes are in feet. The width runs along the piece's back, the depth from back to front, and the back faces
// north (up the plan) before rotation. Rotating turns the back clockwise: 90 faces it east, 180 south, 270 west.
export const FURNITURE_CATALOG: FurnitureSpec[] = [
  { kind: "BED", label: "Bed", widthFt: 5.5, depthFt: 6.5, heightFt: 1.6, colour: "#7a5230" },
  { kind: "SOFA", label: "Sofa", widthFt: 7, depthFt: 3, heightFt: 2.8, colour: "#4f6d8a" },
  { kind: "TABLE", label: "Table", widthFt: 6, depthFt: 3.5, heightFt: 2.5, colour: "#7a5230" },
  { kind: "CHAIR", label: "Chair", widthFt: 1.4, depthFt: 1.4, heightFt: 3, colour: "#5b3a1e" },
  { kind: "WARDROBE", label: "Wardrobe", widthFt: 4, depthFt: 2, heightFt: 6.5, colour: "#8a6a4b" },
  { kind: "DESK", label: "Desk", widthFt: 5, depthFt: 2, heightFt: 2.5, colour: "#7a5230" },
  { kind: "TV_UNIT", label: "TV unit", widthFt: 5, depthFt: 1.4, heightFt: 2, colour: "#2b2b2b" },
  { kind: "COUNTER", label: "Kitchen counter", widthFt: 8, depthFt: 2, heightFt: 3, colour: "#3e4a52" },
  { kind: "FRIDGE", label: "Fridge", widthFt: 2.6, depthFt: 2.6, heightFt: 6, colour: "#d8dde0" },
  { kind: "TOILET", label: "Toilet", widthFt: 1.6, depthFt: 2.2, heightFt: 1.4, colour: "#ffffff" },
  { kind: "BASIN", label: "Wash basin", widthFt: 1.6, depthFt: 1.4, heightFt: 2.8, colour: "#f4f4f4" },
  { kind: "SHRINE", label: "Pooja shelf", widthFt: 4, depthFt: 1.4, heightFt: 3, colour: "#8a5a2b" },
  { kind: "CAR", label: "Car", widthFt: 5.8, depthFt: 14, heightFt: 2.5, colour: "#b03a2e" },
  { kind: "RUG", label: "Rug", widthFt: 8, depthFt: 5, heightFt: 0.06, colour: "#a3584b" },
  { kind: "TREE", label: "Tree", widthFt: 8, depthFt: 8, heightFt: 14, colour: "#3d8b3d", outdoor: true },
  { kind: "SHRUB", label: "Shrub", widthFt: 3, depthFt: 3, heightFt: 3, colour: "#4a9a45", outdoor: true },
  { kind: "FLOWER_BED", label: "Flower bed", widthFt: 6, depthFt: 2, heightFt: 0.5, colour: "#d95f8a", outdoor: true },
  { kind: "BENCH", label: "Garden bench", widthFt: 4.5, depthFt: 1.6, heightFt: 1.5, colour: "#7a5230", outdoor: true },
  { kind: "PLANT", label: "Plant", widthFt: 1.2, depthFt: 1.2, heightFt: 3.5, colour: "#3d8b3d" },
];

export const furnitureSpec = (kind: FurnitureKind): FurnitureSpec =>
  FURNITURE_CATALOG.find((f) => f.kind === kind) ?? FURNITURE_CATALOG[0];

export const furnitureLabel = (kind: FurnitureKind): string => furnitureSpec(kind).label;

// Footprint on the plan once rotation is applied.
export function footprint(item: Pick<PlanFurniture, "widthFt" | "depthFt" | "rotationDeg">): { w: number; d: number } {
  const turned = item.rotationDeg === 90 || item.rotationDeg === 270;
  return turned ? { w: item.depthFt, d: item.widthFt } : { w: item.widthFt, d: item.depthFt };
}

export function nextFurnitureId(existing: PlanFurniture[]): string {
  let n = existing.length + 1;
  const ids = new Set(existing.map((f) => f.id));
  while (ids.has(`f${n}`)) n++;
  return `f${n}`;
}

export function newFurniture(kind: FurnitureKind, x: number, y: number, existing: PlanFurniture[]): PlanFurniture {
  const spec = furnitureSpec(kind);
  return {
    id: nextFurnitureId(existing),
    kind,
    x,
    y,
    widthFt: spec.widthFt,
    depthFt: spec.depthFt,
    heightFt: spec.heightFt,
    rotationDeg: 0,
    colour: spec.colour,
  };
}

const WALL_T = 0.5;
const ROTATION_FOR_WALL: Record<PlanSide, number> = { NORTH: 0, EAST: 90, SOUTH: 180, WEST: 270 };

// A starting arrangement for the drawn rooms: pieces against walls, kept clear of doors (and of windows when tall).
// It is only a suggestion; every piece can be moved, resized, recoloured or deleted afterwards.
export function suggestFurniture(rooms: PlanRoom[], openings: PlanOpening[]): PlanFurniture[] {
  const items: PlanFurniture[] = [];
  const push = (kind: FurnitureKind, x: number, y: number, widthFt: number, depthFt: number, rotationDeg: number, over?: Partial<PlanFurniture>) => {
    const spec = furnitureSpec(kind);
    items.push({
      id: `f${items.length + 1}`, kind, x: round(x), y: round(y), widthFt, depthFt,
      heightFt: spec.heightFt, rotationDeg, colour: spec.colour, ...over,
    });
  };

  for (const room of rooms) {
    const w = room.widthFt;
    const d = room.depthFt;
    const ops = openings.filter((o) => o.roomId === room.id);

    // A spot of `len` along the wall and `dep` out from it, or null when doors/windows are in the way.
    const onWall = (side: PlanSide, len: number, dep: number, tall: boolean) => {
      const horizontal = side === "NORTH" || side === "SOUTH";
      const span = horizontal ? w : d;
      const m = WALL_T + 0.1;
      const free = (from: number, to: number) =>
        !ops.some((o) => o.side === side && (o.kind === "DOOR" || tall) && o.offsetFt < to + 1.5 && o.offsetFt + o.widthFt > from - 1.5);
      for (const s of [(span - len) / 2, m + 0.5, span - m - len - 0.5]) {
        if (s < m - 0.01 || s + len > span - m + 0.01 || !free(s, s + len)) continue;
        const along = s + len / 2;
        const out = m + dep / 2;
        const cx = horizontal ? along : side === "WEST" ? out : w - out;
        const cy = horizontal ? (side === "NORTH" ? out : d - out) : along;
        return { x: room.x + cx, y: room.y + cy };
      }
      return null;
    };
    const place = (sides: PlanSide[], kind: FurnitureKind, len: number, dep: number, over?: Partial<PlanFurniture>) => {
      const tall = (over?.heightFt ?? furnitureSpec(kind).heightFt) > 3;
      for (const side of sides) {
        const p = onWall(side, len, dep, tall);
        if (p) {
          push(kind, p.x, p.y, len, dep, ROTATION_FOR_WALL[side], over);
          return { ...p, side };
        }
      }
      return null;
    };
    const mid = { x: room.x + w / 2, y: room.y + d / 2 };

    switch (room.type) {
      case "BEDROOM":
        if (w < 9 || d < 9) break;
        // Headboard against the wall: back toward that wall.
        place(["SOUTH", "EAST", "WEST"], "BED", 5.5, 6.5);
        place(["EAST", "WEST", "NORTH"], "WARDROBE", 4, 2);
        break;
      case "HALL":
        if (w < 10 || d < 10) break;
        push("RUG", mid.x, mid.y, 8, 5, 0);
        push("TABLE", mid.x, mid.y, 3.5, 2, 0, { heightFt: 1.4 });
        place(["SOUTH", "EAST", "WEST"], "SOFA", 7, 3);
        place(["WEST", "EAST"], "TV_UNIT", 5, 1.4);
        break;
      case "DINING":
        if (w < 9 || d < 8) break;
        push("TABLE", mid.x, mid.y, 6, 3.5, 0);
        push("CHAIR", mid.x - 1.6, mid.y - 2.6, 1.4, 1.4, 180);
        push("CHAIR", mid.x + 1.6, mid.y - 2.6, 1.4, 1.4, 180);
        push("CHAIR", mid.x - 1.6, mid.y + 2.6, 1.4, 1.4, 0);
        push("CHAIR", mid.x + 1.6, mid.y + 2.6, 1.4, 1.4, 0);
        break;
      case "KITCHEN":
        if (w < 7 || d < 7) break;
        place(["WEST", "EAST"], "COUNTER", Math.min(d - 3, 10), 2);
        place(["SOUTH", "NORTH"], "COUNTER", Math.min(w - 4, 8), 2);
        place(["SOUTH", "NORTH", "EAST"], "FRIDGE", 2.6, 2.6);
        break;
      case "BATHROOM":
        if (w < 5 || d < 6) break;
        place(["SOUTH", "WEST", "EAST"], "TOILET", 1.6, 2.2);
        place(["WEST", "EAST", "SOUTH"], "BASIN", 1.6, 1.4);
        break;
      case "POOJA":
        place(["SOUTH", "WEST", "EAST"], "SHRINE", Math.min(w - 1.2, 4), 1.4);
        break;
      case "STUDY":
        place(["WEST", "EAST", "SOUTH"], "DESK", 5, 2);
        break;
      case "PARKING":
        if (w < 7 || d < 12) break;
        push("CAR", mid.x, mid.y, 5.8, 14, 0);
        break;
      default:
        break;
    }
  }
  return items.map((f, i) => ({ ...f, id: `f${i + 1}` }));
}

const round = (n: number) => Math.round(n * 100) / 100;

// Trees, shrubs, flower beds and a bench on the open plot around the house, for the ground floor. It keeps clear of
// the house (plus a 3 ft margin) and of the drive in front of any parking room. Like the room furniture it is only
// a starting point: every piece can be moved, resized, recoloured or deleted.
export function suggestGarden(plotWidth: number, plotDepth: number, rooms: PlanRoom[]): PlanFurniture[] {
  if (rooms.length === 0) return [];
  const minX = Math.min(...rooms.map((r) => r.x)) - 3;
  const maxX = Math.max(...rooms.map((r) => r.x + r.widthFt)) + 3;
  const minY = Math.min(...rooms.map((r) => r.y)) - 3;
  const maxY = Math.max(...rooms.map((r) => r.y + r.depthFt)) + 3;
  const parking = rooms.find((r) => r.type === "PARKING");
  const onDrive = (x: number, y: number) =>
    parking !== undefined && x > parking.x - 2 && x < parking.x + parking.widthFt + 2 && y < parking.y + 2;
  const free = (x: number, y: number, pad: number) =>
    !(x > minX - pad && x < maxX + pad && y > minY - pad && y < maxY + pad) && !onDrive(x, y)
    && x > pad && x < plotWidth - pad && y > pad && y < plotDepth - pad;

  const items: PlanFurniture[] = [];
  const add = (kind: FurnitureKind, x: number, y: number, over?: Partial<PlanFurniture>) => {
    const spec = furnitureSpec(kind);
    items.push({
      id: `g${items.length + 1}`, kind, x: round(x), y: round(y), widthFt: spec.widthFt, depthFt: spec.depthFt,
      heightFt: spec.heightFt, rotationDeg: 0, colour: spec.colour, ...over,
    });
  };

  // Trees on a loose ring near the plot boundary, alternating shades and sizes.
  const shades = ["#2f7d32", "#3d8b3d", "#4a9a45", "#2a6f35"];
  let n = 0;
  for (let x = 4; x < plotWidth - 3; x += 9) {
    for (const y of [4, plotDepth - 4]) {
      if (free(x, y, 3)) {
        add("TREE", x, y, { colour: shades[n % shades.length], widthFt: 6 + (n % 3) * 1.5, depthFt: 6 + (n % 3) * 1.5, heightFt: 11 + (n % 4) * 1.5 });
        n++;
      }
    }
  }
  for (let y = 12; y < plotDepth - 8; y += 10) {
    for (const x of [4, plotWidth - 4]) {
      if (free(x, y, 3)) {
        add("TREE", x, y, { colour: shades[n % shades.length], widthFt: 6 + (n % 3) * 1.5, depthFt: 6 + (n % 3) * 1.5, heightFt: 11 + (n % 4) * 1.5 });
        n++;
      }
    }
  }
  // Shrubs and a flower bed softening the house walls, a bench in the back garden.
  const midX = (minX + maxX) / 2;
  for (const x of [minX + 1, maxX - 1]) {
    if (free(x, minY - 2.5, 0.5)) add("SHRUB", x, minY - 2.5);
  }
  if (free(midX, minY - 2, 0.5)) add("FLOWER_BED", midX, minY - 2, { colour: "#d95f8a" });
  if (free(midX, maxY + 3.5, 0.5)) add("BENCH", midX, maxY + 3.5);
  return items;
}
