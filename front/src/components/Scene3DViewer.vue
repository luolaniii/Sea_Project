<template>
  <div class="scene-3d-viewer-root">
    <div ref="containerRef" class="scene-3d-viewer"></div>
    <!-- 数据源标注（右上角）：让用户明确当前画面基于什么数据渲染 -->
    <div v-if="showSourceBadge && sourceBadge" class="source-badge">
      <div class="badge-title">
        <span class="badge-mark"><Icon name="wave" :size="14" color="#00d4ff" /></span>
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

// 检查WebGL支持
if (!window.WebGLRenderingContext) {
  console.error('浏览器不支持WebGL');
} else {
  const canvas = document.createElement('canvas');
  const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
  if (!gl) {
    console.error('无法创建WebGL上下文');
  }
}

interface Props {
  configJson?: string;
  data?: any[];
  /** 回放截止时刻（含）：仅使用该时刻及之前的观测；不传则使用全部数据 */
  replayEndTime?: string | null;
  /** 数据源名称（如 "NDBC 41002"），用于右上角标注 */
  sourceLabel?: string;
  /** 是否显示右上角数据源徽章 */
  showSourceBadge?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showSourceBadge: true,
});

// ============ 数据源标注相关 ============
const sourceBadge = ref<{
  title: string;
  sourceLabel: string;
  lastObs: string;
  waveInfo: string;
  /** real=实测 estimated=P-M估算 interpolated=插值 default=默认 */
  confidence: 'real' | 'estimated' | 'interpolated' | 'default';
} | null>(null);

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

// ============ Module F: 实测频谱数据驱动波面 ============
/**
 * 波面参数（由最新观测解算得到）
 * - amplitude: 波幅 A (m)，近似 WVHT/2（WVHT 为有效波高 Hs，A≈Hs/2）
 * - period:    波周期 T (s)，优先使用 DPD（主周期），回退 APD（平均周期）
 * - wavelength:波长 L (m)，深水假设下 L = g·T²/(2π)
 * - direction: 波向 (rad)，来自 MWD（从北顺时针）
 * - confidence: 数据置信度来源
 * 参考：Pierson-Moskowitz (1964), Airy 线性波理论, WMO-No.306
 */
interface WaveParams {
  amplitude: number;      // 世界单位（Three.js），由物理幅度映射而来
  physicalAmpM: number;   // 物理幅度 A (m)
  period: number;         // T (s)
  wavelength: number;     // L (m)
  directionRad: number;   // 波向 (弧度)
  confidence: 'real' | 'estimated' | 'interpolated' | 'default';
  rawWVHT?: number;
  rawDPD?: number;
  rawMWD?: number;
  rawWSPD?: number;
}

// 物理常量
const G = 9.81;

/**
 * Pierson-Moskowitz 谱公式：由风速估算有效波高和主周期
 * 文献：Pierson W J, Moskowitz L (1964), J. Geophys. Res., 69(24).
 * H_s = 0.21 * U²/g    (充分发展海浪)
 * T_p = 7.14 * U / g（由 f_p 推得，U 为 10m 风速）
 */
const estimateFromWindSpeed = (u10: number): { hs: number; tp: number } => {
  const u = Math.max(0, u10);
  const hs = 0.21 * (u * u) / G;
  const tp = 7.14 * u / G;
  return { hs, tp };
};

/**
 * 从观测数据中提取波面参数
 * 优先级：实测 WVHT/DPD/MWD → 风速 P-M 估算 → 默认值
 */
