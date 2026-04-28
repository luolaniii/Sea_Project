<template>
  <div class="scene-3d-viewer-root">
    <div ref="containerRef" class="scene-3d-viewer"></div>
    <div v-if="initError" class="viewer-error">{{ initError }}</div>
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

// ================== 环境检查 ==================
if (!window.WebGLRenderingContext) {
  console.error('浏览器不支持WebGL');
} else {
  const canvas = document.createElement('canvas');
  const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
  if (!gl) console.error('无法创建WebGL上下文');
}

// ================== 类型与Props定义 ==================
interface Props {
  configJson?: string;
  data?: any[];
  replayEndTime?: string | null;
  sourceLabel?: string;
  showSourceBadge?: boolean;
}

const props = withDefaults(defineProps<Props>(), { showSourceBadge: true });

type Obs = {
  longitude?: number; latitude?: number; dataValue?: number; depth?: number;
  observationTime?: string; dataTypeCode?: string; dataTypeUnit?: string;
};
type LatLon = { lat: number; lon: number };

// ================== 核心状态 ==================
const sourceBadge = ref<any>(null);
const containerRef = ref<HTMLDivElement>();
const initError = ref('');
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let renderer: THREE.WebGLRenderer | null = null;
let controls: OrbitControls | null = null;
let animationId: number | null = null;
let oceanMesh: THREE.Mesh | null = null;
let layerGroup: THREE.Group | null = null;
let currentConfig: any = null;

const G = 9.81;

// ================== 业务工具函数 ==================
const confidenceLabel = (c?: string) => {
  switch (c) {
    case 'real': return '实测'; case 'estimated': return '公式估算';
    case 'interpolated': return '空间插值'; case 'default': return '气候默认值'; default: return '—';
  }
};

const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v));
const parseTimeMs = (t?: unknown) => { const ms = parseObservationTimeToMs(t ?? ''); return Number.isNaN(ms) ? -1 : ms; };
const parseLatLon = (arr: any): LatLon | null => {
  if (!Array.isArray(arr) || arr.length < 2) return null;
  const a = Number(arr[0]), b = Number(arr[1]);
  if (Number.isNaN(a) || Number.isNaN(b)) return null;
  if (Math.abs(a) > 90 && Math.abs(b) <= 90) return { lon: a, lat: b };
  return { lat: a, lon: b };
};

const filterByReplayEnd = (data: Obs[], replayEndTime?: string | null): Obs[] => {
  if (!replayEndTime) return data;
  const endMs = parseTimeMs(replayEndTime);
  if (endMs < 0) return data;
  return data.filter(d => parseTimeMs(d.observationTime) >= 0 && parseTimeMs(d.observationTime) <= endMs);
};

const groupByTypeCode = (data: Obs[]) => {
  const map: Record<string, Obs[]> = {};
  data.forEach((d) => { const code = (d as any).dataTypeCode; if (code) (map[code] ||= []).push(d); });
  return map;
};

const pickLatest = (list?: Obs[]) => {
  if (!list || list.length === 0) return null;
  let best: Obs | null = null, bestT = -1;
  for (const d of list) {
    const t = parseTimeMs(d.observationTime);
    if (t >= bestT) { bestT = t; best = d; }
  }
  return best;
};

const hexToInt = (hex: string) => new THREE.Color(hex).getHex();
const lerpColor = (c1: THREE.Color, c2: THREE.Color, t: number) => { const c = c1.clone(); c.lerp(c2, t); return c; };
const colorFromStops = (value: number, stops?: Array<{ value: number; color: string }>) => {
  if (!stops || stops.length === 0) return lerpColor(new THREE.Color('#0000ff'), new THREE.Color('#ff0000'), clamp(value, 0, 1)).getHex();
  const sorted = [...stops].sort((a, b) => a.value - b.value);
  if (value <= sorted[0].value) return hexToInt(sorted[0].color);
  if (value >= sorted[sorted.length - 1].value) return hexToInt(sorted[sorted.length - 1].color);
  for (let i = 0; i < sorted.length - 1; i++) {
    const a = sorted[i], b = sorted[i + 1];
    if (value >= a.value && value <= b.value) {
      const t = (value - a.value) / (b.value - a.value || 1);
      return lerpColor(new THREE.Color(a.color), new THREE.Color(b.color), clamp(t, 0, 1)).getHex();
    }
  }
  return hexToInt(sorted[0].color);
};

