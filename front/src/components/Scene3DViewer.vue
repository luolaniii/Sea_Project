<template>
  <div class="scene-3d-viewer-root">
    <div ref="containerRef" class="scene-3d-viewer"></div>
    <div v-if="showSourceBadge && sourceBadge" class="source-badge">
      <div class="badge-title">
        <span class="badge-mark"><Icon name="wave" :size="14" color="#00e5ff" /></span>
        {{ sourceBadge.title }}
      </div>
      <div class="badge-line">
        <span class="badge-label">站点：</span>
        <span class="badge-value">{{ sourceBadge.sourceLabel }}</span>
      </div>
      <div v-if="sourceBadge.lastObs" class="badge-line">
        <span class="badge-label">观测时刻：</span>
        <span class="badge-value">{{ sourceBadge.lastObs }}</span>
      </div>
      <div v-if="sourceBadge.waveInfo" class="badge-line wave-info">
        <span class="badge-label">波面参数：</span>
        <span class="badge-value">{{ sourceBadge.waveInfo }}</span>
      </div>
      <div class="badge-line confidence" :class="'cf-' + (sourceBadge.confidence || 'real')">
        <span class="badge-label">数据置信度：</span>
        <span class="badge-value">{{ confidenceLabel(sourceBadge.confidence) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { parseObservationTimeToMs } from '@/utils/observation-time';
import { parseJsonPreservingLongIds } from '@/utils/json-parse-ids';
import Icon from '@/components/Icon.vue';

interface Props {
  configJson?: string;
  data?: any[];
  replayEndTime?: string | null;
  sourceLabel?: string;
  showSourceBadge?: boolean;
}

const props = withDefaults(defineProps<Props>(), { showSourceBadge: true });

// ---------------- 业务逻辑保持不变（适配后端） START ----------------
const sourceBadge = ref<any>(null);
const confidenceLabel = (c?: string) => {
  switch (c) {
    case 'real': return '实测';
    case 'estimated': return '公式估算';
    case 'interpolated': return '空间插值';
    case 'default': return '气候默认值';
    default: return '—';
  }
};

const containerRef = ref<HTMLDivElement>();
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let renderer: THREE.WebGLRenderer | null = null;
let controls: OrbitControls | null = null;
let animationId: number | null = null;
let oceanMesh: THREE.Mesh | null = null;
let layerGroup: THREE.Group | null = null;
let currentConfig: any = null;

const G = 9.81;
const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v));
const parseTimeMs = (t?: unknown) => {
  const ms = parseObservationTimeToMs(t ?? '');
  return Number.isNaN(ms) ? -1 : ms;
};

const pickLatest = (list?: any[]) => {
  if (!list || list.length === 0) return null;
  let best: any = null;
  let bestT = -1;
  for (const d of list) {
    const t = parseTimeMs(d.observationTime);
    if (t >= bestT) { bestT = t; best = d; }
  }
  return best;
};

const extractWaveParams = (dataByCode: Record<string, any[]>) => {
  const wvhtObs = pickLatest(dataByCode['WAVE_HEIGHT']) || pickLatest(dataByCode['WAVE']);
  const wspdObs = pickLatest(dataByCode['WIND_SPEED']);
  const dpdObs = pickLatest(dataByCode['WAVE_PERIOD']);
  const mwdObs = pickLatest(dataByCode['WAVE_DIRECTION']);

  let hs = 1.5, T = 7, confidence = 'default';
  const wvht = wvhtObs?.dataValue != null ? Number(wvhtObs.dataValue) : null;
  if (wvht != null && wvht > 0) {
    hs = wvht; T = Number(dpdObs?.dataValue) || 6; confidence = 'real';
  } else if (wspdObs?.dataValue != null) {
    const u = Number(wspdObs.dataValue);
    hs = 0.21 * (u * u) / G; T = 7.14 * u / G; confidence = 'estimated';
  }

  hs = clamp(hs, 0, 20); T = clamp(T, 2, 25);
  return {
    amplitude: clamp((hs / 2) * 1.2, 0.4, 6.0),
    physicalAmpM: hs / 2, period: T,
    wavelength: (G * T * T) / (2 * Math.PI),
    directionRad: (Number(mwdObs?.dataValue) || 0) * Math.PI / 180,
    confidence, rawWVHT: wvht, rawDPD: dpdObs?.dataValue, rawMWD: mwdObs?.dataValue
  };
};

