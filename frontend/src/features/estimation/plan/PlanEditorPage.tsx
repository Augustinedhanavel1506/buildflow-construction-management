import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { ConfirmDialog } from "../../../components/ui/ConfirmDialog";
import { Skeleton } from "../../../components/ui/Skeleton";
import { useToast } from "../../../components/ui/ToastProvider";
import { estimationService } from "../../../services/estimationService";
import { getErrorMessage } from "../../../utils/apiError";
import type { HouseRequirement } from "../../../types/estimation";
import type { FurnitureKind, Layout, PlanColumn, PlanFurniture, PlanMetrics, PlanOpening, PlanRoom, PlanSide, RoomType, StructureMetrics } from "../../../types/plan";
import { FURNITURE_CATALOG, furnitureLabel, newFurniture, nextFurnitureId, suggestFurniture } from "../furniture";
import { floorLabel } from "../labels";
import { PlanCanvas } from "./PlanCanvas";
import {
  COLUMN_DEPTH_FT,
  COLUMN_WIDTH_FT,
  ROOM_META,
  ROOM_TYPES,
  DEFAULT_SETBACKS,
  computeMetrics,
  deriveBeams,
  liveSiteCheck,
  findFreeSpot,
  findProblems,
  formatFt,
  newId,
  roomLabel,
  sideLength,
  snap,
} from "./planUtils";
import "./PlanEditorPage.css";

const EMPTY: Layout = { rooms: [], openings: [], columns: [], furniture: [] };
const clampNum = (v: number, min: number, max: number) => Math.min(Math.max(v, min), max);
const SIDES: PlanSide[] = ["NORTH", "EAST", "SOUTH", "WEST"];
const HISTORY_LIMIT = 50;

function key(layout: Layout): string {
  return JSON.stringify(layout);
}

