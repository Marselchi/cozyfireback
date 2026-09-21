"use client";
import { Canvas, useThree } from "@react-three/fiber";
import { memo, useEffect, useMemo, useRef } from "react";
import gsap from "gsap";
import * as THREE from "three";

// ============================================================================
// Icosahedron geometry data (matches THREE.IcosahedronGeometry internals)
// ============================================================================
const PHI = (1 + Math.sqrt(5)) / 2;
const RAW_VERTS: [number, number, number][] = [
  [-1, PHI, 0],
  [1, PHI, 0],
  [-1, -PHI, 0],
  [1, -PHI, 0],
  [0, -1, PHI],
  [0, 1, PHI],
  [0, -1, -PHI],
  [0, 1, -PHI],
  [PHI, 0, -1],
  [PHI, 0, 1],
  [-PHI, 0, -1],
  [-PHI, 0, 1],
];
const RAW_FACES: [number, number, number][] = [
  [0, 11, 5],
  [0, 5, 1],
  [0, 1, 7],
  [0, 7, 10],
  [0, 10, 11],
  [1, 5, 9],
  [5, 11, 4],
  [11, 10, 2],
  [10, 7, 6],
  [7, 1, 8],
  [3, 9, 4],
  [3, 4, 2],
  [3, 2, 6],
  [3, 6, 8],
  [3, 8, 9],
  [4, 9, 5],
  [2, 4, 11],
  [6, 2, 10],
  [8, 6, 7],
  [9, 8, 1],
];
const RADIUS = 1.28;
const AXIS_Z = new THREE.Vector3(0, 0, 1);
const AXIS_Y = new THREE.Vector3(0, 1, 0);

const BODY_COLOR = "#a78bfa";
const NUMBER_COLOR = "#6d28d9";

// Atlas layout: 20 numbers packed into a 5x4 grid.
const ATLAS_COLS = 5;
const ATLAS_ROWS = 4;
const CELL_PX = 128; // per-cell resolution; total atlas is 640x512

interface FaceData {
  v0: THREE.Vector3;
  v1: THREE.Vector3;
  v2: THREE.Vector3;
  centroid: THREE.Vector3;
  normal: THREE.Vector3;
  refDir: THREE.Vector3; // in-plane "up" reference, used for both text rotation (legacy) and UV basis
  number: number;
}

function quaternionFromTo(
  fromAxis: THREE.Vector3,
  toAxis: THREE.Vector3,
  fromRef: THREE.Vector3,
  toRef: THREE.Vector3,
) {
  const base = new THREE.Quaternion().setFromUnitVectors(
    fromAxis.clone().normalize(),
    toAxis.clone().normalize(),
  );
  const rotatedRef = fromRef.clone().applyQuaternion(base);
  const cross = new THREE.Vector3().crossVectors(rotatedRef, toRef);
  const dot = THREE.MathUtils.clamp(rotatedRef.dot(toRef), -1, 1);
  const angle = Math.atan2(cross.dot(toAxis), dot);
  const twist = new THREE.Quaternion().setFromAxisAngle(
    toAxis.clone().normalize(),
    angle,
  );
  return twist.multiply(base);
}