// ================== 海洋波面数据解析 ==================
const extractWaveParams = (dataByCode: Record<string, Obs[]>) => {
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
    amplitude: clamp((hs / 2) * 1.2, 0.4, 6.0), physicalAmpM: hs / 2, period: T,
    wavelength: (G * T * T) / (2 * Math.PI), directionRad: (Number(mwdObs?.dataValue) || 0) * Math.PI / 180,
    confidence, rawWVHT: wvht, rawDPD: dpdObs?.dataValue, rawMWD: mwdObs?.dataValue
  };
};

let currentWaveParams: any = { amplitude: 1, period: 6, wavelength: 100, directionRad: 0 };
const formatWaveInfo = (wp: any) => {
  const hs = wp.rawWVHT != null ? wp.rawWVHT : (wp.physicalAmpM * 2);
  return `Hs=${hs.toFixed(2)}m · T=${wp.period.toFixed(1)}s · MWD=${(wp.rawMWD || 0).toFixed(0)}°`;
};

// ================== 图层渲染逻辑 (完全保留原有数据挂载逻辑) ==================
const getTargetLatLon = (config: any, data?: Obs[]): LatLon => {
  const t = parseLatLon(config?.camera?.target);
  if (t) return t;
  if (config?.layers?.[0]?.position) {
    const p = parseLatLon(config.layers[0].position);
    if (p) return p;
  }
  const list = (data || []).filter(d => d.latitude != null && d.longitude != null) as any[];
  if (list.length > 0) {
    return {
      lat: list.reduce((s, d) => s + Number(d.latitude), 0) / list.length,
      lon: list.reduce((s, d) => s + Number(d.longitude), 0) / list.length
    };
  }
  return { lat: 0, lon: 0 };
};

const createGeoProjector = (config: any, data?: Obs[]) => {
  const target = getTargetLatLon(config, data);
  const distMeters = Number(config?.camera?.distance || 3000000);
  const metersToWorld = distMeters > 0 ? 15 / distMeters : 0.000005;
  const cosLat = Math.cos(target.lat * Math.PI / 180);
  return {
    target, metersToWorld,
    latLonToXZ: (lat: number, lon: number) => ({
      x: (lon - target.lon) * 111000 * cosLat * metersToWorld,
      z: (lat - target.lat) * 111000 * metersToWorld
    })
  };
};

const clearLayerGroup = () => {
  if (!scene || !layerGroup) return;
  scene.remove(layerGroup);
  layerGroup.traverse((obj: any) => {
    if (obj.geometry) obj.geometry.dispose?.();
    if (obj.material) { Array.isArray(obj.material) ? obj.material.forEach((m: any) => m.dispose?.()) : obj.material.dispose?.(); }
  });
  layerGroup = null;
};