const extractWaveParams = (dataByCode: Record<string, Obs[]>): WaveParams => {
  // 同时支持全称 code 和缩写 code
  const wvhtObs = pickLatest(dataByCode['WAVE_HEIGHT']) || pickLatest(dataByCode['WAVE']);
  const dpdObs = pickLatest(dataByCode['WAVE_PERIOD']);
  const mwdObs = pickLatest(dataByCode['WAVE_DIRECTION']);
  const wspdObs = pickLatest(dataByCode['WIND_SPEED']);

  const wvht = wvhtObs?.dataValue != null ? Number(wvhtObs.dataValue) : null;
  const dpd = dpdObs?.dataValue != null ? Number(dpdObs.dataValue) : null;
  const mwd = mwdObs?.dataValue != null ? Number(mwdObs.dataValue) : null;
  const wspd = wspdObs?.dataValue != null ? Number(wspdObs.dataValue) : null;

  let hs: number;       // 有效波高 (m)
  let T: number;        // 周期 (s)
  let confidence: WaveParams['confidence'];

  if (wvht != null && Number.isFinite(wvht) && wvht > 0) {
    hs = wvht;
    T = (dpd != null && Number.isFinite(dpd) && dpd > 0) ? dpd : 6;
    confidence = 'real';
  } else if (wspd != null && Number.isFinite(wspd) && wspd > 0) {
    // P-M 公式估算
    const est = estimateFromWindSpeed(wspd);
    hs = est.hs;
    T = est.tp > 0 ? est.tp : 6;
    confidence = 'estimated';
  } else {
    // 气候默认值（全球平均海况）
    hs = 1.5;
    T = 7;
    confidence = 'default';
  }

  // 限幅：避免极端值引起视觉灾难
  hs = clamp(hs, 0, 20);
  T = clamp(T, 2, 25);

  const physicalAmpM = hs / 2;                 // A ≈ Hs/2
  const wavelength = (G * T * T) / (2 * Math.PI);   // 深水色散关系

  // 映射到 Three 世界单位：海面 PlaneGeometry 50x50。
  // 将物理幅度放大让小波高也清晰可见（1m → 1.2 世界单位）
  // 同时设置较高最低线 0.4，让平静海面也有微浪
  const ampWorld = clamp(physicalAmpM * 1.2, 0.4, 6.0);

  const dirRad = mwd != null && Number.isFinite(mwd) ? (Number(mwd) * Math.PI / 180) : 0;

  return {
    amplitude: ampWorld,
    physicalAmpM,
    period: T,
    wavelength,
    directionRad: dirRad,
    confidence,
    rawWVHT: wvht ?? undefined,
    rawDPD: dpd ?? undefined,
    rawMWD: mwd ?? undefined,
    rawWSPD: wspd ?? undefined,
  };
};

// 当前生效的波面参数（动画循环中读取）
let currentWaveParams: WaveParams = {
  amplitude: 0.2,
  physicalAmpM: 0.5,
  period: 6,
  wavelength: 100,  // g*36/(2π)
  directionRad: 0,
  confidence: 'default',
};

const formatWaveInfo = (wp: WaveParams): string => {
  const parts: string[] = [];
  if (wp.rawWVHT != null) parts.push(`Hs=${wp.rawWVHT.toFixed(2)}m`);
  else parts.push(`Hs≈${(wp.physicalAmpM * 2).toFixed(2)}m`);
  parts.push(`T=${wp.period.toFixed(1)}s`);
  if (wp.rawMWD != null) parts.push(`MWD=${wp.rawMWD.toFixed(0)}°`);
  return parts.join(' · ');
};

type Obs = {
  longitude?: number;
  latitude?: number;
  dataValue?: number;
  depth?: number;
  observationTime?: string;
  dataTypeCode?: string;
  dataTypeUnit?: string;
};

type LatLon = { lat: number; lon: number };

const parseLatLon = (arr: any): LatLon | null => {
  if (!Array.isArray(arr) || arr.length < 2) return null;
  const a = Number(arr[0]);
  const b = Number(arr[1]);
  if (Number.isNaN(a) || Number.isNaN(b)) return null;
  // 默认按 [lat, lon]；如果第一个超出纬度范围，则按 [lon, lat]
  if (Math.abs(a) > 90 && Math.abs(b) <= 90) return { lon: a, lat: b };
  return { lat: a, lon: b };
};

const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v));

const parseTimeMs = (t?: unknown) => {
  const ms = parseObservationTimeToMs(t ?? '');
  return Number.isNaN(ms) ? -1 : ms;
};

/** 回放：只保留 observationTime <= replayEndTime 的记录（无有效时间的行在回放模式下丢弃） */
const filterByReplayEnd = (data: Obs[], replayEndTime?: string | null): Obs[] => {
  if (replayEndTime == null || replayEndTime === '') return data;
  const endMs = parseTimeMs(replayEndTime);
  if (endMs < 0) return data;
  return data.filter((d) => {
    const t = parseTimeMs(d.observationTime);
    if (t < 0) return false;
    return t <= endMs;
  });
};

const groupByTypeCode = (data: Obs[]) => {
  const map: Record<string, Obs[]> = {};
  data.forEach((d) => {
    const code = (d as any).dataTypeCode;
    if (!code) return;
    (map[code] ||= []).push(d);
  });
  return map;
};