let currentWaveParams: any = { amplitude: 1, period: 6, wavelength: 100, directionRad: 0 };

const formatWaveInfo = (wp: any) => {
  const hs = wp.rawWVHT != null ? wp.rawWVHT : (wp.physicalAmpM * 2);
  return `Hs=${hs.toFixed(2)}m · T=${wp.period.toFixed(1)}s · MWD=${(wp.rawMWD || 0).toFixed(0)}°`;
};

// [此处省略复杂的 renderLayers 方法内部实现，建议粘贴您原有的 renderLayers 及其辅助函数以确保图层功能正常]
const renderLayers = (config: any, data: any[]) => {   const addBuoyMarker = (layer: any) => {
  const pos = getLayerPos(layer);
  const poleGeo = new THREE.CylinderGeometry(0.06, 0.06, 1.2, 12);
  const poleMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.4, metalness: 0.2 });
  const pole = new THREE.Mesh(poleGeo, poleMat);
  pole.position.set(pos.x, 0.6, pos.z);
  layerGroup?.add(pole);

  const headGeo = new THREE.SphereGeometry(0.12, 16, 16);
  const headMat = new THREE.MeshStandardMaterial({ color: 0xff3333, emissive: 0x330000, emissiveIntensity: 0.6 });
  const head = new THREE.Mesh(headGeo, headMat);
  head.position.set(pos.x, 1.3, pos.z);
  layerGroup?.add(head);
};

  const addWindVector = (layer: any) => {
    const pos = getLayerPos(layer);
    const bindings = layer?.dataBindings || {};
    const speedCode = bindings.speedTypeCode;
    const dirCode = bindings.directionTypeCode;
    if (!speedCode || !dirCode) return;

    const speed = pickLatest(dataByCode[speedCode])?.dataValue;
    const dirDeg = pickLatest(dataByCode[dirCode])?.dataValue;
    if (speed == null || dirDeg == null) return;

    const style = layer?.style || {};
    const minS = Number(style.minSpeed ?? 0);
    const maxS = Number(style.maxSpeed ?? 20);
    const arrowScaleMeters = Number(style.arrowScale ?? 50000);
    const colorHex = style.color || '#00ffff';

    const s = clamp(Number(speed), minS, maxS);
    const n = (s - minS) / (maxS - minS || 1);

    // 风向：按 “从北顺时针” 的常见定义；为了更直观，箭头指向风吹去的方向（取反）
    const rad = (Number(dirDeg) * Math.PI) / 180;
    const dir = new THREE.Vector3(Math.sin(rad), 0, Math.cos(rad)).normalize().multiplyScalar(-1);

    const length = Math.max(0.8, arrowScaleMeters * metersToWorld * (0.8 + n * 1.6) * 20);
    const arrow = new THREE.ArrowHelper(dir, new THREE.Vector3(pos.x, 1.6, pos.z), length, new THREE.Color(colorHex).getHex(), 0.4, 0.25);
    layerGroup?.add(arrow);
  };

  const addOceanColumn = (layer: any) => {
    const pos = getLayerPos(layer);
    const bindings = layer?.dataBindings || {};
    const tCode = bindings.temperatureTypeCode;
    const sCode = bindings.salinityTypeCode;

    if (!tCode && !sCode) return;

    const style = layer?.style || {};
    const heightScale = Number(style.heightScale ?? 10000);
    const depthToWorld = heightScale > 0 ? 10 / heightScale : 0.001;

    const tStops = style.colorGradientForTemperature as Array<{ value: number; color: string }> | undefined;

    const buildDepthMapLatest = (list?: Obs[]) => {
      const m = new Map<number, Obs>();
      (list || []).forEach((d) => {
        const depth = d.depth != null ? Number(d.depth) : 0;
        if (Number.isNaN(depth)) return;
        const prev = m.get(depth);
        if (!prev) m.set(depth, d);
        else if (parseTimeMs(d.observationTime) >= parseTimeMs(prev.observationTime)) m.set(depth, d);
      });
      return m;
    };

    const tMap = buildDepthMapLatest(dataByCode[tCode]);
    const sMap = buildDepthMapLatest(dataByCode[sCode]);
    const depths = Array.from(new Set([...tMap.keys(), ...sMap.keys()])).sort((a, b) => a - b);
    if (depths.length === 0) return;

    const colGroup = new THREE.Group();
    colGroup.position.set(pos.x, 0, pos.z);
    layerGroup?.add(colGroup);

    const radius = 0.18;
    const gap = 0.45;

    const mkSeg = (yMid: number, h: number, color: number, xOff: number) => {
      const geo = new THREE.CylinderGeometry(radius, radius, Math.max(0.06, h), 18);
      const mat = new THREE.MeshStandardMaterial({ color, roughness: 0.35, metalness: 0.05, transparent: true, opacity: 0.95 });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(xOff, yMid, 0);
      colGroup.add(mesh);
    };

    // 深度：向下为负 y
    for (let i = 0; i < depths.length; i++) {
      const d0 = depths[i];
      const d1 = depths[i + 1] ?? (d0 + 5); // 如果只有一层，给一个默认厚度
      const y0 = -d0 * depthToWorld;
      const y1 = -d1 * depthToWorld;
      const h = Math.abs(y1 - y0);
      const yMid = (y0 + y1) / 2;

      const t = tMap.get(d0)?.dataValue;
      const sVal = sMap.get(d0)?.dataValue;

      if (t != null) {
        mkSeg(yMid, h, colorFromStops(Number(t), tStops), -gap / 2);
      }
      if (sVal != null) {
        // 盐度：简化为青 -> 黄
        const n = clamp((Number(sVal) - Number(style.minSalinity ?? 0)) / (Number(style.maxSalinity ?? 40) - Number(style.minSalinity ?? 0) || 1), 0, 1);
        mkSeg(yMid, h, lerpColor(new THREE.Color('#00ffff'), new THREE.Color('#ffff00'), n).getHex(), gap / 2);
      }
    }

    // 顶部盖帽（更像“水柱”）
    const capGeo = new THREE.CylinderGeometry(0.28, 0.28, 0.06, 24);
    const capMat = new THREE.MeshStandardMaterial({ color: 0xffffff, transparent: true, opacity: 0.25 });
    const cap = new THREE.Mesh(capGeo, capMat);
    cap.position.set(0, 0.02, 0);
    colGroup.add(cap);
  };

  const addWaterQualityHalo = (layer: any) => {
    const pos = getLayerPos(layer);
    const bindings = layer?.dataBindings || {};
    const doCode = bindings.oxygenTypeCode;
    const chlCode = bindings.chlorophyllTypeCode;
    const turbCode = bindings.turbidityTypeCode;
    const phCode = bindings.phTypeCode;

    const style = layer?.style || {};
    const baseRadiusMeters = Number(style.baseRadius ?? 20000);
    let radius = baseRadiusMeters * metersToWorld * 10;
    radius = Math.max(0.8, radius);

    const latestDO = doCode ? pickLatest(dataByCode[doCode])?.dataValue : null;
    const latestCHL = chlCode ? pickLatest(dataByCode[chlCode])?.dataValue : null;
    const latestTurb = turbCode ? pickLatest(dataByCode[turbCode])?.dataValue : null;
    const latestPH = phCode ? pickLatest(dataByCode[phCode])?.dataValue : null;

    if (style.radiusByChlorophyll && latestCHL != null) {
      // 经验映射：0~10 mg/m³
      const n = clamp(Number(latestCHL) / 10, 0, 1);
      radius *= 0.7 + n * 1.2;
    }

    let color = new THREE.Color('#66ccff');
    if (style.colorByTurbidity && latestTurb != null) {
      // 经验映射：0~50 NTU
      const n = clamp(Number(latestTurb) / 50, 0, 1);
      color = lerpColor(new THREE.Color('#00ffff'), new THREE.Color('#ff9933'), n);
    } else if (style.phColorRange && latestPH != null) {
      const r = style.phColorRange;
      const minPH = Number(r.minPH ?? 7.5);
      const maxPH = Number(r.maxPH ?? 8.5);
      const n = clamp((Number(latestPH) - minPH) / (maxPH - minPH || 1), 0, 1);
      color = lerpColor(new THREE.Color(r.acidColor || '#3366ff'), new THREE.Color(r.alkalineColor || '#ff9933'), n);
    }

    let opacity = 0.55;
    if (style.opacityByOxygen && latestDO != null) {
      // 经验映射：0~12 mg/L
      const n = clamp(Number(latestDO) / 12, 0, 1);
      opacity = 0.25 + n * 0.65;
    }

    const ringGeo = new THREE.RingGeometry(radius * 0.7, radius, 64);
    const ringMat = new THREE.MeshBasicMaterial({
      color: color.getHex(),
      transparent: true,
      opacity,
      side: THREE.DoubleSide,
      depthWrite: false,
    });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = -Math.PI / 2;
    ring.position.set(pos.x, 0.03, pos.z);
    layerGroup?.add(ring);
  };

  // 逐层渲染
  layers.forEach((layer: any) => {
    const type = layer?.type;
    if (type === 'BUOY_MARKER') addBuoyMarker(layer);
    else if (type === 'VECTOR_ARROW') addWindVector(layer);
    else if (type === 'OCEAN_COLUMN') addOceanColumn(layer);
    else if (type === 'WATER_QUALITY_HALO') addWaterQualityHalo(layer);
  });
};
// ---------------- 业务逻辑保持不变 END ----------------