const renderLayers = (config: any, data: any[]) => {
  if (!scene) return;
  const obs = filterByReplayEnd(data, props.replayEndTime);
  clearLayerGroup();
  layerGroup = new THREE.Group(); layerGroup.name = 'scene_layers';
  scene.add(layerGroup);

  const { metersToWorld, latLonToXZ, target } = createGeoProjector(config, obs);
  const dataByCode = groupByTypeCode(obs);

  currentWaveParams = extractWaveParams(dataByCode);

  const latestAny = pickLatest(obs);
  const sourceFromData = (obs.find(d => (d as any).dataSourceName) as any)?.dataSourceName;
  const stationFromData = (obs.find(d => (d as any).stationId) as any)?.stationId;

  sourceBadge.value = {
    title: config?.sceneName || '海洋场景',
    sourceLabel: props.sourceLabel || sourceFromData || config?.dataQuery?.dataSourceName || config?.sourceLabel || (stationFromData ? `站点 ${stationFromData}` : '未指定数据源'),
    lastObs: latestAny?.observationTime ? String(latestAny.observationTime) : '',
    waveInfo: formatWaveInfo(currentWaveParams),
    confidence: currentWaveParams.confidence,
  };

  if (camera && controls) {
    const center = latLonToXZ(target.lat, target.lon);
    controls.target.set(center.x, 0, center.z);
    const dist = Number(config?.camera?.distance || 3000000) * metersToWorld;
    camera.position.set(center.x, Math.max(3, dist * 0.35), center.z + Math.max(6, dist * 0.6));
    camera.lookAt(controls.target);
    controls.update();
  }

  const layers = Array.isArray(config?.layers) ? config.layers : [];
  const getLayerPos = (layer: any) => {
    const p = parseLatLon(layer?.position) || target;
    const xz = latLonToXZ(p.lat, p.lon);
    return { lat: p.lat, lon: p.lon, x: xz.x, z: xz.z };
  };

  const addBuoyMarker = (layer: any) => {
    const pos = getLayerPos(layer);
    const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 1.2, 12), new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.4 }));
    pole.position.set(pos.x, 0.6, pos.z); layerGroup?.add(pole);
    const head = new THREE.Mesh(new THREE.SphereGeometry(0.12, 16, 16), new THREE.MeshStandardMaterial({ color: 0xff3333, emissive: 0x330000, emissiveIntensity: 0.6 }));
    head.position.set(pos.x, 1.3, pos.z); layerGroup?.add(head);
  };

  const addWindVector = (layer: any) => {
    const bindings = layer?.dataBindings || {};
    const speed = pickLatest(dataByCode[bindings.speedTypeCode])?.dataValue;
    const dirDeg = pickLatest(dataByCode[bindings.directionTypeCode])?.dataValue;
    if (speed == null || dirDeg == null) return;
    const pos = getLayerPos(layer), style = layer?.style || {};
    const n = (clamp(Number(speed), Number(style.minSpeed ?? 0), Number(style.maxSpeed ?? 20)) - Number(style.minSpeed ?? 0)) / (Number(style.maxSpeed ?? 20) - Number(style.minSpeed ?? 0) || 1);
    const rad = (Number(dirDeg) * Math.PI) / 180;
    const dir = new THREE.Vector3(Math.sin(rad), 0, Math.cos(rad)).normalize().multiplyScalar(-1);
    const length = Math.max(0.8, Number(style.arrowScale ?? 50000) * metersToWorld * (0.8 + n * 1.6) * 20);
    layerGroup?.add(new THREE.ArrowHelper(dir, new THREE.Vector3(pos.x, 1.6, pos.z), length, new THREE.Color(style.color || '#00ffff').getHex(), 0.4, 0.25));
  };

  const addOceanColumn = (layer: any) => {
    const bindings = layer?.dataBindings || {};
    const tMap = new Map<number, Obs>(), sMap = new Map<number, Obs>();
    (dataByCode[bindings.temperatureTypeCode] || []).forEach(d => { if (d.depth != null) tMap.set(Number(d.depth), d); });
    (dataByCode[bindings.salinityTypeCode] || []).forEach(d => { if (d.depth != null) sMap.set(Number(d.depth), d); });
    const depths = Array.from(new Set([...tMap.keys(), ...sMap.keys()])).sort((a, b) => a - b);
    if (depths.length === 0) return;

    const pos = getLayerPos(layer), style = layer?.style || {};
    const depthToWorld = Number(style.heightScale ?? 10000) > 0 ? 10 / Number(style.heightScale ?? 10000) : 0.001;
    const colGroup = new THREE.Group(); colGroup.position.set(pos.x, 0, pos.z); layerGroup?.add(colGroup);

    for (let i = 0; i < depths.length; i++) {
      const d0 = depths[i], d1 = depths[i + 1] ?? (d0 + 5);
      const y0 = -d0 * depthToWorld, y1 = -d1 * depthToWorld, h = Math.abs(y1 - y0), yMid = (y0 + y1) / 2;
      const t = tMap.get(d0)?.dataValue, sVal = sMap.get(d0)?.dataValue;

      const mkSeg = (colorHex: number, xOff: number) => {
        const mesh = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.18, Math.max(0.06, h), 18), new THREE.MeshStandardMaterial({ color: colorHex, roughness: 0.35, transparent: true, opacity: 0.95 }));
        mesh.position.set(xOff, yMid, 0); colGroup.add(mesh);
      };
      if (t != null) mkSeg(colorFromStops(Number(t), style.colorGradientForTemperature as any), -0.225);
      if (sVal != null) mkSeg(lerpColor(new THREE.Color('#00ffff'), new THREE.Color('#ffff00'), clamp((Number(sVal) - Number(style.minSalinity ?? 0)) / (Number(style.maxSalinity ?? 40) - Number(style.minSalinity ?? 0) || 1), 0, 1)).getHex(), 0.225);
    }
    const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.28, 0.06, 24), new THREE.MeshStandardMaterial({ color: 0xffffff, transparent: true, opacity: 0.25 }));
    cap.position.set(0, 0.02, 0); colGroup.add(cap);
  };

  const addWaterQualityHalo = (layer: any) => {
    const pos = getLayerPos(layer), bindings = layer?.dataBindings || {}, style = layer?.style || {};
    let radius = Math.max(0.8, Number(style.baseRadius ?? 20000) * metersToWorld * 10);
    const chl = pickLatest(dataByCode[bindings.chlorophyllTypeCode])?.dataValue;
    if (style.radiusByChlorophyll && chl != null) radius *= 0.7 + clamp(Number(chl) / 10, 0, 1) * 1.2;

    let color = new THREE.Color('#66ccff');
    const turb = pickLatest(dataByCode[bindings.turbidityTypeCode])?.dataValue;
    const ph = pickLatest(dataByCode[bindings.phTypeCode])?.dataValue;
    if (style.colorByTurbidity && turb != null) color = lerpColor(new THREE.Color('#00ffff'), new THREE.Color('#ff9933'), clamp(Number(turb) / 50, 0, 1));
    else if (style.phColorRange && ph != null) color = lerpColor(new THREE.Color(style.phColorRange.acidColor || '#3366ff'), new THREE.Color(style.phColorRange.alkalineColor || '#ff9933'), clamp((Number(ph) - Number(style.phColorRange.minPH ?? 7.5)) / (Number(style.phColorRange.maxPH ?? 8.5) - Number(style.phColorRange.minPH ?? 7.5) || 1), 0, 1));

    let opacity = 0.55;
    const oxy = pickLatest(dataByCode[bindings.oxygenTypeCode])?.dataValue;
    if (style.opacityByOxygen && oxy != null) opacity = 0.25 + clamp(Number(oxy) / 12, 0, 1) * 0.65;

    const ring = new THREE.Mesh(new THREE.RingGeometry(radius * 0.7, radius, 64), new THREE.MeshBasicMaterial({ color: color.getHex(), transparent: true, opacity, side: THREE.DoubleSide, depthWrite: false }));
    ring.rotation.x = -Math.PI / 2; ring.position.set(pos.x, 0.03, pos.z); layerGroup?.add(ring);
  };

  layers.forEach((layer: any) => {
    if (layer?.type === 'BUOY_MARKER') addBuoyMarker(layer);
    else if (layer?.type === 'VECTOR_ARROW') addWindVector(layer);
    else if (layer?.type === 'OCEAN_COLUMN') addOceanColumn(layer);
    else if (layer?.type === 'WATER_QUALITY_HALO') addWaterQualityHalo(layer);
  });
};