const pickLatest = (list?: Obs[]) => {
  if (!list || list.length === 0) return null;
  let best: Obs | null = null;
  let bestT = -1;
  for (const d of list) {
    const t = parseTimeMs(d.observationTime);
    if (t >= bestT) {
      bestT = t;
      best = d;
    }
  }
  return best;
};

const hexToInt = (hex: string) => new THREE.Color(hex).getHex();

const lerpColor = (c1: THREE.Color, c2: THREE.Color, t: number) => {
  const c = c1.clone();
  c.lerp(c2, t);
  return c;
};

const colorFromStops = (value: number, stops?: Array<{ value: number; color: string }>) => {
  if (!stops || stops.length === 0) {
    // 默认蓝 -> 红
    const n = clamp(value, 0, 1);
    return lerpColor(new THREE.Color('#0000ff'), new THREE.Color('#ff0000'), n).getHex();
  }
  const sorted = [...stops].sort((a, b) => a.value - b.value);
  if (value <= sorted[0].value) return hexToInt(sorted[0].color);
  if (value >= sorted[sorted.length - 1].value) return hexToInt(sorted[sorted.length - 1].color);
  for (let i = 0; i < sorted.length - 1; i++) {
    const a = sorted[i];
    const b = sorted[i + 1];
    if (value >= a.value && value <= b.value) {
      const t = (value - a.value) / (b.value - a.value || 1);
      return lerpColor(new THREE.Color(a.color), new THREE.Color(b.color), clamp(t, 0, 1)).getHex();
    }
  }
  return hexToInt(sorted[0].color);
};

const getTargetLatLon = (config: any, data?: Obs[]): LatLon => {
  const t = parseLatLon(config?.camera?.target);
  if (t) return t;
  const layers = config?.layers;
  if (Array.isArray(layers) && layers.length > 0) {
    const p = parseLatLon(layers[0]?.position);
    if (p) return p;
  }
  // fallback: 数据均值
  const list = (data || []).filter((d) => d.latitude != null && d.longitude != null) as Required<Pick<Obs, 'latitude' | 'longitude'>>[];
  if (list.length > 0) {
    const lat = list.reduce((s, d) => s + Number(d.latitude), 0) / list.length;
    const lon = list.reduce((s, d) => s + Number(d.longitude), 0) / list.length;
    return { lat, lon };
  }
  return { lat: 0, lon: 0 };
};

const createGeoProjector = (config: any, data?: Obs[]) => {
  const target = getTargetLatLon(config, data);
  const distanceMeters = Number(config?.camera?.distance || 3000000);
  // 将 “配置里的米” 映射到 Three 世界单位：默认把 3,000,000m 映射为 ~15
  const metersToWorld = distanceMeters > 0 ? 15 / distanceMeters : 0.000005;
  const degToMetersLat = 111_000;
  const cosLat = Math.cos((target.lat * Math.PI) / 180);

  const latLonToXZ = (lat: number, lon: number) => {
    const dLat = lat - target.lat;
    const dLon = lon - target.lon;
    const x = dLon * degToMetersLat * cosLat * metersToWorld;
    const z = dLat * degToMetersLat * metersToWorld;
    return { x, z };
  };

  return { target, metersToWorld, latLonToXZ };
};

const clearLayerGroup = () => {
  if (!scene || !layerGroup) return;
  scene.remove(layerGroup);
  layerGroup.traverse((obj) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const anyObj = obj as any;
    if (anyObj.geometry) anyObj.geometry.dispose?.();
    if (anyObj.material) {
      if (Array.isArray(anyObj.material)) anyObj.material.forEach((m: any) => m.dispose?.());
      else anyObj.material.dispose?.();
    }
  });
  layerGroup = null;
};

