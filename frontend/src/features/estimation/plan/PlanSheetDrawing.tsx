import type { PlanFloorLike } from "./sheetTypes";
import { SHEET_AREA_HEIGHT_MM, SHEET_AREA_WIDTH_MM, chooseScale, feetInches, mmToFt } from "./sheetGeometry";

const WALL = 0.5;
const INK = "#1b1f24";

// A drawing at a true scale: the svg is sized in millimetres, so printed at 100% one millimetre on paper is
// `scale` millimetres in the house. The drawing coordinates are feet, y running down the page (north at the top).
export function PlanSheetDrawing({ floor }: { floor: PlanFloorLike }) {
  const rooms = floor.rooms;
  if (rooms.length === 0) return null;

  const minX = Math.min(...rooms.map((r) => r.x));
  const maxX = Math.max(...rooms.map((r) => r.x + r.widthFt));
  const minY = Math.min(...rooms.map((r) => r.y));
  const maxY = Math.max(...rooms.map((r) => r.y + r.depthFt));
  const width = maxX - minX;
  const height = maxY - minY;

  const MARGIN_NEAR = 11; // ft of room for the dimension chains on the top and left
  const MARGIN_FAR = 3;
  const scale = chooseScale(width + MARGIN_NEAR + MARGIN_FAR, height + MARGIN_NEAR + MARGIN_FAR + 5);
  const fontFt = mmToFt(2.4, scale);
  const smallFt = mmToFt(1.9, scale);
  const tick = mmToFt(1.2, scale);
  const thin = mmToFt(0.18, scale);

  const vx = minX - MARGIN_NEAR;
  const vy = minY - MARGIN_NEAR;
  const vw = width + MARGIN_NEAR + MARGIN_FAR;
  const vh = height + MARGIN_NEAR + MARGIN_FAR + 5; // the extra room under the plan holds the scale bar
  const widthMm = Math.min(SHEET_AREA_WIDTH_MM, (vw * 304.8) / scale);
  const heightMm = Math.min(SHEET_AREA_HEIGHT_MM, (vh * 304.8) / scale);

  const xs = [...new Set(rooms.flatMap((r) => [r.x, r.x + r.widthFt]))].sort((a, b) => a - b);
  const ys = [...new Set(rooms.flatMap((r) => [r.y, r.y + r.depthFt]))].sort((a, b) => a - b);

  const dimH = (x1: number, x2: number, y: number, label: string, key: string) => (
    <g key={key}>
      <line x1={x1} y1={y} x2={x2} y2={y} stroke={INK} strokeWidth={thin} />
      {[x1, x2].map((x) => (
        <line key={x} x1={x - tick / 2} y1={y + tick / 2} x2={x + tick / 2} y2={y - tick / 2} stroke={INK} strokeWidth={thin * 2} />
      ))}
      <line x1={x1} y1={y - tick * 1.5} x2={x1} y2={y + tick * 1.5} stroke={INK} strokeWidth={thin} />
      <line x1={x2} y1={y - tick * 1.5} x2={x2} y2={y + tick * 1.5} stroke={INK} strokeWidth={thin} />
      <text x={(x1 + x2) / 2} y={y - smallFt * 0.35} fontSize={smallFt} textAnchor="middle" fill={INK}>{label}</text>
    </g>
  );
  const dimV = (y1: number, y2: number, x: number, label: string, key: string) => (
    <g key={key}>
      <line x1={x} y1={y1} x2={x} y2={y2} stroke={INK} strokeWidth={thin} />
      {[y1, y2].map((y) => (
        <line key={y} x1={x - tick / 2} y1={y + tick / 2} x2={x + tick / 2} y2={y - tick / 2} stroke={INK} strokeWidth={thin * 2} />
      ))}
      <line x1={x - tick * 1.5} y1={y1} x2={x + tick * 1.5} y2={y1} stroke={INK} strokeWidth={thin} />
      <line x1={x - tick * 1.5} y1={y2} x2={x + tick * 1.5} y2={y2} stroke={INK} strokeWidth={thin} />
      <text transform={`translate(${x - smallFt * 0.35} ${(y1 + y2) / 2}) rotate(-90)`} fontSize={smallFt} textAnchor="middle" fill={INK}>{label}</text>
    </g>
  );

  // Scale bar: 0, 5 and 10 ft (or 0, 10, 20 ft at the small scales).
  const unit = scale >= 150 ? 10 : 5;
  const barY = maxY + 2.4;

  const sideVectors = {
    NORTH: { a: [1, 0], n: [0, 1] },
    SOUTH: { a: [1, 0], n: [0, -1] },
    WEST: { a: [0, 1], n: [1, 0] },
    EAST: { a: [0, 1], n: [-1, 0] },
  } as const;

  return (
    <div className="bf-sheet__scale-wrap">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width={`${widthMm}mm`}
        height={`${heightMm}mm`}
        viewBox={`${vx} ${vy} ${vw} ${vh}`}
        fontFamily="Inter, Arial, sans-serif"
        role="img"
        aria-label={`Floor plan drawn at 1:${scale}`}
      >
        {rooms.map((r) => (
          <rect key={`f${r.id}`} x={r.x} y={r.y} width={r.widthFt} height={r.depthFt} fill="#fbfaf7" />
        ))}

        {/* Staircase treads */}
        {rooms.filter((r) => r.type === "STAIRCASE").map((r) => {
          const alongX = r.widthFt >= r.depthFt;
          const n = Math.max(6, Math.round((alongX ? r.widthFt : r.depthFt) / 0.9));
          return (
            <g key={`s${r.id}`} stroke={INK} strokeWidth={thin}>
              {Array.from({ length: n - 1 }, (_, i) => {
                const f = (i + 1) / n;
                return alongX
                  ? <line key={i} x1={r.x + r.widthFt * f} y1={r.y + WALL / 2} x2={r.x + r.widthFt * f} y2={r.y + r.depthFt - WALL / 2} />
                  : <line key={i} x1={r.x + WALL / 2} y1={r.y + r.depthFt * f} x2={r.x + r.widthFt - WALL / 2} y2={r.y + r.depthFt * f} />;
              })}
            </g>
          );
        })}

        {/* Walls */}
        {rooms.map((r) => (
          <rect key={`w${r.id}`} x={r.x} y={r.y} width={r.widthFt} height={r.depthFt} fill="none" stroke={INK} strokeWidth={WALL} strokeLinejoin="miter" />
        ))}

        {/* Openings: a gap in the wall, then a leaf and swing for doors, three lines for windows */}
        {floor.openings.map((o) => {
          const r = rooms.find((x) => x.id === o.roomId);
          if (!r) return null;
          const { a, n } = sideVectors[o.side];
          const px = o.side === "EAST" ? r.x + r.widthFt : o.side === "WEST" ? r.x : r.x + o.offsetFt;
          const py = o.side === "SOUTH" ? r.y + r.depthFt : o.side === "NORTH" ? r.y : r.y + o.offsetFt;
          const w = o.widthFt;
          const gap = (
            <rect
              x={Math.min(px, px + a[0] * w) - (a[0] === 0 ? WALL * 0.7 : 0)}
              y={Math.min(py, py + a[1] * w) - (a[1] === 0 ? WALL * 0.7 : 0)}
              width={a[0] !== 0 ? w : WALL * 1.4}
              height={a[1] !== 0 ? w : WALL * 1.4}
              fill="#fff"
            />
          );
          if (o.kind === "WINDOW") {
            const cx = (d: number) => px + n[0] * d;
            const cy = (d: number) => py + n[1] * d;
            return (
              <g key={o.id}>
                {gap}
                {[-WALL * 0.3, 0, WALL * 0.3].map((d) => (
                  <line key={d} x1={cx(d)} y1={cy(d)} x2={cx(d) + a[0] * w} y2={cy(d) + a[1] * w} stroke={INK} strokeWidth={thin * 1.3} />
                ))}
              </g>
            );
          }
          // Door: hinge at the start of the opening, leaf swung into the room, arc to the closed position.
          const hx = px;
          const hy = py;
          const leafX = hx + n[0] * w;
          const leafY = hy + n[1] * w;
          const endX = hx + a[0] * w;
          const endY = hy + a[1] * w;
          const sweep = a[0] * n[1] - a[1] * n[0] > 0 ? 1 : 0;
          return (
            <g key={o.id}>
              {gap}
              <line x1={hx} y1={hy} x2={leafX} y2={leafY} stroke={INK} strokeWidth={thin * 2} />
              <path d={`M ${endX} ${endY} A ${w} ${w} 0 0 ${sweep} ${leafX} ${leafY}`} fill="none" stroke={INK} strokeWidth={thin} strokeDasharray={`${tick} ${tick}`} />
            </g>
          );
        })}

        {/* Columns */}
        {floor.columns.map((c) => (
          <rect key={c.id} x={c.x - c.widthFt / 2} y={c.y - c.depthFt / 2} width={c.widthFt} height={c.depthFt} fill={INK} />
        ))}

        {/* Room names, internal sizes and areas */}
        {rooms.map((r) => {
          const name = (r.name || r.type).toUpperCase();
          const size = `${feetInches(r.widthFt)} × ${feetInches(r.depthFt)}`;
          // Shrink the text to fit narrow rooms rather than letting it run into the neighbours.
          const fit = (chars: number, base: number) => Math.min(base, (r.widthFt - WALL * 2) / (chars * 0.62));
          return (
            <g key={`t${r.id}`} textAnchor="middle" fill={INK}>
              <text x={r.x + r.widthFt / 2} y={r.y + r.depthFt / 2 - fontFt * 0.2} fontSize={fit(name.length, fontFt)} fontWeight={700}>{name}</text>
              <text x={r.x + r.widthFt / 2} y={r.y + r.depthFt / 2 + fontFt * 0.95} fontSize={fit(size.length, smallFt)}>{size}</text>
            </g>
          );
        })}

        {/* Dimension chains: each wall-to-wall run, then the overall size */}
        {xs.slice(0, -1).map((x, i) => dimH(x, xs[i + 1], minY - 3, feetInches(xs[i + 1] - x), `dx${i}`))}
        {dimH(minX, maxX, minY - 6.5, `Overall ${feetInches(width)}`, "dxall")}
        {ys.slice(0, -1).map((y, i) => dimV(y, ys[i + 1], minX - 3, feetInches(ys[i + 1] - y), `dy${i}`))}
        {dimV(minY, maxY, minX - 6.5, `Overall ${feetInches(height)}`, "dyall")}

        {/* Scale bar: two blocks of `unit` feet */}
        <g>
          {[0, 1].map((i) => (
            <rect key={i} x={minX + i * unit} y={barY} width={unit} height={tick * 1.2} fill={i % 2 === 0 ? INK : "#fff"} stroke={INK} strokeWidth={thin} />
          ))}
          {[0, 1, 2].map((i) => (
            <text key={i} x={minX + i * unit} y={barY + tick * 1.2 + smallFt * 1.1} fontSize={smallFt} textAnchor="middle" fill={INK}>
              {i * unit}{i === 2 ? " ft" : ""}
            </text>
          ))}
        </g>

        {/* North arrow: north (the road side) is the top of the sheet */}
        <g transform={`translate(${maxX - 1.5} ${maxY + 3.4})`} fill={INK}>
          <polygon points={`0,${-smallFt * 2} ${smallFt * 0.7},0 0,${-smallFt * 0.5} ${-smallFt * 0.7},0`} />
          <text y={smallFt * 1.2} fontSize={smallFt} textAnchor="middle" fontWeight={700}>N</text>
        </g>
      </svg>
      <p className="bf-sheet__scale-note">Scale 1:{scale} when printed on A4 at 100%. Dimensions are between wall lines, in feet and inches.</p>
    </div>
  );
}