// ================== 核心视觉优化：高端科技感无垠深海 ==================
const initScene = () => {
  if (!containerRef.value) return;
  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;
  if (width === 0 || height === 0) { setTimeout(initScene, 100); return; }
  initError.value = '';

  // 1. 明亮日光氛围 + 雾效（呼应整体海洋科技清亮主题）
  const SKY_HORIZON = 0xdcefff; // 远处天与海交融的浅天蓝
  scene = new THREE.Scene();
  scene.background = new THREE.Color(SKY_HORIZON);
  scene.fog = new THREE.FogExp2(SKY_HORIZON, 0.011);

  camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 2000);
  camera.position.set(0, 15, 30);

  try {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.05;
    containerRef.value.appendChild(renderer.domElement);
  } catch (error) {
    console.error('创建 3D 渲染器失败:', error);
    initError.value = '当前浏览器/WebGL 环境不可用，无法渲染 3D 场景';
    return;
  }

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.maxPolarAngle = Math.PI / 2 - 0.02;

  // 2. 自然日光布光：暖色环境光 + 太阳直射 + 海面反射青光
  scene.add(new THREE.AmbientLight(0xffffff, 0.85));

  const sunLight = new THREE.DirectionalLight(0xfff4e6, 1.4);
  sunLight.position.set(60, 110, 30);
  scene.add(sunLight);

  const skyLight = new THREE.HemisphereLight(0xa9d8f5, 0x9bc0d8, 0.55);
  scene.add(skyLight);

  const rimLight = new THREE.DirectionalLight(0x7dd3fc, 0.5);
  rimLight.position.set(-40, 30, -60);
  scene.add(rimLight);

  // 3. 广阔的海面网格
  const oceanGeometry = new THREE.PlaneGeometry(400, 400, 200, 200);
  const colors = new Float32Array(oceanGeometry.attributes.position.count * 3);
  oceanGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

  const oceanMaterial = new THREE.MeshPhongMaterial({
    vertexColors: true,
    shininess: 110,
    specular: 0xffffff,
    transparent: true,
    opacity: 0.96,
    side: THREE.DoubleSide,
  });
  oceanMesh = new THREE.Mesh(oceanGeometry, oceanMaterial);
  oceanMesh.rotation.x = -Math.PI / 2;
  scene.add(oceanMesh);

  // 4. 浅色调海水色盘（清亮蓝白）
  const SEA_DEEP = new THREE.Color('#0284c7');     // 主色调海洋蓝
  const SEA_SHALLOW = new THREE.Color('#7dd3fc');  // 浅水青蓝
  const SEA_FOAM = new THREE.Color('#ffffff');     // 浪花白

  // 伪分形噪声：不依赖外部库，用多频正弦叠加打散重复波纹
  const fbmNoise = (x: number, y: number, t: number) => {
    const n1 = Math.sin(x * 0.17 + y * 0.11 + t * 1.35);
    const n2 = Math.sin(x * 0.39 - y * 0.27 - t * 0.92);
    const n3 = Math.cos(x * 0.63 + y * 0.51 + t * 0.58);
    return n1 * 0.5 + n2 * 0.32 + n3 * 0.18;
  };

  // 5. 波浪动画计算循环
  const animate = () => {
    if (!scene || !camera || !renderer) return;
    const time = Date.now() * 0.00045; // 稍慢的速率，显得更恢弘
    const wp = currentWaveParams;
    const k = (2 * Math.PI) / Math.max(wp.wavelength, 1);
    const omega = (2 * Math.PI) / Math.max(wp.period, 0.5);
    const pos = oceanMesh!.geometry.attributes.position;
    const col = oceanMesh!.geometry.attributes.color;

    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i);
      const y = pos.getY(i);

      // 距离衰减：让远处的波浪趋于平静，解决由于分辨率不足导致的摩尔纹闪烁现象
      const dist = Math.sqrt(x*x + y*y);
      const atten = Math.max(0.05, 1.0 - dist / 200);

      // 多层波场：主方向波 + 横向干扰 + 斜向余弦 + 分形噪声
      const p1 = k * 0.6 * (x * Math.cos(wp.directionRad) + y * Math.sin(wp.directionRad)) - omega * time;
      const p2 = k * 1.5 * (x + y) - omega * 1.5 * time;
      const p3 =
        k * 2.15 * (x * Math.cos(wp.directionRad + Math.PI / 3) + y * Math.sin(wp.directionRad + Math.PI / 3)) -
        omega * 0.82 * time;
      const noise = fbmNoise(x, y, time);

      const z = (
        wp.amplitude * Math.cos(p1) +
        0.3 * wp.amplitude * Math.cos(p2) +
        0.18 * wp.amplitude * Math.cos(p3) +
        0.14 * wp.amplitude * noise
      ) * atten;
      pos.setZ(i, z);

      // 基于高度的顶点着色，产生波峰变白、波谷深蓝的效果
      const norm = Math.max(-1, Math.min(1, z / (wp.amplitude || 1)));
      let c;
      if (norm > 0.7) c = SEA_SHALLOW.clone().lerp(SEA_FOAM, (norm - 0.7) / 0.3);
      else c = SEA_DEEP.clone().lerp(SEA_SHALLOW, (norm + 1) / 1.7);
      col.setXYZ(i, c.r, c.g, c.b);
    }

    pos.needsUpdate = true;
    col.needsUpdate = true;
    oceanMesh!.geometry.computeVertexNormals(); // 重新计算法线让光照跟随波浪起伏

    controls!.update();
    try {
      renderer.render(scene, camera);
    } catch (error) {
      console.error('3D 渲染失败:', error);
      initError.value = '3D 渲染中断，请刷新后重试';
      return;
    }
    animationId = requestAnimationFrame(animate);
  };
  animate();

  if (props.configJson) {
    try { currentConfig = parseJsonPreservingLongIds(props.configJson); } catch (e) {}
  }
  renderLayers(currentConfig || {}, props.data || []);
  window.addEventListener('resize', handleResize);
};

