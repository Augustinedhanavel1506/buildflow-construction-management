import * as THREE from "three";
import type { FloorPlan } from "../../../types/plan";
import { FLOOR_LEVEL_FT, STOREY_FT } from "./houseModel";

const INK = "#ff6a00";

// 12.5 ft -> 12′ 6″
export function feetInches(ft: number): string {
  const totalInches = Math.round(ft * 12);
  const f = Math.floor(totalInches / 12);
  const i = totalInches % 12;
  return i === 0 ? `${f}′` : `${f}′ ${i}″`;
}

function label(text: string, size: number, bold = false): THREE.Sprite {
  const pad = 14;
  const fontPx = 44;
  const canvas = document.createElement("canvas");
  const g = canvas.getContext("2d");
  if (g) g.font = `${bold ? "700" : "600"} ${fontPx}px Inter, Arial, sans-serif`;
  const textWidth = g ? g.measureText(text).width : text.length * 24;
  canvas.width = Math.ceil(textWidth + pad * 2);
  canvas.height = fontPx + pad * 2;
  const ctx = canvas.getContext("2d");
  if (ctx) {
    ctx.font = `${bold ? "700" : "600"} ${fontPx}px Inter, Arial, sans-serif`;
    ctx.fillStyle = "rgba(255,255,255,0.94)";
    ctx.beginPath();
    ctx.roundRect(0, 0, canvas.width, canvas.height, 18);
    ctx.fill();
    ctx.strokeStyle = INK;
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.fillStyle = "#222";
    ctx.textBaseline = "middle";
    ctx.fillText(text, pad, canvas.height / 2 + 2);
  }
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, depthTest: false, transparent: true }));
  sprite.scale.set((canvas.width / canvas.height) * size, size, 1);
  sprite.renderOrder = 20;
  return sprite;
}

function line(group: THREE.Group, points: [number, number, number][]): void {
  const geometry = new THREE.BufferGeometry().setFromPoints(points.map((p) => new THREE.Vector3(...p)));
  const l = new THREE.Line(geometry, new THREE.LineBasicMaterial({ color: INK, depthTest: false }));
  l.renderOrder = 15;
  group.add(l);
}

// A dimension from (x1,z1) to (x2,z2) at height y, drawn with end ticks and its length in the middle.
function dimension(group: THREE.Group, x1: number, z1: number, x2: number, z2: number, y: number, size: number, text?: string) {
  const length = Math.hypot(x2 - x1, z2 - z1);
  if (length < 0.2) return;
  const tx = (z2 - z1) / length;
  const tz = -(x2 - x1) / length;
  const tick = 0.4;
  line(group, [[x1, y, z1], [x2, y, z2]]);
  line(group, [[x1 - tx * tick, y, z1 - tz * tick], [x1 + tx * tick, y, z1 + tz * tick]]);
  line(group, [[x2 - tx * tick, y, z2 - tz * tick], [x2 + tx * tick, y, z2 + tz * tick]]);
  const s = label(text ?? feetInches(length), size);
  s.position.set((x1 + x2) / 2, y + 0.3, (z1 + z2) / 2);
  group.add(s);
}

// Orange dimension lines for one floor with its roof off: each room's width and depth, its area, the size of
// every door and window, the whole building's overall size, and the storey height.
export function buildMeasurements(floor: FloorPlan, plotWidth: number, plotDepth: number): THREE.Group {
  const group = new THREE.Group();
  const base = floor.floorLevel * STOREY_FT;
  const y = base + FLOOR_LEVEL_FT + 0.15;
  // Label height scales with the building so the text stays readable from the usual viewing distances.
  const span = floor.rooms.length > 0
    ? Math.max(Math.max(...floor.rooms.map((r) => r.x + r.widthFt)) - Math.min(...floor.rooms.map((r) => r.x)),
      Math.max(...floor.rooms.map((r) => r.y + r.depthFt)) - Math.min(...floor.rooms.map((r) => r.y)))
    : Math.min(plotWidth, plotDepth);
  const size = Math.max(1.3, span / 20);

  for (const r of floor.rooms) {
    const inset = 0.9;
    dimension(group, r.x + inset, r.y + inset, r.x + r.widthFt - inset, r.y + inset, y, size);
    dimension(group, r.x + inset, r.y + inset, r.x + inset, r.y + r.depthFt - inset, y, size);
    const name = label(`${r.name || r.type}  ${Math.round(r.widthFt * r.depthFt)} sq.ft`, size * 1.15, true);
    name.position.set(r.x + r.widthFt / 2, y + 0.4, r.y + r.depthFt / 2);
    group.add(name);
  }

  for (const o of floor.openings) {
    const r = floor.rooms.find((x) => x.id === o.roomId);
    if (!r) continue;
    const tag = label(`${o.kind === "DOOR" ? "D" : "W"} ${feetInches(o.widthFt)}`, size * 0.8);
    const along = o.offsetFt + o.widthFt / 2;
    const px = o.side === "WEST" ? r.x : o.side === "EAST" ? r.x + r.widthFt : r.x + along;
    const pz = o.side === "NORTH" ? r.y : o.side === "SOUTH" ? r.y + r.depthFt : r.y + along;
    tag.position.set(px, y + 1.6, pz);
    group.add(tag);
  }

  if (floor.rooms.length > 0) {
    const minX = Math.min(...floor.rooms.map((r) => r.x));
    const maxX = Math.max(...floor.rooms.map((r) => r.x + r.widthFt));
    const minZ = Math.min(...floor.rooms.map((r) => r.y));
    const maxZ = Math.max(...floor.rooms.map((r) => r.y + r.depthFt));
    const off = 3;
    dimension(group, minX, minZ - off, maxX, minZ - off, y, size * 1.2, `Overall ${feetInches(maxX - minX)}`);
    dimension(group, minX - off, minZ, minX - off, maxZ, y, size * 1.2, `Overall ${feetInches(maxZ - minZ)}`);
    // Storey height, drawn up the building's front-left corner.
    line(group, [[minX, base, minZ], [minX, base + STOREY_FT, minZ]]);
    const h = label(`Height ${feetInches(STOREY_FT)}`, size);
    h.position.set(minX, base + STOREY_FT / 2, minZ - 0.5);
    group.add(h);
  }
  return group;
}
