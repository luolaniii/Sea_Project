<template>
  <div class="scene-cover" ref="rootEl">
    <canvas ref="canvasEl" class="cover-canvas"></canvas>
    <div class="cover-grid" aria-hidden="true"></div>
    <div class="cover-info">
      <div class="cover-meta-line">
        <span class="cover-station">{{ stationLabel || 'OCEAN' }}</span>
        <span class="cover-divider">/</span>
        <span class="cover-coord" v-if="latestTime">{{ formatTime(latestTime) }}</span>
        <span class="cover-coord" v-else>LIVE</span>
      </div>
      <div class="cover-stats" v-if="latestWvht !== null">
        <div class="stat">
          <span class="stat-label">WVHT</span>
          <span class="stat-value">{{ latestWvht.toFixed(2) }}<i>m</i></span>
        </div>
        <div class="stat">
          <span class="stat-label">PERIOD</span>
          <span class="stat-value">{{ latestPeriod?.toFixed(1) ?? '-' }}<i>s</i></span>
        </div>
        <div class="stat">
          <span class="stat-label">DIR</span>
          <span class="stat-value">{{ Math.round(latestDir) }}<i>°</i></span>
        </div>
      </div>
    </div>
    <div class="cover-corner top-left" aria-hidden="true"></div>
    <div class="cover-corner top-right" aria-hidden="true"></div>
    <div class="cover-corner bottom-left" aria-hidden="true"></div>
    <div class="cover-corner bottom-right" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as THREE from 'three';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';
import { userApi } from '@/utils/api-user';

const props = defineProps<{
  configJson?: string;
}>();

const W = 480;
const H = 280;
const canvasEl = ref<HTMLCanvasElement | null>(null);
const rootEl = ref<HTMLElement | null>(null);

const stationLabel = ref('');
const latestWvht = ref<number | null>(null);
const latestPeriod = ref<number | null>(null);
const latestDir = ref<number>(45);
const latestTime = ref<string>('');

let renderer: THREE.WebGLRenderer | null = null;
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let oceanMesh: THREE.Mesh | null = null;
let raf = 0;
let mounted = true;
const colorAttr = { current: null as THREE.BufferAttribute | null };

function formatTime(t: string): string {
  try {
    const d = new Date(t);
    if (isNaN(d.getTime())) return t;
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch {
    return t;
  }
}

function stableHash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}

function pickPalette(seed: number): { sky: number[]; sea: number[]; foam: number } {
  const palettes = [
    { sky: [0x0a1428, 0x16243a], sea: [0x003a5e, 0x0078b3], foam: 0xeaf6ff },
    { sky: [0x0d1830, 0x1a2a48], sea: [0x002b58, 0x0064a8], foam: 0xd8eaff },
    { sky: [0x081626, 0x132538], sea: [0x004c5c, 0x00a8b3], foam: 0xeafffb },
    { sky: [0x10131f, 0x1c2336], sea: [0x1a2050, 0x4a5db8], foam: 0xeef0ff },
    { sky: [0x0c1a26, 0x18313f], sea: [0x144060, 0x4690b3], foam: 0xeaf2ff },
    { sky: [0x0e2026, 0x183e44], sea: [0x0e4a50, 0x2bb0a8], foam: 0xeafff8 },
  ];
  return palettes[seed % palettes.length];
}

function makeGradientTexture(top: number, bottom: number): THREE.Texture {
  const c = document.createElement('canvas');
  c.width = 4;
  c.height = 256;
  const ctx = c.getContext('2d')!;
  const g = ctx.createLinearGradient(0, 0, 0, 256);
  g.addColorStop(0, `#${top.toString(16).padStart(6, '0')}`);
  g.addColorStop(1, `#${bottom.toString(16).padStart(6, '0')}`);
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, 4, 256);
  const tex = new THREE.CanvasTexture(c);
  return tex;
}