// ================== 生命周期与清理 ==================
const handleResize = () => {
  if (!containerRef.value || !camera || !renderer) return;
  camera.aspect = containerRef.value.clientWidth / containerRef.value.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight);
};

const cleanup = () => {
  if (animationId !== null) { cancelAnimationFrame(animationId); animationId = null; }
  window.removeEventListener('resize', handleResize);
  clearLayerGroup();
  controls?.dispose(); controls = null;
  if (renderer && containerRef.value) { containerRef.value.removeChild(renderer.domElement); renderer.dispose(); }
  if (scene) {
    scene.traverse((obj) => {
      if (obj instanceof THREE.Mesh) {
        obj.geometry.dispose();
        Array.isArray(obj.material) ? obj.material.forEach(m => m.dispose()) : obj.material.dispose();
      }
    });
  }
  scene = null; camera = null; renderer = null; oceanMesh = null; currentConfig = null;
};

// ================== 侦听器 ==================
watch(() => props.configJson, () => {
  if (props.configJson && scene) {
    try {
      currentConfig = parseJsonPreservingLongIds(props.configJson);
      renderLayers(currentConfig || {}, props.data || []);
    } catch (e) { console.error('解析场景配置失败:', e); }
  }
});
watch(() => props.data, (newData) => { if (scene) renderLayers(currentConfig || {}, newData || []); }, { deep: true, immediate: true });
watch(() => props.replayEndTime, () => { if (scene) renderLayers(currentConfig || {}, props.data || []); });

