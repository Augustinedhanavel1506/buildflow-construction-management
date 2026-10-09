import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { Skeleton } from "../../../components/ui/Skeleton";
import { useToast } from "../../../components/ui/ToastProvider";
import { estimationService } from "../../../services/estimationService";
import { getErrorMessage } from "../../../utils/apiError";
import type { FurnitureKind, PlanFurniture, PlanRoom, PlansResponse } from "../../../types/plan";
import { FURNITURE_CATALOG, furnitureLabel, furnitureSpec, newFurniture, suggestFurniture, suggestGarden } from "../furniture";
import { buildMeasurements } from "./measurements";
import { floorLabel } from "../labels";
import {
  type BuiltHouse,
  type HouseColours,
  type RoofStyle,
  DEFAULT_COLOURS,
  EYE_FT,
  STOREY_FT,
  buildGarden,
  buildHouse,
  disposeGroup,
  roomColour,
} from "./houseModel";
import "./HomeView3DPage.css";

type Mode = "ORBIT" | "WALK";

const WALL_PALETTE = ["#efe6d6", "#ffffff", "#e8d9c0", "#d9e4dd", "#cfd8e8", "#f2d6d0", "#e9e3a8", "#b8c7b0"];
const ROOF_PALETTE = ["#a0522d", "#7a3b2e", "#555d66", "#b5651d", "#3b5b4a", "#c9b79c"];
const WALK_SPEED_FT_S = 7;
const WALK_RADIUS = 1;

function loadColours(id: string | undefined): HouseColours {
  try {
    const raw = localStorage.getItem(`bf-3d-${id}`);
    if (raw) return { ...DEFAULT_COLOURS, ...(JSON.parse(raw) as Partial<HouseColours>) };
  } catch {
    // storage unavailable or corrupt: fall back to defaults
  }
  return DEFAULT_COLOURS;
}

interface Engine {
  renderer: THREE.WebGLRenderer;
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  controls: OrbitControls;
  houseGroup: THREE.Group | null;
  gardenGroup: THREE.Group | null;
  built: BuiltHouse | null;
  mode: Mode;
  walkLevel: number;
  yaw: number;
  pitch: number;
  keys: Set<string>;
  orbitPos: THREE.Vector3;
  orbitTarget: THREE.Vector3;
  lamp: THREE.PointLight;
  person: THREE.Group;
  spin: boolean;
  measureGroup: THREE.Group | null;
}

interface EditActions {
  select: (key: string | null) => void;
  move: (level: number, id: string, x: number, y: number) => void;
  key: (key: string) => boolean;
}

// A simple standing figure, 5.5 ft tall, marking where a walk-through will start.
function makePerson(): THREE.Group {
  const person = new THREE.Group();
  const part = (geo: THREE.BufferGeometry, colour: string, y: number) => {
    const m = new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ color: colour }));
    m.position.y = y;
    m.castShadow = true;
    person.add(m);
  };
  part(new THREE.CylinderGeometry(0.35, 0.3, 2.6, 10), "#2f3b52", 1.3);
  part(new THREE.CylinderGeometry(0.6, 0.45, 2, 10), "#e07a3f", 3.6);
  part(new THREE.SphereGeometry(0.45, 12, 10), "#e0b48c", 5.05);
  return person;
}