function buildFaces(): FaceData[] {
  const verts = RAW_VERTS.map(([x, y, z]) =>
    new THREE.Vector3(x, y, z).setLength(RADIUS),
  );
  const faces: FaceData[] = RAW_FACES.map(([a, b, c]) => {
    let v0 = verts[a],
      v1 = verts[b],
      v2 = verts[c];
    const centroid = v0.clone().add(v1).add(v2).divideScalar(3);
    const normal = centroid.clone().normalize();

    // Ensure winding matches outward normal (needed now that we build real
    // triangle geometry, not just placing text on top of an existing mesh).
    const windingNormal = new THREE.Vector3()
      .crossVectors(v1.clone().sub(v0), v2.clone().sub(v0))
      .normalize();
    if (windingNormal.dot(normal) < 0) {
      const tmp = v1;
      v1 = v2;
      v2 = tmp;
    }

    const refDir = v0.clone().sub(centroid).normalize();
    return { v0, v1, v2, centroid, normal, refDir, number: 0 };
  });

  // Assign 1..20 so opposite faces sum to 21 (standard d20 convention)
  const assigned = new Array(faces.length).fill(false);
  let pair = 0;
  for (let i = 0; i < faces.length; i++) {
    if (assigned[i]) continue;
    let bestJ = -1,
      bestDot = Infinity;
    for (let j = 0; j < faces.length; j++) {
      if (j === i || assigned[j]) continue;
      const d = faces[i].normal.dot(faces[j].normal);
      if (d < bestDot) {
        bestDot = d;
        bestJ = j;
      }
    }
    faces[i].number = pair + 1;
    faces[bestJ].number = 20 - pair;
    assigned[i] = true;
    assigned[bestJ] = true;
    pair++;
  }
  return faces;
}

// ============================================================================
// Texture atlas: bake all 20 numbers (+ body color background) into one canvas
// ============================================================================
function buildAtlasTexture(faces: FaceData[]): THREE.CanvasTexture {
  const canvas = document.createElement("canvas");
  canvas.width = ATLAS_COLS * CELL_PX;
  canvas.height = ATLAS_ROWS * CELL_PX;
  const ctx = canvas.getContext("2d")!;

  // Uniform background across the whole atlas: any UV bleed at cell seams
  // (from mipmapping/filtering) is invisible since it's the same color everywhere.
  ctx.fillStyle = BODY_COLOR;
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  ctx.fillStyle = NUMBER_COLOR;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.font = `600 ${Math.floor(CELL_PX * 0.3)}px ui-sans-serif, system-ui, sans-serif`;

  faces.forEach((f, i) => {
    const col = i % ATLAS_COLS;
    const row = Math.floor(i / ATLAS_COLS);
    const cx = col * CELL_PX + CELL_PX / 2;
    const cy = row * CELL_PX + CELL_PX / 2;
    ctx.fillText(String(f.number), cx, cy);
  });

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.generateMipmaps = false; // avoids any residual bleed risk at cell edges
  texture.minFilter = THREE.LinearFilter;
  texture.magFilter = THREE.LinearFilter;
  texture.needsUpdate = true;
  return texture;
}

// ============================================================================
// Build ONE non-indexed BufferGeometry for all 20 faces, with per-face UVs
// mapping into the atlas — this is what collapses 20 Text draw calls into 0.
// ============================================================================
function buildDieGeometry(faces: FaceData[]): THREE.BufferGeometry {
  const positions = new Float32Array(faces.length * 3 * 3);
  const uvs = new Float32Array(faces.length * 3 * 2);

  // First pass: find the max in-plane extent across all vertices, so every
  // face's triangle is scaled consistently into its atlas cell.
  let maxExtent = 0;
  faces.forEach((f) => {
    const rightAxis = f.refDir.clone().cross(f.normal).normalize();
    const upAxis = f.refDir;
    for (const v of [f.v0, f.v1, f.v2]) {
      const rel = v.clone().sub(f.centroid);
      maxExtent = Math.max(
        maxExtent,
        Math.abs(rel.dot(rightAxis)),
        Math.abs(rel.dot(upAxis)),
      );
    }
  });

  const padding = 0.88; // shrink factor so triangle stays inside its cell with margin

  faces.forEach((f, i) => {
    const rightAxis = f.refDir.clone().cross(f.normal).normalize();
    const upAxis = f.refDir;
    const col = i % ATLAS_COLS;
    const row = Math.floor(i / ATLAS_COLS);
    const uCenter = (col + 0.5) / ATLAS_COLS;
    const vCenter = 1 - (row + 0.5) / ATLAS_ROWS; // flip: canvas row 0 = atlas top = V near 1

    [f.v0, f.v1, f.v2].forEach((v, vi) => {
      const pIdx = (i * 3 + vi) * 3;
      positions[pIdx] = v.x;
      positions[pIdx + 1] = v.y;
      positions[pIdx + 2] = v.z;

      const rel = v.clone().sub(f.centroid);
      const nx = (rel.dot(rightAxis) / maxExtent) * padding;
      const ny = (rel.dot(upAxis) / maxExtent) * padding;

      const uIdx = (i * 3 + vi) * 2;
      uvs[uIdx] = uCenter + (nx * 0.5) / ATLAS_COLS;
      uvs[uIdx + 1] = vCenter + (ny * 0.5) / ATLAS_ROWS;
    });
  });

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute("uv", new THREE.BufferAttribute(uvs, 2));
  geometry.computeVertexNormals(); // non-indexed + unique verts per face => proper flat shading
  return geometry;
}

