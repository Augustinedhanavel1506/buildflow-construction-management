import type { FloorPlan, PlanOpening, PlanRoom, PlanSide } from "../../../types/plan";
import { feetInches, mmToFt } from "./sheetGeometry";

const INK = "#1b1f24";
const STOREY = 10;
const SILL = 3;
const HEAD = 7;

export type ElevationView = "FRONT" | "REAR" | "LEFT" | "RIGHT";

export const ELEVATION_TITLES: Record<ElevationView, string> = {
  FRONT: "Front elevation (road side)",
  REAR: "Rear elevation",
  LEFT: "Left side elevation (west)",
  RIGHT: "Right side elevation (east)",
};

export interface ElevationStyle {
  exterior: string;
  roof: string;
  roofStyle: "FLAT" | "SLOPED";
}

interface Frame {
  width: number;
  height: number;
}

// The side of each wall the viewer sees, and how the plan axis maps to the page: a viewer at the north looking
// south has west on the right, so the front view is mirrored along x; the east view is mirrored along y.
const FACING: Record<ElevationView, PlanSide> = { FRONT: "NORTH", REAR: "SOUTH", LEFT: "WEST", RIGHT: "EAST" };

function overlapping(floorRooms: PlanRoom[], room: PlanRoom, side: PlanSide, from: number, to: number): boolean {
  const mirror: PlanSide = side === "NORTH" ? "SOUTH" : side === "SOUTH" ? "NORTH" : side === "WEST" ? "EAST" : "WEST";
  const horizontal = side === "NORTH" || side === "SOUTH";
  const line = side === "NORTH" ? room.y : side === "SOUTH" ? room.y + room.depthFt : side === "WEST" ? room.x : room.x + room.widthFt;
  return floorRooms.some((o) => {
    if (o.id === room.id) return false;
    const theirs = mirror === "NORTH" ? o.y : mirror === "SOUTH" ? o.y + o.depthFt : mirror === "WEST" ? o.x : o.x + o.widthFt;
    if (Math.abs(theirs - line) > 0.05) return false;
    const a = horizontal ? o.x : o.y;
    const b = horizontal ? o.x + o.widthFt : o.y + o.depthFt;
    return a < to - 0.01 && b > from + 0.01;
  });
}

// Size of the elevation (feet) so the sheet can pick one scale for all four views.
export function elevationFrame(floors: FloorPlan[], view: ElevationView, style: ElevationStyle): Frame {
  const rooms = floors.flatMap((f) => f.rooms);
  if (rooms.length === 0) return { width: 0, height: 0 };
  const alongX = view === "FRONT" || view === "REAR";
  const lo = Math.min(...rooms.map((r) => (alongX ? r.x : r.y)));
  const hi = Math.max(...rooms.map((r) => (alongX ? r.x + r.widthFt : r.y + r.depthFt)));
  const top = (Math.max(...floors.map((f) => f.floorLevel)) + 1) * STOREY;
  const rise = style.roofStyle === "SLOPED" ? Math.min(8, (hi - lo) * 0.28) : 0.7;
  return { width: hi - lo + 3, height: top + rise };
}

