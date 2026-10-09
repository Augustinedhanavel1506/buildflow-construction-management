import { useId, useRef, type PointerEvent as ReactPointerEvent } from "react";
import type { PlanBeam, PlanColumn, PlanFurniture, PlanOpening, PlanRoom } from "../../../types/plan";
import { footprint, furnitureLabel } from "../furniture";
import { MIN_SIDE_FT, ROOM_META, formatFt, openingSegment, roomLabel, snap } from "./planUtils";
import "./PlanCanvas.css";

interface ColumnDrag {
  id: string;
  startX: number;
  startY: number;
  original: PlanColumn;
  moved: boolean;
}

interface FurnitureDrag {
  id: string;
  startX: number;
  startY: number;
  original: PlanFurniture;
  moved: boolean;
}

interface DragState {
  mode: "move" | "resize";
  id: string;
  startX: number;
  startY: number;
  original: PlanRoom;
  moved: boolean;
}

export interface PlanCanvasProps {
  plotWidth: number;
  plotDepth: number;
  rooms: PlanRoom[];
  openings: PlanOpening[];
  columns?: PlanColumn[];
  beams?: PlanBeam[];
  furniture?: PlanFurniture[];
  selectedFurnitureId?: string | null;
  onSelectFurniture?: (id: string | null) => void;
  onChangeFurniture?: (furniture: PlanFurniture[], commit: boolean) => void;
  // The buildable area inside the setbacks; the margin around it is shaded.
  envelope?: { x: number; y: number; widthFt: number; depthFt: number } | null;
  selectedColumnId?: string | null;
  onSelectColumn?: (id: string | null) => void;
  onColumnsChange?: (columns: PlanColumn[], commit: boolean) => void;
  selectedRoomId?: string | null;
  selectedOpeningId?: string | null;
  problemRoomIds?: Set<string>;
  readOnly?: boolean;
  onSelectRoom?: (id: string | null) => void;
  onSelectOpening?: (id: string) => void;
  // commit is false while dragging (live preview) and true once, when the drag ends.
  onRoomsChange?: (rooms: PlanRoom[], commit: boolean) => void;
}

const PAD = 3;
const PAD_TOP = 4.5;