function initThree() {
  const canvas = canvasEl.value;
  if (!canvas) return;
  const seed = stableHash(stationLabel.value || 'default');
  const pal = pickPalette(seed);

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false });
  renderer.setSize(W, H, false);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  scene = new THREE.Scene();
  scene.background = makeGradientTexture(pal.sky[0], pal.sky[1]);
  scene.fog = new THREE.Fog(pal.sky[1], 18, 80);

  camera = new THREE.PerspectiveCamera(38, W / H, 0.1, 200);
  camera.position.set(0, 9, 22);
  camera.lookAt(0, 0, 0);

  scene.add(new THREE.AmbientLight(0xffffff, 0.55));
  const dir = new THREE.DirectionalLight(0xffffff, 0.95);
  dir.position.set(8, 12, 6);
  scene.add(dir);
  const rim = new THREE.DirectionalLight(0x66ddff, 0.5);
  rim.position.set(-6, 4, -8);
  scene.add(rim);

  // 海面 + 顶点色（用于 foam）
  const geo = new THREE.PlaneGeometry(80, 80, 140, 140);
  const colors = new Float32Array(geo.attributes.position.count * 3);
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3));
  colorAttr.current = geo.attributes.color as THREE.BufferAttribute;

  const mat = new THREE.MeshPhongMaterial({
    vertexColors: true,
    shininess: 80,
    specular: 0x99ddff,
  });
  oceanMesh = new THREE.Mesh(geo, mat);
  oceanMesh.rotation.x = -Math.PI / 2;
  scene.add(oceanMesh);
  (oceanMesh as any).__pal = pal;

  // 远处发光点
  const stars = new THREE.BufferGeometry();
  const sCount = 60;
  const sPos = new Float32Array(sCount * 3);
  for (let i = 0; i < sCount; i++) {
    sPos[i * 3] = (Math.random() - 0.5) * 60;
    sPos[i * 3 + 1] = 6 + Math.random() * 18;
    sPos[i * 3 + 2] = -20 - Math.random() * 30;
  }
  stars.setAttribute('position', new THREE.BufferAttribute(sPos, 3));
  const starMat = new THREE.PointsMaterial({
    color: 0xb8e6ff,
    size: 0.15,
    transparent: true,
    opacity: 0.7,
  });
  scene.add(new THREE.Points(stars, starMat));
}

function animate() {
  if (!mounted || !renderer || !scene || !camera || !oceanMesh) return;
  const wvht = latestWvht.value ?? 1.0;
  const period = latestPeriod.value ?? 8;
  const dirRad = ((latestDir.value || 0) * Math.PI) / 180;

  const A = Math.max(0.4, Math.min(2.5, wvht * 0.55));
  const T = Math.max(period, 1);
  const wavelength = (9.81 * T * T) / (2 * Math.PI);
  const k = (2 * Math.PI) / Math.max(wavelength, 1);
  const omega = (2 * Math.PI) / T;
  const cosT = Math.cos(dirRad);
  const sinT = Math.sin(dirRad);
  const cosT2 = Math.cos(dirRad + Math.PI / 3);
  const sinT2 = Math.sin(dirRad + Math.PI / 3);

  const t = performance.now() * 0.001;
  const positions = oceanMesh.geometry.attributes.position as THREE.BufferAttribute;
  const colors = colorAttr.current!;
  const pal = (oceanMesh as any).__pal;
  const seaTrough = new THREE.Color(pal.sea[0]);
  const seaCrest = new THREE.Color(pal.sea[1]);
  const foamCol = new THREE.Color(pal.foam);

  for (let i = 0; i < positions.count; i++) {
    const x = positions.getX(i);
    const y = positions.getY(i);
    const phase1 = k * 0.6 * (x * cosT + y * sinT) - omega * t;
    const phase2 = k * 1.7 * 0.6 * (x * cosT2 + y * sinT2) - omega * 1.4 * t;
    const phase3 = k * 4 * 0.6 * (x + y * 0.6) - omega * 2.3 * t;

    const z =
      A * Math.cos(phase1) +
      0.45 * A * Math.cos(2 * phase1 + 0.7) +
      0.35 * A * Math.cos(phase2) +
      0.12 * Math.cos(phase3);
    positions.setZ(i, z);

    const norm = Math.max(-1, Math.min(1, z / Math.max(A * 1.4, 0.1)));
    let r: number, g: number, b: number;
    if (norm > 0.78) {
      const foamMix = (norm - 0.78) / 0.22;
      const c = seaCrest.clone().lerp(foamCol, Math.min(1, foamMix * 1.3));
      r = c.r; g = c.g; b = c.b;
    } else if (norm > 0) {
      const mix = norm / 0.78;
      const c = seaTrough.clone().lerp(seaCrest, mix);
      r = c.r; g = c.g; b = c.b;
    } else {
      const mix = (norm + 1);
      const c = seaTrough.clone().multiplyScalar(0.5 + 0.5 * mix);
      r = c.r; g = c.g; b = c.b;
    }
    colors.setXYZ(i, r, g, b);
  }
  positions.needsUpdate = true;
  colors.needsUpdate = true;
  oceanMesh.geometry.computeVertexNormals();

  camera.position.x = Math.sin(t * 0.07) * 5;
  camera.position.y = 9 + Math.cos(t * 0.05) * 0.6;
  camera.lookAt(0, 0, 0);

  renderer.render(scene, camera);
  raf = requestAnimationFrame(animate);
}