// Module-level singletons: computed once per app load, not per render/mount.
const FACES = buildFaces();
const EDGES_GEOMETRY = new THREE.EdgesGeometry(
  new THREE.IcosahedronGeometry(RADIUS, 0),
);

function eulerForValue(value: number) {
  const face = FACES.find((f) => f.number === value) ?? FACES[0];
  const quat = quaternionFromTo(face.normal, AXIS_Z, face.refDir, AXIS_Y);
  return new THREE.Euler().setFromQuaternion(quat, "XYZ");
}

const TWO_PI = Math.PI * 2;

const WAIT_DURATION = 2.5;
const WAIT_DELTA = { x: 6.28, y: 9.42, z: 3.14 }; // must match the values above

// Cubic Hermite: p0=0, p1=1, m1=0 (comes to rest), m0=initial slope (matched velocity)
function velocityMatchedEase(m0: number) {
  return (t: number) => {
    const t2 = t * t;
    const t3 = t2 * t;
    const h10 = t3 - 2 * t2 + t; // weights m0
    const h01 = -2 * t3 + 3 * t2; // weights p1 (=1)
    return h10 * m0 + h01;
  };
}

function axisEase(
  deltaRotation: number,
  landDuration: number,
  axis: "x" | "y" | "z",
) {
  const velocity = WAIT_DELTA[axis] / WAIT_DURATION; // rad/s while waiting
  // normalized slope: how far this axis would travel at wait-speed
  // relative to how far it actually needs to travel, scaled by duration
  const m0 = (velocity * landDuration) / deltaRotation;
  return velocityMatchedEase(m0);
}

// Finds the smallest angle >= `current` that's congruent to `targetMod`
// (mod 2π), then adds a few extra full turns so the landing keeps spinning
// forward in the same direction instead of snapping backward or freezing.
function landingAngle(current: number, targetMod: number, extraTurns: number) {
  const k = Math.ceil((current - targetMod) / TWO_PI) + extraTurns;
  return targetMod + k * TWO_PI;
}