export function PlanCanvas({
  plotWidth,
  plotDepth,
  rooms,
  openings,
  columns = [],
  beams = [],
  furniture = [],
  selectedFurnitureId,
  onSelectFurniture,
  onChangeFurniture,
  envelope,
  selectedColumnId,
  onSelectColumn,
  onColumnsChange,
  selectedRoomId,
  selectedOpeningId,
  problemRoomIds,
  readOnly,
  onSelectRoom,
  onSelectOpening,
  onRoomsChange,
}: PlanCanvasProps) {
  const svgRef = useRef<SVGSVGElement>(null);
  const dragRef = useRef<DragState | null>(null);
  const columnDragRef = useRef<ColumnDrag | null>(null);
  const furnitureDragRef = useRef<FurnitureDrag | null>(null);
  const furnitureRef = useRef(furniture);
  furnitureRef.current = furniture;
  const columnsRef = useRef(columns);
  columnsRef.current = columns;
  const roomsRef = useRef(rooms);
  roomsRef.current = rooms;
  const patternId = useId().replace(/:/g, "");

  const viewX = -PAD;
  const viewY = -PAD_TOP;
  const viewW = plotWidth + PAD * 2;
  const viewH = plotDepth + PAD_TOP + PAD;

  // Pointer position in plan feet, whatever the on-screen size of the drawing.
  function toFeet(event: { clientX: number; clientY: number }) {
    const rect = svgRef.current!.getBoundingClientRect();
    return {
      x: viewX + ((event.clientX - rect.left) / rect.width) * viewW,
      y: viewY + ((event.clientY - rect.top) / rect.height) * viewH,
    };
  }

  function startDrag(event: ReactPointerEvent, room: PlanRoom, mode: "move" | "resize") {
    if (readOnly) return;
    event.stopPropagation();
    onSelectRoom?.(room.id);
    const point = toFeet(event);
    dragRef.current = { mode, id: room.id, startX: point.x, startY: point.y, original: room, moved: false };
    svgRef.current?.setPointerCapture(event.pointerId);
  }

  function startColumnDrag(event: ReactPointerEvent, column: PlanColumn) {
    if (readOnly) return;
    event.stopPropagation();
    onSelectColumn?.(column.id);
    onSelectRoom?.(null);
    const point = toFeet(event);
    columnDragRef.current = { id: column.id, startX: point.x, startY: point.y, original: column, moved: false };
    svgRef.current?.setPointerCapture(event.pointerId);
  }

  function startFurnitureDrag(event: ReactPointerEvent, item: PlanFurniture) {
    if (readOnly) return;
    event.stopPropagation();
    onSelectFurniture?.(item.id);
    const point = toFeet(event);
    furnitureDragRef.current = { id: item.id, startX: point.x, startY: point.y, original: item, moved: false };
    svgRef.current?.setPointerCapture(event.pointerId);
  }

  function handleMove(event: ReactPointerEvent) {
    const furnDrag = furnitureDragRef.current;
    if (furnDrag) {
      const point = toFeet(event);
      const o = furnDrag.original;
      const fp = footprint(o);
      const clamp = (v: number, half: number, max: number) => Math.min(Math.max(v, half), Math.max(max - half, half));
      const next = {
        ...o,
        x: clamp(snap(o.x + point.x - furnDrag.startX), fp.w / 2, plotWidth),
        y: clamp(snap(o.y + point.y - furnDrag.startY), fp.d / 2, plotDepth),
      };
      if (next.x !== o.x || next.y !== o.y) furnDrag.moved = true;
      onChangeFurniture?.(furnitureRef.current.map((f) => (f.id === furnDrag.id ? next : f)), false);
      return;
    }
    const colDrag = columnDragRef.current;
    if (colDrag) {
      const point = toFeet(event);
      const o = colDrag.original;
      const next = {
        ...o,
        x: Math.min(Math.max(snap(o.x + point.x - colDrag.startX), 0), plotWidth),
        y: Math.min(Math.max(snap(o.y + point.y - colDrag.startY), 0), plotDepth),
      };
      if (next.x !== o.x || next.y !== o.y) colDrag.moved = true;
      onColumnsChange?.(columnsRef.current.map((c) => (c.id === colDrag.id ? next : c)), false);
      return;
    }
    const drag = dragRef.current;
    if (!drag) return;
    const point = toFeet(event);
    const dx = point.x - drag.startX;
    const dy = point.y - drag.startY;
    const o = drag.original;

    let next: PlanRoom;
    if (drag.mode === "move") {
      next = {
        ...o,
        x: Math.min(Math.max(snap(o.x + dx), 0), Math.max(plotWidth - o.widthFt, 0)),
        y: Math.min(Math.max(snap(o.y + dy), 0), Math.max(plotDepth - o.depthFt, 0)),
      };
    } else {
      next = {
        ...o,
        widthFt: Math.min(Math.max(snap(o.widthFt + dx), MIN_SIDE_FT), Math.max(plotWidth - o.x, MIN_SIDE_FT)),
        depthFt: Math.min(Math.max(snap(o.depthFt + dy), MIN_SIDE_FT), Math.max(plotDepth - o.y, MIN_SIDE_FT)),
      };
    }
    if (next.x !== o.x || next.y !== o.y || next.widthFt !== o.widthFt || next.depthFt !== o.depthFt) {
      drag.moved = true;
    }
    onRoomsChange?.(roomsRef.current.map((r) => (r.id === drag.id ? next : r)), false);
  }

  function endDrag(event: ReactPointerEvent) {
    const furnDrag = furnitureDragRef.current;
    if (furnDrag) {
      furnitureDragRef.current = null;
      svgRef.current?.releasePointerCapture(event.pointerId);
      if (furnDrag.moved) onChangeFurniture?.(furnitureRef.current, true);
      return;
    }
    const colDrag = columnDragRef.current;
    if (colDrag) {
      columnDragRef.current = null;
      svgRef.current?.releasePointerCapture(event.pointerId);
      if (colDrag.moved) onColumnsChange?.(columnsRef.current, true);
      return;
    }
    const drag = dragRef.current;
    if (!drag) return;
    dragRef.current = null;
    svgRef.current?.releasePointerCapture(event.pointerId);
    if (drag.moved) {
      onRoomsChange?.(roomsRef.current, true);
    }
  }

  const selectedRoom = rooms.find((r) => r.id === selectedRoomId);

  return (
    <svg
      ref={svgRef}
      className={`bf-plan-canvas ${readOnly ? "bf-plan-canvas--readonly" : ""}`}
      viewBox={`${viewX} ${viewY} ${viewW} ${viewH}`}
      onPointerMove={handleMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      role="img"
      aria-label={`Floor plan of a ${plotWidth} by ${plotDepth} foot plot`}
    >
      <defs>
        <pattern id={`${patternId}-minor`} width="1" height="1" patternUnits="userSpaceOnUse">
          <path d="M 1 0 L 0 0 0 1" className="bf-plan-canvas__grid-minor" />
        </pattern>
        <pattern id={`${patternId}-major`} width="5" height="5" patternUnits="userSpaceOnUse">
          <rect width="5" height="5" fill={`url(#${patternId}-minor)`} />
          <path d="M 5 0 L 0 0 0 5" className="bf-plan-canvas__grid-major" />
        </pattern>
      </defs>

      <rect
        x={0}
        y={0}
        width={plotWidth}
        height={plotDepth}
        fill={`url(#${patternId}-major)`}
        className="bf-plan-canvas__plot"
        onPointerDown={() => {
          onSelectRoom?.(null);
          onSelectFurniture?.(null);
        }}
      />

      {envelope && envelope.widthFt > 0 && envelope.depthFt > 0 && (
        <g pointerEvents="none">
          <path
            className="bf-plan-canvas__setback"
            fillRule="evenodd"
            d={`M0 0H${plotWidth}V${plotDepth}H0Z M${envelope.x} ${envelope.y}h${envelope.widthFt}v${envelope.depthFt}h-${envelope.widthFt}Z`}
          />
          <rect x={envelope.x} y={envelope.y} width={envelope.widthFt} height={envelope.depthFt} className="bf-plan-canvas__envelope" />
        </g>
      )}

      <text x={plotWidth / 2} y={-2.6} className="bf-plan-canvas__road" textAnchor="middle">
        ROAD / FRONT
      </text>
      <text x={plotWidth / 2} y={-0.9} className="bf-plan-canvas__dimension" textAnchor="middle">
        {formatFt(plotWidth)} ft
      </text>
      <text
        x={plotWidth + 1.6}
        y={plotDepth / 2}
        className="bf-plan-canvas__dimension"
        textAnchor="middle"
        transform={`rotate(90 ${plotWidth + 1.6} ${plotDepth / 2})`}
      >
        {formatFt(plotDepth)} ft
      </text>

      {rooms.map((room) => {
        const meta = ROOM_META[room.type];
        const problem = problemRoomIds?.has(room.id);
        const selected = room.id === selectedRoomId;
        const name = roomLabel(room);
        // Glyphs average about 0.58 of the font size wide; shrink the name to fit the room, and
        // drop the size line when it would not fit, instead of spilling over neighbouring rooms.
        const fitFont = (room.widthFt - 0.8) / Math.max(name.length * 0.58, 1);
        const font = Math.min(Math.max(Math.min(room.widthFt, room.depthFt) / 7, 0.55), 0.95, fitFont);
        const sizeText = `${formatFt(room.widthFt)}′ × ${formatFt(room.depthFt)}′ · ${Math.round(room.widthFt * room.depthFt)} sq.ft`;
        const sizeFits = sizeText.length * font * 0.8 * 0.58 <= room.widthFt - 0.6;
        const small = Math.min(room.widthFt, room.depthFt) < 4 || !sizeFits || font < 0.4;
        const cx = room.x + room.widthFt / 2;
        const cy = room.y + room.depthFt / 2;
        return (
          <g key={room.id}>
            <rect
              x={room.x}
              y={room.y}
              width={room.widthFt}
              height={room.depthFt}
              fill={meta.color}
              fillOpacity={0.3}
              stroke={problem ? undefined : meta.color}
              className={[
                "bf-plan-canvas__room",
                selected ? "bf-plan-canvas__room--selected" : "",
                problem ? "bf-plan-canvas__room--problem" : "",
              ].filter(Boolean).join(" ")}
              onPointerDown={(e) => startDrag(e, room, "move")}
            />
            <text x={cx} y={small ? cy + font * 0.35 : cy - font * 0.2} className="bf-plan-canvas__label" fontSize={font} textAnchor="middle" pointerEvents="none">
              {font < 0.4 ? "" : name}
            </text>
            {!small && (
              <text x={cx} y={cy + font * 1.15} className="bf-plan-canvas__sublabel" fontSize={font * 0.8} textAnchor="middle" pointerEvents="none">
                {sizeText}
              </text>
            )}
          </g>
        );
      })}

      {beams.map((b, i) => (
        <line key={`beam-${i}`} x1={b.x1} y1={b.y1} x2={b.x2} y2={b.y2} className="bf-plan-canvas__beam" />
      ))}

      {openings.map((opening) => {
        const room = rooms.find((r) => r.id === opening.roomId);
        if (!room) return null;
        const seg = openingSegment(room, opening);
        const selected = opening.id === selectedOpeningId;
        const horizontal = seg.y1 === seg.y2;
        return (
          <g key={opening.id} className={selected ? "bf-plan-canvas__opening--selected" : ""}>
            <line {...seg} className="bf-plan-canvas__opening-gap" />
            {opening.kind === "DOOR" ? (
              <line {...seg} className="bf-plan-canvas__door" />
            ) : (
              <>
                <line
                  x1={seg.x1 + (horizontal ? 0 : -0.18)}
                  y1={seg.y1 + (horizontal ? -0.18 : 0)}
                  x2={seg.x2 + (horizontal ? 0 : -0.18)}
                  y2={seg.y2 + (horizontal ? -0.18 : 0)}
                  className="bf-plan-canvas__window"
                />
                <line
                  x1={seg.x1 + (horizontal ? 0 : 0.18)}
                  y1={seg.y1 + (horizontal ? 0.18 : 0)}
                  x2={seg.x2 + (horizontal ? 0 : 0.18)}
                  y2={seg.y2 + (horizontal ? 0.18 : 0)}
                  className="bf-plan-canvas__window"
                />
              </>
            )}
            {!readOnly && (
              <line
                {...seg}
                className="bf-plan-canvas__opening-hit"
                onPointerDown={(e) => {
                  e.stopPropagation();
                  onSelectRoom?.(room.id);
                  onSelectOpening?.(opening.id);
                }}
              />
            )}
          </g>
        );
      })}

      {furniture.map((item) => {
        const fp = footprint(item);
        const label = furnitureLabel(item.kind);
        const selected = item.id === selectedFurnitureId;
        const font = Math.min(0.7, Math.max(fp.w, fp.d) / 6);
        const showLabel = Math.min(fp.w, fp.d) >= 1.4 && label.length * font * 0.58 <= fp.w - 0.3;
        return (
          <g key={item.id}>
            <g transform={`translate(${item.x} ${item.y}) rotate(${item.rotationDeg})`}>
              <rect
                x={-item.widthFt / 2}
                y={-item.depthFt / 2}
                width={item.widthFt}
                height={item.depthFt}
                fill={item.colour ?? "#999999"}
                fillOpacity={0.85}
                className={selected ? "bf-plan-canvas__furniture bf-plan-canvas__furniture--selected" : "bf-plan-canvas__furniture"}
                onPointerDown={(e) => startFurnitureDrag(e, item)}
              />
              <line
                x1={-item.widthFt / 2 + 0.15}
                y1={-item.depthFt / 2 + 0.12}
                x2={item.widthFt / 2 - 0.15}
                y2={-item.depthFt / 2 + 0.12}
                className="bf-plan-canvas__furniture-back"
                pointerEvents="none"
              />
            </g>
            {showLabel && (
              <text x={item.x} y={item.y + font * 0.35} className="bf-plan-canvas__furniture-label" fontSize={font} textAnchor="middle" pointerEvents="none">
                {label}
              </text>
            )}
          </g>
        );
      })}

      {columns.map((c) => (
        <rect
          key={c.id}
          x={c.x - Math.max(c.widthFt, 0.9) / 2}
          y={c.y - Math.max(c.depthFt, 0.9) / 2}
          width={Math.max(c.widthFt, 0.9)}
          height={Math.max(c.depthFt, 0.9)}
          className={c.id === selectedColumnId ? "bf-plan-canvas__column bf-plan-canvas__column--selected" : "bf-plan-canvas__column"}
          onPointerDown={(e) => startColumnDrag(e, c)}
        />
      ))}

      {!readOnly && selectedRoom && (
        <rect
          x={selectedRoom.x + selectedRoom.widthFt - 0.6}
          y={selectedRoom.y + selectedRoom.depthFt - 0.6}
          width={1.2}
          height={1.2}
          className="bf-plan-canvas__handle"
          onPointerDown={(e) => startDrag(e, selectedRoom, "resize")}
        />
      )}
    </svg>
  );
}