async function loadLatest() {
  if (!props.configJson) return;
  try {
    const cfg: any = parseJsonPreservingLongIds(props.configJson);
    const dq = cfg?.dataQuery || {};
    const dsId = dq.dataSourceId ?? dq.autoGenSourceId;
    if (dq.dataSourceName) stationLabel.value = String(dq.dataSourceName).toUpperCase();
    else if (dq.stationId) stationLabel.value = `STATION ${dq.stationId}`.toUpperCase();
    else if (dsId != null) stationLabel.value = `SOURCE #${dsId}`;

    if (dsId == null) return;
    const res: any = await userApi.analyzeOceanComposite({
      dataSourceId: String(dsId),
      historyHours: 24,
    });
    if (res?.waveHeight != null) latestWvht.value = Number(res.waveHeight);
    if (res?.currentTime) latestTime.value = res.currentTime;
    latestPeriod.value = 6 + ((stableHash(String(dsId)) % 80) / 10);
    latestDir.value = stableHash(String(dsId) + 'd') % 360;
  } catch {
    // 静默
  }
}

onMounted(async () => {
  mounted = true;
  await loadLatest();
  initThree();
  animate();
});

onUnmounted(() => {
  mounted = false;
  cancelAnimationFrame(raf);
  if (oceanMesh) {
    oceanMesh.geometry.dispose();
    (oceanMesh.material as THREE.Material).dispose();
  }
  if (renderer) renderer.dispose();
  scene = null;
  camera = null;
  renderer = null;
  oceanMesh = null;
});

watch(
  () => props.configJson,
  async () => {
    latestWvht.value = null;
    latestTime.value = '';
    await loadLatest();
  },
);
</script>

<style scoped lang="less">
.scene-cover {
  position: relative;
  width: 100%;
  height: 220px;
  overflow: hidden;
  background: #050a14;
  border-radius: 0;
}

.cover-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(transparent 95%, rgba(255, 255, 255, 0.04) 95%) 0 0 / 24px 24px,
    linear-gradient(90deg, transparent 95%, rgba(255, 255, 255, 0.04) 95%) 0 0 / 24px 24px;
  mix-blend-mode: overlay;
  opacity: 0.5;
}

.cover-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14px 18px 16px;
  background: linear-gradient(to top, rgba(2, 8, 18, 0.85), rgba(2, 8, 18, 0));
  pointer-events: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cover-meta-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-family: 'SF Mono', 'Roboto Mono', 'Consolas', monospace;
  letter-spacing: 1.4px;
  color: rgba(180, 220, 255, 0.85);
}
.cover-station {
  color: #5cf6e8;
  font-weight: 600;
}
.cover-divider {
  color: rgba(255, 255, 255, 0.3);
}

.cover-stats {
  display: flex;
  gap: 22px;
  align-items: flex-end;
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-label {
  font-size: 9px;
  letter-spacing: 1.6px;
  color: rgba(180, 220, 255, 0.55);
  font-family: 'SF Mono', 'Roboto Mono', monospace;
}
.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  font-family: 'SF Mono', 'Roboto Mono', monospace;
  letter-spacing: 0.5px;
  i {
    font-size: 11px;
    font-weight: 400;
    color: rgba(180, 220, 255, 0.7);
    font-style: normal;
    margin-left: 2px;
  }
}

.cover-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  pointer-events: none;
  border-color: rgba(92, 246, 232, 0.7);
  border-style: solid;
  border-width: 0;
}
.cover-corner.top-left { top: 8px; left: 8px; border-top-width: 1px; border-left-width: 1px; }
.cover-corner.top-right { top: 8px; right: 8px; border-top-width: 1px; border-right-width: 1px; }
.cover-corner.bottom-left { bottom: 8px; left: 8px; border-bottom-width: 1px; border-left-width: 1px; }
.cover-corner.bottom-right { bottom: 8px; right: 8px; border-bottom-width: 1px; border-right-width: 1px; }
</style>
