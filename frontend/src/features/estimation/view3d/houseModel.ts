import * as THREE from "three";
import { footprint } from "../furniture";
import { buildFurnitureMesh, placeFurniture, selectionOutline } from "./furniture3d";
import type { PlanOpening, PlanRoom, PlansResponse, PlanSide } from "../../../types/plan";

export type RoofStyle = "FLAT" | "SLOPED";

export interface HouseColours {
  exterior: string;
  roof: string;
  roofStyle: RoofStyle;
  rooms: Record<string, string>;
}

export const DEFAULT_COLOURS: HouseColours = {
  exterior: "#efe6d6",
  roof: "#a0522d",
  roofStyle: "SLOPED",
  rooms: {},
};

export const STOREY_FT = 10;
export const EYE_FT = 5.5;
const WALL_T = 0.4;
const SLAB_T = 0.4;
const FLOOR_T = 0.15;
export const FLOOR_LEVEL_FT = FLOOR_T;
const DOOR_TOP = 7;
const SILL = 3;

type FloorKind = "wood" | "tile" | "concrete" | "marble";
const textures = new Map<FloorKind, THREE.CanvasTexture>();

// Procedural floor patterns so no image files have to be downloaded.
function floorTexture(kind: FloorKind): THREE.CanvasTexture {
  const cached = textures.get(kind);
  if (cached) return cached;
  const size = 256;
  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const g = canvas.getContext("2d");
  if (g) {
    if (kind === "wood") {
      const plank = size / 4;
      for (let i = 0; i < 4; i++) {
        const shade = 150 + ((i * 37) % 40);
        g.fillStyle = `rgb(${shade + 40}, ${shade - 10}, ${shade - 60})`;
        g.fillRect(0, i * plank, size, plank);
        g.fillStyle = "rgba(60,30,10,0.35)";
        g.fillRect(0, i * plank, size, 2);
        g.fillRect((i * 97) % size, i * plank, 2, plank);
      }
    } else if (kind === "tile" || kind === "marble") {
      g.fillStyle = kind === "tile" ? "#e6ebee" : "#f0ece4";
      g.fillRect(0, 0, size, size);
      g.strokeStyle = kind === "tile" ? "#b9c2c8" : "#cfc6b6";
      g.lineWidth = 3;
      const step = size / 2;
      for (let i = 0; i <= 2; i++) {
        g.beginPath();
        g.moveTo(i * step, 0);
        g.lineTo(i * step, size);
        g.moveTo(0, i * step);
        g.lineTo(size, i * step);
        g.stroke();
      }
    } else {
      g.fillStyle = "#8d8d8d";
      g.fillRect(0, 0, size, size);
      for (let i = 0; i < 900; i++) {
        const s = 110 + ((i * 7919) % 60);
        g.fillStyle = `rgba(${s},${s},${s},0.5)`;
        g.fillRect((i * 53) % size, (i * 97) % size, 2, 2);
      }
    }
  }
  const tex = new THREE.CanvasTexture(canvas);
  tex.wrapS = THREE.RepeatWrapping;
  tex.wrapT = THREE.RepeatWrapping;
  tex.colorSpace = THREE.SRGBColorSpace;
  textures.set(kind, tex);
  return tex;
}

function floorKindFor(type: string): FloorKind {
  if (type === "BATHROOM" || type === "KITCHEN" || type === "UTILITY") return "tile";
  if (type === "PARKING" || type === "STAIRCASE" || type === "BALCONY") return "concrete";
  if (type === "POOJA" || type === "HALL" || type === "DINING") return "marble";
  return "wood";
}

export interface Collider {
  level: number;
  minX: number;
  maxX: number;
  minZ: number;
  maxZ: number;
}

// A flight of stairs from `level` up to `level + 1`, rising along +x (axis "x") or +z (axis "z") of this rectangle.
export interface Stair {
  level: number;
  minX: number;
  maxX: number;
  minZ: number;
  maxZ: number;
  axis: "x" | "z";
  // 1 when the flight rises toward larger x / z, -1 when it rises toward smaller.
  dir: 1 | -1;
}

export interface BuiltHouse {
  stairs: Stair[];
  // Every placed piece, keyed `${level}:${id}`, so the page can pick, highlight and drag them.
  furniture: Map<string, THREE.Object3D>;
  group: THREE.Group;
  colliders: Collider[];
  levels: number[];
  centre: THREE.Vector3;
  size: number;
}