// A simple 3D model of the drawn plan: walls with the openings cut out, a roof in the chosen colour, painted
// rooms, and a garden around the house. Orbit it from outside or walk through it at eye height.
export function HomeView3DPage() {
  const { id } = useParams();
  const requirementId = Number(id);
  const mountRef = useRef<HTMLDivElement>(null);
  const engineRef = useRef<Engine | null>(null);
  const plansRef = useRef<PlansResponse | null>(null);
  const [plans, setPlans] = useState<PlansResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [colours, setColours] = useState<HouseColours>(() => loadColours(id));
  const [hideRoof, setHideRoof] = useState(false);
  const [mode, setMode] = useState<Mode>("ORBIT");
  const [walkLevel, setWalkLevel] = useState(0);
  const [selectedRoom, setSelectedRoom] = useState<string | null>(null);
  const [webglFailed, setWebglFailed] = useState(false);
  const [spin, setSpin] = useState(false);
  const pendingRoom = useRef<{ room: PlanRoom; level: number } | null>(null);
  const { showToast } = useToast();
  // Furniture being edited per floor, and which floors have changes that are not saved yet.
  const [furniture, setFurniture] = useState<Record<number, PlanFurniture[]>>({});
  const [dirty, setDirty] = useState<number[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [editLevel, setEditLevel] = useState(0);
  const [showMeasures, setShowMeasures] = useState(true);
  const [saving, setSaving] = useState(false);
  const [autoSuggested, setAutoSuggested] = useState(false);
  const furnitureRef = useRef<Record<number, PlanFurniture[]>>({});
  const actionsRef = useRef<EditActions>({ select: () => undefined, move: () => undefined, key: () => false });

  useEffect(() => {
    let cancelled = false;
    estimationService
      .getPlans(requirementId)
      .then((p) => {
        if (cancelled) return;
        setPlans(p);
        const drawnFloors = p.floors.filter((f) => f.rooms.length > 0);
        const none = drawnFloors.every((f) => (f.furniture ?? []).length === 0);
        const next: Record<number, PlanFurniture[]> = {};
        for (const fl of drawnFloors) {
          next[fl.floorLevel] = none
            ? [...suggestFurniture(fl.rooms, fl.openings), ...(fl === drawnFloors[0] ? suggestGarden(p.plotWidthFt, p.plotLengthFt, fl.rooms) : [])]
            : fl.furniture ?? [];
        }
        setFurniture(next);
        if (none && drawnFloors.length > 0) {
          // A starting arrangement so the rooms are not empty; it counts as unsaved until the user saves it.
          setDirty(drawnFloors.map((fl) => fl.floorLevel));
          setAutoSuggested(true);
        }
        if (drawnFloors.length > 0) setEditLevel(drawnFloors[0].floorLevel);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, [requirementId]);

  // Colours live with the house on the server; the browser copy is only a fallback until that has loaded.
  const styleLoaded = useRef(false);
  useEffect(() => {
    let cancelled = false;
    estimationService
      .getHouseStyle(requirementId)
      .then((saved) => {
        if (cancelled) return;
        if (Object.keys(saved).length > 0) setColours({ ...DEFAULT_COLOURS, ...(saved as Partial<HouseColours>) });
      })
      .catch(() => undefined)
      .finally(() => {
        if (!cancelled) styleLoaded.current = true;
      });
    return () => {
      cancelled = true;
    };
  }, [requirementId]);

  useEffect(() => {
    try {
      localStorage.setItem(`bf-3d-${id}`, JSON.stringify(colours));
    } catch {
      // not persisted; the choice still applies for this visit
    }
    if (!styleLoaded.current) return;
    const timer = window.setTimeout(() => {
      estimationService.saveHouseStyle(requirementId, colours).catch(() => undefined);
    }, 800);
    return () => window.clearTimeout(timer);
  }, [colours, id, requirementId]);

  useEffect(() => {
    plansRef.current = plans;
  }, [plans]);

  useEffect(() => {
    furnitureRef.current = furniture;
  }, [furniture]);

  const drawn = plans?.floors.filter((f) => f.rooms.length > 0) ?? [];
  const hasPlan = drawn.length > 0;
  const livePlans = useMemo(
    () => (plans ? { ...plans, floors: plans.floors.map((f) => ({ ...f, furniture: furniture[f.floorLevel] ?? [] })) } : null),
    [plans, furniture],
  );

  // Scene, camera, controls and the render loop, created once the plan is known to exist.
  useEffect(() => {
    const mount = mountRef.current;
    if (!mount || !hasPlan) return;
    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true });
    } catch {
      setWebglFailed(true);
      return;
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    mount.appendChild(renderer.domElement);
    const scene = new THREE.Scene();
    scene.background = new THREE.Color("#bfe0f5");
    scene.add(new THREE.HemisphereLight("#ffffff", "#b9b29c", 1.0));
    const sun = new THREE.DirectionalLight("#fff4dc", 1.5);
    const plotW = plansRef.current?.plotWidthFt ?? 50;
    const plotL = plansRef.current?.plotLengthFt ?? 70;
    sun.position.set(plotW / 2 + 50, 110, plotL / 2 - 45);
    sun.target.position.set(plotW / 2, 0, plotL / 2);
    sun.castShadow = true;
    sun.shadow.mapSize.set(2048, 2048);
    const reach = Math.max(plotW, plotL) * 0.9;
    Object.assign(sun.shadow.camera, { left: -reach, right: reach, top: reach, bottom: -reach, near: 10, far: 300 });
    sun.shadow.bias = -0.0005;
    scene.add(sun, sun.target);
    const camera = new THREE.PerspectiveCamera(55, 1, 0.3, 1000);
    // Indoors the sun is mostly blocked by walls and slabs, so the walker carries a soft light.
    const lamp = new THREE.PointLight("#fff1d6", 0, 0, 0);
    camera.add(lamp);
    scene.add(camera);
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.maxPolarAngle = Math.PI / 2 - 0.02;

    const person = makePerson();
    scene.add(person);
    const engine: Engine = {
      renderer, scene, camera, controls,
      houseGroup: null, gardenGroup: null, built: null,
      mode: "ORBIT", walkLevel: 0, yaw: Math.PI, pitch: 0,
      keys: new Set(), orbitPos: new THREE.Vector3(), orbitTarget: new THREE.Vector3(), lamp, person, spin: false, measureGroup: null,
    };
    engineRef.current = engine;
    // Handle for browser tests in development builds only.
    if (import.meta.env.DEV) (window as unknown as { __bf3d?: Engine }).__bf3d = engine;

    const resize = () => {
      const w = mount.clientWidth;
      const h = mount.clientHeight;
      renderer.setSize(w, h);
      camera.aspect = w / Math.max(h, 1);
      camera.updateProjectionMatrix();
    };
    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(mount);

    let dragging = false;
    let lastX = 0;
    let lastY = 0;
    const el = renderer.domElement;
    const lockMove = (e: MouseEvent) => {
      if (engine.mode !== "WALK" || document.pointerLockElement !== el) return;
      engine.yaw -= e.movementX * 0.0025;
      engine.pitch = Math.max(-1.3, Math.min(1.3, engine.pitch - e.movementY * 0.0025));
    };
    document.addEventListener("mousemove", lockMove);
    const raycaster = new THREE.Raycaster();
    const ndc = new THREE.Vector2();
    const aim = (e: PointerEvent) => {
      const r = el.getBoundingClientRect();
      ndc.set(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
      raycaster.setFromCamera(ndc, camera);
    };
    interface Drag { level: number; id: string; root: THREE.Object3D; offX: number; offZ: number; y: number; moved: boolean }
    let drag: Drag | null = null;
    let pressed: { x: number; y: number } | null = null;
    const hitPoint = new THREE.Vector3();
    const snap = (v: number, max: number) => Math.min(Math.max(Math.round(v * 2) / 2, 0), max);

    const down = (e: PointerEvent) => {
      if (engine.mode === "ORBIT") {
        // Pressing on a piece selects it and starts moving it; pressing elsewhere is an ordinary orbit.
        aim(e);
        const roots = [...(engine.built?.furniture.values() ?? [])];
        const hit = raycaster.intersectObjects(roots, true)[0];
        let o: THREE.Object3D | null = hit ? hit.object : null;
        while (o && o.userData.furnitureId === undefined) o = o.parent;
        if (o) {
          const level = o.userData.level as number;
          const id = o.userData.furnitureId as string;
          const y = level * STOREY_FT + 0.15;
          const plane = new THREE.Plane(new THREE.Vector3(0, 1, 0), -y);
          if (raycaster.ray.intersectPlane(plane, hitPoint)) {
            drag = { level, id, root: o, offX: o.position.x - hitPoint.x, offZ: o.position.z - hitPoint.z, y, moved: false };
            engine.controls.enabled = false;
            actionsRef.current.select(`${level}:${id}`);
            el.setPointerCapture(e.pointerId);
          }
        } else {
          pressed = { x: e.clientX, y: e.clientY };
        }
        return;
      }
      // A click captures the mouse for free 360 degree looking (Esc releases it); dragging still works without it.
      if (e.pointerType === "mouse" && document.pointerLockElement !== el) {
        try {
          void el.requestPointerLock();
        } catch {
          // pointer lock refused: drag-to-look still works
        }
      }
      dragging = true;
      lastX = e.clientX;
      lastY = e.clientY;
      el.setPointerCapture(e.pointerId);
    };
    const move = (e: PointerEvent) => {
      if (drag) {
        aim(e);
        const plane = new THREE.Plane(new THREE.Vector3(0, 1, 0), -drag.y);
        if (raycaster.ray.intersectPlane(plane, hitPoint)) {
          const plot = plansRef.current;
          drag.root.position.x = snap(hitPoint.x + drag.offX, plot?.plotWidthFt ?? 1000);
          drag.root.position.z = snap(hitPoint.z + drag.offZ, plot?.plotLengthFt ?? 1000);
          drag.moved = true;
        }
        return;
      }
      if (!dragging || engine.mode !== "WALK") return;
      engine.yaw -= (e.clientX - lastX) * 0.005;
      engine.pitch = Math.max(-1.2, Math.min(1.2, engine.pitch - (e.clientY - lastY) * 0.005));
      lastX = e.clientX;
      lastY = e.clientY;
    };
    const up = (e: PointerEvent) => {
      dragging = false;
      if (drag) {
        if (drag.moved) actionsRef.current.move(drag.level, drag.id, drag.root.position.x, drag.root.position.z);
        drag = null;
        engine.controls.enabled = engine.mode === "ORBIT";
      } else if (pressed && Math.hypot(e.clientX - pressed.x, e.clientY - pressed.y) < 5) {
        actionsRef.current.select(null);
      }
      pressed = null;
    };
    el.addEventListener("pointerdown", down);
    el.addEventListener("pointermove", move);
    el.addEventListener("pointerup", up);


    const typing = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement | null)?.tagName;
      return tag === "INPUT" || tag === "SELECT" || tag === "TEXTAREA";
    };
    const keyDown = (e: KeyboardEvent) => {
      if (typing(e)) return;
      if (engine.mode === "ORBIT" && actionsRef.current.key(e.key)) {
        e.preventDefault();
        return;
      }
      engine.keys.add(e.key.toLowerCase());
      if (engine.mode === "WALK" && e.key.startsWith("Arrow")) e.preventDefault();
    };
    const keyUp = (e: KeyboardEvent) => engine.keys.delete(e.key.toLowerCase());
    window.addEventListener("keydown", keyDown);
    window.addEventListener("keyup", keyUp);

    const blocked = (x: number, z: number) => {
      const built = engine.built;
      if (!built) return false;
      return built.colliders.some((c) =>
        c.level === engine.walkLevel
        && x > c.minX - WALK_RADIUS && x < c.maxX + WALK_RADIUS
        && z > c.minZ - WALK_RADIUS && z < c.maxZ + WALK_RADIUS);
    };

    const clock = new THREE.Clock();
    let frame = 0;
    const tick = () => {
      frame = requestAnimationFrame(tick);
      const dt = Math.min(clock.getDelta(), 0.1);
      if (engine.mode === "WALK") {
        const k = engine.keys;
        const fwd = (k.has("w") || k.has("arrowup") ? 1 : 0) - (k.has("s") || k.has("arrowdown") ? 1 : 0);
        const strafe = (k.has("d") ? 1 : 0) - (k.has("a") ? 1 : 0);
        if (k.has("arrowleft") || k.has("q")) engine.yaw += 1.8 * dt;
        if (k.has("arrowright") || k.has("e")) engine.yaw -= 1.8 * dt;
        if (engine.spin) engine.yaw -= 0.45 * dt;
        if (fwd !== 0 || strafe !== 0) {
          const speed = WALK_SPEED_FT_S * dt * (k.has("shift") ? 1.8 : 1);
          const dx = (-Math.sin(engine.yaw) * fwd + Math.cos(engine.yaw) * strafe) * speed;
          const dz = (-Math.cos(engine.yaw) * fwd - Math.sin(engine.yaw) * strafe) * speed;
          const p = camera.position;
          const maxX = (plansRef.current?.plotWidthFt ?? 1000) - 1;
          const maxZ = (plansRef.current?.plotLengthFt ?? 1000) - 1;
          const nx = Math.min(Math.max(p.x + dx, 1), maxX);
          const nz = Math.min(Math.max(p.z + dz, 1), maxZ);
          if (!blocked(nx, p.z)) p.x = nx;
          if (!blocked(p.x, nz)) p.z = nz;
        }
        // On a flight of stairs the eye rises or falls with the walker's progress along it.
        const p2 = camera.position;
        const stair = engine.built?.stairs.find((s) => p2.x >= s.minX && p2.x <= s.maxX && p2.z >= s.minZ && p2.z <= s.maxZ
          && (engine.walkLevel === s.level || engine.walkLevel === s.level + 1));
        if (stair) {
          const along = stair.axis === "x" ? (p2.x - stair.minX) / (stair.maxX - stair.minX) : (p2.z - stair.minZ) / (stair.maxZ - stair.minZ);
          const progress = Math.min(Math.max(stair.dir > 0 ? along : 1 - along, 0), 1);
          camera.position.y = stair.level * STOREY_FT + EYE_FT + progress * STOREY_FT;
          engine.walkLevel = progress > 0.5 ? stair.level + 1 : stair.level;
        } else {
          camera.position.y = engine.walkLevel * STOREY_FT + EYE_FT;
        }
        camera.rotation.set(engine.pitch, engine.yaw, 0, "YXZ");
      } else {
        controls.update();
      }
      renderer.render(scene, camera);
    };
    tick();

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      el.removeEventListener("pointerdown", down);
      el.removeEventListener("pointermove", move);
      el.removeEventListener("pointerup", up);
      document.removeEventListener("mousemove", lockMove);
      if (document.pointerLockElement === el) document.exitPointerLock();
      window.removeEventListener("keydown", keyDown);
      window.removeEventListener("keyup", keyUp);
      controls.dispose();
      if (engine.houseGroup) disposeGroup(engine.houseGroup);
      if (engine.gardenGroup) disposeGroup(engine.gardenGroup);
      renderer.dispose();
      mount.removeChild(renderer.domElement);
      engineRef.current = null;
    };
  }, [hasPlan]);

  // Rebuild the house whenever the plan or a colour changes (it is small, so this is cheap).
  useEffect(() => {
    const engine = engineRef.current;
    if (!engine || !livePlans || !hasPlan) return;
    if (engine.houseGroup) {
      engine.scene.remove(engine.houseGroup);
      disposeGroup(engine.houseGroup);
    }
    const built = buildHouse(livePlans, colours, hideRoof, editLevel, selected);
    engine.houseGroup = built.group;
    engine.built = built;
    engine.scene.add(built.group);
    // The garden follows the placed trees, so it is rebuilt with the house.
    const firstBuild = !engine.gardenGroup;
    if (engine.gardenGroup) {
      engine.scene.remove(engine.gardenGroup);
      disposeGroup(engine.gardenGroup);
    }
    engine.gardenGroup = buildGarden(livePlans, built);
    engine.scene.add(engine.gardenGroup);
    if (firstBuild) {
      // First view: from the road, looking at the front of the house.
      const d = built.size * 1.05 + 14;
      engine.camera.position.set(built.centre.x + d * 0.35, d * 0.55, built.centre.z - d);
      engine.controls.target.set(built.centre.x, 4, built.centre.z);
      engine.controls.update();
      const first = drawn[0]?.rooms.find((r) => r.type === "HALL") ?? drawn[0]?.rooms[0];
      if (first) engine.person.position.set(first.x + first.widthFt / 2, drawn[0].floorLevel * STOREY_FT + 0.15, first.y + first.depthFt / 2);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [livePlans, colours, hideRoof, editLevel, selected, hasPlan, webglFailed]);

  useEffect(() => {
    if (engineRef.current) engineRef.current.spin = spin;
  }, [spin]);

  // With the roof off, dimension lines and labels for the floor being studied.
  useEffect(() => {
    const engine = engineRef.current;
    if (!engine || !livePlans) return;
    if (engine.measureGroup) {
      engine.scene.remove(engine.measureGroup);
      disposeGroup(engine.measureGroup);
      engine.measureGroup = null;
    }
    const floor = livePlans.floors.find((f) => f.floorLevel === editLevel && f.rooms.length > 0);
    if (hideRoof && showMeasures && mode === "ORBIT" && floor) {
      engine.measureGroup = buildMeasurements(floor, livePlans.plotWidthFt, livePlans.plotLengthFt);
      engine.scene.add(engine.measureGroup);
    }
  }, [livePlans, hideRoof, showMeasures, editLevel, mode, hasPlan, webglFailed]);

  // Furniture edits.
  const markDirty = (level: number) => setDirty((d) => (d.includes(level) ? d : [...d, level]));
  const mutate = (level: number, fn: (items: PlanFurniture[]) => PlanFurniture[]) => {
    setFurniture((prev) => ({ ...prev, [level]: fn(prev[level] ?? []) }));
    markDirty(level);
  };
  const [selLevel, selId] = selected ? [Number(selected.split(":")[0]), selected.split(":")[1]] : [editLevel, ""];
  const selItem = selected ? (furniture[selLevel] ?? []).find((i) => i.id === selId) ?? null : null;
  const patchSelected = (patch: Partial<PlanFurniture>) => {
    if (selItem) mutate(selLevel, (items) => items.map((i) => (i.id === selItem.id ? { ...i, ...patch } : i)));
  };
  const addItem = (kind: FurnitureKind) => {
    const floor = drawn.find((f) => f.floorLevel === editLevel);
    if (!floor) return;
    const room = floor.rooms.find((r) => r.id === selectedRoom) ?? floor.rooms[0];
    const count = (furniture[editLevel] ?? []).length;
    // Garden pieces start in the open plot (front-left corner), the rest in the chosen room.
    const item = furnitureSpec(kind).outdoor
      ? newFurniture(kind, 4 + (count % 6) * 3, 4, furniture[editLevel] ?? [])
      : newFurniture(kind, room.x + room.widthFt / 2 + (count % 4) * 0.5, room.y + room.depthFt / 2 + (count % 4) * 0.5, furniture[editLevel] ?? []);
    mutate(editLevel, (items) => [...items, item]);
    setSelected(`${editLevel}:${item.id}`);
  };
  const removeSelected = () => {
    if (!selItem) return;
    mutate(selLevel, (items) => items.filter((i) => i.id !== selItem.id));
    setSelected(null);
  };
  const rotateSelected = () => patchSelected({ rotationDeg: selItem ? (selItem.rotationDeg + 90) % 360 : 0 });
  const duplicateSelected = () => {
    if (!selItem) return;
    const copy = newFurniture(selItem.kind, selItem.x + 1, selItem.y + 1, furniture[selLevel] ?? []);
    mutate(selLevel, (items) => [...items, { ...copy, widthFt: selItem.widthFt, depthFt: selItem.depthFt, heightFt: selItem.heightFt, rotationDeg: selItem.rotationDeg, colour: selItem.colour }]);
    setSelected(`${selLevel}:${copy.id}`);
  };
  const autoFurnish = () => {
    const floor = drawn.find((f) => f.floorLevel === editLevel);
    if (!floor) return;
    // Rooms only: any garden pieces already on the floor stay.
    mutate(editLevel, (items) => [...items.filter((i) => furnitureSpec(i.kind).outdoor), ...suggestFurniture(floor.rooms, floor.openings)]);
    setSelected(null);
    showToast("success", "Furniture arranged. Move, resize or remove anything, then Save.");
  };
  const plantGarden = () => {
    const floor = drawn.find((f) => f.floorLevel === editLevel);
    if (!floor || !plans) return;
    mutate(editLevel, (items) => [...items.filter((i) => !furnitureSpec(i.kind).outdoor), ...suggestGarden(plans.plotWidthFt, plans.plotLengthFt, floor.rooms)]);
    setSelected(null);
    showToast("success", "Garden planted around the house. Move or remove anything, then Save.");
  };
  const clearFurniture = () => {
    mutate(editLevel, () => []);
    setSelected(null);
  };
  async function saveFurniture() {
    if (!plans) return;
    setSaving(true);
    try {
      for (const level of dirty) {
        const floor = plans.floors.find((f) => f.floorLevel === level);
        if (!floor) continue;
        const sent = furniture[level] ?? [];
        const saved = await estimationService.savePlan(requirementId, level, {
          rooms: floor.rooms, openings: floor.openings, columns: floor.columns, furniture: sent,
        });
        setPlans((p) => (p ? { ...p, floors: p.floors.map((f) => (f.floorLevel === level ? { ...saved, furniture: f.furniture } : f)) } : p));
        // Only floors untouched since the request went out are clean; edits made meanwhile stay unsaved.
        if (furnitureRef.current[level] === undefined || (furnitureRef.current[level] ?? []) === sent) {
          setDirty((d) => d.filter((l) => l !== level));
        }
      }
      setAutoSuggested(false);
      showToast("success", "Furniture saved with the plan.");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  // Latest edit handlers for the pointer and keyboard code inside the render engine.
  useEffect(() => {
    actionsRef.current = {
      select: (key) => setSelected(key),
      move: (level, id, x, y) => mutate(level, (items) => items.map((i) => (i.id === id ? { ...i, x, y } : i))),
      key: (key) => {
        if (!selItem) return false;
        const step = 0.5;
        if (key === "Delete" || key === "Backspace") removeSelected();
        else if (key === "r" || key === "R") rotateSelected();
        else if (key === "ArrowLeft") patchSelected({ x: Math.max(0, selItem.x - step) });
        else if (key === "ArrowRight") patchSelected({ x: selItem.x + step });
        else if (key === "ArrowUp") patchSelected({ y: Math.max(0, selItem.y - step) });
        else if (key === "ArrowDown") patchSelected({ y: selItem.y + step });
        else return false;
        return true;
      },
    };
  });

  // Switch between orbiting and walking.
  useEffect(() => {
    const engine = engineRef.current;
    if (!engine || !engine.built) return;
    if (mode === "WALK" && engine.mode !== "WALK") {
      engine.orbitPos.copy(engine.camera.position);
      engine.orbitTarget.copy(engine.controls.target);
      const pending = pendingRoom.current;
      pendingRoom.current = null;
      const level = pending ? pending.level : walkLevel;
      const floor = plans?.floors.find((f) => f.floorLevel === level && f.rooms.length > 0);
      const start = pending?.room ?? floor?.rooms.find((r) => r.type === "HALL") ?? floor?.rooms[0];
      engine.walkLevel = level;
      engine.camera.position.set(
        start ? start.x + start.widthFt / 2 : engine.built.centre.x, level * STOREY_FT + EYE_FT,
        start ? start.y + start.depthFt / 2 : engine.built.centre.z);
      engine.person.visible = false;
      engine.yaw = 0;
      engine.pitch = 0;
      engine.controls.enabled = false;
      engine.lamp.intensity = 1.1;
      engine.camera.fov = 75;
      engine.camera.updateProjectionMatrix();
    } else if (mode === "ORBIT" && engine.mode === "WALK") {
      // Leave the figure standing where the walk ended.
      engine.person.position.set(engine.camera.position.x, engine.walkLevel * STOREY_FT + 0.15, engine.camera.position.z);
      engine.person.visible = true;
      if (document.pointerLockElement === engine.renderer.domElement) document.exitPointerLock();
      engine.camera.position.copy(engine.orbitPos);
      engine.controls.target.copy(engine.orbitTarget);
      engine.controls.enabled = true;
      engine.lamp.intensity = 0;
      engine.camera.fov = 55;
      engine.camera.updateProjectionMatrix();
      engine.camera.rotation.set(0, 0, 0);
      engine.controls.update();
    }
    engine.mode = mode;
  }, [mode, walkLevel, plans]);

  function setView(kind: "front" | "top") {
    const engine = engineRef.current;
    if (!engine?.built) return;
    setMode("ORBIT");
    const { centre, size } = engine.built;
    if (kind === "top") {
      engine.camera.position.set(centre.x, size * 1.5 + 14, centre.z + 0.01);
    } else {
      engine.camera.position.set(centre.x, size * 0.45 + 8, centre.z - (size * 1.0 + 16));
    }
    engine.controls.target.set(centre.x, 4, centre.z);
    engine.controls.update();
  }

  // Jump the walker into a room, entering walk mode first if needed.
  function goToRoom(room: PlanRoom, level: number) {
    const engine = engineRef.current;
    if (!engine) return;
    if (engine.mode === "WALK") {
      engine.walkLevel = level;
      setWalkLevel(level);
      engine.camera.position.set(room.x + room.widthFt / 2, level * STOREY_FT + EYE_FT, room.y + room.depthFt / 2);
      engine.yaw = 0;
      engine.pitch = 0;
    } else {
      pendingRoom.current = { room, level };
      setWalkLevel(level);
      setMode("WALK");
    }
  }

  const setRoomColour =(roomId: string, value: string) =>
    setColours((c) => ({ ...c, rooms: { ...c.rooms, [roomId]: value } }));
  const paintAllRooms = (value: string) =>
    setColours((c) => ({
      ...c,
      rooms: Object.fromEntries(drawn.flatMap((f) => f.rooms).map((r) => [r.id, value])),
    }));

  if (error) return <div className="bf-view3d__error">{error}</div>;
  if (!plans) return <Skeleton height={520} />;

  const allRooms = drawn.flatMap((f) => f.rooms.map((r) => ({ ...r, level: f.floorLevel })));
  const activeRoom = allRooms.find((r) => r.id === selectedRoom) ?? allRooms[0];

  return (
    <div className="bf-view3d">
      <Link to={`/estimation/${requirementId}/plan`} className="bf-view3d__back">← Back to plan</Link>
      <div className="bf-view3d__header">
        <div>
          <h1>3D home view</h1>
          <p>
            Pick the wall and roof colours, then orbit the house or walk inside it. A concept model of your drawn plan,
            not a photo-real render.
          </p>
        </div>
      </div>

      {!hasPlan ? (
        <Card>Draw and save a floor plan first; the 3D house is built from it.</Card>
      ) : webglFailed ? (
        <Card>This browser could not start 3D graphics (WebGL). Try a current Chrome, Edge or Firefox.</Card>
      ) : (
        <div className="bf-view3d__body">
          <div className="bf-view3d__stage">
            <div ref={mountRef} className="bf-view3d__canvas" />
            <div className="bf-view3d__toolbar">
              <button className={mode === "ORBIT" ? "is-active" : ""} onClick={() => setMode("ORBIT")}>Orbit</button>
              <button className={mode === "WALK" ? "is-active" : ""} onClick={() => setMode("WALK")}>Walk inside</button>
              <button onClick={() => setView("front")}>Front view</button>
              <button onClick={() => setView("top")}>Top view</button>
              {mode === "WALK" && (
                <button className={spin ? "is-active" : ""} onClick={() => setSpin((s) => !s)}>360° look around</button>
              )}
              {mode === "WALK" && drawn.length > 1 && (
                <select value={walkLevel} aria-label="Floor to walk on"
                  onChange={(e) => {
                    const f = drawn.find((x) => x.floorLevel === Number(e.target.value));
                    if (f) goToRoom(f.rooms[0], f.floorLevel);
                  }}>
                  {drawn.map((f) => <option key={f.floorLevel} value={f.floorLevel}>{floorLabel(f.floorLevel)}</option>)}
                </select>
              )}
            </div>
            <div className="bf-view3d__goto">
              <span>Go inside:</span>
              {allRooms.map((r) => (
                <button key={r.id} onClick={() => goToRoom(r, r.level)}>{r.name || r.type}</button>
              ))}
            </div>
            <p className="bf-view3d__hint">
              {mode === "WALK"
                ? "Walk: W A S D or arrows to move. Click the picture to look around freely in 360° with the mouse (Esc releases it), or use Q / E. Shift = faster."
                : "Orbit: drag to rotate, scroll to zoom, right-drag to pan. Click a piece of furniture to select it and drag it to move it. Choose Walk inside to go in."}
            </p>
          </div>

          <aside className="bf-view3d__panel">
            <Card>
              <h3>Outside</h3>
              <ColourRow label="Wall colour" value={colours.exterior} palette={WALL_PALETTE}
                onChange={(v) => setColours((c) => ({ ...c, exterior: v }))} />
              <ColourRow label="Roof colour" value={colours.roof} palette={ROOF_PALETTE}
                onChange={(v) => setColours((c) => ({ ...c, roof: v }))} />
              <div className="bf-view3d__row">
                <span>Roof style</span>
                <div className="bf-view3d__seg">
                  {(["SLOPED", "FLAT"] as RoofStyle[]).map((s) => (
                    <button key={s} className={colours.roofStyle === s ? "is-active" : ""}
                      onClick={() => setColours((c) => ({ ...c, roofStyle: s }))}>
                      {s === "SLOPED" ? "Sloped" : "Flat"}
                    </button>
                  ))}
                </div>
              </div>
              <label className="bf-view3d__check">
                <input type="checkbox" checked={hideRoof} onChange={(e) => setHideRoof(e.target.checked)} />
                Hide the roof (see inside from above)
              </label>
              <label className="bf-view3d__check">
                <input type="checkbox" checked={showMeasures} onChange={(e) => setShowMeasures(e.target.checked)} />
                Show measurements when the roof is hidden
              </label>
            </Card>

            <Card>
              <h3>Furniture</h3>
              {drawn.length > 1 && (
                <label className="bf-view3d__field">
                  <span>Floor</span>
                  <select value={editLevel} onChange={(e) => { setEditLevel(Number(e.target.value)); setSelected(null); }}>
                    {drawn.map((f) => <option key={f.floorLevel} value={f.floorLevel}>{floorLabel(f.floorLevel)}</option>)}
                  </select>
                </label>
              )}
              <p className="bf-view3d__note">
                {dirty.length > 0
                  ? autoSuggested ? "A suggested arrangement, not saved yet." : "Unsaved furniture changes."
                  : "Furniture is saved with the plan."}
              </p>
              <div className="bf-view3d__catalog">
                {FURNITURE_CATALOG.map((spec) => (
                  <button key={spec.kind} onClick={() => addItem(spec.kind)}>+ {spec.label}</button>
                ))}
              </div>
              {selItem ? (
                <div className="bf-view3d__edit">
                  <strong>{furnitureLabel(selItem.kind)}</strong>
                  <div className="bf-view3d__dims">
                    <DimField label="Width ft" value={selItem.widthFt} min={0.3} max={25} k={`${selected}w`} onCommit={(v) => patchSelected({ widthFt: v })} />
                    <DimField label="Depth ft" value={selItem.depthFt} min={0.3} max={25} k={`${selected}d`} onCommit={(v) => patchSelected({ depthFt: v })} />
                    <DimField label="Height ft" value={selItem.heightFt} min={0.05} max={9} k={`${selected}h`} onCommit={(v) => patchSelected({ heightFt: v })} />
                  </div>
                  <ColourRow label="Colour" value={selItem.colour ?? "#888888"} palette={FURNITURE_PALETTE}
                    onChange={(v) => patchSelected({ colour: v })} />
                  <div className="bf-view3d__buttons">
                    <Button variant="secondary" onClick={rotateSelected}>Rotate 90°</Button>
                    <Button variant="secondary" onClick={duplicateSelected}>Duplicate</Button>
                    <Button variant="secondary" onClick={removeSelected}>Delete</Button>
                  </div>
                </div>
              ) : (
                <p className="bf-view3d__note">Click a piece in the 3D view to select it. Drag it to move; R rotates, Delete removes, arrow keys nudge.</p>
              )}
              <div className="bf-view3d__buttons">
                <Button variant="secondary" onClick={autoFurnish}>Auto-furnish this floor</Button>
                {editLevel === (drawn[0]?.floorLevel ?? 0) && <Button variant="secondary" onClick={plantGarden}>Plant garden</Button>}
                <Button variant="secondary" onClick={clearFurniture}>Clear floor</Button>
                <Button onClick={saveFurniture} isLoading={saving} disabled={dirty.length === 0}>Save furniture</Button>
              </div>
            </Card>

            <Card>
              <h3>Room paint (inside walls)</h3>
              <div className="bf-view3d__rooms">
                {allRooms.map((r) => (
                  <button key={r.id}
                    className={`bf-view3d__room ${activeRoom?.id === r.id ? "is-active" : ""}`}
                    onClick={() => setSelectedRoom(r.id)}>
                    <span className="bf-view3d__swatch" style={{ background: roomColour(r, colours) }} />
                    {r.name || r.type}
                  </button>
                ))}
              </div>
              {activeRoom && (
                <ColourRow label={activeRoom.name || activeRoom.type} value={roomColour(activeRoom, colours)}
                  palette={WALL_PALETTE} onChange={(v) => setRoomColour(activeRoom.id, v)} />
              )}
              <Button variant="secondary" onClick={() => paintAllRooms(roomColour(activeRoom ?? allRooms[0], colours))}>
                Paint all rooms this colour
              </Button>
              <Button variant="secondary" onClick={() => setColours(DEFAULT_COLOURS)}>Reset colours</Button>
              <p className="bf-view3d__note">Colour choices are saved with this house.</p>
            </Card>
          </aside>
        </div>
      )}
    </div>
  );
}

function ColourRow({ label, value, palette, onChange }: {
  label: string; value: string; palette: string[]; onChange: (v: string) => void;
}) {
  return (
    <div className="bf-view3d__colour">
      <span>{label}</span>
      <div className="bf-view3d__palette">
        {palette.map((c) => (
          <button key={c} aria-label={`${label} ${c}`} className={value.toLowerCase() === c ? "is-active" : ""}
            style={{ background: c }} onClick={() => onChange(c)} />
        ))}
        <input type="color" value={value} onChange={(e) => onChange(e.target.value)} aria-label={`${label} custom`} />
      </div>
    </div>
  );
}

const FURNITURE_PALETTE = ["#7a5230", "#5b3a1e", "#8a6a4b", "#4f6d8a", "#3e4a52", "#2b2b2b", "#d8dde0", "#a3584b", "#3d8b3d", "#ffffff"];

// A number box that applies its value when focus leaves it or Enter is pressed, so partly typed numbers are not rejected.
function DimField({ label, value, min, max, k, onCommit }: {
  label: string; value: number; min: number; max: number; k: string; onCommit: (v: number) => void;
}) {
  const apply = (raw: string) => {
    const v = Number(raw);
    if (raw.trim() !== "" && Number.isFinite(v)) onCommit(Math.min(Math.max(v, min), max));
  };
  return (
    <label className="bf-view3d__field">
      <span>{label}</span>
      <input key={`${k}${value}`} type="number" step="0.1" min={min} max={max} defaultValue={value}
        onBlur={(e) => apply(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") apply(e.currentTarget.value); }} />
    </label>
  );
}
