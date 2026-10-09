// A4 landscape with 10 mm margins is 277 x 190 mm. The drawing gets the left part, the schedule the right.
export const SHEET_AREA_WIDTH_MM = 172;
export const SHEET_AREA_HEIGHT_MM = 140;

export const SCALES = [50, 75, 100, 125, 150, 200, 250, 300, 400, 500];

// Drawing scale as 1:n, the smallest n at which `widthFt` x `heightFt` fits the drawing area.
export function chooseScale(widthFt: number, heightFt: number): number {
  const needed = Math.max((widthFt * 304.8) / SHEET_AREA_WIDTH_MM, (heightFt * 304.8) / SHEET_AREA_HEIGHT_MM);
  return SCALES.find((s) => s >= needed) ?? SCALES[SCALES.length - 1];
}

// Feet covered by `mm` millimetres of paper at 1:scale.
export const mmToFt = (mm: number, scale: number): number => (mm * scale) / 304.8;

// 12.5 -> 12′ 6″
export function feetInches(ft: number): string {
  const totalInches = Math.round(ft * 12);
  const f = Math.floor(totalInches / 12);
  const i = totalInches % 12;
  return i === 0 ? `${f}′` : `${f}′ ${i}″`;
}

// Like chooseScale, for any rectangle of paper, in millimetres.
export function chooseScaleIn(widthFt: number, heightFt: number, areaWidthMm: number, areaHeightMm: number): number {
  const needed = Math.max((widthFt * 304.8) / areaWidthMm, (heightFt * 304.8) / areaHeightMm);
  return SCALES.find((s) => s >= needed) ?? SCALES[SCALES.length - 1];
}