export const DEFAULT_ROOM_COLOURS: Record<string, string> = {
  BEDROOM: "#f2e3d3",
  BATHROOM: "#d5e6ea",
  KITCHEN: "#f4ecc8",
  HALL: "#efe9df",
  DINING: "#efe3d0",
  POOJA: "#f8e7b9",
  BALCONY: "#e5e5e5",
  STAIRCASE: "#e0dcd5",
  PARKING: "#d0d0d0",
  UTILITY: "#e2e2dc",
  STUDY: "#e1e6ef",
  OTHER: "#ececec",
};

export function roomColour(room: PlanRoom, colours: HouseColours): string {
  return colours.rooms[room.id] ?? DEFAULT_ROOM_COLOURS[room.type] ?? "#ececec";
}

// Plan x runs along the plot width and plan y along its depth (y = 0 is the road-facing front);
// they become three.js x and z. Everything is in feet.
export function buildHouse(
  plans: PlansResponse,
  colours: HouseColours,
  hideRoof: boolean,
  cutLevel: number,
  selectedFurniture: string | null,
): BuiltHouse {
  const group = new THREE.Group();
  const colliders: Collider[] = [];
  const stairs: Stair[] = [];
  const cache = new Map<string, THREE.MeshLambertMaterial>();
  const mat = (colour: string, extra?: THREE.MeshLambertMaterialParameters) => {
    const key = colour + JSON.stringify(extra ?? {});
    let m = cache.get(key);
    if (!m) {
      m = new THREE.MeshLambertMaterial({ color: colour, ...extra });
      cache.set(key, m);
    }
    return m;
  };
  const glass = mat("#8fc8f0", { transparent: true, opacity: 0.35, side: THREE.DoubleSide });

  // With the roof off, floors above the one being studied are left out so it can be seen from above.
  const floors = plans.floors.filter((f) => f.rooms.length > 0 && !(hideRoof && f.floorLevel > cutLevel)).sort((a, b) => a.floorLevel - b.floorLevel);
  const topLevel = floors.length > 0 ? floors[floors.length - 1].floorLevel : 0;

  const box = (
    x0: number, x1: number, y0: number, y1: number, z0: number, z1: number,
    materials: THREE.Material | THREE.Material[],
  ) => {
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(x1 - x0, y1 - y0, z1 - z0), materials);
    mesh.position.set((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    group.add(mesh);
  };

  for (const floor of floors) {
    const base = floor.floorLevel * STOREY_FT;
    const rooms = floor.rooms;
    const outside = (px: number, py: number) =>
      !rooms.some((r) => px > r.x + 0.01 && px < r.x + r.widthFt - 0.01 && py > r.y + 0.01 && py < r.y + r.depthFt - 0.01);

    // A staircase room continues up when the floor above has a staircase over it, and arrives from below the same way.
    const overlaps = (a: PlanRoom, b: PlanRoom) =>
      a.x < b.x + b.widthFt - 0.01 && b.x < a.x + a.widthFt - 0.01 && a.y < b.y + b.depthFt - 0.01 && b.y < a.y + a.depthFt - 0.01;
    const stairOn = (room: PlanRoom, level: number) =>
      room.type === "STAIRCASE"
      && (plans.floors.find((f) => f.floorLevel === level)?.rooms ?? []).some((r) => r.type === "STAIRCASE" && overlaps(r, room));

    for (const room of rooms) {
      const stairUp = stairOn(room, floor.floorLevel + 1);
      const stairDown = stairOn(room, floor.floorLevel - 1);
      const inside = mat(roomColour(room, colours));
      const exterior = mat(colours.exterior);
      const x0 = room.x;
      const x1 = room.x + room.widthFt;
      const z0 = room.y;
      const z1 = room.y + room.depthFt;
      const top = base + STOREY_FT;

      if (!stairDown) box(x0, x1, base, base + FLOOR_T, z0, z1, mat("#cfc3b0"));
      const floorGeo = new THREE.PlaneGeometry(room.widthFt, room.depthFt);
      const uv = floorGeo.attributes.uv;
      for (let i = 0; i < uv.count; i++) {
        uv.setXY(i, (uv.getX(i) * room.widthFt) / 4, (uv.getY(i) * room.depthFt) / 4);
      }
      floorGeo.rotateX(-Math.PI / 2);
      const floorMesh = new THREE.Mesh(floorGeo, new THREE.MeshLambertMaterial({ map: floorTexture(floorKindFor(room.type)) }));
      floorMesh.position.set((x0 + x1) / 2, base + FLOOR_T + 0.01, (z0 + z1) / 2);
      floorMesh.receiveShadow = true;
      if (!stairDown) group.add(floorMesh);

      if (stairUp) {
        // Steps climb along the longer side, 16 risers to the next floor, with a handrail on one side.
        const sx0 = x0 + WALL_T;
        const sx1 = x1 - WALL_T;
        const sz0 = z0 + WALL_T;
        const sz1 = z1 - WALL_T;
        const axis: "x" | "z" = sx1 - sx0 >= sz1 - sz0 ? "x" : "z";
        const steps = 16;
        // Rise toward the end where the staircase on the floor above has its door, so the top is not walled off.
        const upper = (plans.floors.find((f) => f.floorLevel === floor.floorLevel + 1)?.rooms ?? [])
          .find((r) => r.type === "STAIRCASE" && overlaps(r, room));
        const upperDoor = upper
          ? plans.floors.find((f) => f.floorLevel === floor.floorLevel + 1)?.openings.find((o) => o.roomId === upper.id && o.kind === "DOOR")
          : undefined;
        let dir: 1 | -1 = 1;
        if (upper && upperDoor) {
          const along = upperDoor.offsetFt + upperDoor.widthFt / 2;
          const doorX = upperDoor.side === "WEST" ? upper.x : upperDoor.side === "EAST" ? upper.x + upper.widthFt : upper.x + along;
          const doorZ = upperDoor.side === "NORTH" ? upper.y : upperDoor.side === "SOUTH" ? upper.y + upper.depthFt : upper.y + along;
          const doorAt = axis === "x" ? doorX : doorZ;
          const middle = axis === "x" ? (sx0 + sx1) / 2 : (sz0 + sz1) / 2;
          dir = doorAt >= middle ? 1 : -1;
        }
        const len = axis === "x" ? sx1 - sx0 : sz1 - sz0;
        const tread = mat("#d9d2c5");
        for (let i = 0; i < steps; i++) {
          const a0 = i * (len / steps);
          const a1 = (i + 1) * (len / steps);
          const h = ((i + 1) * STOREY_FT) / steps;
          // Mirrored when the flight rises toward smaller coordinates.
          const [m0, m1] = dir > 0 ? [a0, a1] : [len - a1, len - a0];
          if (axis === "x") box(sx0 + m0, sx0 + m1, base + FLOOR_T, base + FLOOR_T + h, sz0, sz1, tread);
          else box(sx0, sx1, base + FLOOR_T, base + FLOOR_T + h, sz0 + m0, sz0 + m1, tread);
        }
        stairs.push({ level: floor.floorLevel, minX: sx0, maxX: sx1, minZ: sz0, maxZ: sz1, axis, dir });
      }

      const isTop = floor.floorLevel === topLevel;
      if (!(isTop && hideRoof) && !stairUp) {
        const white = mat("#fbfbfb");
        const underside = new THREE.MeshBasicMaterial({ color: "#efebe3" });
        if (isTop && colours.roofStyle === "FLAT") {
          const roof = mat(colours.roof);
          box(x0 - 0.5, x1 + 0.5, top - SLAB_T, top + 0.3, z0 - 0.5, z1 + 0.5, [roof, roof, roof, underside, roof, roof]);
        } else {
          box(x0, x1, top - SLAB_T, top, z0, z1, [white, white, white, underside, white, white]);
        }
      }

      // A door is recorded on one room only, but it also opens the neighbouring room's wall that shares the line.
      const openingsOf = (side: PlanSide): PlanOpening[] => {
        const own = floor.openings.filter((o) => o.roomId === room.id && o.side === side);
        const shared: PlanOpening[] = [];
        for (const other of rooms) {
          if (other.id === room.id) continue;
          const mirror: PlanSide = side === "NORTH" ? "SOUTH" : side === "SOUTH" ? "NORTH" : side === "WEST" ? "EAST" : "WEST";
          const horizontal = side === "NORTH" || side === "SOUTH";
          const myLine = side === "NORTH" ? room.y : side === "SOUTH" ? room.y + room.depthFt
            : side === "WEST" ? room.x : room.x + room.widthFt;
          const theirLine = mirror === "NORTH" ? other.y : mirror === "SOUTH" ? other.y + other.depthFt
            : mirror === "WEST" ? other.x : other.x + other.widthFt;
          if (Math.abs(myLine - theirLine) > 0.05) continue;
          for (const o of floor.openings) {
            if (o.roomId !== other.id || o.side !== mirror || o.kind !== "DOOR") continue;
            const shift = horizontal ? other.x - room.x : other.y - room.y;
            shared.push({ ...o, id: `${o.id}~${room.id}`, roomId: room.id, side, offsetFt: o.offsetFt + shift });
          }
        }
        return [...own, ...shared];
      };
      const wall = (side: PlanSide) => {
        const horizontal = side === "NORTH" || side === "SOUTH";
        const full = horizontal ? room.widthFt : room.depthFt;
        const inset = horizontal ? 0 : WALL_T;
        const a0 = inset;
        const a1 = full - inset;
        const ops = openingsOf(side)
          .map((o: PlanOpening) => ({ ...o, s: Math.max(a0, o.offsetFt), e: Math.min(a1, o.offsetFt + o.widthFt) }))
          .filter((o) => o.e > o.s)
          .sort((p, q) => p.s - q.s);

        const piece = (s: number, e: number, y0: number, y1: number) => {
          if (e - s < 0.01 || y1 - y0 < 0.01) return;
          const mid = (s + e) / 2;
          const edgeX = horizontal ? room.x + mid : side === "WEST" ? room.x : room.x + room.widthFt;
          const edgeY = horizontal ? (side === "NORTH" ? room.y : room.y + room.depthFt) : room.y + mid;
          const nx = side === "WEST" ? -1 : side === "EAST" ? 1 : 0;
          const ny = side === "NORTH" ? -1 : side === "SOUTH" ? 1 : 0;
          const external = outside(edgeX + nx * 0.6, edgeY + ny * 0.6);
          const out = external ? exterior : inside;
          // Box face order: +x, -x, +y, -y, +z, -z.
          const faces: THREE.Material[] = [inside, inside, inside, inside, inside, inside];
          if (side === "NORTH") faces[5] = out;
          if (side === "SOUTH") faces[4] = out;
          if (side === "WEST") faces[1] = out;
          if (side === "EAST") faces[0] = out;

          let bx0: number, bx1: number, bz0: number, bz1: number;
          if (horizontal) {
            bx0 = room.x + s;
            bx1 = room.x + e;
            bz0 = side === "NORTH" ? z0 : z1 - WALL_T;
            bz1 = side === "NORTH" ? z0 + WALL_T : z1;
          } else {
            bz0 = room.y + s;
            bz1 = room.y + e;
            bx0 = side === "WEST" ? x0 : x1 - WALL_T;
            bx1 = side === "WEST" ? x0 + WALL_T : x1;
          }
          box(bx0, bx1, base + y0, base + y1, bz0, bz1, faces);
          if (y0 <= EYE_FT && y1 >= EYE_FT) {
            colliders.push({ level: floor.floorLevel, minX: bx0, maxX: bx1, minZ: bz0, maxZ: bz1 });
          }
        };

        let cursor = a0;
        for (const o of ops) {
          piece(cursor, o.s, FLOOR_T, STOREY_FT);
          if (o.kind === "DOOR") {
            piece(o.s, o.e, DOOR_TOP, STOREY_FT);
            const jamb = mat("#6b4423");
            for (const at of [o.s, o.e - 0.15]) {
              if (horizontal) {
                const jz = side === "NORTH" ? z0 : z1 - WALL_T;
                box(room.x + at, room.x + at + 0.15, base + FLOOR_T, base + DOOR_TOP, jz - 0.03, jz + WALL_T + 0.03, jamb);
              } else {
                const jx = side === "WEST" ? x0 : x1 - WALL_T;
                box(jx - 0.03, jx + WALL_T + 0.03, base + FLOOR_T, base + DOOR_TOP, room.y + at, room.y + at + 0.15, jamb);
              }
            }
          } else {
            piece(o.s, o.e, FLOOR_T, SILL);
            piece(o.s, o.e, DOOR_TOP, STOREY_FT);
            const mid = (o.s + o.e) / 2;
            const w = o.e - o.s;
            const gx = horizontal ? room.x + mid : side === "WEST" ? x0 + WALL_T / 2 : x1 - WALL_T / 2;
            const gz = horizontal ? (side === "NORTH" ? z0 + WALL_T / 2 : z1 - WALL_T / 2) : room.y + mid;
            const pane = new THREE.Mesh(
              new THREE.BoxGeometry(horizontal ? w : 0.06, DOOR_TOP - SILL, horizontal ? 0.06 : w), glass);
            pane.position.set(gx, base + (SILL + DOOR_TOP) / 2, gz);
            group.add(pane);
          }
          cursor = Math.max(cursor, o.e);
        }
        piece(cursor, a1, FLOOR_T, STOREY_FT);
      };
      (["NORTH", "SOUTH", "WEST", "EAST"] as PlanSide[]).forEach(wall);

      // Ceiling light and furniture.
      if (room.type !== "PARKING" && !(isTop && hideRoof)) {
        box(room.x + room.widthFt / 2 - 0.6, room.x + room.widthFt / 2 + 0.6, top - SLAB_T - 0.08, top - SLAB_T,
          room.y + room.depthFt / 2 - 0.6, room.y + room.depthFt / 2 + 0.6, new THREE.MeshBasicMaterial({ color: "#fffbe8" }));
      }
    }
  }

  const furnitureRoots = new Map<string, THREE.Object3D>();
  for (const floor of floors) {
    for (const item of floor.furniture ?? []) {
      const root = buildFurnitureMesh(item);
      placeFurniture(root, item, floor.floorLevel * STOREY_FT + FLOOR_T);
      root.userData = { furnitureId: item.id, level: floor.floorLevel };
      const key = `${floor.floorLevel}:${item.id}`;
      if (selectedFurniture === key) root.add(selectionOutline(item));
      group.add(root);
      furnitureRoots.set(key, root);
      if (item.heightFt > 1) {
        const fp = footprint(item);
        colliders.push({
          level: floor.floorLevel, minX: item.x - fp.w / 2, maxX: item.x + fp.w / 2,
          minZ: item.y - fp.d / 2, maxZ: item.y + fp.d / 2,
        });
      }
    }
  }

  // Sloped roof: one gable prism over everything on the top floor.
  const topFloor = floors.find((f) => f.floorLevel === topLevel);
  if (topFloor && !hideRoof && colours.roofStyle === "SLOPED") {
    const minX = Math.min(...topFloor.rooms.map((r) => r.x)) - 1.5;
    const maxX = Math.max(...topFloor.rooms.map((r) => r.x + r.widthFt)) + 1.5;
    const minZ = Math.min(...topFloor.rooms.map((r) => r.y)) - 1.5;
    const maxZ = Math.max(...topFloor.rooms.map((r) => r.y + r.depthFt)) + 1.5;
    const spanAlongZ = maxX - minX >= maxZ - minZ;
    const span = spanAlongZ ? maxZ - minZ : maxX - minX;
    const length = spanAlongZ ? maxX - minX : maxZ - minZ;
    const rise = Math.min(8, span * 0.28);
    const shape = new THREE.Shape();
    shape.moveTo(0, 0);
    shape.lineTo(span, 0);
    shape.lineTo(span / 2, rise);
    shape.closePath();
    const geometry = new THREE.ExtrudeGeometry(shape, { depth: length, bevelEnabled: false });
    const roof = new THREE.Mesh(geometry, mat(colours.roof, { side: THREE.DoubleSide }));
    const y = (topLevel + 1) * STOREY_FT;
    if (spanAlongZ) {
      // Ridge runs along x: span goes along z, extrusion along x.
      geometry.rotateY(-Math.PI / 2);
      roof.position.set(maxX, y, minZ);
    } else {
      roof.position.set(minX, y, minZ);
    }
    group.add(roof);
  }

  const allRooms = floors.flatMap((f) => f.rooms);
  const minX = Math.min(...allRooms.map((r) => r.x), plans.plotWidthFt);
  const maxX = Math.max(...allRooms.map((r) => r.x + r.widthFt), 0);
  const minZ = Math.min(...allRooms.map((r) => r.y), plans.plotLengthFt);
  const maxZ = Math.max(...allRooms.map((r) => r.y + r.depthFt), 0);
  return {
    group,
    stairs,
    furniture: furnitureRoots,
    colliders,
    levels: floors.map((f) => f.floorLevel),
    centre: new THREE.Vector3((minX + maxX) / 2, 0, (minZ + maxZ) / 2),
    size: Math.max(maxX - minX, maxZ - minZ, 20),
  };
}

// Lawn, compound wall, drive and trees around the house, kept out of the house footprint.
export function buildGarden(plans: PlansResponse, house: BuiltHouse): THREE.Group {
  // Trees the user placed (or accepted) replace the automatic ones.
  const ownTrees = plans.floors.some((f) => (f.furniture ?? []).some((i) => i.kind === "TREE"));
  const garden = new THREE.Group();
  const w = plans.plotWidthFt;
  const l = plans.plotLengthFt;

  const lawn = new THREE.Mesh(new THREE.BoxGeometry(w, 0.2, l), new THREE.MeshLambertMaterial({ color: "#5f9e4b" }));
  lawn.position.set(w / 2, -0.1, l / 2);
  lawn.receiveShadow = true;
  garden.add(lawn);

  const parking = plans.floors.find((f) => f.floorLevel === 0)?.rooms.find((r) => r.type === "PARKING");
  const driveX = parking ? parking.x + parking.widthFt / 2 : w / 2;
  const driveW = parking ? Math.min(parking.widthFt, 12) : 10;
  const driveEnd = parking ? parking.y : l * 0.2;
  const drive = new THREE.Mesh(new THREE.BoxGeometry(driveW, 0.22, driveEnd), new THREE.MeshLambertMaterial({ color: "#8c8c8c" }));
  drive.position.set(driveX, -0.09, driveEnd / 2);
  garden.add(drive);

  const wallMat = new THREE.MeshLambertMaterial({ color: "#e9e4da" });
  const wallPiece = (x0: number, x1: number, z0: number, z1: number) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(x1 - x0, 4, z1 - z0), wallMat);
    m.position.set((x0 + x1) / 2, 2, (z0 + z1) / 2);
    garden.add(m);
  };
  const gate0 = driveX - driveW / 2 - 1;
  const gate1 = driveX + driveW / 2 + 1;
  wallPiece(0, Math.max(gate0, 0.3), -0.3, 0.3);
  wallPiece(Math.min(gate1, w - 0.3), w, -0.3, 0.3);
  wallPiece(0, w, l - 0.3, l + 0.3);
  wallPiece(-0.3, 0.3, 0, l);
  wallPiece(w - 0.3, w + 0.3, 0, l);

  // Deterministic pseudo-random so the garden does not change on every rebuild.
  let seed = 7;
  const rand = () => {
    seed = (seed * 16807) % 2147483647;
    return seed / 2147483647;
  };
  const trunk = new THREE.MeshLambertMaterial({ color: "#7a5230" });
  const greens = ["#2f7d32", "#3d8b3d", "#4a9a45", "#2a6f35"].map((c) => new THREE.MeshLambertMaterial({ color: c }));
  const half = house.size / 2 + 5;
  for (let x = 3; x < w - 1 && !ownTrees; x += 6.5) {
    for (let z = 3; z < l - 1; z += 6.5) {
      const px = x + (rand() - 0.5) * 3;
      const pz = z + (rand() - 0.5) * 3;
      const nearHouse = Math.abs(px - house.centre.x) < half + 1 && Math.abs(pz - house.centre.z) < half + 1
        && px > house.centre.x - house.size / 2 - 4 && px < house.centre.x + house.size / 2 + 4;
      const onDrive = Math.abs(px - driveX) < driveW / 2 + 2 && pz < driveEnd + 3;
      if (nearHouse || onDrive || rand() < 0.35) continue;
      const height = 7 + rand() * 6;
      const t = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.5, height * 0.45, 6), trunk);
      t.position.set(px, height * 0.225, pz);
      garden.add(t);
      const crown = new THREE.Mesh(new THREE.SphereGeometry(height * 0.32, 10, 8), greens[Math.floor(rand() * greens.length)]);
      crown.position.set(px, height * 0.6, pz);
      crown.castShadow = true;
      garden.add(crown);
    }
  }
  return garden;
}

export function disposeGroup(group: THREE.Object3D): void {
  group.traverse((obj) => {
    const mesh = obj as THREE.Mesh;
    if (mesh.geometry) mesh.geometry.dispose();
    const m = mesh.material;
    if (Array.isArray(m)) m.forEach((x) => x.dispose());
    else if (m) m.dispose();
  });
}
