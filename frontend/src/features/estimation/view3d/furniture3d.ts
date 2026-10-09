import * as THREE from "three";
import type { PlanFurniture } from "../../../types/plan";
import { furnitureSpec } from "../furniture";

// Local frame of every piece: x runs along its width, z from back (-z) to front (+z), y up. The root is
// positioned at the piece's centre on the floor and turned about y by -rotationDeg.
export function buildFurnitureMesh(item: PlanFurniture): THREE.Group {
  const root = new THREE.Group();
  const colour = item.colour ?? furnitureSpec(item.kind).colour;
  const w = item.widthFt;
  const d = item.depthFt;
  const h = item.heightFt;
  const mats = new Map<string, THREE.MeshLambertMaterial>();
  const part = (pw: number, pd: number, ph: number, x: number, y: number, z: number, c: string) => {
    let m = mats.get(c);
    if (!m) {
      m = new THREE.MeshLambertMaterial({ color: c });
      mats.set(c, m);
    }
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(Math.max(pw, 0.02), Math.max(ph, 0.02), Math.max(pd, 0.02)), m);
    mesh.position.set(x, y + ph / 2, z);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    root.add(mesh);
    return mesh;
  };

  switch (item.kind) {
    case "BED": {
      const baseH = h * 0.55;
      part(w, d, baseH, 0, 0, 0, colour);
      part(w - 0.2, d - 0.4, h - baseH, 0, baseH, 0.1, "#e6e0f0");
      part(w * 0.38, 1.1, 0.35, -w * 0.22, h, -d / 2 + 1.1, "#ffffff");
      part(w * 0.38, 1.1, 0.35, w * 0.22, h, -d / 2 + 1.1, "#ffffff");
      part(w, 0.3, h * 2.1, 0, 0, -d / 2 + 0.15, darker(colour));
      break;
    }
    case "SOFA": {
      part(w, d, h * 0.4, 0, 0, 0, colour);
      part(w, 0.8, h, 0, 0, -d / 2 + 0.4, colour);
      part(0.7, d, h * 0.7, -w / 2 + 0.35, 0, 0, darker(colour));
      part(0.7, d, h * 0.7, w / 2 - 0.35, 0, 0, darker(colour));
      break;
    }
    case "TABLE":
    case "DESK": {
      const top = Math.min(0.2, h * 0.15);
      part(w, d, top, 0, h - top, 0, colour);
      for (const sx of [-1, 1]) {
        for (const sz of [-1, 1]) part(0.25, 0.25, h - top, sx * (w / 2 - 0.2), 0, sz * (d / 2 - 0.2), darker(colour));
      }
      break;
    }
    case "CHAIR": {
      part(w, d, h * 0.16, 0, h * 0.28, 0, colour);
      part(w, 0.2, h * 0.55, 0, h * 0.44, -d / 2 + 0.1, colour);
      for (const sx of [-1, 1]) {
        for (const sz of [-1, 1]) part(0.15, 0.15, h * 0.28, sx * (w / 2 - 0.1), 0, sz * (d / 2 - 0.1), darker(colour));
      }
      break;
    }
    case "TOILET": {
      part(w * 0.85, d * 0.45, h * 0.45, 0, 0, d * 0.22, colour);
      part(w, d * 0.28, h * 0.9, 0, 0, -d / 2 + d * 0.14, colour);
      break;
    }
    case "BASIN": {
      part(w * 0.4, d * 0.5, h * 0.72, 0, 0, 0, colour);
      part(w, d, 0.3, 0, h * 0.72, 0, colour);
      break;
    }
    case "CAR": {
      part(w, d, h * 0.45, 0, 0.5, 0, colour);
      part(w * 0.86, d * 0.42, h * 0.45, 0, 0.5 + h * 0.45, d * 0.04, lighter(colour));
      part(w * 0.8, d * 0.38, h * 0.32, 0, 0.55 + h * 0.45, d * 0.04, "#9bc9e8");
      for (const sx of [-1, 1]) {
        for (const sz of [-1, 1]) part(0.45, 1.5, 1, sx * (w / 2 - 0.2), 0, sz * (d * 0.32), "#222222");
      }
      break;
    }
    case "PLANT": {
      part(w * 0.7, d * 0.7, h * 0.28, 0, 0, 0, "#8a5a2b");
      const crown = new THREE.Mesh(new THREE.SphereGeometry(Math.max(w, d) * 0.55, 10, 8),
        new THREE.MeshLambertMaterial({ color: colour }));
      crown.position.set(0, h * 0.6, 0);
      crown.castShadow = true;
      root.add(crown);
      break;
    }
    case "TREE": {
      part(0.7, 0.7, h * 0.4, 0, 0, 0, "#7a5230");
      const crown = new THREE.Mesh(new THREE.SphereGeometry(Math.max(w, d) * 0.5, 12, 10), new THREE.MeshLambertMaterial({ color: colour }));
      crown.position.set(0, h * 0.68, 0);
      crown.scale.y = (h * 0.6) / Math.max(w, d);
      crown.castShadow = true;
      root.add(crown);
      break;
    }
    case "SHRUB": {
      const bush = new THREE.Mesh(new THREE.SphereGeometry(Math.max(w, d) * 0.5, 10, 8), new THREE.MeshLambertMaterial({ color: colour }));
      bush.position.set(0, h / 2, 0);
      bush.scale.y = h / Math.max(w, d);
      bush.castShadow = true;
      root.add(bush);
      break;
    }
    case "FLOWER_BED": {
      part(w, d, h * 0.6, 0, 0, 0, "#6b4a2b");
      for (let i = 0; i < Math.max(3, Math.round(w / 1.1)); i++) {
        part(0.5, 0.5, 0.5, -w / 2 + 0.6 + i * 1.1, h * 0.6, ((i % 2) - 0.5) * d * 0.4, i % 2 ? colour : "#f2c14e");
      }
      break;
    }
    case "BENCH": {
      part(w, d, 0.2, 0, h * 0.6, 0, colour);
      part(w, 0.2, h * 0.5, 0, h * 0.7, -d / 2 + 0.1, colour);
      for (const sx of [-1, 1]) part(0.25, d, h * 0.6, sx * (w / 2 - 0.3), 0, 0, darker(colour));
      break;
    }
    case "SHRINE": {
      part(w, d, h, 0, 0, 0, colour);
      part(w - 0.4, d - 0.4, 0.3, 0, h, 0, "#d4af37");
      break;
    }
    default:
      // WARDROBE, COUNTER, FRIDGE, TV_UNIT, RUG: one block.
      part(w, d, h, 0, 0, 0, colour);
      break;
  }
  return root;
}

// Place a built piece at its position and turn (the back faces north before rotation).
export function placeFurniture(root: THREE.Object3D, item: PlanFurniture, floorY: number): void {
  root.position.set(item.x, floorY, item.y);
  root.rotation.y = (-item.rotationDeg * Math.PI) / 180;
}

export function selectionOutline(item: PlanFurniture): THREE.LineSegments {
  const geometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(item.widthFt + 0.3, Math.max(item.heightFt, 0.6) + 0.3, item.depthFt + 0.3));
  const outline = new THREE.LineSegments(geometry, new THREE.LineBasicMaterial({ color: "#ffb300", depthTest: false }));
  outline.position.y = Math.max(item.heightFt, 0.6) / 2;
  outline.renderOrder = 10;
  return outline;
}

function shade(hex: string, factor: number): string {
  const c = new THREE.Color(hex);
  c.multiplyScalar(factor);
  return `#${c.getHexString()}`;
}
const darker = (hex: string) => shade(hex, 0.75);
const lighter = (hex: string) => shade(hex, 1.2);