onMounted(async () => { await nextTick(); setTimeout(initScene, 100); });
onUnmounted(cleanup);
</script>

<style scoped lang="less">
.scene-3d-viewer-root {
  width: 100%; height: 100%; min-height: 600px; position: relative;
  background: #dcefff; /* 与3D雾效一致的浅天蓝，避免初始化时闪屏 */
}
.scene-3d-viewer {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  overflow: hidden;
}

.viewer-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid rgba(220, 38, 38, 0.25);
  background: rgba(254, 242, 242, 0.9);
  color: #b91c1c;
  font-size: 12px;
  z-index: 30;
}

/* 玻璃拟物风格的信息悬浮面板 — 浅色海洋科技主题 */
.source-badge {
  position: absolute; top: 20px; right: 20px;
  min-width: 260px; padding: 18px 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  color: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 32px rgba(2, 132, 199, 0.18);
  pointer-events: none; z-index: 20;
}
.badge-title {
  font-size: 14px; font-weight: 700; margin-bottom: 12px;
  display: flex; align-items: center; gap: 8px; color: #0f172a;
  letter-spacing: 0.2px;
}
.badge-mark {
  display: flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 7px;
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  border: 1px solid rgba(2, 132, 199, 0.4);
  box-shadow: 0 4px 10px rgba(2, 132, 199, 0.25);
}
.badge-line {
  display: flex; gap: 8px; font-size: 12.5px; margin-bottom: 6px;
  letter-spacing: 0.2px; color: #334155;
}
.badge-label { color: #64748b; font-weight: 500; }
.badge-value { font-weight: 500; }
.wave-info .badge-value {
  color: #0284c7;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-weight: 600;
}
.confidence {
  margin-top: 10px; padding-top: 10px;
  border-top: 1px dashed rgba(15, 23, 42, 0.12);
}
.cf-real .badge-value { color: #16a34a; }
.cf-estimated .badge-value { color: #d97706; }
.cf-interpolated .badge-value { color: #ea580c; }
.cf-default .badge-value { color: #dc2626; }
</style>