const renderLayers = (config: any, data: Obs[]) => {
  if (!scene) return;
  const obs = filterByReplayEnd(data, props.replayEndTime);
  clearLayerGroup();
  layerGroup = new THREE.Group();
  layerGroup.name = 'scene_layers';
  scene.add(layerGroup);

  const { metersToWorld, latLonToXZ, target } = createGeoProjector(config, obs);
  const dataByCode = groupByTypeCode(obs);

  // 更新波面参数（实测→P-M 估算→默认）
  currentWaveParams = extractWaveParams(dataByCode);

  // 更新右上角数据源徽章
  const latestAny = (() => {
    let best: Obs | null = null;
    let bestT = -1;
    for (const d of obs) {
      const t = parseTimeMs(d.observationTime);
      if (t >= bestT) { bestT = t; best = d; }
    }
    return best;
  })();
  // 优先级：显式 prop > 配置名 > 观测数据中的 dataSourceName > 站点ID
  const sourceFromData = (obs.find((d) => (d as any).dataSourceName) as any)?.dataSourceName;
  const stationFromData = (obs.find((d) => (d as any).stationId) as any)?.stationId;
  sourceBadge.value = {
    title: config?.sceneName || '海洋场景',
    sourceLabel: props.sourceLabel
      || sourceFromData
      || config?.dataQuery?.dataSourceName
      || config?.sourceLabel
      || (stationFromData ? `站点 ${stationFromData}` : '未指定数据源'),
    lastObs: latestAny?.observationTime ? String(latestAny.observationTime) : '',
    waveInfo: formatWaveInfo(currentWaveParams),
    confidence: currentWaveParams.confidence,
  };

  // ========== 相机：支持 globe target/distance 的最小实现 ==========
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

const initScene = () => {
  if (!containerRef.value) {
    console.warn('容器引用为空，无法初始化场景');
    return;
  }
  
  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;
  
  if (width === 0 || height === 0) {
    console.warn('容器尺寸为0，等待下一帧重试');
    setTimeout(initScene, 100);
    return;
  }

  // 莫兰迪暖米沙背景：低饱和度的奶咖+灰白色调，与青蓝色海面形成柔和的暖冷对比
  // 比纯白柔和、比深色明亮，让浪面起伏在浅色衬托下更立体
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0xd4c9b8);
  scene.fog = new THREE.Fog(0xd4c9b8, 18, 60);

  // 创建相机
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
  camera.position.set(0, 5, 10);
  camera.lookAt(0, 0, 0);

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(width, height);
  renderer.shadowMap.enabled = true;
  containerRef.value.appendChild(renderer.domElement);

  // 交互控制
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.08;
  controls.rotateSpeed = 0.5;
  controls.zoomSpeed = 0.8;
  controls.panSpeed = 0.6;

  // 添加光源
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
  directionalLight.position.set(10, 10, 5);
  directionalLight.castShadow = true;
  scene.add(directionalLight);

  // 添加海洋平面（顶点色 → 浪尖泡沫；高频细分 → 平滑波形）
  const oceanGeometry = new THREE.PlaneGeometry(50, 50, 200, 200);
  const oceanColors = new Float32Array(oceanGeometry.attributes.position.count * 3);
  oceanGeometry.setAttribute('color', new THREE.BufferAttribute(oceanColors, 3));
  const oceanMaterial = new THREE.MeshPhongMaterial({
    vertexColors: true,
    shininess: 90,
    specular: 0x99ddff,
    transparent: true,
    opacity: 0.95,
    side: THREE.DoubleSide,
  });
  oceanMesh = new THREE.Mesh(oceanGeometry, oceanMaterial);
  oceanMesh.rotation.x = -Math.PI / 2;
  oceanMesh.receiveShadow = true;
  scene.add(oceanMesh);

  // foam 调色
  const SEA_TROUGH = new THREE.Color(0x002b50);
  const SEA_CREST = new THREE.Color(0x0a8eb3);
  const FOAM_COL = new THREE.Color(0xeaf6ff);

  // 浪花粒子（仅在波峰高度 > 阈值时随机出现，给海面"白头浪"感）
  const foamCount = 600;
  const foamGeo = new THREE.BufferGeometry();
  const foamPos = new Float32Array(foamCount * 3);
  for (let i = 0; i < foamCount; i++) {
    foamPos[i * 3] = (Math.random() - 0.5) * 50;
    foamPos[i * 3 + 1] = -100; // 默认隐藏在海面下
    foamPos[i * 3 + 2] = (Math.random() - 0.5) * 50;
  }
  foamGeo.setAttribute('position', new THREE.BufferAttribute(foamPos, 3));
  const foamMat = new THREE.PointsMaterial({
    color: 0xffffff,
    size: 0.18,
    transparent: true,
    opacity: 0.85,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });
  const foamPoints = new THREE.Points(foamGeo, foamMat);
  scene.add(foamPoints);
  (oceanMesh as any).__foam = { points: foamPoints, geo: foamGeo, count: foamCount };

  // ============ 添加波浪动画（Airy 线性波理论 + 双向叠加） ============
  // η(x,y,t) = A·cos(kx·cosθ + ky·sinθ - ω·t) + 0.3A·cos(2k·... - 2ω·t + φ)
  // k = 2π/L, ω = 2π/T, θ = 波向（MWD）
  // 叠加二阶小振幅波提升真实感（可视化专用，非严格物理）
  //
  // 世界单位换算：PlaneGeometry(50x50) 代表海面；为使波长可视，引入空间压缩因子
  // 当 L≈56m (T=6s) 时，世界单位应让 k 产生肉眼可辨的波峰密度
  const SPACE_COMPRESS = 0.8; // 世界单位 → 米 的视觉压缩（一个世界单位≈若干米）
  // ⏱ 波浪动画速度：1.0=正常，0.5=半速，0.3=更慢更柔和。降这个值能让海面起伏看起来更舒缓
  const TIME_SCALE = 0.50;
  const animate = () => {
    if (!scene || !camera || !renderer) return;

    const time = Date.now() * 0.001 * TIME_SCALE;
    const wp = currentWaveParams;
    const k = (2 * Math.PI) / Math.max(wp.wavelength, 1); // 波数 (1/m)
    const omega = (2 * Math.PI) / Math.max(wp.period, 0.5); // 角频率 (1/s)
    const cosT = Math.cos(wp.directionRad);
    const sinT = Math.sin(wp.directionRad);
    const A = wp.amplitude;

    const positions = oceanMesh?.geometry.attributes.position;
    const colorsAttr = oceanMesh?.geometry.attributes.color as THREE.BufferAttribute | undefined;
    if (positions) {
      // 第二个交叉方向波
      const cross_cosT = Math.cos(wp.directionRad + Math.PI / 3);
      const cross_sinT = Math.sin(wp.directionRad + Math.PI / 3);
      const k2 = k * 1.7;
      const omega2 = omega * 1.3;
      const k3 = k * 4;
      const omega3 = omega * 2.2;

      // foam 阈值：超过该值的顶点 → 白色，并撒粒子
      const foamThreshold = A * 0.78;
      const foamData = (oceanMesh as any)?.__foam;
      const foamPosArr = foamData?.geo?.attributes?.position?.array as Float32Array | undefined;
      let foamWriteIdx = 0;
      const foamMax = foamData?.count ?? 0;

      for (let i = 0; i < positions.count; i++) {
        const x = positions.getX(i);
        const y = positions.getY(i);
        const phase1 = k * SPACE_COMPRESS * (x * cosT + y * sinT) - omega * time;
        const phase2 = k2 * SPACE_COMPRESS * (x * cross_cosT + y * cross_sinT) - omega2 * time;
        const phase3 = k3 * SPACE_COMPRESS * (x + y * 0.6) - omega3 * time;

        const z =
          A * Math.cos(phase1) +
          0.5 * A * Math.cos(2 * phase1 + 0.7) +
          0.4 * A * Math.cos(phase2) +
          0.12 * Math.cos(phase3);
        positions.setZ(i, z);

        if (colorsAttr) {
          const norm = Math.max(-1, Math.min(1, z / Math.max(A * 1.4, 0.1)));
          let r: number, g: number, b: number;
          if (norm > 0.78) {
            const foamMix = Math.min(1, (norm - 0.78) / 0.22 * 1.4);
            const c = SEA_CREST.clone().lerp(FOAM_COL, foamMix);
            r = c.r; g = c.g; b = c.b;
          } else if (norm > 0) {
            const mix = norm / 0.78;
            const c = SEA_TROUGH.clone().lerp(SEA_CREST, mix);
            r = c.r; g = c.g; b = c.b;
          } else {
            const mix = (norm + 1);
            const c = SEA_TROUGH.clone().multiplyScalar(0.45 + 0.55 * mix);
            r = c.r; g = c.g; b = c.b;
          }
          colorsAttr.setXYZ(i, r, g, b);
        }

        // 浪花粒子分布（仅每隔几个顶点采样一次，避免密度过高）
        if (foamPosArr && z > foamThreshold && foamWriteIdx < foamMax && (i % 3 === 0)) {
          // 把粒子放到该顶点上方一点点
          // 注意：oceanMesh 旋转 -PI/2，世界坐标里 y 来自原 z；但因为粒子直接添加到 scene，需手动换算
          foamPosArr[foamWriteIdx * 3] = x + (Math.random() - 0.5) * 0.4;
          foamPosArr[foamWriteIdx * 3 + 1] = z + 0.08;            // 稍高于浪尖
          foamPosArr[foamWriteIdx * 3 + 2] = -y + (Math.random() - 0.5) * 0.4;
          foamWriteIdx++;
        }
      }
      positions.needsUpdate = true;
      if (colorsAttr) colorsAttr.needsUpdate = true;
      // 把没用到的粒子塞到水面下面藏起来
      if (foamPosArr) {
        for (let j = foamWriteIdx; j < foamMax; j++) {
          foamPosArr[j * 3 + 1] = -100;
        }
        foamData.geo.attributes.position.needsUpdate = true;
      }
      oceanMesh!.geometry.computeVertexNormals();
    }

    controls?.update();
    // 渲染场景
    renderer.render(scene, camera);
    animationId = requestAnimationFrame(animate);
  };

  animate();

  // 解析配置
  if (props.configJson) {
    try {
      const config = parseJsonPreservingLongIds(props.configJson);
      currentConfig = config;
    } catch (e) {
      console.error('解析场景配置失败:', e);
    }
  }

  // 初次渲染 layers
  renderLayers(currentConfig || {}, (props.data || []) as Obs[]);

  // 窗口大小调整
  window.addEventListener('resize', handleResize);
};