// ================== 核心视觉优化：高端海洋渲染 ==================
const initScene = () => {
  if (!containerRef.value) return;
  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;
  if (width === 0 || height === 0) { setTimeout(initScene, 100); return; }

  // 1. 深海氛围与无边际雾效
  const OCEAN_VOID = new THREE.Color('#010814'); // 极深背景
  scene = new THREE.Scene();
  scene.background = OCEAN_VOID;
  // 使用指数雾消除边缘感，远处自动隐于黑暗
  scene.fog = new THREE.FogExp2(OCEAN_VOID, 0.012);

  camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 2000);
  camera.position.set(0, 15, 30);

  renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: "high-performance" });
  renderer.setSize(width, height);
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.toneMapping = THREE.ACESFilmicToneMapping; // 电影级色调
  containerRef.value.appendChild(renderer.domElement);

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.maxPolarAngle = Math.PI / 2.1; // 限制仰角，防止穿帮

  // 2. 电影级灯光
  const ambientLight = new THREE.AmbientLight('#001133', 1.0);
  scene.add(ambientLight);

  const moonLight = new THREE.DirectionalLight('#aaccff', 2.0);
  moonLight.position.set(-100, 150, -50);
  scene.add(moonLight);

  const spotLight = new THREE.DirectionalLight('#00ffff', 0.5);
  spotLight.position.set(50, 20, 50);
  scene.add(spotLight);

  // 3. 巨幅海面几何体 (400x400)
  const oceanGeometry = new THREE.PlaneGeometry(400, 400, 220, 220);
  const colors = new Float32Array(oceanGeometry.attributes.position.count * 3);
  oceanGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

  const oceanMaterial = new THREE.MeshStandardMaterial({
    vertexColors: true,
    roughness: 0.1,
    metalness: 0.9,
    transparent: true,
    opacity: 0.98,
  });
  oceanMesh = new THREE.Mesh(oceanGeometry, oceanMaterial);
  oceanMesh.rotation.x = -Math.PI / 2;
  scene.add(oceanMesh);

  // 4. 颜色常量
  const SEA_DEEP = new THREE.Color('#01122a');
  const SEA_SHALLOW = new THREE.Color('#005c8a');
  const SEA_FOAM = new THREE.Color('#e0f8ff');

  // 5. 动画循环
  const animate = () => {
    if (!scene || !camera || !renderer) return;
    const time = Date.now() * 0.0005;
    const wp = currentWaveParams;
    const k = (2 * Math.PI) / Math.max(wp.wavelength, 1);
    const omega = (2 * Math.PI) / Math.max(wp.period, 0.5);
    const pos = oceanMesh!.geometry.attributes.position;
    const col = oceanMesh!.geometry.attributes.color;

    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i);
      const y = pos.getY(i);

      // 距离衰减：让远处的波浪变小，防止摩尔纹干扰
      const dist = Math.sqrt(x*x + y*y);
      const atten = Math.max(0.1, 1.0 - dist / 250);

      const p1 = k * 0.6 * (x * Math.cos(wp.directionRad) + y * Math.sin(wp.directionRad)) - omega * time;
      const p2 = k * 1.5 * (x + y) - omega * 1.5 * time;

      const z = (wp.amplitude * Math.cos(p1) + 0.3 * wp.amplitude * Math.cos(p2)) * atten;
      pos.setZ(i, z);

      // 顶点着色优化
      const norm = Math.max(-1, Math.min(1, z / (wp.amplitude || 1)));
      let c;
      if (norm > 0.7) c = SEA_SHALLOW.clone().lerp(SEA_FOAM, (norm - 0.7) / 0.3);
      else c = SEA_DEEP.clone().lerp(SEA_SHALLOW, (norm + 1) / 1.7);
      col.setXYZ(i, c.r, c.g, c.b);
    }
    pos.needsUpdate = true;
    col.needsUpdate = true;
    oceanMesh!.geometry.computeVertexNormals();

    controls!.update();
    renderer.render(scene, camera);
    animationId = requestAnimationFrame(animate);
  };
  animate();

  if (props.configJson) currentConfig = parseJsonPreservingLongIds(props.configJson);
  renderLayers(currentConfig || {}, props.data || []);
  window.addEventListener('resize', handleResize);
};