function Die({
  rolling,
  value,
  onSettled,
}: Readonly<{
  rolling: boolean;
  value?: number;
  onSettled?: () => void;
}>) {
  const spinGroup = useRef<THREE.Group>(null);
  const { invalidate } = useThree();

  // Built once on mount, reused across every roll (not per-value).
  const dieGeometry = useMemo(() => buildDieGeometry(FACES), []);
  const atlasTexture = useMemo(() => buildAtlasTexture(FACES), []);
  useEffect(() => () => atlasTexture.dispose(), [atlasTexture]);
  useEffect(() => () => dieGeometry.dispose(), [dieGeometry]);

  const waitTweenRef = useRef<gsap.core.Tween | null>(null);
  const landTweensRef = useRef<gsap.core.Tween[]>([]);
  const landedValueRef = useRef<number | null>(null);

  useEffect(() => {
    const g = spinGroup.current;
    if (!g) return;

    if (!rolling) {
      // Idle, preview, or already-settled: show the resolved value statically.
      waitTweenRef.current?.kill();
      landTweensRef.current.forEach((t) => t.kill());
      waitTweenRef.current = null;
      landTweensRef.current = [];
      landedValueRef.current = null;
      const e = eulerForValue(value ?? 20);
      g.rotation.set(e.x, e.y, e.z);
      invalidate();
      return;
    }

    if (value === undefined) {
      // Rolling, but the backend hasn't responded yet: spin with no fixed
      // destination. Relative "+=" deltas with repeat:-1 keep accumulating
      // each cycle instead of resetting, so the motion never stutters.
      waitTweenRef.current ??= gsap.to(g.rotation, {
        x: "+=6.28",
        y: "+=9.42",
        z: "+=3.14",
        duration: 2.5,
        ease: "none",
        repeat: -1,
        repeatRefresh: true, // <-- continues accumulating instead of resetting each cycle
        onUpdate: invalidate,
      });
      return;
    }

    // The roll just came back (or we're re-entering with the same value —
    // guard below prevents restarting a landing tween that's already running).
    if (landedValueRef.current === value) return;
    landedValueRef.current = value;

    waitTweenRef.current?.kill(); // stops in place, no jump — this IS the handoff
    waitTweenRef.current = null;
    landTweensRef.current.forEach((t) => t.kill());

    const target = eulerForValue(value);
    const finalX = landingAngle(g.rotation.x, target.x, 2);
    const finalY = landingAngle(g.rotation.y, target.y, 3);
    const finalZ = landingAngle(g.rotation.z, target.z, 2);

    const landDuration = 3.5;

    const tx = gsap.to(g.rotation, {
      x: finalX,
      duration: landDuration,
      ease: axisEase(finalX - g.rotation.x, landDuration, "x"),
      onUpdate: invalidate,
    });
    const ty = gsap.to(g.rotation, {
      y: finalY,
      duration: landDuration,
      ease: axisEase(finalY - g.rotation.y, landDuration, "y"),
      onUpdate: invalidate,
    });
    const tz = gsap.to(g.rotation, {
      z: finalZ,
      duration: landDuration,
      ease: axisEase(finalZ - g.rotation.z, landDuration, "z"),
      onUpdate: invalidate,
      onComplete: () => onSettled?.(), // fires once, all three share duration so this lands last
    });

    landTweensRef.current = [tx, ty, tz];
  }, [rolling, value, invalidate, onSettled]);

  useEffect(() => {
    return () => {
      waitTweenRef.current?.kill();
      landTweensRef.current.forEach((t) => t.kill());
    };
  }, []);

  return (
    <group ref={spinGroup}>
      <mesh geometry={dieGeometry}>
        <meshStandardMaterial
          map={atlasTexture}
          roughness={0.3} //NOSONAR
          metalness={0.38}
        />
      </mesh>
      <lineSegments scale={1.018} geometry={EDGES_GEOMETRY}>
        <lineBasicMaterial color="#2e1065" transparent opacity={0.9} />
      </lineSegments>
    </group>
  );
}

const MemoDie = memo(Die);

// Stable references so <Canvas> never sees a "new" object identity on re-render.
const CAMERA = { position: [0, 0, 4.3] as [number, number, number], fov: 34 };
const GL_PROPS = { antialias: true, powerPreference: "low-power" as const };

export function D20Canvas({
  rolling,
  value,
  onSettled,
}: Readonly<{
  rolling: boolean;
  /** Leave undefined while a roll's result is still pending — the die will
   *  spin indefinitely instead of animating toward a face. */
  value?: number;
  /** Fires once the landing animation finishes settling on `value`. */
  onSettled?: () => void;
}>) {
  return (
    <div
      className="relative size-40"
      aria-label="Three-dimensional twenty-sided die"
    >
      <Canvas camera={CAMERA} dpr={[1, 1.5]} gl={GL_PROPS} frameloop="demand">
        <ambientLight intensity={1.7} />
        <directionalLight
          position={[3, 4, 5]}
          intensity={3.2}
          color="#fff0c2"
        />
        <pointLight position={[-3, -2, 2]} intensity={12} color="#8b5cf6" />
        <MemoDie rolling={rolling} value={value} onSettled={onSettled} />
      </Canvas>
    </div>
  );
}

export function D20Preview() {
  return <D20Canvas rolling={false} value={20} />;
}

export default D20Canvas;