const handleResize = () => {
  if (!containerRef.value || !camera || !renderer) return;

  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;

  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);
};

const cleanup = () => {
  if (animationId !== null) {
    cancelAnimationFrame(animationId);
    animationId = null;
  }

  window.removeEventListener('resize', handleResize);

  clearLayerGroup();
  controls?.dispose();
  controls = null;

  if (renderer && containerRef.value) {
    containerRef.value.removeChild(renderer.domElement);
    renderer.dispose();
  }

  if (scene) {
    scene.traverse((object) => {
      if (object instanceof THREE.Mesh) {
        object.geometry.dispose();
        if (Array.isArray(object.material)) {
          object.material.forEach((m) => m.dispose());
        } else {
          object.material.dispose();
        }
      }
    });
  }

  scene = null;
  camera = null;
  renderer = null;
  oceanMesh = null;
  currentConfig = null;
};

watch(
  () => props.configJson,
  () => {
    if (props.configJson && scene) {
      try {
        const config = parseJsonPreservingLongIds(props.configJson);
        currentConfig = config;
        renderLayers(currentConfig || {}, (props.data || []) as Obs[]);
      } catch (e) {
        console.error('解析场景配置失败:', e);
      }
    }
  }
);

watch(
  () => props.data,
  (newData) => {
    if (scene) {
      renderLayers(currentConfig || {}, (newData || []) as Obs[]);
    }
  },
  { deep: true, immediate: true }
);