export function ElevationDrawing({ floors, view, style, scale }: {
  floors: FloorPlan[]; view: ElevationView; style: ElevationStyle; scale: number;
}) {
  const sorted = [...floors].sort((a, b) => a.floorLevel - b.floorLevel).filter((f) => f.rooms.length > 0);
  if (sorted.length === 0) return null;
  const allRooms = sorted.flatMap((f) => f.rooms);
  const alongX = view === "FRONT" || view === "REAR";
  const mirrored = view === "FRONT" || view === "RIGHT";
  const lo = Math.min(...allRooms.map((r) => (alongX ? r.x : r.y)));
  const hi = Math.max(...allRooms.map((r) => (alongX ? r.x + r.widthFt : r.y + r.depthFt)));
  const span = hi - lo;
  // Plan coordinate -> horizontal position on the page, in feet from the left edge of the building.
  const h = (p: number) => (mirrored ? hi - p : p - lo);
  const hRange = (a: number, b: number): [number, number] => {
    const p = h(a);
    const q = h(b);
    return p <= q ? [p, q] : [q, p];
  };

  const fontFt = mmToFt(2.2, scale);
  const thin = mmToFt(0.18, scale);
  const tick = mmToFt(1.2, scale);
  const topLevel = sorted[sorted.length - 1].floorLevel;
  const topZ = (topLevel + 1) * STOREY;

  // Roof, like the 3D view: a gable prism whose ridge runs along the longer side of the top floor, or a flat slab.
  const topRooms = sorted[sorted.length - 1].rooms;
  const tMinX = Math.min(...topRooms.map((r) => r.x));
  const tMaxX = Math.max(...topRooms.map((r) => r.x + r.widthFt));
  const tMinY = Math.min(...topRooms.map((r) => r.y));
  const tMaxY = Math.max(...topRooms.map((r) => r.y + r.depthFt));
  const ridgeAlongX = tMaxX - tMinX >= tMaxY - tMinY;
  const roofSpan = (ridgeAlongX ? tMaxY - tMinY : tMaxX - tMinX) + 3;
  const rise = Math.min(8, roofSpan * 0.28);
  const seesGable = style.roofStyle === "SLOPED" && (ridgeAlongX ? !alongX : alongX);

  const marginL = 4;
  const marginR = 11;
  const marginTop = 3;
  const marginBottom = 8;
  const peak = style.roofStyle === "SLOPED" ? rise : 0.7;
  const vw = span + 3 + marginL + marginR;
  const vh = topZ + peak + marginTop + marginBottom;
  const x0 = -1.5 - marginL;
  const y0 = -(topZ + peak + marginTop);

  const out: React.ReactNode[] = [];

  // Walls per floor: the union of the rooms' runs along the view, so a stepped plan shows its steps.
  sorted.forEach((f) => {
    const runs = f.rooms.map((r) => hRange(alongX ? r.x : r.y, alongX ? r.x + r.widthFt : r.y + r.depthFt)).sort((a, b) => a[0] - b[0]);
    const merged: [number, number][] = [];
    for (const run of runs) {
      const last = merged[merged.length - 1];
      if (last && run[0] <= last[1] + 0.05) last[1] = Math.max(last[1], run[1]);
      else merged.push([run[0], run[1]]);
    }
    const base = f.floorLevel * STOREY;
    merged.forEach(([a, b], i) => {
      out.push(<rect key={`w${f.floorLevel}-${i}`} x={a} y={-(base + STOREY)} width={b - a} height={STOREY} fill={style.exterior} stroke={INK} strokeWidth={thin * 2} />);
    });
    // Slab edge line at the top of each floor
    out.push(<line key={`sl${f.floorLevel}`} x1={merged[0][0]} y1={-(base + STOREY)} x2={merged[merged.length - 1][1]} y2={-(base + STOREY)} stroke={INK} strokeWidth={thin * 3} />);

    // Doors and windows on the wall that faces the viewer
    const facing = FACING[view];
    f.openings.forEach((o: PlanOpening) => {
      const room = f.rooms.find((r) => r.id === o.roomId);
      if (!room || o.side !== facing) return;
      const from = (alongX ? room.x : room.y) + o.offsetFt;
      const to = from + o.widthFt;
      if (overlapping(f.rooms, room, o.side, from, to)) return; // an inside wall, not seen from outside
      const [a, b] = hRange(from, to);
      if (o.kind === "DOOR") {
        out.push(<rect key={o.id} x={a} y={-(base + HEAD)} width={b - a} height={HEAD} fill="#8a5a2b" stroke={INK} strokeWidth={thin * 1.5} />);
        out.push(<line key={`${o.id}m`} x1={(a + b) / 2} y1={-(base + HEAD)} x2={(a + b) / 2} y2={-base} stroke={INK} strokeWidth={thin} />);
      } else {
        out.push(<rect key={o.id} x={a} y={-(base + HEAD)} width={b - a} height={HEAD - SILL} fill="#bfe0f5" stroke={INK} strokeWidth={thin * 1.5} />);
        out.push(<line key={`${o.id}m`} x1={(a + b) / 2} y1={-(base + HEAD)} x2={(a + b) / 2} y2={-(base + SILL)} stroke={INK} strokeWidth={thin} />);
        out.push(<line key={`${o.id}s`} x1={a - 0.3} y1={-(base + SILL)} x2={b + 0.3} y2={-(base + SILL)} stroke={INK} strokeWidth={thin * 2} />);
      }
    });
  });

  // Roof
  let roof: React.ReactNode;
  if (style.roofStyle === "FLAT") {
    roof = <rect x={-0.5} y={-(topZ + 0.7)} width={span + 1} height={0.7} fill={style.roof} stroke={INK} strokeWidth={thin * 2} />;
  } else if (seesGable) {
    const c = span / 2;
    const half = roofSpan / 2;
    roof = <polygon points={`${c - half},${-topZ} ${c + half},${-topZ} ${c},${-(topZ + rise)}`} fill={style.roof} stroke={INK} strokeWidth={thin * 2} />;
  } else {
    // Seen from the slope side: the roof is a band across the whole width, ridge line at the top.
    roof = <g>
      <rect x={-1.5} y={-(topZ + rise)} width={span + 3} height={rise} fill={style.roof} stroke={INK} strokeWidth={thin * 2} />
      <line x1={-1.5} y1={-(topZ + rise * 0.55)} x2={span + 1.5} y2={-(topZ + rise * 0.55)} stroke={INK} strokeWidth={thin} strokeDasharray={`${tick} ${tick}`} />
    </g>;
  }

  const levels = [0, ...sorted.map((f) => (f.floorLevel + 1) * STOREY)];
  const dimX = span + 1.5 + 4;

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={`${(vw * 304.8) / scale}mm`}
      height={`${(vh * 304.8) / scale}mm`}
      viewBox={`${x0} ${y0} ${vw} ${vh}`}
      fontFamily="Inter, Arial, sans-serif"
      role="img"
      aria-label={ELEVATION_TITLES[view]}
    >
      {/* Ground, hatched below the line */}
      <rect x={x0} y={0} width={vw} height={1.6} fill="#e9e6df" />
      {Array.from({ length: Math.floor(vw / 1.4) }, (_, i) => (
        <line key={i} x1={x0 + i * 1.4} y1={1.6} x2={x0 + i * 1.4 - 1.2} y2={0} stroke={INK} strokeWidth={thin} />
      ))}
      <line x1={x0} y1={0} x2={x0 + vw} y2={0} stroke={INK} strokeWidth={thin * 4} />

      {out}
      {roof}

      {/* Levels: a marker and height at ground, each floor top and the roof */}
      {[...levels, ...(peak > 1.5 ? [topZ + peak] : [])].filter((v, i, arr) => arr.indexOf(v) === i).map((z) => (
        <g key={`lv${z}`}>
          <line x1={span + 1.6} y1={-z} x2={dimX} y2={-z} stroke={INK} strokeWidth={thin} strokeDasharray={`${tick} ${tick * 0.6}`} />
          <polygon points={`${dimX},${-z} ${dimX + tick * 0.9},${-z - tick * 1.3} ${dimX - tick * 0.9},${-z - tick * 1.3}`} fill={INK} />
          <text x={dimX + tick * 1.6} y={z === 0 ? -fontFt * 0.5 : -z + fontFt * 0.3} fontSize={fontFt}>{z === 0 ? "±0′" : `+${feetInches(z)}`}</text>
        </g>
      ))}

      {/* Overall width */}
      <g>
        <line x1={0} y1={4.2} x2={span} y2={4.2} stroke={INK} strokeWidth={thin} />
        {[0, span].map((p) => (
          <g key={p}>
            <line x1={p} y1={3.2} x2={p} y2={5.2} stroke={INK} strokeWidth={thin} />
            <line x1={p - tick / 2} y1={4.2 + tick / 2} x2={p + tick / 2} y2={4.2 - tick / 2} stroke={INK} strokeWidth={thin * 2} />
          </g>
        ))}
        <text x={span / 2} y={4.2 + fontFt * 1.25} fontSize={fontFt} textAnchor="middle">{feetInches(span)}</text>
      </g>
    </svg>
  );
}