export function PlanEditorPage() {
  const { id } = useParams<{ id: string }>();
  const requirementId = Number(id);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [requirement, setRequirement] = useState<HouseRequirement | null>(null);
  const [layouts, setLayouts] = useState<Record<number, Layout>>({});
  const [baseline, setBaseline] = useState<Record<number, string>>({});
  // Quantities the server derived from the last saved drawing of each floor.
  const [savedMetrics, setSavedMetrics] = useState<Record<number, PlanMetrics>>({});
  const [savedStructure, setSavedStructure] = useState<Record<number, StructureMetrics>>({});
  const [selectedColumn, setSelectedColumn] = useState<string | null>(null);
  const [selectedFurniture, setSelectedFurniture] = useState<string | null>(null);
  // Site rules as typed: strings so a field can be empty ("use the default / not set").
  const [site, setSite] = useState({ front: "", rear: "", left: "", right: "", coverage: "", far: "" });
  const [savedSite, setSavedSite] = useState("");
  const [history, setHistory] = useState<Record<number, Layout[]>>({});
  const [active, setActive] = useState(0);
  const [selectedRoom, setSelectedRoom] = useState<string | null>(null);
  const [selectedOpening, setSelectedOpening] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);
  const [confirmSuggest, setConfirmSuggest] = useState(false);
  const preDragRef = useRef<Layout | null>(null);

  const load = useCallback(async () => {
    try {
      const [req, plans, siteCheck] = await Promise.all([
        estimationService.get(requirementId),
        estimationService.getPlans(requirementId),
        estimationService.getSiteCheck(requirementId),
      ]);
      setRequirement(req);
      const r = siteCheck.rules;
      const loaded = {
        front: r.setbackFrontFt?.toString() ?? "",
        rear: r.setbackRearFt?.toString() ?? "",
        left: r.setbackLeftFt?.toString() ?? "",
        right: r.setbackRightFt?.toString() ?? "",
        coverage: r.maxCoveragePercent?.toString() ?? "",
        far: r.maxFar?.toString() ?? "",
      };
      setSite(loaded);
      setSavedSite(JSON.stringify(loaded));
      const nextLayouts: Record<number, Layout> = {};
      const nextBaseline: Record<number, string> = {};
      const nextMetrics: Record<number, PlanMetrics> = {};
      const nextStructure: Record<number, StructureMetrics> = {};
      for (const floor of plans.floors) {
        if (floor.saved) {
          nextMetrics[floor.floorLevel] = floor.metrics;
          nextStructure[floor.floorLevel] = floor.structure;
        }
        const layout: Layout = { rooms: floor.rooms, openings: floor.openings, columns: floor.columns, furniture: floor.furniture ?? [] };
        nextLayouts[floor.floorLevel] = layout;
        nextBaseline[floor.floorLevel] = key(layout);
      }
      setLayouts(nextLayouts);
      setBaseline(nextBaseline);
      setSavedMetrics(nextMetrics);
      setSavedStructure(nextStructure);
      setActive(plans.floors[0]?.floorLevel ?? 0);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [requirementId]);

  useEffect(() => {
    load();
  }, [load]);

  const layout = layouts[active] ?? EMPTY;
  const plotWidth = requirement?.plotWidthFt ?? 0;
  const plotDepth = requirement?.plotLengthFt ?? 0;
  const problems = useMemo(() => findProblems(layout, plotWidth, plotDepth), [layout, plotWidth, plotDepth]);
  const metrics = useMemo(() => computeMetrics(layout), [layout]);
  const num = (v: string) => (v.trim() === "" || Number.isNaN(Number(v)) ? null : Number(v));
  const siteInput = useMemo(
    () => ({
      plotWidth,
      plotDepth,
      front: num(site.front),
      rear: num(site.rear),
      left: num(site.left),
      right: num(site.right),
      maxCoverage: num(site.coverage),
      maxFar: num(site.far),
    }),
    [plotWidth, plotDepth, site],
  );
  const siteCheck = useMemo(
    () => liveSiteCheck(siteInput, layouts, requirement?.floors ?? []),
    [siteInput, layouts, requirement],
  );
  const siteDirty = JSON.stringify(site) !== savedSite;
  const beams = useMemo(() => deriveBeams(layout.columns, layout.rooms), [layout.columns, layout.rooms]);
  const isDirty = (level: number) => key(layouts[level] ?? EMPTY) !== (baseline[level] ?? key(EMPTY));
  const anyDirty = requirement?.floors.some((f) => isDirty(f.floorLevel)) ?? false;

  useEffect(() => {
    if (!anyDirty) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  });

  function commit(level: number, next: Layout, previous: Layout) {
    setLayouts((current) => ({ ...current, [level]: next }));
    setHistory((current) => ({ ...current, [level]: [...(current[level] ?? []), previous].slice(-HISTORY_LIMIT) }));
  }

  function change(next: Layout) {
    commit(active, next, layout);
  }

  const undo = useCallback(() => {
    const stack = history[active] ?? [];
    if (stack.length === 0) return;
    const previous = stack[stack.length - 1];
    setLayouts((current) => ({ ...current, [active]: previous }));
    setHistory((current) => ({ ...current, [active]: stack.slice(0, -1) }));
    setSelectedRoom(null);
    setSelectedOpening(null);
    setSelectedColumn(null);
    setSelectedFurniture(null);
  }, [history, active]);

  function handleFurnitureChange(furniture: PlanFurniture[], isFinal: boolean) {
    if (!isFinal) {
      if (preDragRef.current === null) preDragRef.current = layout;
      setLayouts((current) => ({ ...current, [active]: { ...layout, furniture } }));
      return;
    }
    const before = preDragRef.current ?? layout;
    preDragRef.current = null;
    commit(active, { ...layout, furniture }, before);
  }

  function selectFurniture(furnitureId: string | null) {
    setSelectedFurniture(furnitureId);
    if (furnitureId) {
      setSelectedRoom(null);
      setSelectedOpening(null);
      setSelectedColumn(null);
    }
  }

  function addFurniture(kind: FurnitureKind) {
    const anchor = layout.rooms.find((r) => r.id === selectedRoom) ?? layout.rooms[0];
    const cx = anchor ? anchor.x + anchor.widthFt / 2 : plotWidth / 2;
    const cy = anchor ? anchor.y + anchor.depthFt / 2 : plotDepth / 2;
    const item = newFurniture(kind, snap(cx), snap(cy), layout.furniture);
    change({ ...layout, furniture: [...layout.furniture, item] });
    selectFurniture(item.id);
  }

  function updateFurniture(furnitureId: string, patch: Partial<PlanFurniture>) {
    change({ ...layout, furniture: layout.furniture.map((f) => (f.id === furnitureId ? { ...f, ...patch } : f)) });
  }

  function duplicateFurniture(item: PlanFurniture) {
    const copy: PlanFurniture = { ...item, id: nextFurnitureId(layout.furniture), x: item.x + 1, y: item.y + 1 };
    change({ ...layout, furniture: [...layout.furniture, copy] });
    selectFurniture(copy.id);
  }

  function autoFurnish() {
    const furniture = suggestFurniture(layout.rooms, layout.openings);
    change({ ...layout, furniture });
    setSelectedFurniture(null);
    showToast("success", furniture.length > 0 ? `${furniture.length} furniture pieces placed. Drag them to adjust.` : "No rooms here are big enough to furnish.");
  }

  function clearFurniture() {
    if (layout.furniture.length === 0) return;
    change({ ...layout, furniture: [] });
    setSelectedFurniture(null);
  }

  function handleRoomsChange(rooms: PlanRoom[], isFinal: boolean) {
    if (!isFinal) {
      if (preDragRef.current === null) preDragRef.current = layout;
      setLayouts((current) => ({ ...current, [active]: { ...layout, rooms } }));
      return;
    }
    const before = preDragRef.current ?? layout;
    preDragRef.current = null;
    commit(active, { ...layout, rooms }, before);
  }

  function handleColumnsChange(columns: PlanColumn[], isFinal: boolean) {
    if (!isFinal) {
      if (preDragRef.current === null) preDragRef.current = layout;
      setLayouts((current) => ({ ...current, [active]: { ...layout, columns } }));
      return;
    }
    const before = preDragRef.current ?? layout;
    preDragRef.current = null;
    commit(active, { ...layout, columns }, before);
  }

  function addColumn() {
    const anchor = layout.rooms.find((r) => r.id === selectedRoom);
    const column: PlanColumn = {
      id: newId("c"),
      x: anchor ? anchor.x : Math.round(plotWidth / 2),
      y: anchor ? anchor.y : Math.round(plotDepth / 2),
      widthFt: COLUMN_WIDTH_FT,
      depthFt: COLUMN_DEPTH_FT,
    };
    change({ ...layout, columns: [...layout.columns, column] });
    setSelectedColumn(column.id);
    setSelectedFurniture(null);
    setSelectedRoom(null);
    setSelectedOpening(null);
  }

  async function downloadCad() {
    setIsBusy(true);
    try {
      await estimationService.downloadDxf(requirementId);
      showToast("success", "CAD file (DXF) downloaded. Open it in AutoCAD, BricsCAD or LibreCAD.");
    } catch {
      showToast("warning", "Save at least one floor before exporting a CAD file.");
    } finally {
      setIsBusy(false);
    }
  }

  async function suggestColumns(style: "grid" | "junctions" = "grid") {
    if (layout.rooms.length === 0) {
      showToast("warning", "Draw some rooms first; columns are placed at their corners.");
      return;
    }
    setIsBusy(true);
    try {
      const columns = await estimationService.suggestColumns(requirementId, layout, style);
      change({ ...layout, columns });
      setSelectedColumn(null);
      showToast("success", `${columns.length} columns placed (${style === "grid" ? "balanced grid" : "every junction"}). Drag them to adjust.`);
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  function updateColumn(columnId: string, patch: Partial<PlanColumn>) {
    change({ ...layout, columns: layout.columns.map((c) => (c.id === columnId ? { ...c, ...patch } : c)) });
  }

  function addRoom(type: RoomType) {
    const meta = ROOM_META[type];
    const spot = findFreeSpot(layout.rooms, meta.width, meta.depth, plotWidth, plotDepth);
    if (!spot) {
      showToast("warning", "There is no free space left on this floor for another room of that size.");
      return;
    }
    const count = layout.rooms.filter((r) => r.type === type).length + 1;
    const room: PlanRoom = {
      id: newId("r"),
      type,
      name: count > 1 ? `${meta.label} ${count}` : meta.label,
      x: spot.x,
      y: spot.y,
      widthFt: meta.width,
      depthFt: meta.depth,
    };
    change({ ...layout, rooms: [...layout.rooms, room] });
    setSelectedRoom(room.id);
    setSelectedOpening(null);
  }

  const deleteSelected = useCallback(() => {
    if (selectedFurniture) {
      change({ ...layout, furniture: layout.furniture.filter((f) => f.id !== selectedFurniture) });
      setSelectedFurniture(null);
    } else if (selectedColumn) {
      change({ ...layout, columns: layout.columns.filter((c) => c.id !== selectedColumn) });
      setSelectedColumn(null);
    } else if (selectedOpening) {
      change({ ...layout, openings: layout.openings.filter((o) => o.id !== selectedOpening) });
      setSelectedOpening(null);
    } else if (selectedRoom) {
      change({
        rooms: layout.rooms.filter((r) => r.id !== selectedRoom),
        openings: layout.openings.filter((o) => o.roomId !== selectedRoom),
        columns: layout.columns,
        furniture: layout.furniture,
      });
      setSelectedRoom(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedFurniture, selectedColumn, selectedOpening, selectedRoom, layout, active]);

  function updateRoom(roomId: string, patch: Partial<PlanRoom>) {
    change({ ...layout, rooms: layout.rooms.map((r) => (r.id === roomId ? { ...r, ...patch } : r)) });
  }

  function addOpening(kind: "DOOR" | "WINDOW", side: PlanSide) {
    const room = layout.rooms.find((r) => r.id === selectedRoom);
    if (!room) return;
    const width = kind === "DOOR" ? 3 : 3.5;
    const length = sideLength(room, side);
    if (length < width + 1) {
      showToast("warning", `That wall is too short for a ${kind.toLowerCase()}.`);
      return;
    }
    const opening: PlanOpening = {
      id: newId("o"),
      roomId: room.id,
      kind,
      side,
      offsetFt: snap((length - width) / 2),
      widthFt: width,
    };
    change({ ...layout, openings: [...layout.openings, opening] });
    setSelectedOpening(opening.id);
  }

  function updateOpening(openingId: string, patch: Partial<PlanOpening>) {
    change({ ...layout, openings: layout.openings.map((o) => (o.id === openingId ? { ...o, ...patch } : o)) });
  }

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      const target = event.target as HTMLElement;
      if (["INPUT", "SELECT", "TEXTAREA"].includes(target.tagName)) return;
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "z") {
        event.preventDefault();
        undo();
      } else if (event.key === "Delete" || event.key === "Backspace") {
        if (selectedRoom || selectedOpening || selectedColumn || selectedFurniture) {
          event.preventDefault();
          deleteSelected();
        }
      } else if (selectedRoom && !selectedOpening && event.key.startsWith("Arrow")) {
        event.preventDefault();
        const dx = event.key === "ArrowLeft" ? -0.5 : event.key === "ArrowRight" ? 0.5 : 0;
        const dy = event.key === "ArrowUp" ? -0.5 : event.key === "ArrowDown" ? 0.5 : 0;
        const room = layout.rooms.find((r) => r.id === selectedRoom);
        if (room) {
          const x = Math.min(Math.max(room.x + dx, 0), plotWidth - room.widthFt);
          const y = Math.min(Math.max(room.y + dy, 0), plotDepth - room.depthFt);
          change({ ...layout, rooms: layout.rooms.map((r) => (r.id === room.id ? { ...r, x, y } : r)) });
        }
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [undo, deleteSelected, selectedRoom, selectedOpening, selectedColumn, selectedFurniture, layout, plotWidth, plotDepth]);

  async function suggest() {
    setConfirmSuggest(false);
    setIsBusy(true);
    try {
      const plans = await estimationService.suggestPlans(requirementId);
      for (const floor of plans.floors) {
        const previous = layouts[floor.floorLevel] ?? EMPTY;
        commit(
          floor.floorLevel,
          { rooms: floor.rooms, openings: floor.openings, columns: floor.columns, furniture: suggestFurniture(floor.rooms, floor.openings) },
          previous,
        );
      }
      setSelectedFurniture(null);
      setSelectedRoom(null);
      setSelectedOpening(null);
      showToast("success", "Layout suggested. Drag rooms to adjust it, then save.");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  function requestSuggest() {
    if (siteDirty) {
      showToast("warning", "Save the site rules first so the suggested layout uses them.");
      return;
    }
    const hasContent = Object.values(layouts).some((l) => l.rooms.length > 0);
    if (hasContent) setConfirmSuggest(true);
    else suggest();
  }

  async function saveFloor(level: number): Promise<boolean> {
    const target = layouts[level] ?? EMPTY;
    const found = findProblems(target, plotWidth, plotDepth);
    if (found.messages.length > 0) {
      showToast("error", `${floorLabel(level)}: ${found.messages[0]}`);
      return false;
    }
    const saved = await estimationService.savePlan(requirementId, level, target);
    setSavedMetrics((current) => ({ ...current, [level]: saved.metrics }));
    setSavedStructure((current) => ({ ...current, [level]: saved.structure }));
    setBaseline((current) => ({ ...current, [level]: key(target) }));
    return true;
  }

  async function saveSite() {
    setIsBusy(true);
    try {
      await estimationService.saveSiteRules(requirementId, {
        setbackFrontFt: siteInput.front,
        setbackRearFt: siteInput.rear,
        setbackLeftFt: siteInput.left,
        setbackRightFt: siteInput.right,
        maxCoveragePercent: siteInput.maxCoverage,
        maxFar: siteInput.maxFar,
      });
      setSavedSite(JSON.stringify(site));
      showToast("success", "Site rules saved. Suggest layout now keeps within these setbacks.");
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  async function saveActive() {
    setIsBusy(true);
    try {
      if (await saveFloor(active)) showToast("success", `${floorLabel(active)} saved.`);
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  async function saveAndApply() {
    if (!requirement) return;
    setIsBusy(true);
    try {
      for (const floor of requirement.floors) {
        if (isDirty(floor.floorLevel) && !(await saveFloor(floor.floorLevel))) return;
      }
      await estimationService.applyPlans(requirementId);
      showToast("success", "Plan applied. Regenerate the estimate to price it.");
      navigate(`/estimation/${requirementId}`);
    } catch (err) {
      showToast("error", getErrorMessage(err));
    } finally {
      setIsBusy(false);
    }
  }

  if (error) {
    return <div className="bf-plan-editor__error">{error}</div>;
  }
  if (!requirement) {
    return <Skeleton height={520} />;
  }

  const selectedFurnitureItem = layout.furniture.find((f) => f.id === selectedFurniture) ?? null;
  const room = layout.rooms.find((r) => r.id === selectedRoom) ?? null;
  const roomOpenings = room ? layout.openings.filter((o) => o.roomId === room.id) : [];
  const checklist = requirement.floors.find((f) => f.floorLevel === active);
  const nothingDrawn = Object.values(layouts).every((l) => l.rooms.length === 0);

  return (
    <div className="bf-plan-editor">
      <Link to={`/estimation/${requirementId}`} className="bf-plan-editor__back">← Back to estimate</Link>

      <div className="bf-plan-editor__header">
        <div>
          <h1>Floor plan</h1>
          <p>
            {requirement.label} · plot {formatFt(plotWidth)} × {formatFt(plotDepth)} ft. Drag rooms to move them, use the
            corner handle to resize, and add doors, windows and furniture from the panel. Drag furniture to place it,
            and press Delete to remove the selected piece.
          </p>
        </div>
        <div className="bf-plan-editor__actions">
          <Button variant="secondary" onClick={requestSuggest} isLoading={isBusy}>Suggest layout</Button>
          <Button variant="secondary" onClick={undo} disabled={(history[active] ?? []).length === 0}>Undo</Button>
          <Button variant="secondary" onClick={saveActive} isLoading={isBusy} disabled={!isDirty(active)}>Save floor</Button>
          <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/3d`)} disabled={nothingDrawn}>3D home view</Button>
          <Button variant="secondary" onClick={() => navigate(`/estimation/${requirementId}/plan-sheet`)} disabled={nothingDrawn}>Plan sheet (PDF)</Button>
          <Button variant="secondary" onClick={downloadCad} isLoading={isBusy} disabled={nothingDrawn}>Download CAD (DXF)</Button>
          <Button onClick={saveAndApply} isLoading={isBusy} disabled={nothingDrawn}>Save & use in estimate</Button>
        </div>
      </div>

      <div className="bf-plan-editor__disclaimer">
        This is a concept plan for visualising and estimating. It is not a sanctioned or structural drawing; have an
        architect or engineer prepare and approve the final design.
      </div>

      <div className="bf-plan-editor__tabs">
        {requirement.floors.map((floor) => (
          <button
            key={floor.floorLevel}
            className={`bf-plan-editor__tab ${active === floor.floorLevel ? "bf-plan-editor__tab--active" : ""}`}
            onClick={() => {
              setActive(floor.floorLevel);
              setSelectedFurniture(null);
              setSelectedRoom(null);
              setSelectedOpening(null);
            }}
          >
            {floorLabel(floor.floorLevel)}
            {isDirty(floor.floorLevel) && <span className="bf-plan-editor__dot" title="Unsaved changes" />}
          </button>
        ))}
      </div>

      <div className="bf-plan-editor__body">
        <div className="bf-plan-editor__canvas">
          <PlanCanvas
            plotWidth={plotWidth}
            plotDepth={plotDepth}
            rooms={layout.rooms}
            openings={layout.openings}
            columns={layout.columns}
            beams={beams}
            furniture={layout.furniture}
            selectedFurnitureId={selectedFurniture}
            onSelectFurniture={selectFurniture}
            onChangeFurniture={handleFurnitureChange}
            envelope={siteCheck.envelope}
            selectedColumnId={selectedColumn}
            onSelectColumn={(columnId) => {
              setSelectedColumn(columnId);
              if (columnId) setSelectedFurniture(null);
            }}
            onColumnsChange={handleColumnsChange}
            selectedRoomId={selectedRoom}
            selectedOpeningId={selectedOpening}
            problemRoomIds={problems.roomIds}
            onSelectRoom={(roomId) => {
              setSelectedRoom(roomId);
              setSelectedOpening(null);
              if (roomId) {
                setSelectedColumn(null);
                setSelectedFurniture(null);
              }
            }}
            onSelectOpening={setSelectedOpening}
            onRoomsChange={handleRoomsChange}
          />
          {layout.rooms.length === 0 && (
            <div className="bf-plan-editor__empty">
              <strong>No rooms drawn on this floor yet.</strong>
              <span>Let the app suggest a layout from your room checklist, or add rooms from the panel.</span>
              <Button onClick={requestSuggest} isLoading={isBusy}>Suggest a layout</Button>
            </div>
          )}
          {problems.messages.length > 0 && (
            <ul className="bf-plan-editor__problems">
              {problems.messages.map((m) => (
                <li key={m}>{m}</li>
              ))}
            </ul>
          )}
        </div>

        <aside className="bf-plan-editor__panel">
          <Card>
            <h3>Add a room</h3>
            <div className="bf-plan-editor__palette">
              {ROOM_TYPES.map((type) => (
                <button key={type} onClick={() => addRoom(type)}>
                  <span className="bf-plan-editor__swatch" style={{ background: ROOM_META[type].color }} />
                  {ROOM_META[type].label}
                </button>
              ))}
            </div>
          </Card>

          <Card>
            <h3>Site rules</h3>
            <p className="bf-plan-editor__hint">
              Enter the setbacks and limits your local authority requires. Blank setbacks use planning defaults
              ({DEFAULT_SETBACKS.front} ft front, {DEFAULT_SETBACKS.rear} ft rear, {DEFAULT_SETBACKS.left} ft sides); blank limits are not checked.
            </p>
            <div className="bf-plan-editor__grid2">
              {([["front", "Front (ft)", DEFAULT_SETBACKS.front], ["rear", "Rear (ft)", DEFAULT_SETBACKS.rear], ["left", "Left (ft)", DEFAULT_SETBACKS.left], ["right", "Right (ft)", DEFAULT_SETBACKS.right]] as const).map(([field, label, fallback]) => (
                <label className="bf-plan-editor__field" key={field}>
                  {label}
                  <input type="number" min="0" max="40" step="0.5" placeholder={String(fallback)} value={site[field]} onChange={(e) => setSite((c) => ({ ...c, [field]: e.target.value }))} />
                </label>
              ))}
              <label className="bf-plan-editor__field">
                Max coverage (%)
                <input type="number" min="1" max="100" step="1" placeholder="not set" value={site.coverage} onChange={(e) => setSite((c) => ({ ...c, coverage: e.target.value }))} />
              </label>
              <label className="bf-plan-editor__field">
                Max FAR
                <input type="number" min="0.1" max="10" step="0.05" placeholder="not set" value={site.far} onChange={(e) => setSite((c) => ({ ...c, far: e.target.value }))} />
              </label>
            </div>
            <dl className="bf-plan-editor__summary">
              <div>
                <dt>Ground coverage</dt>
                <dd className={siteCheck.coverage.status === "EXCEEDS" ? "bf-plan-editor__bad" : undefined}>
                  {siteCheck.coverage.value.toFixed(1)}%{siteCheck.coverage.status === "EXCEEDS" ? " over limit" : siteCheck.coverage.status === "OK" ? " ok" : ""}
                </dd>
              </div>
              <div>
                <dt>Floor area ratio</dt>
                <dd className={siteCheck.far.status === "EXCEEDS" ? "bf-plan-editor__bad" : undefined}>
                  {siteCheck.far.value.toFixed(2)}{siteCheck.far.status === "EXCEEDS" ? " over limit" : siteCheck.far.status === "OK" ? " ok" : ""}
                </dd>
              </div>
            </dl>
            {siteCheck.noRoom && <p className="bf-plan-editor__bad">These setbacks leave no buildable area on the plot.</p>}
            {siteCheck.violations.length > 0 && (
              <ul className="bf-plan-editor__site-issues">
                {siteCheck.violations.map((v) => (
                  <li key={v.roomId}>{floorLabel(v.floorLevel)}: {v.message}</li>
                ))}
              </ul>
            )}
            <Button variant="secondary" onClick={saveSite} isLoading={isBusy} disabled={!siteDirty}>Save site rules</Button>
            <p className="bf-plan-editor__hint">A concept check against the numbers you entered, not a statutory approval.</p>
          </Card>

          <Card>
            <h3>Furniture</h3>
            <p className="bf-plan-editor__hint">
              For visualising only; furniture is not priced. Click a piece to add it to the selected room, then drag it
              into place.
            </p>
            <div className="bf-plan-editor__palette">
              {FURNITURE_CATALOG.map((spec) => (
                <button key={spec.kind} onClick={() => addFurniture(spec.kind)}>
                  <span className="bf-plan-editor__swatch" style={{ background: spec.colour }} />
                  {spec.label}
                </button>
              ))}
            </div>
            <div className="bf-plan-editor__palette" style={{ marginTop: "var(--space-3)" }}>
              <button onClick={autoFurnish} disabled={layout.rooms.length === 0}>Auto-furnish this floor</button>
              <button onClick={clearFurniture} disabled={layout.furniture.length === 0}>Clear furniture</button>
            </div>
            {selectedFurnitureItem && (
              <div style={{ marginTop: "var(--space-3)" }}>
                <div className="bf-plan-editor__panel-title">
                  <h4>Selected: {furnitureLabel(selectedFurnitureItem.kind)}</h4>
                  <button className="bf-plan-editor__delete" onClick={deleteSelected}>Delete</button>
                </div>
                <div className="bf-plan-editor__grid2">
                  {([["widthFt", "Width (ft)", 0.3, 25], ["depthFt", "Depth (ft)", 0.3, 25], ["heightFt", "Height (ft)", 0.05, 40]] as const).map(([field, label, min, max]) => (
                    <label className="bf-plan-editor__field" key={field}>
                      {label}
                      <input
                        type="number"
                        step="0.1"
                        min={min}
                        max={max}
                        value={selectedFurnitureItem[field]}
                        onChange={(e) => {
                          const value = Number(e.target.value);
                          if (e.target.value !== "" && Number.isFinite(value)) {
                            updateFurniture(selectedFurnitureItem.id, { [field]: clampNum(value, min, max) });
                          }
                        }}
                      />
                    </label>
                  ))}
                  <label className="bf-plan-editor__field">
                    Colour
                    <input
                      type="color"
                      value={selectedFurnitureItem.colour ?? "#999999"}
                      onChange={(e) => updateFurniture(selectedFurnitureItem.id, { colour: e.target.value })}
                    />
                  </label>
                </div>
                <div className="bf-plan-editor__palette" style={{ marginTop: "var(--space-2)" }}>
                  <button onClick={() => updateFurniture(selectedFurnitureItem.id, { rotationDeg: (selectedFurnitureItem.rotationDeg + 90) % 360 })}>Rotate 90°</button>
                  <button onClick={() => duplicateFurniture(selectedFurnitureItem)}>Duplicate</button>
                </div>
              </div>
            )}
          </Card>

          <Card>
            <h3>Structure</h3>
            <p className="bf-plan-editor__hint">
              Columns at wall junctions and beams along the walls between them. With columns on every floor, concrete and
              steel come from this frame instead of per-sq.ft rules.
            </p>
            <div className="bf-plan-editor__palette">
              <button onClick={() => suggestColumns("grid")}>Suggest columns</button>
              <button onClick={() => suggestColumns("junctions")}>Every junction</button>
              <button onClick={addColumn}>+ Column</button>
            </div>
            <dl className="bf-plan-editor__summary" style={{ marginTop: "var(--space-3)" }}>
              <div><dt>Columns</dt><dd>{layout.columns.length}</dd></div>
              <div><dt>Beam length</dt><dd>{formatFt(beams.reduce((t, b) => t + Math.abs(b.x2 - b.x1) + Math.abs(b.y2 - b.y1), 0))} ft</dd></div>
            </dl>
            {savedStructure[active] && !isDirty(active) && savedStructure[active].columnCount > 0 && (
              <dl className="bf-plan-editor__summary" style={{ marginTop: "var(--space-2)" }}>
                <div><dt>Frame concrete</dt><dd>{savedStructure[active].concreteM3.toLocaleString("en-IN", { maximumFractionDigits: 1 })} m³</dd></div>
                <div><dt>Frame steel</dt><dd>{Math.round(savedStructure[active].steelKg).toLocaleString("en-IN")} kg</dd></div>
              </dl>
            )}
            {selectedColumn && (() => {
              const column = layout.columns.find((c) => c.id === selectedColumn);
              if (!column) return null;
              return (
                <div style={{ marginTop: "var(--space-3)" }}>
                  <div className="bf-plan-editor__panel-title">
                    <h4>Selected column</h4>
                    <button className="bf-plan-editor__delete" onClick={deleteSelected}>Delete</button>
                  </div>
                  <div className="bf-plan-editor__grid2">
                    {([["x", "X (ft)"], ["y", "Y (ft)"], ["widthFt", "Width (ft)"], ["depthFt", "Depth (ft)"]] as const).map(([field, label]) => (
                      <label className="bf-plan-editor__field" key={field}>
                        {label}
                        <input type="number" step="0.25" min="0" value={column[field]} onChange={(e) => Number.isFinite(Number(e.target.value)) && updateColumn(column.id, { [field]: Number(e.target.value) })} />
                      </label>
                    ))}
                  </div>
                </div>
              );
            })()}
          </Card>

          {room && (
            <Card>
              <div className="bf-plan-editor__panel-title">
                <h3>{roomLabel(room)}</h3>
                <button className="bf-plan-editor__delete" onClick={deleteSelected}>Delete</button>
              </div>
              <label className="bf-plan-editor__field">
                Name
                <input value={room.name ?? ""} onChange={(e) => updateRoom(room.id, { name: e.target.value })} />
              </label>
              <label className="bf-plan-editor__field">
                Type
                <select value={room.type} onChange={(e) => updateRoom(room.id, { type: e.target.value as RoomType })}>
                  {ROOM_TYPES.map((t) => (
                    <option key={t} value={t}>{ROOM_META[t].label}</option>
                  ))}
                </select>
              </label>
              <div className="bf-plan-editor__grid2">
                {([["x", "X (ft)"], ["y", "Y (ft)"], ["widthFt", "Width (ft)"], ["depthFt", "Depth (ft)"]] as const).map(([field, label]) => (
                  <label className="bf-plan-editor__field" key={field}>
                    {label}
                    <input
                      type="number"
                      step="0.5"
                      min="0"
                      value={room[field]}
                      onChange={(e) => {
                        const value = Number(e.target.value);
                        if (Number.isFinite(value)) updateRoom(room.id, { [field]: value });
                      }}
                    />
                  </label>
                ))}
              </div>

              <h4>Doors and windows</h4>
              <div className="bf-plan-editor__add-opening">
                {SIDES.map((side) => (
                  <div key={side}>
                    <span>{side.charAt(0) + side.slice(1).toLowerCase()}</span>
                    <button onClick={() => addOpening("DOOR", side)}>+ Door</button>
                    <button onClick={() => addOpening("WINDOW", side)}>+ Window</button>
                  </div>
                ))}
              </div>
              {roomOpenings.map((o) => (
                <div className={`bf-plan-editor__opening ${selectedOpening === o.id ? "bf-plan-editor__opening--selected" : ""}`} key={o.id} onClick={() => setSelectedOpening(o.id)}>
                  <strong>{o.kind === "DOOR" ? "Door" : "Window"}</strong>
                  <select aria-label="Wall" value={o.side} onChange={(e) => updateOpening(o.id, { side: e.target.value as PlanSide })}>
                    {SIDES.map((s) => (
                      <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>
                    ))}
                  </select>
                  <input aria-label="Offset" type="number" step="0.5" min="0" value={o.offsetFt} onChange={(e) => Number.isFinite(Number(e.target.value)) && updateOpening(o.id, { offsetFt: Number(e.target.value) })} />
                  <input aria-label="Width" type="number" step="0.5" min="1" value={o.widthFt} onChange={(e) => Number.isFinite(Number(e.target.value)) && updateOpening(o.id, { widthFt: Number(e.target.value) })} />
                  <button aria-label="Remove opening" onClick={() => change({ ...layout, openings: layout.openings.filter((x) => x.id !== o.id) })}>×</button>
                </div>
              ))}
            </Card>
          )}

          <Card>
            <h3>{floorLabel(active)} summary</h3>
            <dl className="bf-plan-editor__summary">
              <div><dt>Rooms</dt><dd>{layout.rooms.length}</dd></div>
              <div><dt>Drawn area</dt><dd>{Math.round(metrics.builtUpAreaSqft).toLocaleString("en-IN")} sq.ft</dd></div>
              <div><dt>Bedrooms / baths</dt><dd>{metrics.bedrooms} / {metrics.bathrooms}</dd></div>
              <div><dt>Doors / windows</dt><dd>{metrics.doors} / {metrics.windows}</dd></div>
              {checklist && (
                <div><dt>Checklist area</dt><dd>{checklist.floorAreaSqft.toLocaleString("en-IN")} sq.ft</dd></div>
              )}
            </dl>
            {savedMetrics[active] && !isDirty(active) && (
              <>
                <h4>Quantities from this drawing</h4>
                <dl className="bf-plan-editor__summary">
                  <div><dt>External wall</dt><dd>{formatFt(savedMetrics[active].externalWallFt)} ft</dd></div>
                  <div><dt>Internal wall</dt><dd>{formatFt(savedMetrics[active].internalWallFt)} ft</dd></div>
                  <div><dt>Wall area (net)</dt><dd>{Math.round(savedMetrics[active].wallAreaSqft).toLocaleString("en-IN")} sq.ft</dd></div>
                  <div><dt>Plaster / paint area</dt><dd>{Math.round(savedMetrics[active].plasterAreaSqft).toLocaleString("en-IN")} sq.ft</dd></div>
                </dl>
                <p className="bf-plan-editor__hint">
                  Assumes a 10 ft floor height, 7 ft doors and 4 ft windows. When every floor is drawn, the estimate uses
                  these instead of the built-up-area rules.
                </p>
              </>
            )}
            <p className="bf-plan-editor__hint">
              "Save & use in estimate" replaces each drawn floor's area, room counts and openings in the checklist with
              what is drawn here, so the estimate follows the plan.
            </p>
          </Card>
        </aside>
      </div>

      {confirmSuggest && (
        <ConfirmDialog
          title="Replace the current drawings?"
          description="A suggested layout will replace what is drawn on every floor. You can undo it with Undo."
          confirmLabel="Replace"
          onConfirm={suggest}
          onCancel={() => setConfirmSuggest(false)}
        />
      )}
    </div>
  );
}