watch(
  () => props.replayEndTime,
  () => {
    if (scene) {
      renderLayers(currentConfig || {}, (props.data || []) as Obs[]);
    }
  }
);

onMounted(async () => {
  // 等待DOM完全渲染
  await nextTick();
  
  // 再等待一帧确保容器有尺寸
  setTimeout(() => {
    initScene();
  }, 100);
});

onUnmounted(() => {
  cleanup();
});
</script>

<style scoped lang="less">
.scene-3d-viewer-root {
  width: 100%;
  height: 100%;
  min-height: 600px;
  position: relative;
}
.scene-3d-viewer {
  width: 100%;
  height: 100%;
  min-height: 600px;
  position: relative;
  overflow: hidden;
  background: #0a0e27;
}

/* Module E: 数据源徽章（右上角） */
.source-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  min-width: 220px;
  max-width: 320px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(10, 14, 39, 0.78);
  backdrop-filter: blur(6px);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid rgba(102, 126, 234, 0.35);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.3);
  pointer-events: none;
  z-index: 20;
}
.badge-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #cde3ff;
  letter-spacing: 0.3px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.badge-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: rgba(0, 212, 255, 0.12);
  border: 1px solid rgba(0, 212, 255, 0.35);
}
.badge-line {
  display: flex;
  gap: 4px;
  line-height: 1.5;
  word-break: break-all;
}
.badge-label {
  color: rgba(200, 220, 255, 0.75);
  flex-shrink: 0;
}
.badge-value {
  color: rgba(255, 255, 255, 0.95);
}
.wave-info .badge-value {
  color: #5cf6c4;
  font-family: 'Consolas', monospace;
}
.confidence {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed rgba(102, 126, 234, 0.3);
}
.cf-real .badge-value { color: #66ff88; }
.cf-estimated .badge-value { color: #ffcb5e; }
.cf-interpolated .badge-value { color: #ff9c5e; }
.cf-default .badge-value { color: #ff6b6b; }
</style>