const handleResize = () => {
  if (!containerRef.value || !camera || !renderer) return;
  camera.aspect = containerRef.value.clientWidth / containerRef.value.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight);
};

const cleanup = () => { /* 粘贴原代码清理逻辑 */ };
onMounted(async () => { await nextTick(); setTimeout(initScene, 100); });
onUnmounted(cleanup);
</script>

<style scoped lang="less">
.scene-3d-viewer-root {
  width: 100%; height: 100%; min-height: 600px; position: relative;
  background: #010814;
}
.scene-3d-viewer { width: 100%; height: 100%; }

.source-badge {
  position: absolute; top: 20px; right: 20px;
  min-width: 260px; padding: 18px;
  border-radius: 12px;
  background: rgba(8, 18, 36, 0.45);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px);
  color: #fff; border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  pointer-events: none; z-index: 20;
}
.badge-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; display: flex; align-items: center; gap: 8px; }
.badge-mark {
  display: flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 6px;
  background: rgba(0, 229, 255, 0.15); border: 1px solid rgba(0, 229, 255, 0.4);
}
.badge-line { display: flex; gap: 8px; font-size: 13px; margin-bottom: 4px; }
.badge-label { color: rgba(255,255,255,0.6); }
.wave-info .badge-value { color: #00e5ff; font-family: 'JetBrains Mono', monospace; }
.cf-real .badge-value { color: #4ade80; }
</style>